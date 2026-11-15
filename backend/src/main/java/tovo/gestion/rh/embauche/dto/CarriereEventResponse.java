package tovo.gestion.rh.embauche.dto;

import java.time.LocalDate;

public record CarriereEventResponse(String type, LocalDate dateDebut, LocalDate dateFin,
                                    String libelle, String details) {}
