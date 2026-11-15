package tovo.gestion.rh.embauche.dto;
import tovo.gestion.rh.embauche.entity.EmployeAdresse;
import java.time.LocalDate;
public record AdresseResponse(Long id, String type, String adresse, String ville, LocalDate dateDebut, LocalDate dateFin) {
    public static AdresseResponse from(EmployeAdresse a) { return new AdresseResponse(a.getId(), a.getType(), a.getAdresse(), a.getVille(), a.getDateDebut(), a.getDateFin()); }
}
