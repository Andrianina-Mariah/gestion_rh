package tovo.gestion.rh.utilisateur.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UtilisateurResponse {
    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private Boolean actif;
    private Boolean changementMotDePasseRequis;
}
