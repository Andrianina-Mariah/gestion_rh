package tovo.gestion.rh.conge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import tovo.gestion.rh.conge.dto.*;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class CongeSoldeService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public List<Map<String,Object>> types(){return jdbc.queryForList("SELECT * FROM type_conge ORDER BY libelle");}
    public Map<String,Object> type(long id){return jdbc.queryForMap("SELECT * FROM type_conge WHERE id=?",id);}
    @Transactional public Map<String,Object> creerType(TypeCongeRequest r){verifierUnite(r.uniteDecompte());Long id=jdbc.queryForObject("INSERT INTO type_conge(code,libelle,remunere,debite_solde,justificatif_obligatoire,justificatif_apres_jours,duree_max_jours,demi_journee_autorisee,unite_decompte,actif,solde_negatif_autorise) VALUES(?,?,?,?,?,?,?,?,?,?,?) RETURNING id",Long.class,r.code(),r.libelle(),r.remunere(),r.debiteSolde(),r.justificatifObligatoire(),r.justificatifApresJours(),r.dureeMaxJours(),r.demiJourneeAutorisee(),r.uniteDecompte(),r.actif(),r.soldeNegatifAutorise());return type(id);}
    @Transactional public Map<String,Object> modifierType(long id,TypeCongeRequest r){verifierUnite(r.uniteDecompte());int n=jdbc.update("UPDATE type_conge SET code=?,libelle=?,remunere=?,debite_solde=?,justificatif_obligatoire=?,justificatif_apres_jours=?,duree_max_jours=?,demi_journee_autorisee=?,unite_decompte=?,actif=?,solde_negatif_autorise=? WHERE id=?",r.code(),r.libelle(),r.remunere(),r.debiteSolde(),r.justificatifObligatoire(),r.justificatifApresJours(),r.dureeMaxJours(),r.demiJourneeAutorisee(),r.uniteDecompte(),r.actif(),r.soldeNegatifAutorise(),id);if(n==0)throw new IllegalArgumentException("Type de congé introuvable");return type(id);}
    @Transactional public void supprimerType(long id){if(jdbc.update("UPDATE type_conge SET actif=false WHERE id=?",id)==0)throw new IllegalArgumentException("Type de congé introuvable");}
    private void verifierUnite(String u){if(!Set.of("OUVRES","OUVRABLES").contains(u))throw new IllegalArgumentException("Unité de décompte invalide");}

    public List<Map<String,Object>> droits(long typeId){type(typeId);return jdbc.queryForList("SELECT * FROM droit_conge WHERE type_conge_id=? ORDER BY type_contrat NULLS FIRST,anciennete_min_mois",typeId);}
    @Transactional public Map<String,Object> creerDroit(DroitCongeRequest r){verifierDroit(r);Long id=jdbc.queryForObject("INSERT INTO droit_conge(type_conge_id,type_contrat,regle_acquisition,periode_acquisition,anciennete_min_mois,plafond_report,validite_report_mois) VALUES(?,?,?,?,?,?,?) RETURNING id",Long.class,r.typeCongeId(),r.typeContrat(),r.regleAcquisition(),r.periodeAcquisition(),r.ancienneteMinMois(),r.plafondReport(),r.validiteReportMois());return jdbc.queryForMap("SELECT * FROM droit_conge WHERE id=?",id);}
    @Transactional public Map<String,Object> modifierDroit(long id,DroitCongeRequest r){verifierDroit(r);int n=jdbc.update("UPDATE droit_conge SET type_conge_id=?,type_contrat=?,regle_acquisition=?,periode_acquisition=?,anciennete_min_mois=?,plafond_report=?,validite_report_mois=? WHERE id=?",r.typeCongeId(),r.typeContrat(),r.regleAcquisition(),r.periodeAcquisition(),r.ancienneteMinMois(),r.plafondReport(),r.validiteReportMois(),id);if(n==0)throw new IllegalArgumentException("Règle de droit introuvable");return jdbc.queryForMap("SELECT * FROM droit_conge WHERE id=?",id);}
    @Transactional public void supprimerDroit(long id){if(jdbc.update("DELETE FROM droit_conge WHERE id=?",id)==0)throw new IllegalArgumentException("Règle de droit introuvable");}
    private void verifierDroit(DroitCongeRequest r){type(r.typeCongeId());if(!Set.of("MOIS","ANNEE").contains(r.periodeAcquisition()))throw new IllegalArgumentException("Période d'acquisition invalide");}

    public List<Map<String,Object>> soldes(long employeId,Integer annee){int y=annee==null?Year.now().getValue():annee;return jdbc.queryForList("SELECT s.id,s.employe_id AS \"employeId\",s.type_conge_id AS \"typeCongeId\",t.code,t.libelle,s.annee,s.acquis,s.pris,s.en_attente AS \"enAttente\",s.reporte,s.ajuste,(s.acquis+s.reporte+s.ajuste-s.pris-s.en_attente) AS restant,t.solde_negatif_autorise AS \"soldeNegatifAutorise\" FROM solde_conge s JOIN type_conge t ON t.id=s.type_conge_id WHERE s.employe_id=? AND s.annee=? ORDER BY t.libelle",employeId,y);}
    public List<Map<String,Object>> rapportSoldes(int annee){return jdbc.queryForList("SELECT COALESCE(dep.id,0) AS departement_id,COALESCE(dep.nom,'Sans service') AS departement,t.libelle AS type_conge,COUNT(DISTINCT s.employe_id) AS nb_employes,ROUND(SUM(s.acquis+s.reporte+s.ajuste-s.pris-s.en_attente),2) AS jours_restants,ROUND(SUM(s.reporte),2) AS jours_reportes,ROUND(SUM((SELECT COALESCE(SUM(m.quantite),0) FROM mouvement_solde m WHERE m.solde_conge_id=s.id AND m.type='REPORT' AND t.validite_report_mois IS NOT NULL AND (m.date+(t.validite_report_mois||' months')::interval)::date>=CURRENT_DATE AND NOT EXISTS(SELECT 1 FROM mouvement_solde x WHERE x.solde_conge_id=m.solde_conge_id AND x.cle_idempotence='EXP-REPORT-MVT:'||m.id)),2) AS jours_a_risque_expiration FROM solde_conge s JOIN type_conge t ON t.id=s.type_conge_id JOIN employe e ON e.id=s.employe_id LEFT JOIN affectation a ON a.employe_id=e.id AND a.date_affectation<=CURRENT_DATE AND (a.date_fin IS NULL OR a.date_fin>=CURRENT_DATE) LEFT JOIN departement dep ON dep.id=a.departement_id WHERE s.annee=? GROUP BY dep.id,dep.nom,t.id,t.libelle ORDER BY dep.nom,t.libelle",annee);}
    public long employeConnecte(String email){return jdbc.queryForObject("SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?)",Long.class,email);}

    @Transactional public Map<String,Object> ajuster(long employeId,AjustementSoldeRequest r,Integer utilisateurId,String role){
        Map<String,Object> type=type(r.typeCongeId()); if(!Boolean.TRUE.equals(type.get("debite_solde")))throw new IllegalArgumentException("Ce type de congé ne débite pas de solde");
        long sid=obtenirSolde(employeId,r.typeCongeId(),r.annee());
        Map<String,Object> before=jdbc.queryForMap("SELECT acquis,pris,en_attente,reporte,ajuste FROM solde_conge WHERE id=? FOR UPDATE",sid);
        BigDecimal after=decimal(before,"acquis").add(decimal(before,"reporte")).add(decimal(before,"ajuste")).add(r.quantite()).subtract(decimal(before,"pris")).subtract(decimal(before,"en_attente"));
        if(after.signum()<0&&!Boolean.TRUE.equals(type.get("solde_negatif_autorise")))throw new IllegalArgumentException("L'ajustement rendrait le solde disponible négatif");
        jdbc.update("UPDATE solde_conge SET ajuste=ajuste+? WHERE id=?",r.quantite(),sid);
        jdbc.update("INSERT INTO mouvement_solde(solde_conge_id,type,quantite,date,motif,cree_par_id) VALUES(?,'AJUSTEMENT',?,CURRENT_DATE,?,?)",sid,r.quantite(),r.motif(),utilisateurId);
        audit(utilisateurId,role,"AJUSTEMENT_SOLDE",sid,mapper.valueToTree(before).toString(),"{\"quantite\":"+r.quantite()+",\"motif\":\""+escape(r.motif())+"\"}");
        return jdbc.queryForMap("SELECT *,acquis+reporte+ajuste-pris-en_attente AS restant FROM solde_conge WHERE id=?",sid);
    }
    private String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
    private BigDecimal decimal(Map<String,Object> m,String k){return new BigDecimal(m.get(k).toString());}
    private long obtenirSolde(long emp,long type,int year){jdbc.update("INSERT INTO solde_conge(employe_id,type_conge_id,annee) VALUES(?,?,?) ON CONFLICT(employe_id,type_conge_id,annee) DO NOTHING",emp,type,year);return jdbc.queryForObject("SELECT id FROM solde_conge WHERE employe_id=? AND type_conge_id=? AND annee=?",Long.class,emp,type,year);}
    private void audit(Integer uid,String role,String action,long id,String before,String after){jdbc.update("INSERT INTO journal_audit(utilisateur_id,role,action,entite,entite_id,ancienne_valeur,nouvelle_valeur) VALUES(?,?,?,'solde_conge',?,CAST(? AS jsonb),CAST(? AS jsonb))",uid,role,action,id,before,after);}

    @Transactional public void initialiserEmploye(long id){Integer year=Year.now().getValue();jdbc.update("INSERT INTO solde_conge(employe_id,type_conge_id,annee) SELECT ?,id,? FROM type_conge WHERE actif=true AND debite_solde=true ON CONFLICT(employe_id,type_conge_id,annee) DO NOTHING",id,year);}

    @Scheduled(cron="0 15 1 1 * *", zone="Indian/Antananarivo")
    @Transactional public void acquisitionMensuelle(){acquerirMois(YearMonth.now().minusMonths(1));}
    @Transactional public void acquerirMois(YearMonth mois){
        LocalDate debut=mois.atDay(1),fin=mois.atEndOfMonth();
        List<Map<String,Object>> employees=jdbc.queryForList("SELECT id,date_embauche,date_sortie FROM employe WHERE statut='ACTIF' AND date_embauche<=? AND (date_sortie IS NULL OR date_sortie>=?)",fin,debut);
        List<Map<String,Object>> types=jdbc.queryForList("SELECT id FROM type_conge WHERE actif=true AND debite_solde=true");
        for(var e:employees){long emp=((Number)e.get("id")).longValue();LocalDate hire=((java.sql.Date)e.get("date_embauche")).toLocalDate();LocalDate start=hire.isAfter(debut)?hire:debut;Object out=e.get("date_sortie");LocalDate end=out==null||((java.sql.Date)out).toLocalDate().isAfter(fin)?fin:((java.sql.Date)out).toLocalDate();
            long jours=Math.max(0,ChronoUnit.DAYS.between(start,end)+1);if(jours==0)continue;
            String contract=jdbc.query("SELECT type_contrat FROM contrat WHERE employe_id=? AND statut='VALIDE' AND date_debut<=? AND (date_fin IS NULL OR date_fin>=?) ORDER BY date_debut DESC LIMIT 1",rs->rs.next()?rs.getString(1):null,emp,fin,debut);
            long anciennete=Math.max(0,ChronoUnit.MONTHS.between(YearMonth.from(hire),YearMonth.from(fin)));
            for(var t:types){long type=((Number)t.get("id")).longValue();List<Map<String,Object>> rules=jdbc.queryForList("SELECT * FROM droit_conge WHERE type_conge_id=? AND (type_contrat IS NULL OR type_contrat=?) AND anciennete_min_mois<=? ORDER BY (type_contrat IS NOT NULL) DESC,anciennete_min_mois DESC LIMIT 1",type,contract,anciennete);if(rules.isEmpty())continue;
                var rule=rules.get(0);BigDecimal base=new BigDecimal(rule.get("regle_acquisition").toString());if("ANNEE".equals(rule.get("periode_acquisition")))base=base.divide(BigDecimal.valueOf(12),6,RoundingMode.HALF_UP);
                BigDecimal credit=base.multiply(BigDecimal.valueOf(jours)).divide(BigDecimal.valueOf(mois.lengthOfMonth()),2,RoundingMode.HALF_UP);if(credit.signum()<=0)continue;
                long sid=obtenirSolde(emp,type,mois.getYear());String key="ACQ:"+mois;
                int n=jdbc.update("INSERT INTO mouvement_solde(solde_conge_id,type,quantite,date,motif,cle_idempotence) VALUES(?,'ACQUISITION',?,?,?,?) ON CONFLICT(solde_conge_id,cle_idempotence) WHERE cle_idempotence IS NOT NULL DO NOTHING",sid,credit,fin,"Acquisition mensuelle "+mois,key);
                if(n>0)jdbc.update("UPDATE solde_conge SET acquis=acquis+? WHERE id=?",credit,sid);
            }
        }
    }

    @Scheduled(cron="0 30 2 1 1 *", zone="Indian/Antananarivo")
    @Transactional public void reportAnnuel(){reporterAnnee(Year.now().getValue()-1);}
    @Transactional public void reporterAnnee(int annee){
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT s.*,t.plafond_report,t.validite_report_mois FROM solde_conge s JOIN type_conge t ON t.id=s.type_conge_id WHERE s.annee=? AND t.debite_solde=true",annee);
        for(var row:rows){long oldId=((Number)row.get("id")).longValue(),emp=((Number)row.get("employe_id")).longValue(),type=((Number)row.get("type_conge_id")).longValue();int next=annee+1;long newId=obtenirSolde(emp,type,next);
            BigDecimal available=decimal(row,"acquis").add(decimal(row,"reporte")).add(decimal(row,"ajuste")).subtract(decimal(row,"pris")).subtract(decimal(row,"en_attente")).max(BigDecimal.ZERO);
            BigDecimal cap=new BigDecimal(row.get("plafond_report").toString());BigDecimal carry=available.min(cap).setScale(2,RoundingMode.HALF_UP);BigDecimal expired=available.subtract(carry);
            if(carry.signum()>0)ajouterReport(oldId,newId,carry,annee);
            if(expired.signum()>0)jdbc.update("INSERT INTO mouvement_solde(solde_conge_id,type,quantite,date,motif,cle_idempotence) VALUES(?,'EXPIRATION',?,?,?,?) ON CONFLICT(solde_conge_id,cle_idempotence) WHERE cle_idempotence IS NOT NULL DO NOTHING",oldId,expired,LocalDate.of(next,1,1),"Jours non reportables expirés", "EXP:"+next);
        }
    }
    @Scheduled(cron="0 45 1 1 * *", zone="Indian/Antananarivo")
    @Transactional public void expirerReports(){
        LocalDate cutoff=LocalDate.now().withDayOfMonth(1).minusDays(1);
        List<Map<String,Object>> reports=jdbc.queryForList("SELECT m.id,m.solde_conge_id,m.quantite,m.date,t.validite_report_mois FROM mouvement_solde m JOIN solde_conge s ON s.id=m.solde_conge_id JOIN type_conge t ON t.id=s.type_conge_id WHERE m.type='REPORT' AND t.validite_report_mois IS NOT NULL AND m.date + (t.validite_report_mois || ' months')::interval <= ?",cutoff);
        for(var r:reports){long movement=((Number)r.get("id")).longValue(),sid=((Number)r.get("solde_conge_id")).longValue();BigDecimal q=new BigDecimal(r.get("quantite").toString());int inserted=jdbc.update("INSERT INTO mouvement_solde(solde_conge_id,type,quantite,date,motif,cle_idempotence) VALUES(?,'EXPIRATION',?,?,?,?) ON CONFLICT(solde_conge_id,cle_idempotence) WHERE cle_idempotence IS NOT NULL DO NOTHING",sid,q,cutoff,"Expiration du report arrivé à échéance","EXP-REPORT-MVT:"+movement);if(inserted>0)jdbc.update("UPDATE solde_conge SET reporte=GREATEST(0,reporte-?) WHERE id=?",q,sid);}
    }
    private void ajouterReport(long oldId,long newId,BigDecimal n,int annee){int inserted=jdbc.update("INSERT INTO mouvement_solde(solde_conge_id,type,quantite,date,motif,cle_idempotence) VALUES(?,'REPORT',?,?,?,?) ON CONFLICT(solde_conge_id,cle_idempotence) WHERE cle_idempotence IS NOT NULL DO NOTHING",newId,n,LocalDate.of(annee+1,1,1),"Report annuel","REPORT:"+annee);if(inserted>0)jdbc.update("UPDATE solde_conge SET reporte=reporte+? WHERE id=?",n,newId);}
}
