package tovo.gestion.rh.temps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.temps.dto.DemandeHeureSupRequest;

import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor
public class HeureSupService {
    private final JdbcTemplate jdbc;
    private final TempsPlanningService access;
    private final TempsCalculService calcul;

    @Transactional public Map<String,Object> demander(String email,boolean privileged,boolean manager,DemandeHeureSupRequest r){
        long id;
        if(r.employeId()==null){id=jdbc.queryForObject("SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?)",Long.class,email);}
        else {id=r.employeId();if(!privileged)access.verifierAccesEmploye(id,email,manager);}
        calcul.recalculer(id,r.date(),false);
        Integer disponible=jdbc.queryForObject("SELECT COALESCE((SELECT minutes_sup FROM journee_travail WHERE employe_id=? AND date=?),0)",Integer.class,id,r.date());
        Integer deja=jdbc.queryForObject("SELECT COALESCE(SUM(minutes),0) FROM heure_supplementaire WHERE employe_id=? AND date=? AND statut IN ('DEMANDEE','VALIDEE')",Integer.class,id,r.date());
        if(r.minutes()>disponible-deja)throw new IllegalArgumentException("La demande dépasse les minutes supplémentaires constatées et encore disponibles");
        var rules=jdbc.queryForMap("SELECT taux_sup_30,taux_sup_50,seuil_sup_50_minutes FROM regle_temps WHERE id=1");
        LocalDate monday=r.date().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        Integer semaine=jdbc.queryForObject("SELECT COALESCE(SUM(minutes),0) FROM heure_supplementaire WHERE employe_id=? AND date BETWEEN ? AND ? AND statut IN ('DEMANDEE','VALIDEE')",Integer.class,id,monday,monday.plusDays(6));
        BigDecimalHolder rate=new BigDecimalHolder((semaine+r.minutes())>((Number)rules.get("seuil_sup_50_minutes")).intValue()?((Number)rules.get("taux_sup_50")).doubleValue():((Number)rules.get("taux_sup_30")).doubleValue());
        Long requestId=jdbc.queryForObject("INSERT INTO heure_supplementaire(employe_id,date,minutes,taux,statut,motif) VALUES(?,?,?,?,'DEMANDEE',?) RETURNING id",Long.class,id,r.date(),r.minutes(),rate.value,r.motif());
        calcul.verifierQuota(id,r.date());
        return jdbc.queryForMap("SELECT id,employe_id AS \"employeId\",date,minutes,taux,statut,motif FROM heure_supplementaire WHERE id=?",requestId);
    }

    @Transactional public Map<String,Object> decider(long requestId,String email,Integer utilisateurId,boolean manager,boolean privileged,boolean approuver){
        Map<String,Object> request=jdbc.queryForMap("SELECT id,employe_id,date,minutes,statut FROM heure_supplementaire WHERE id=? FOR UPDATE",requestId);
        long employe=((Number)request.get("employe_id")).longValue();if(!privileged)access.verifierAccesEmploye(employe,email,manager);
        if(!"DEMANDEE".equals(request.get("statut")))throw new IllegalStateException("Cette demande a déjà été traitée");
        LocalDate date=((java.sql.Date)request.get("date")).toLocalDate();
        if(approuver){calcul.recalculer(employe,date,false);Integer total=jdbc.queryForObject("SELECT minutes_sup FROM journee_travail WHERE employe_id=? AND date=?",Integer.class,employe,date);Integer already=jdbc.queryForObject("SELECT COALESCE(SUM(minutes),0) FROM heure_supplementaire WHERE employe_id=? AND date=? AND statut='VALIDEE'",Integer.class,employe,date);Integer requested=((Number)request.get("minutes")).intValue();if(total==null||requested>total-already)throw new IllegalStateException("Le temps supplémentaire constaté ne couvre plus cette demande");}
        jdbc.update("UPDATE heure_supplementaire SET statut=?,valide_par_id=?,motif=COALESCE(motif,'') WHERE id=?",approuver?"VALIDEE":"REFUSEE",utilisateurId,requestId);
        calcul.recalculer(employe,date,false);calcul.verifierQuota(employe,date);
        return jdbc.queryForMap("SELECT id,employe_id AS \"employeId\",date,minutes,taux,statut,valide_par_id AS \"valideParId\",motif FROM heure_supplementaire WHERE id=?",requestId);
    }
    public List<Map<String,Object>> rechercher(String email,boolean manager,boolean privileged,Long employeId,LocalDate date,String statut){
        Long id=employeId;
        boolean equipe=manager&&!privileged&&id==null;
        if(id==null&&!privileged&&!equipe)id=jdbc.queryForObject("SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?)",Long.class,email);
        if(id!=null&&!privileged)access.verifierAccesEmploye(id,email,manager);
        if(statut!=null&&!Set.of("DEMANDEE","VALIDEE","REFUSEE").contains(statut))throw new IllegalArgumentException("Statut de demande invalide");
        String sql="SELECT h.id,h.employe_id AS \"employeId\",h.date,h.minutes,h.taux,h.statut,h.valide_par_id AS \"valideParId\",h.motif FROM heure_supplementaire h WHERE (?::bigint IS NULL OR h.employe_id=?) AND (?::date IS NULL OR h.date=?) AND (?::varchar IS NULL OR h.statut=?)";
        if(equipe)sql="WITH RECURSIVE equipe(id) AS (SELECT e.id FROM employe e JOIN candidat c ON c.id=e.candidat_id WHERE lower(c.email)=lower(?) UNION SELECT a.employe_id FROM affectation a JOIN equipe t ON a.manager_id=t.id WHERE a.date_affectation<=CURRENT_DATE AND (a.date_fin IS NULL OR a.date_fin>=CURRENT_DATE)) "+sql+" AND h.employe_id IN (SELECT id FROM equipe)";
        sql+=" ORDER BY h.date DESC,h.id DESC";
        return equipe?jdbc.queryForList(sql,email,id,id,date,date,statut,statut):jdbc.queryForList(sql,id,id,date,date,statut,statut);
    }
    public Integer utilisateurId(String email){return jdbc.queryForObject("SELECT id FROM utilisateur WHERE lower(email)=lower(?)",Integer.class,email);}
    private static final class BigDecimalHolder {final java.math.BigDecimal value;BigDecimalHolder(double n){value=java.math.BigDecimal.valueOf(n);}}
}
