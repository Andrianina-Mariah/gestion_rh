package tovo.gestion.rh.temps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tovo.gestion.rh.temps.dto.PointageRequest.SensPointage;

import java.io.*;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service @RequiredArgsConstructor
public class PointageService {
    private static final ZoneId ZONE = ZoneId.of("Indian/Antananarivo");
    private static final int FENETRE_DOUBLON_SECONDES = 10;
    private final JdbcTemplate jdbc;
    private final TempsPlanningService access;
    private final TempsCalculService calcul;
    private final NotificationAnomalieService notifications;

    @Transactional
    public Map<String,Object> pointer(String email,SensPointage sens) {
        Long employeId=employePourEmail(email);
        OffsetDateTime maintenant=OffsetDateTime.now(ZoneOffset.UTC);
        return enregistrer(employeId,sens,maintenant,"WEB",null,null,null,null);
    }

    @Transactional
    public Map<String,Object> corriger(String email,boolean rh,boolean manager,String role,
            long employeId,Long origineId,OffsetDateTime dateHeure,SensPointage sens,String motif) {
        Integer utilisateurId=jdbc.queryForObject("SELECT id FROM utilisateur WHERE lower(email)=lower(?)",Integer.class,email);
        if(!rh) access.verifierAccesEmploye(employeId,email,manager);
        jdbc.queryForObject("SELECT id FROM employe WHERE id=? FOR UPDATE",Long.class,employeId);
        OffsetDateTime original=null;
        if(origineId!=null){var origine=jdbc.queryForMap("SELECT id,employe_id,date_heure,sens FROM pointage WHERE id=?",origineId);if(((Number)origine.get("employe_id")).longValue()!=employeId)throw new IllegalArgumentException("Le pointage d'origine n'appartient pas à cet employé");original=asOffsetDateTime(origine.get("date_heure"));}
        Long id=jdbc.queryForObject("INSERT INTO pointage(employe_id,date_heure,sens,source,cree_par_id,valide_par_id,date_validation,motif_correction,pointage_origine_id) VALUES(?,?,?,'MANUEL',?,?,CURRENT_TIMESTAMP,?,?) RETURNING id",Long.class,employeId,dateHeure.withOffsetSameInstant(ZoneOffset.UTC),sens.name(),utilisateurId,utilisateurId,motif,origineId);
        audit(utilisateurId,role,origineId==null?"CREATION":"MODIFICATION",id,origineId==null?null:"{\"pointageOrigineId\":"+origineId+",\"dateHeure\":\""+original+"\"}","{\"dateHeure\":\""+dateHeure+"\",\"sens\":\""+sens+"\",\"motif\":\""+json(motif)+"\"}");
        if(original!=null)calcul.recalculer(employeId,original.atZoneSameInstant(ZONE).toLocalDate(),false);
        LocalDate correctedDate=dateHeure.atZoneSameInstant(ZONE).toLocalDate();
        if(original==null||!correctedDate.equals(original.atZoneSameInstant(ZONE).toLocalDate()))calcul.recalculer(employeId,correctedDate,false);
        return pointage(id);
    }

