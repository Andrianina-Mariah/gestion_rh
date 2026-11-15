package tovo.gestion.rh.temps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class RappelAnomaliePlanifie {
    private final NotificationAnomalieService notifications;
    @Scheduled(cron="${app.time.anomaly-reminder-cron:0 0 9 * * *}",zone="Indian/Antananarivo")
    public void rappeler(){notifications.rappelerAnomalies();}
}
