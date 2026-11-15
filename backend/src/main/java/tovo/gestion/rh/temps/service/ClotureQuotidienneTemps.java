package tovo.gestion.rh.temps.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;

@Component @RequiredArgsConstructor @Slf4j
public class ClotureQuotidienneTemps {
    private static final ZoneId ZONE=ZoneId.of("Indian/Antananarivo");
    private final JdbcTemplate jdbc;
    private final TempsCalculService calcul;

    @Scheduled(cron="${app.time.daily-close-cron:0 10 0 * * *}",zone="Indian/Antananarivo")
    public void cloturerVeille(){
        LocalDate yesterday=LocalDate.now(ZONE).minusDays(1);
        List<Long> ids=jdbc.queryForList("SELECT employe_id FROM journee_travail WHERE date=? UNION SELECT employe_id FROM planning WHERE date=?",Long.class,yesterday,yesterday);
        for(Long id:ids){try{calcul.recalculer(id,yesterday,true);}catch(RuntimeException ex){log.warn("Échec de clôture de la journée {} pour l'employé {} : {}",yesterday,id,ex.getMessage());}}
    }
}
