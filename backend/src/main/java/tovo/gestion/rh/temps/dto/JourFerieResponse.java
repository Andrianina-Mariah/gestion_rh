package tovo.gestion.rh.temps.dto;

import java.time.LocalDate;

public record JourFerieResponse(Long id, LocalDate date, String libelle, boolean paye) {}
