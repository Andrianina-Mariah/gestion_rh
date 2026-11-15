package tovo.gestion.rh.embauche.dto;
import tovo.gestion.rh.embauche.entity.EmployeContact;
import java.time.LocalDate;
public record ContactResponse(Long id, String type, String valeur, boolean principal, LocalDate dateDebut, LocalDate dateFin) {
    public static ContactResponse from(EmployeContact c) { return new ContactResponse(c.getId(), c.getType(), c.getValeur(), Boolean.TRUE.equals(c.getPrincipal()), c.getDateDebut(), c.getDateFin()); }
}
