package tovo.gestion.rh.conge.event;
import java.time.LocalDate;
public record CongeAnnuleEvent(Long demandeId, Long employeId, LocalDate dateDebut, LocalDate dateFin) {}
