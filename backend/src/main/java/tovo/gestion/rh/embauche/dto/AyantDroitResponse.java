package tovo.gestion.rh.embauche.dto;
import tovo.gestion.rh.embauche.entity.AyantDroit;
import java.time.LocalDate;
public record AyantDroitResponse(Long id, String nom, String lien, LocalDate dateNaissance) {
    public static AyantDroitResponse from(AyantDroit a) { return new AyantDroitResponse(a.getId(), a.getNom(), a.getLien(), a.getDateNaissance()); }
}