    @Transactional
    public Map<String,Object> importer(MultipartFile fichier,String email,String role) throws IOException {
        Integer utilisateurId=jdbc.queryForObject("SELECT id FROM utilisateur WHERE lower(email)=lower(?)",Integer.class,email);
        if(fichier==null||fichier.isEmpty())throw new IllegalArgumentException("Le fichier CSV est vide");
        int lus=0,acceptes=0; List<Map<String,Object>> rejetes=new ArrayList<>();Set<String> recalculs=new LinkedHashSet<>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(fichier.getInputStream(), StandardCharsets.UTF_8))){
            String header=reader.readLine(); if(header==null)throw new IllegalArgumentException("CSV vide");
            String[] cols=header.replace("\uFEFF","").trim().toLowerCase(Locale.ROOT).split("[,;]",-1);
            int matricule=col(cols,"matricule"),date=col(cols,"date_heure","dateheure","timestamp"),sens=col(cols,"sens");
            if(matricule<0||date<0||sens<0)throw new IllegalArgumentException("Colonnes requises : matricule,date_heure,sens");
            String line; int numero=1;
            while((line=reader.readLine())!=null){numero++; if(line.isBlank())continue; lus++;
                try {
                    String[] values=parseCsv(line); String mat=value(values,matricule); Long emp=jdbc.queryForObject("SELECT id FROM employe WHERE matricule=?",Long.class,mat);
                    OffsetDateTime instant=parseDate(value(values,date)); SensPointage direction=SensPointage.valueOf(value(values,sens).toUpperCase(Locale.ROOT));
                    Map<String,Object> result=enregistrer(emp,direction,instant,"IMPORT",utilisateurId,null,null,null);
                    if(Boolean.TRUE.equals(result.get("accepte"))){acceptes++;recalculs.add(emp+"|"+instant.atZoneSameInstant(ZONE).toLocalDate());} else rejetes.add(Map.of("ligne",numero,"motif",result.get("motif")));
                } catch(Exception ex){rejetes.add(Map.of("ligne",numero,"motif",Optional.ofNullable(ex.getMessage()).orElse("Ligne invalide")));}
            }
        }
        for(String key:recalculs){String[] part=key.split("\\|");calcul.recalculer(Long.parseLong(part[0]),LocalDate.parse(part[1]),false);}
        Map<String,Object> report=new LinkedHashMap<>();report.put("lignesLues",lus);report.put("lignesAcceptees",acceptes);report.put("lignesRejetees",rejetes.size());report.put("rejets",rejetes);return report;
    }

    @Transactional
    public Map<String,Object> enregistrer(Long employeId,SensPointage sens,OffsetDateTime instant,String source,
            Integer creeParId,Long valideParId,OffsetDateTime validation,String motif) {
        jdbc.queryForObject("SELECT id FROM employe WHERE id=? FOR UPDATE",Long.class,employeId);
        OffsetDateTime utc=instant.withOffsetSameInstant(ZoneOffset.UTC);
        List<Map<String,Object>> recent=jdbc.queryForList("SELECT id FROM pointage p WHERE employe_id=? AND abs(EXTRACT(EPOCH FROM (date_heure-?)))<=? AND NOT EXISTS(SELECT 1 FROM pointage c WHERE c.pointage_origine_id=p.id) LIMIT 1",employeId,utc,FENETRE_DOUBLON_SECONDES);
        if(!recent.isEmpty()) {anomalie(employeId,utc,"DOUBLON","Pointage répété dans la fenêtre anti double clic");return resultat(false,null,"Pointage ignoré : doublon dans les 10 secondes");}
        List<Map<String,Object>> dernier=jdbc.queryForList("SELECT p.id,p.sens,p.date_heure FROM pointage p WHERE employe_id=? AND NOT EXISTS(SELECT 1 FROM pointage c WHERE c.pointage_origine_id=p.id) ORDER BY date_heure DESC,id DESC LIMIT 1",employeId);
        if(!dernier.isEmpty()&&utc.isBefore(asOffsetDateTime(dernier.getFirst().get("date_heure")))){anomalie(employeId,utc,"HORODATAGE_INVALIDE","Pointage antérieur au dernier pointage enregistré");return resultat(false,null,"Horodatage antérieur au dernier pointage");}
        boolean entreeOuverte=!dernier.isEmpty()&&"ENTREE".equals(dernier.getFirst().get("sens"));
        if((sens==SensPointage.ENTREE&&entreeOuverte)||(sens==SensPointage.SORTIE&&!entreeOuverte)){
            String type=sens==SensPointage.ENTREE?"DOUBLON":"SORTIE_MANQUANTE";
            anomalie(employeId,utc,type,sens==SensPointage.ENTREE?"Deux entrées successives":"Sortie sans entrée ouverte");
            return resultat(false,null,sens==SensPointage.ENTREE?"Une entrée est déjà ouverte":"Aucune entrée ouverte pour cette sortie");
        }
        Long id=jdbc.queryForObject("INSERT INTO pointage(employe_id,date_heure,sens,source,cree_par_id,valide_par_id,date_validation,motif_correction,pointage_origine_id) VALUES(?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,employeId,utc,sens.name(),source,creeParId,valideParId,validation,motif,null);
        LocalDate jour=sens==SensPointage.ENTREE?utc.atZoneSameInstant(ZONE).toLocalDate():asOffsetDateTime(dernier.getFirst().get("date_heure")).atZoneSameInstant(ZONE).toLocalDate();
        if(!"IMPORT".equals(source))calcul.recalculer(employeId,jour,false);
        return resultat(true,id,null);
    }

    public List<Map<String,Object>> pointages(long id,LocalDate du,LocalDate au){
        if(au.isBefore(du))throw new IllegalArgumentException("La date de fin précède la date de début");
        return jdbc.queryForList("SELECT id,employe_id AS \"employeId\",date_heure AS \"dateHeure\",sens,source,cree_par_id AS \"creeParId\",motif_correction AS \"motifCorrection\",pointage_origine_id AS \"pointageOrigineId\" FROM pointage WHERE employe_id=? AND date_heure>=? AND date_heure<? ORDER BY date_heure,id",id,du.atStartOfDay(ZONE).toOffsetDateTime(),au.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime());
    }
    public List<Map<String,Object>> aujourdhui(String email){Long id=employePourEmail(email);return pointages(id,LocalDate.now(ZONE),LocalDate.now(ZONE));}

    private Long employePourEmail(String email){try{return jdbc.queryForObject("SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?)",Long.class,email);}catch(EmptyResultDataAccessException e){throw new IllegalArgumentException("Aucun employé associé au compte connecté");}}
    private void anomalie(long emp,OffsetDateTime instant,String type,String message){LocalDate date=instant.atZoneSameInstant(ZONE).toLocalDate();Long id=jdbc.queryForObject("INSERT INTO anomalie_pointage(employe_id,date,type,statut,commentaire) VALUES(?,?,?,'OUVERTE',?) RETURNING id",Long.class,emp,date,type,message);notifications.notifier(id,emp,date,type);}
    private Map<String,Object> resultat(boolean accepte,Long id,String motif){Map<String,Object>m=new LinkedHashMap<>();m.put("accepte",accepte);m.put("pointageId",id);if(motif!=null)m.put("motif",motif);return m;}
    private Map<String,Object> pointage(long id){return jdbc.queryForMap("SELECT id,employe_id AS \"employeId\",date_heure AS \"dateHeure\",sens,source,motif_correction AS \"motifCorrection\",pointage_origine_id AS \"pointageOrigineId\" FROM pointage WHERE id=?",id);}
    private void audit(Integer utilisateurId,String role,String action,long id,String avant,String apres){jdbc.update("INSERT INTO journal_audit(utilisateur_id,role,action,entite,entite_id,ancienne_valeur,nouvelle_valeur) VALUES(?,?,?,'pointage',?,CAST(? AS jsonb),CAST(? AS jsonb))",utilisateurId,role,action,id,avant,apres);}
    private static int col(String[] cols,String... names){for(int i=0;i<cols.length;i++)for(String n:names)if(cols[i].trim().equals(n))return i;return -1;}
    private static String value(String[] values,int i){if(i>=values.length||values[i].isBlank())throw new IllegalArgumentException("Valeur CSV manquante");return values[i].trim().replaceAll("^\"|\"$","");}
    private static String[] parseCsv(String line){return line.split("[,;]",-1);}
    private static OffsetDateTime parseDate(String v){try{return OffsetDateTime.parse(v);}catch(DateTimeParseException e){try{return LocalDateTime.parse(v).atZone(ZONE).toOffsetDateTime();}catch(DateTimeParseException ex){throw new IllegalArgumentException("date_heure doit être ISO-8601");}}}
    private static OffsetDateTime asOffsetDateTime(Object value){
        if(value instanceof OffsetDateTime dateTime)return dateTime;
        if(value instanceof Timestamp timestamp)return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        if(value instanceof java.time.LocalDateTime dateTime)return dateTime.atZone(ZONE).toOffsetDateTime();
        if(value instanceof java.time.Instant instant)return instant.atOffset(ZoneOffset.UTC);
        throw new IllegalStateException("Type d'horodatage de pointage non pris en charge : "+(value==null?"null":value.getClass().getName()));
    }
    private static String json(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");}
}
