package tovo.gestion.rh.embauche.dto;
import tovo.gestion.rh.embauche.entity.ContactUrgence;
public record ContactUrgenceResponse(Long id, String nom, String lienParente, String telephone, boolean principal) {
    public static ContactUrgenceResponse from(ContactUrgence c) { return new ContactUrgenceResponse(c.getId(), c.getNom(), c.getLienParente(), c.getTelephone(), Boolean.TRUE.equals(c.getPrincipal())); }
}
