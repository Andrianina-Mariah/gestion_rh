package tovo.gestion.rh.temps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.temps.dto.*;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;

import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class TempsPlanningService {
    private final JdbcTemplate jdbc;

    public List<Map<String,Object>> regimes() {
        return jdbc.queryForList("SELECT id,code,libelle,heures_hebdo AS \"heuresHebdo\",jours_travailles_par_semaine AS \"joursTravaillesParSemaine\",tolerance_retard_min AS \"toleranceRetardMin\" FROM regime_travail ORDER BY libelle");
    }
    @Transactional public Map<String,Object> creerRegime(RegimeRequest r) {
        Long id=jdbc.queryForObject("INSERT INTO regime_travail(code,libelle,heures_hebdo,jours_travailles_par_semaine,tolerance_retard_min) VALUES(?,?,?,?,?) RETURNING id",Long.class,r.code(),r.libelle(),r.heuresHebdo(),r.joursTravaillesParSemaine(),r.toleranceRetardMin());
        return regime(id);
    }
    @Transactional public Map<String,Object> modifierRegime(long id, RegimeRequest r) {
        int n=jdbc.update("UPDATE regime_travail SET code=?,libelle=?,heures_hebdo=?,jours_travailles_par_semaine=?,tolerance_retard_min=? WHERE id=?",r.code(),r.libelle(),r.heuresHebdo(),r.joursTravaillesParSemaine(),r.toleranceRetardMin(),id);
        if(n==0) throw new ResourceNotFoundException("Régime",id);
        verifierCoherence(id); return regime(id);
    }
    public Map<String,Object> regime(long id) {
        return jdbc.query("SELECT id,code,libelle,heures_hebdo AS \"heuresHebdo\",jours_travailles_par_semaine AS \"joursTravaillesParSemaine\",tolerance_retard_min AS \"toleranceRetardMin\" FROM regime_travail WHERE id=?",rs->{if(!rs.next()) throw new ResourceNotFoundException("Régime",id); Map<String,Object> m=new LinkedHashMap<>(); for(String k:List.of("id","code","libelle","heuresHebdo","joursTravaillesParSemaine","toleranceRetardMin"))m.put(k,rs.getObject(k)); return m;},id);
    }
    @Transactional public void supprimerRegime(long id) { if(jdbc.update("DELETE FROM regime_travail WHERE id=?",id)==0) throw new ResourceNotFoundException("Régime",id); }
    public List<Map<String,Object>> horaires(long regimeId) { regime(regimeId); return jdbc.queryForList("SELECT id,regime_travail_id AS \"regimeTravailId\",jour_semaine AS \"jourSemaine\",heure_debut AS \"heureDebut\",heure_fin AS \"heureFin\",pause_minutes AS \"pauseMinutes\",travaille FROM horaire_type WHERE regime_travail_id=? ORDER BY jour_semaine",regimeId); }
    @Transactional public List<Map<String,Object>> remplacerHoraires(long regimeId,List<HoraireTypeRequest> horaires) {
        regime(regimeId); Set<Integer> days=new HashSet<>();
        for(var h:horaires){ if(!days.add(h.jourSemaine())) throw new IllegalArgumentException("Un horaire par jour de semaine au maximum"); if(h.travaille()&&h.heureFin().equals(h.heureDebut())) throw new IllegalArgumentException("L'heure de début et de fin ne peuvent pas être identiques"); }
        jdbc.update("DELETE FROM horaire_type WHERE regime_travail_id=?",regimeId);
        horaires.forEach(h->jdbc.update("INSERT INTO horaire_type(regime_travail_id,jour_semaine,heure_debut,heure_fin,pause_minutes,travaille) VALUES(?,?,?,?,?,?)",regimeId,h.jourSemaine(),h.heureDebut(),h.heureFin(),h.pauseMinutes(),h.travaille()));
        verifierCoherence(regimeId); return horaires(regimeId);
    }
    private void verifierCoherence(long id){
        var r=regime(id); Object scheduled=jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN travaille THEN EXTRACT(EPOCH FROM (heure_fin-heure_debut+CASE WHEN heure_fin<=heure_debut THEN INTERVAL '1 day' ELSE INTERVAL '0 day' END))/3600-pause_minutes/60.0 ELSE 0 END),0) FROM horaire_type WHERE regime_travail_id=?",Object.class,id);
        double hours=((Number)scheduled).doubleValue(); double expected=((Number)r.get("heuresHebdo")).doubleValue();
        Integer days=jdbc.queryForObject("SELECT COUNT(*) FROM horaire_type WHERE regime_travail_id=?",Integer.class,id);
        if(days>0&&Math.abs(hours-expected)>0.01) throw new IllegalArgumentException("Les horaires définissent "+hours+" h, alors que le régime prévoit "+expected+" h hebdomadaires");
    }
    @Transactional public void assigner(AssignationRegimeRequest r){
        if(r.dateFin()!=null&&r.dateFin().isBefore(r.dateDebut()))throw new IllegalArgumentException("La date de fin précède la date de début");
        try { jdbc.update("INSERT INTO employe_regime(employe_id,regime_travail_id,date_debut,date_fin) VALUES(?,?,?,?)",r.employeId(),r.regimeTravailId(),r.dateDebut(),r.dateFin()); }
        catch(DataIntegrityViolationException ex) { throw new IllegalStateException("L'employé a déjà un régime assigné sur cette période, ou une référence est invalide"); }
    }
    public List<Map<String,Object>> joursFeries(Integer annee){
        if(annee==null)return jdbc.queryForList("SELECT id,date,libelle,paye FROM jour_ferie ORDER BY date");
        return jdbc.queryForList("SELECT id,date,libelle,paye FROM jour_ferie WHERE date >= ? AND date < ? ORDER BY date",LocalDate.of(annee,1,1),LocalDate.of(annee+1,1,1));
    }
    @Transactional public Map<String,Object> creerJourFerie(JourFerieRequest r){
        Long id=jdbc.queryForObject("INSERT INTO jour_ferie(date,libelle,paye) VALUES(?,?,?) RETURNING id",Long.class,r.date(),r.libelle(),r.paye());
        return jdbc.queryForMap("SELECT id,date,libelle,paye FROM jour_ferie WHERE id=?",id);
    }
    @Transactional public int generer(GenererPlanningRequest r){
        if(r.au().isBefore(r.du()))throw new IllegalArgumentException("La date de fin précède la date de début");
        if((r.employeId()==null)==(r.departementId()==null))throw new IllegalArgumentException("Fournir soit employeId, soit departementId");
        String employees=r.employeId()!=null?"SELECT ?::bigint AS id":"SELECT DISTINCT employe_id AS id FROM affectation WHERE departement_id=? AND date_affectation<=? AND (date_fin IS NULL OR date_fin>=?)";
        List<Long> ids=r.employeId()!=null?List.of(r.employeId()):jdbc.queryForList(employees,Long.class,r.departementId(),r.au(),r.du()); int count=0;
        for(long emp:ids){
            var periods=jdbc.queryForList("SELECT regime_travail_id,date_debut,date_fin FROM employe_regime WHERE employe_id=? AND date_debut<=? AND (date_fin IS NULL OR date_fin>=?)",emp,r.au(),r.du());
            for(LocalDate d=r.du();!d.isAfter(r.au());d=d.plusDays(1)){
                if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM jour_ferie WHERE date=?)",Boolean.class,d))continue;
                for(var p:periods){LocalDate start=((java.sql.Date)p.get("date_debut")).toLocalDate(); Object endObj=p.get("date_fin"); LocalDate end=endObj==null?null:((java.sql.Date)endObj).toLocalDate(); if(d.isBefore(start)||(end!=null&&d.isAfter(end)))continue;
                    long regime=((Number)p.get("regime_travail_id")).longValue(); int dow=d.getDayOfWeek().getValue();
                    var shifts=jdbc.queryForList("SELECT heure_debut,heure_fin,pause_minutes FROM horaire_type WHERE regime_travail_id=? AND jour_semaine=? AND travaille=true",regime,dow);
                    for(var sh:shifts)count+=jdbc.update("INSERT INTO planning(employe_id,date,heure_debut,heure_fin,type,source,pause_minutes) VALUES(?,?,?,?,'TRAVAIL','REGIME',?) ON CONFLICT(employe_id,date,heure_debut) DO NOTHING",emp,d,sh.get("heure_debut"),sh.get("heure_fin"),sh.get("pause_minutes"));
                }
            }
        }
        return count;
    }
    @Transactional public Map<String,Object> modifierPlanning(ModifierPlanningRequest r){
        if(r.heureFin().equals(r.heureDebut()))throw new IllegalArgumentException("L'heure de début et de fin ne peuvent pas être identiques");
        if(!Set.of("TRAVAIL","REPOS","CONGE","ABSENCE","FERIE").contains(r.type()))throw new IllegalArgumentException("Type de planning invalide");
        Long id=jdbc.query("INSERT INTO planning(employe_id,date,heure_debut,heure_fin,type,source,motif) VALUES(?,?,?,?,?,'MANUEL',?) ON CONFLICT(employe_id,date,heure_debut) DO UPDATE SET heure_fin=EXCLUDED.heure_fin,type=EXCLUDED.type,source='MANUEL',motif=EXCLUDED.motif RETURNING id",rs->rs.next()?rs.getLong(1):null,r.employeId(),r.date(),r.heureDebut(),r.heureFin(),r.type(),r.motif());
        return planning(id);
    }
    public List<PlanningResponse> planningEmploye(long id,LocalDate du,LocalDate au){return jdbc.query("SELECT id,employe_id,date,heure_debut,heure_fin,type,source,motif FROM planning WHERE employe_id=? AND date BETWEEN ? AND ? ORDER BY date,heure_debut",(rs,n)->new PlanningResponse(rs.getLong("id"),rs.getLong("employe_id"),rs.getDate("date").toLocalDate(),rs.getTime("heure_debut").toLocalTime(),rs.getTime("heure_fin").toLocalTime(),rs.getString("type"),rs.getString("source"),rs.getString("motif")),id,du,au);}
    public void verifierAccesEmploye(long id,String email,boolean manager){
        String sql="SELECT EXISTS(SELECT 1 FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE e.id=? AND lower(c.email)=lower(?))";
        boolean allowed=jdbc.queryForObject(sql,Boolean.class,id,email);
        if(!allowed&&manager) allowed=jdbc.queryForObject("WITH RECURSIVE equipe(id) AS (SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?) UNION SELECT a.employe_id FROM affectation a JOIN equipe t ON a.manager_id=t.id WHERE a.date_affectation<=CURRENT_DATE AND (a.date_fin IS NULL OR a.date_fin>=CURRENT_DATE)) SELECT EXISTS(SELECT 1 FROM equipe WHERE id=?)",Boolean.class,email,id);
        if(!allowed) throw new org.springframework.security.access.AccessDeniedException("Accès limité à votre dossier");
    }
    public void verifierAccesEquipe(int dept,String email){
        boolean allowed=jdbc.queryForObject("WITH RECURSIVE equipe(id) AS (SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?) UNION SELECT a.employe_id FROM affectation a JOIN equipe t ON a.manager_id=t.id WHERE a.date_affectation<=CURRENT_DATE AND (a.date_fin IS NULL OR a.date_fin>=CURRENT_DATE)) SELECT EXISTS(SELECT 1 FROM affectation a JOIN equipe t ON t.id=a.employe_id WHERE a.departement_id=? AND a.date_affectation<=CURRENT_DATE AND (a.date_fin IS NULL OR a.date_fin>=CURRENT_DATE))",Boolean.class,email,dept);
        if(!allowed) throw new org.springframework.security.access.AccessDeniedException("Département hors de votre équipe");
    }
    public List<Map<String,Object>> planningEquipe(int dept,LocalDate du,LocalDate au,String managerEmail){
        String sql="SELECT p.id,p.employe_id AS \"employeId\",p.date,p.heure_debut AS \"heureDebut\",p.heure_fin AS \"heureFin\",p.type,p.source,p.motif FROM planning p JOIN affectation a ON a.employe_id=p.employe_id AND a.departement_id=? AND a.date_affectation<=p.date AND (a.date_fin IS NULL OR a.date_fin>=p.date) WHERE p.date BETWEEN ? AND ?";
        if(managerEmail!=null) sql="WITH RECURSIVE equipe(id) AS (SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?) UNION SELECT a.employe_id FROM affectation a JOIN equipe t ON a.manager_id=t.id WHERE a.date_affectation<=CURRENT_DATE AND (a.date_fin IS NULL OR a.date_fin>=CURRENT_DATE)) "+sql+" AND p.employe_id IN (SELECT id FROM equipe)";
        sql+=" ORDER BY p.date,p.employe_id,p.heure_debut";
        return managerEmail==null?jdbc.queryForList(sql,dept,du,au):jdbc.queryForList(sql,managerEmail,dept,du,au);
    }
    private Map<String,Object> planning(long id){return jdbc.queryForMap("SELECT id,employe_id AS \"employeId\",date,heure_debut AS \"heureDebut\",heure_fin AS \"heureFin\",type,source,motif FROM planning WHERE id=?",id);}
    @Transactional public void integrerAbsence(long employeId,LocalDate du,LocalDate au,String type){if(!Set.of("CONGE","ABSENCE").contains(type)||au.isBefore(du))throw new IllegalArgumentException("Type ou période d'absence invalide");for(LocalDate d=du;!d.isAfter(au);d=d.plusDays(1)){String source="CONGE".equals(type)?"CONGE":"MANUEL";String motif="CONGE".equals(type)?"Intégration bloc 7":"Absence bloc 7";int updated=jdbc.update("UPDATE planning SET type_avant_conge=type,source_avant_conge=source,motif_avant_conge=motif,type=?,source=?,motif=? WHERE employe_id=? AND date=? AND type NOT IN ('CONGE','ABSENCE')",type,source,motif,employeId,d);Integer existing=jdbc.queryForObject("SELECT COUNT(*) FROM planning WHERE employe_id=? AND date=?",Integer.class,employeId,d);if(updated==0&&(existing==null||existing==0))jdbc.update("INSERT INTO planning(employe_id,date,heure_debut,heure_fin,type,source,motif,genere_pour_conge) VALUES(?,?,TIME '00:00',TIME '23:59',?,?,?,TRUE)",employeId,d,type,source,motif);}}
}
