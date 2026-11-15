package tovo.gestion.rh.temps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor
public class AnomalieService {
    private static final Set<String> STATUTS=Set.of("OUVERTE","JUSTIFIEE","CORRIGEE","IGNOREE");
    private static final Set<String> TYPES=Set.of("ENTREE_MANQUANTE","SORTIE_MANQUANTE","DOUBLON","HORS_PLANNING","DUREE_EXCESSIVE","RETARD_NON_JUSTIFIE","HORODATAGE_INVALIDE","ABSENCE_SANS_CONGE");
    private final JdbcTemplate jdbc;

    public List<Map<String,Object>> rechercher(String email,String role,String statut,String type,LocalDate du,LocalDate au,Integer departementId){
        if(au!=null&&du!=null&&au.isBefore(du))throw new IllegalArgumentException("La date de fin précède la date de début");
        if(statut!=null&&!STATUTS.contains(statut))throw new IllegalArgumentException("Statut d'anomalie invalide");
        if(type!=null&&!TYPES.contains(type))throw new IllegalArgumentException("Type d'anomalie invalide");
        String sql="SELECT a.id,a.employe_id AS \"employeId\",a.date,a.type,a.statut,a.commentaire,a.commentaire_traitement AS \"commentaireTraitement\",a.traitee_par_id AS \"traiteeParId\",a.traitee_le AS \"traiteeLe\" FROM anomalie_pointage a WHERE (?::varchar IS NULL OR a.statut=?) AND (?::varchar IS NULL OR a.type=?) AND (?::date IS NULL OR a.date>=?) AND (?::date IS NULL OR a.date<=?) AND (?::integer IS NULL OR EXISTS(SELECT 1 FROM affectation af WHERE af.employe_id=a.employe_id AND af.departement_id=? AND af.date_affectation<=a.date AND (af.date_fin IS NULL OR af.date_fin>=a.date)))";
        boolean manager="MANAGER".equals(role),employee="EMPLOYE".equals(role),privileged="RH".equals(role)||"ADMIN".equals(role);
        if(employee)sql+=" AND a.employe_id=(SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?))";
        if(manager)sql="WITH RECURSIVE equipe(id) AS (SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?) UNION SELECT af.employe_id FROM affectation af JOIN equipe t ON af.manager_id=t.id WHERE af.date_affectation<=CURRENT_DATE AND (af.date_fin IS NULL OR af.date_fin>=CURRENT_DATE)) "+sql+" AND a.employe_id IN (SELECT id FROM equipe)";
        sql+=" ORDER BY a.date DESC,a.id DESC";
        if(manager)return jdbc.queryForList(sql,email,statut,statut,type,type,du,du,au,au,departementId,departementId);
        if(employee)return jdbc.queryForList(sql,statut,statut,type,type,du,du,au,au,departementId,departementId,email);
        return jdbc.queryForList(sql,statut,statut,type,type,du,du,au,au,departementId,departementId);
    }

    @Transactional public Map<String,Object> traiter(long id,String email,Integer utilisateurId,String role,String action,String commentaire){
        if("EMPLOYE".equals(role)&&!"justifier".equals(action))throw new AccessDeniedException("Un salarié peut uniquement justifier ses propres anomalies");
        String statut=switch(action){case "justifier"->"JUSTIFIEE";case "corriger"->"CORRIGEE";case "ignorer"->"IGNOREE";default->throw new IllegalArgumentException("Action d'anomalie inconnue");};
        Map<String,Object> anomaly;
        try{anomaly=jdbc.queryForMap("SELECT id,employe_id,date,statut,type FROM anomalie_pointage WHERE id=? FOR UPDATE",id);}catch(org.springframework.dao.EmptyResultDataAccessException ex){throw new ResourceNotFoundException("Anomalie",id);}
        if(!"RH".equals(role)&&!"ADMIN".equals(role)){
            boolean allowed;
            if("EMPLOYE".equals(role)) allowed=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?) AND e.id=?)",Boolean.class,email,anomaly.get("employe_id"));
            else allowed=jdbc.queryForObject("WITH RECURSIVE equipe(id) AS (SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?) UNION SELECT af.employe_id FROM affectation af JOIN equipe t ON af.manager_id=t.id WHERE af.date_affectation<=CURRENT_DATE AND (af.date_fin IS NULL OR af.date_fin>=CURRENT_DATE)) SELECT EXISTS(SELECT 1 FROM equipe WHERE id=?)",Boolean.class,email,anomaly.get("employe_id"));
            if(!allowed)throw new AccessDeniedException("Anomalie hors de votre équipe");
        }
        if(!"OUVERTE".equals(anomaly.get("statut")))throw new IllegalStateException("Cette anomalie a déjà été traitée");
        String old="{\"statut\":\"OUVERTE\"}";String next="{\"statut\":\""+statut+"\",\"commentaire\":\""+json(commentaire)+"\"}";
        jdbc.update("UPDATE anomalie_pointage SET statut=?,commentaire_traitement=?,traitee_par_id=?,traitee_le=CURRENT_TIMESTAMP WHERE id=?",statut,commentaire,utilisateurId,id);
        jdbc.update("INSERT INTO journal_audit(utilisateur_id,role,action,entite,entite_id,ancienne_valeur,nouvelle_valeur) VALUES(?,?,'MODIFICATION','anomalie_pointage',?,CAST(? AS jsonb),CAST(? AS jsonb))",utilisateurId,role,id,old,next);
        return jdbc.queryForMap("SELECT id,employe_id AS \"employeId\",date,type,statut,commentaire,commentaire_traitement AS \"commentaireTraitement\",traitee_par_id AS \"traiteeParId\",traitee_le AS \"traiteeLe\" FROM anomalie_pointage WHERE id=?",id);
    }
    private static String json(String text){return text.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");}
}
