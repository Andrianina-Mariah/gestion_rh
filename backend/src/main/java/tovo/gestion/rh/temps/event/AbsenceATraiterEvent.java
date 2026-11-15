package tovo.gestion.rh.temps.event;

import java.time.LocalDate;

public record AbsenceATraiterEvent(Long anomalieId, Long employeId, LocalDate date, int minutes) {}
