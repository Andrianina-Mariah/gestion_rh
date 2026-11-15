package tovo.gestion.rh.embauche.dto;

import tovo.gestion.rh.embauche.entity.*;
import tovo.gestion.rh.onboarding.dto.DocumentResponse;
import java.util.List;

public record DossierEmployeResponse(
        EmployeResponse identite, String anciennete, List<AdresseResponse> adresses, List<ContactResponse> contacts,
        List<ContactUrgenceResponse> contactsUrgence, List<AyantDroitResponse> ayantsDroit,
        List<ContratResponse> contrats, List<AffectationResponse> affectations,
        List<DocumentResponse> documents, List<SalaireResponse> salaires, SalaireResponse salaireCourant,
        List<DemandeModificationResponse> demandesModification
) {}
