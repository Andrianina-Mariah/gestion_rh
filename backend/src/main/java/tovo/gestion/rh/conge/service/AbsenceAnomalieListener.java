package tovo.gestion.rh.conge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import tovo.gestion.rh.temps.event.AbsenceATraiterEvent;
import tovo.gestion.rh.temps.service.TempsPlanningService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component @RequiredArgsConstructor
public class AbsenceAnomalieListener {
    private final JdbcTemplate jdbc;
    private final TempsPlanningService planning;

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void absenceATraiter(AbsenceATraiterEvent event){creer(event.anomalieId(),event.employeId(),event.date(),event.minutes());}

    @Scheduled(cron="0 5 2 * * *",zone="Indian/Antananarivo")
    @Transactional public void rattraperAbsencesAnomalies(){List<Map<String,Object>> rows=jdbc.queryForList("SELECT q.anomalie_id,q.employe_id,q.date,q.minutes FROM absence_a_qualifier q WHERE q.statut='A_QUALIFIER' AND NOT EXISTS(SELECT 1 FROM absence a WHERE a.anomalie_id=q.anomalie_id) AND NOT EXISTS(SELECT 1 FROM periode_temps p WHERE p.annee=EXTRACT(YEAR FROM q.date)::int AND p.mois=EXTRACT(MONTH FROM q.date)::int AND p.statut<>'OUVERTE')");for(var r:rows)creer(((Number)r.get("anomalie_id")).longValue(),((Number)r.get("employe_id")).longValue(),asDate(r.get("date")),((Number)r.get("minutes")).intValue());}

    private void creer(long anomaly,long employee,LocalDate date,int minutes){int delay=jdbc.queryForObject("SELECT delai_justificatif_jours FROM politique_absence WHERE id=1",Integer.class);int inserted=jdbc.update("INSERT INTO absence(employe_id,type,date_debut,date_fin,statut,source,commentaire,anomalie_id,date_limite_justificatif) VALUES(?,'AUTRE',?,?,'CONSTATEE','POINTAGE',?,?,?) ON CONFLICT(anomalie_id) WHERE anomalie_id IS NOT NULL DO NOTHING",employee,date,date,"Anomalie de pointage #"+anomaly+" : "+minutes+" minutes à qualifier",anomaly,date.plusDays(delay));if(inserted>0)planning.integrerAbsence(employee,date,date,"ABSENCE");}
    private LocalDate asDate(Object value){return value instanceof LocalDate d?d:((java.sql.Date)value).toLocalDate();}
}
