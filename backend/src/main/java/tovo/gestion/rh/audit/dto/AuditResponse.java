package tovo.gestion.rh.audit.dto;

import java.time.LocalDateTime;

public record AuditResponse(Long id, LocalDateTime dateHeure, Integer utilisateurId,
                            String role, String action, String entite, Long entiteId,
                            String ancienneValeur, String nouvelleValeur, String adresseIp) {}
