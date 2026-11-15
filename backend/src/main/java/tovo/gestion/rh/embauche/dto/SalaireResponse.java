package tovo.gestion.rh.embauche.dto;

import tovo.gestion.rh.embauche.entity.HistoriqueSalaire;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaireResponse(Long id, BigDecimal montant, String devise, LocalDate dateDebut,
                              LocalDate dateFin, String motif, Long contratId) {
    public static SalaireResponse from(HistoriqueSalaire s) {
        return new SalaireResponse(s.getId(), s.getMontant(), s.getDevise(), s.getDateDebut(),
                s.getDateFin(), s.getMotif(), s.getContrat() == null ? null : s.getContrat().getId());
    }
}
