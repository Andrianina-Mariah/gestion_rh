package tovo.gestion.rh.onboarding.dto;

import java.time.LocalDate;

public record DocumentAlerteResponse(Long documentId, Long employeId, String type,
                                     String statut, LocalDate dateExpiration, String raison) {}
