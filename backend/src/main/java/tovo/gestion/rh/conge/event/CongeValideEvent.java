package tovo.gestion.rh.conge.event;
import java.time.LocalDate;
public record CongeValideEvent(Long demandeId, Long employeId, Long typeCongeId, LocalDate dateDebut, LocalDate dateFin, java.math.BigDecimal jours) {}
