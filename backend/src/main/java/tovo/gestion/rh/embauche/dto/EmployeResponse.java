package tovo.gestion.rh.embauche.dto;

import tovo.gestion.rh.embauche.entity.Employe;

import java.time.LocalDate;

public record EmployeResponse(
        Long id,
        Long candidatId,
        String matricule,
        String nom,
        String prenom,
        String email,
        String telephone,
        String statut,
        LocalDate dateEmbauche,
        LocalDate dateNaissance,
        String lieuNaissance,
        String sexe,
        String nationalite,
        String cin,
        String numeroCnaps,
        String situationFamiliale,
        Integer nombreEnfants,
        LocalDate dateSortie,
        String motifSortie,
        String poste,
        String departement,
        AffectationResponse affectationCourante,
        ContratResponse contratCourant
) {
    public static EmployeResponse from(Employe employe,
                                      AffectationResponse affectation,
                                      ContratResponse contrat) {
        return new EmployeResponse(
                employe.getId(),
                employe.getCandidat() != null ? employe.getCandidat().getId() : null,
                employe.getMatricule(),
                employe.getCandidat() != null ? employe.getCandidat().getNom() : null,
                employe.getCandidat() != null ? employe.getCandidat().getPrenom() : null,
                employe.getCandidat() != null ? employe.getCandidat().getEmail() : null,
                employe.getCandidat() != null ? employe.getCandidat().getTelephone() : null,
                employe.getStatut() == null ? null : employe.getStatut().name(),
                employe.getDateEmbauche(),
                employe.getDateNaissance(),
                employe.getLieuNaissance(),
                employe.getSexe(),
                employe.getNationalite(),
                employe.getCin(),
                employe.getNumeroCnaps(),
                employe.getSituationFamiliale(),
                employe.getNombreEnfants(),
                employe.getDateSortie(),
                employe.getMotifSortie(),
                affectation != null ? affectation.poste() : null,
                affectation != null ? affectation.departement() : null,
                affectation,
                contrat
        );
    }
}
