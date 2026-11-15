package tovo.gestion.rh.temps.event;

import java.time.Instant;

public record PeriodeTempsValideeEvent(Long periodeId, int annee, int mois, Integer valideeParId, Instant dateValidation) {}
