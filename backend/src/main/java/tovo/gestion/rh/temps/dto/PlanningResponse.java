package tovo.gestion.rh.temps.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record PlanningResponse(Long id, Long employeId, LocalDate date, LocalTime heureDebut,
        LocalTime heureFin, String type, String source, String motif) {}
