package tovo.gestion.rh.temps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.temps.dto.ReouverturePeriodeRequest;
import tovo.gestion.rh.temps.dto.ValidationPeriodeRequest;
import tovo.gestion.rh.temps.dto.CreerPeriodeRequest;
import tovo.gestion.rh.temps.event.PeriodeTempsValideeEvent;
import tovo.gestion.rh.conge.service.DemandeCongeService;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class PeriodeTempsService {
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher eventPublisher;
    private final DemandeCongeService demandesConge;

    public List<Map<String,Object>> lister(){return jdbc.queryForList("SELECT id,annee,mois,statut,validee_par_id AS \"valideeParId\",date_validation AS \"dateValidation\",motif_derogation AS \"motifDerogation\",date_export AS \"dateExport\" FROM periode_temps ORDER BY annee DESC,mois DESC");}
    @Transactional public Map<String,Object> creer(CreerPeriodeRequest r){jdbc.update("INSERT INTO periode_temps(annee,mois) VALUES(?,?) ON CONFLICT(annee,mois) DO NOTHING",r.annee(),r.mois());Long id=jdbc.queryForObject("SELECT id FROM periode_temps WHERE annee=? AND mois=?",Long.class,r.annee(),r.mois());return details(id);}
    public Integer utilisateurId(String email){return jdbc.queryForObject("SELECT id FROM utilisateur WHERE lower(email)=lower(?)",Integer.class,email);}

    @Transactional public Map<String,Object> valider(long id,String role,Integer utilisateurId,ValidationPeriodeRequest request){
        Map<String,Object> period=periodeVerrouillee(id);if(!"OUVERTE".equals(period.get("statut")))throw new IllegalStateException("Seule une période ouverte peut être validée");
        int year=((Number)period.get("annee")).intValue(),month=((Number)period.get("mois")).intValue();LocalDate from=LocalDate.of(year,month,1),to=from.withDayOfMonth(from.lengthOfMonth());
        Long open=jdbc.queryForObject("SELECT COUNT(*) FROM anomalie_pointage WHERE statut='OUVERTE' AND date BETWEEN ? AND ?",Long.class,from,to);
        String derogation=request==null?null:request.motifDerogation();if(open>0&&(derogation==null||derogation.isBlank()))throw new IllegalStateException("La période contient "+open+" anomalie(s) ouverte(s) ; une dérogation motivée est requise");
        genererVariables(id,from,to);
        Instant now=Instant.now();jdbc.update("UPDATE periode_temps SET statut='VALIDEE',validee_par_id=?,date_validation=?,motif_derogation=? WHERE id=?",utilisateurId,now,derogation==null||derogation.isBlank()?null:derogation,id);
        audit(utilisateurId,role,"MODIFICATION","periode_temps",id,"{\"statut\":\"OUVERTE\"}","{\"statut\":\"VALIDEE\",\"derogation\":\""+json(derogation==null?"":derogation)+"\"}");
        eventPublisher.publishEvent(new PeriodeTempsValideeEvent(id,year,month,utilisateurId,now));
        return details(id);
    }

    @Transactional public Map<String,Object> reouvrir(long id,String role,Integer utilisateurId,ReouverturePeriodeRequest request){
        Map<String,Object> period=periodeVerrouillee(id);if("OUVERTE".equals(period.get("statut")))throw new IllegalStateException("La période est déjà ouverte");
        String old="{\"statut\":\""+period.get("statut")+"\"}";
        jdbc.update("UPDATE periode_temps SET statut='OUVERTE',reouverte_par_id=?,date_reouverture=CURRENT_TIMESTAMP,motif_reouverture=?,date_export=NULL WHERE id=?",utilisateurId,request.motif(),id);
        LocalDate from=LocalDate.of(((Number)period.get("annee")).intValue(),((Number)period.get("mois")).intValue(),1);genererVariables(id,from,from.withDayOfMonth(from.lengthOfMonth()));
        audit(utilisateurId,role,"MODIFICATION","periode_temps",id,old,"{\"statut\":\"OUVERTE\",\"motif\":\""+json(request.motif())+"\"}");return details(id);
    }

    @Transactional public ExportResult exporter(long id,String format,String role,Integer utilisateurId){
        if(!Set.of("json","csv").contains(format.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Format attendu : json ou csv");
        Map<String,Object> period=periodeVerrouillee(id);if("OUVERTE".equals(period.get("statut")))throw new IllegalStateException("La période doit être validée avant export");
        if("VALIDEE".equals(period.get("statut")))jdbc.update("UPDATE periode_temps SET statut='EXPORTEE',date_export=CURRENT_TIMESTAMP WHERE id=?",id);
        List<Map<String,Object>> rows=variables(id);
        audit(utilisateurId,role,"EXPORT","variable_paie",id,null,"{\"periodeId\":"+id+",\"format\":\""+format.toLowerCase(Locale.ROOT)+"\"}");
        if("csv".equalsIgnoreCase(format))return new ExportResult(null,csv(rows),"text/csv; charset=UTF-8");
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("periode",details(id));payload.put("variables",rows);return new ExportResult(payload,null,"application/json");
    }

    private Map<String,Object> periodeVerrouillee(long id){try{return jdbc.queryForMap("SELECT id,annee,mois,statut,validee_par_id,date_validation,motif_derogation FROM periode_temps WHERE id=? FOR UPDATE",id);}catch(org.springframework.dao.EmptyResultDataAccessException ex){throw new ResourceNotFoundException("Période",id);}}
    private Map<String,Object> details(long id){return jdbc.queryForMap("SELECT id,annee,mois,statut,validee_par_id AS \"valideeParId\",date_validation AS \"dateValidation\",motif_derogation AS \"motifDerogation\",reouverte_par_id AS \"reouverteParId\",date_reouverture AS \"dateReouverture\",motif_reouverture AS \"motifReouverture\",date_export AS \"dateExport\" FROM periode_temps WHERE id=?",id);}
    private List<Map<String,Object>> variables(long id){return jdbc.queryForList("SELECT v.id,v.employe_id AS \"employeId\",e.matricule,v.type,v.quantite_minutes AS \"quantiteMinutes\",v.quantite_jours AS \"quantiteJours\",v.source FROM variable_paie v JOIN employe e ON e.id=v.employe_id WHERE v.periode_id=? ORDER BY e.matricule,v.type",id);}
    private void genererVariables(long period,LocalDate from,LocalDate to){
        jdbc.update("DELETE FROM variable_paie WHERE periode_id=?",period);
        insertFromDays(period,"HEURES_NORMALES","minutes_normales",from,to);
        insertFromDays(period,"NUIT","minutes_nuit",from,to);
        insertFromDays(period,"DIMANCHE_FERIE","minutes_dimanche_ferie",from,to);
        insertFromDays(period,"RETARD","minutes_retard",from,to);
        insertFromDays(period,"ABSENCE","minutes_absence",from,to);
        genererVariablesCongeAbsence(period,from,to);
        jdbc.update("INSERT INTO variable_paie(employe_id,periode_id,type,quantite_minutes,source) SELECT employe_id,?,'HEURES_SUP_30',SUM(minutes),'VALIDATION_PERIODE' FROM heure_supplementaire WHERE date BETWEEN ? AND ? AND statut='VALIDEE' AND taux<40 GROUP BY employe_id HAVING SUM(minutes)>0",period,from,to);
        jdbc.update("INSERT INTO variable_paie(employe_id,periode_id,type,quantite_minutes,source) SELECT employe_id,?,'HEURES_SUP_50',SUM(minutes),'VALIDATION_PERIODE' FROM heure_supplementaire WHERE date BETWEEN ? AND ? AND statut='VALIDEE' AND taux>=40 GROUP BY employe_id HAVING SUM(minutes)>0",period,from,to);
    }
    private void insertFromDays(long period,String type,String column,LocalDate from,LocalDate to){jdbc.update("INSERT INTO variable_paie(employe_id,periode_id,type,quantite_minutes,source) SELECT employe_id,?,?,SUM("+column+"),'VALIDATION_PERIODE' FROM journee_travail WHERE date BETWEEN ? AND ? GROUP BY employe_id HAVING SUM("+column+")>0",period,type,from,to);}
    private void genererVariablesCongeAbsence(long period,LocalDate from,LocalDate to){
        var leaves=jdbc.queryForList("SELECT d.id,d.employe_id,d.type_conge_id,d.date_debut,d.date_fin,d.demi_journee_debut,d.demi_journee_fin,d.nb_jours,t.remunere FROM demande_conge d JOIN type_conge t ON t.id=d.type_conge_id WHERE d.statut='VALIDEE_RH' AND d.date_debut<=? AND d.date_fin>=?",to,from);
        for(var leave:leaves){long employee=((Number)leave.get("employe_id")).longValue(),typeId=((Number)leave.get("type_conge_id")).longValue();LocalDate start=date(leave.get("date_debut")),end=date(leave.get("date_fin"));boolean halfStart=Boolean.TRUE.equals(leave.get("demi_journee_debut")),halfEnd=Boolean.TRUE.equals(leave.get("demi_journee_fin"));java.math.BigDecimal days=demandesConge.joursPourDemandePeriode(employee,start,end,halfStart,halfEnd,from,to,typeId);if(days.signum()>0){String type=Boolean.TRUE.equals(leave.get("remunere"))?"JOURS_CONGE_PAYE":"JOURS_CONGE_SANS_SOLDE";insertDaysVariable(employee,period,type,days,"VALIDATION_CONGE");}}
        var absences=jdbc.queryForList("SELECT a.id,a.employe_id,a.type,a.date_debut,a.date_fin,COALESCE(tc.remunere,FALSE) AS remunere FROM absence a LEFT JOIN demande_conge d ON d.id=a.demande_conge_id LEFT JOIN type_conge tc ON tc.id=d.type_conge_id OR (d.id IS NULL AND upper(tc.code)='MALADIE') WHERE a.demande_conge_id IS NULL AND a.type IN ('INJUSTIFIEE','MALADIE') AND a.date_debut<=? AND a.date_fin>=?",to,from);
        for(var absence:absences){long employee=((Number)absence.get("employe_id")).longValue();LocalDate start=date(absence.get("date_debut")),end=date(absence.get("date_fin"));LocalDate sliceStart=start.isAfter(from)?start:from,sliceEnd=end.isBefore(to)?end:to;java.math.BigDecimal days=demandesConge.joursAbsenceCalendaires(employee,sliceStart,sliceEnd);if(days.signum()<=0)continue;String type="INJUSTIFIEE".equals(absence.get("type"))?"JOURS_ABSENCE_INJUSTIFIEE":Boolean.TRUE.equals(absence.get("remunere"))?"JOURS_MALADIE_PAYEE":"JOURS_MALADIE_NON_PAYEE";insertDaysVariable(employee,period,type,days,"VALIDATION_ABSENCE");}
    }
    private void insertDaysVariable(long employee,long period,String type,java.math.BigDecimal days,String source){jdbc.update("INSERT INTO variable_paie(employe_id,periode_id,type,quantite_minutes,quantite_jours,source) VALUES(?,?,?,0,?,?)",employee,period,type,days,source);}
    private LocalDate date(Object value){return value instanceof LocalDate d?d:((java.sql.Date)value).toLocalDate();}
    private void audit(Integer user,String role,String action,String entity,long id,String old,String next){jdbc.update("INSERT INTO journal_audit(utilisateur_id,role,action,entite,entite_id,ancienne_valeur,nouvelle_valeur) VALUES(?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb))",user,role,action,entity,id,old,next);}
    private static String csv(List<Map<String,Object>> rows){StringBuilder b=new StringBuilder("employeId,matricule,type,quantiteMinutes,quantiteJours,source\r\n");for(var r:rows)b.append(r.get("employeId")).append(',').append(csvCell(r.get("matricule"))).append(',').append(r.get("type")).append(',').append(r.get("quantiteMinutes")).append(',').append(r.get("quantiteJours")).append(',').append(csvCell(r.get("source"))).append("\r\n");return b.toString();}
    private static String csvCell(Object s){String v=String.valueOf(s);return v.contains(",")||v.contains("\"")?"\""+v.replace("\"","\"\"")+"\"":v;}
    private static String json(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");}
    public record ExportResult(Map<String,Object> json,String csv,String contentType){}
}
