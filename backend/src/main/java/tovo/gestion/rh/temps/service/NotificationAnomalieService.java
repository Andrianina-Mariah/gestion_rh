package tovo.gestion.rh.temps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor
public class NotificationAnomalieService {
    private final JdbcTemplate jdbc;

    @Transactional public void notifier(long anomalyId,long employeId,LocalDate date,String type){
        List<Integer> recipients=jdbc.queryForList("SELECT DISTINCT u.id FROM utilisateur u JOIN candidat c ON lower(c.email)=lower(u.email) JOIN employe e ON e.candidat_id=c.id WHERE e.id=? UNION SELECT DISTINCT u.id FROM affectation a JOIN employe manager ON manager.id=a.manager_id JOIN candidat c ON c.id=manager.candidat_id JOIN utilisateur u ON lower(u.email)=lower(c.email) WHERE a.employe_id=? AND a.date_affectation<=? AND (a.date_fin IS NULL OR a.date_fin>=?)",Integer.class,employeId,employeId,date,date);
        String message="Anomalie de pointage "+type+" pour le "+date+" (employé "+employeId+")";
        for(Integer recipient:recipients)jdbc.update("INSERT INTO notification(destinataire_id,message,lien) VALUES(?,?,?)",recipient,message,"/anomalies/"+anomalyId);
    }

    @Transactional public int rappelerAnomalies(){
        Integer jours=jdbc.queryForObject("SELECT rappel_anomalie_jours FROM regle_temps WHERE id=1",Integer.class);
        LocalDate dateRappel=LocalDate.now(java.time.ZoneId.of("Indian/Antananarivo"));
        var open=jdbc.queryForList("SELECT id,employe_id,date,type FROM anomalie_pointage WHERE statut='OUVERTE' AND date<=? ORDER BY date",dateRappel.minusDays(jours));
        int count=0;
        for(var row:open){long id=((Number)row.get("id")).longValue(),emp=((Number)row.get("employe_id")).longValue();LocalDate date=((java.sql.Date)row.get("date")).toLocalDate();String type=(String)row.get("type");
            if(jdbc.update("INSERT INTO anomalie_rappel(anomalie_id,date_rappel) VALUES(?,?) ON CONFLICT(anomalie_id,date_rappel) DO NOTHING",id,dateRappel)==1){notifier(id,emp,date,type);count++;}}
        return count;
    }
}
