package tovo.gestion.rh.utilisateur.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Integer id;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private Boolean changementMotDePasseRequis;
}
