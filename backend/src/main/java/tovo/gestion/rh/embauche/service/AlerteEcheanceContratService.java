package tovo.gestion.rh.embauche.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlerteEcheanceContratService {
    private final ContratService contratService;

    @Scheduled(cron = "${application.contracts.expiry-alert-cron:0 0 8 * * *}")
    public void signalerEcheancesProches() {
        int jours = 30;
        int nombre = contratService.findEcheances(jours).size();
        if (nombre > 0) {
            log.warn("{} contrat(s) valide(s) arrivent à échéance dans {} jours", nombre, jours);
        }
    }
}
