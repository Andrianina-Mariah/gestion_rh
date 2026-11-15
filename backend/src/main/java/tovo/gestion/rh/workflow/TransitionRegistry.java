package tovo.gestion.rh.workflow;

import lombok.Getter;
import org.springframework.stereotype.Component;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;
import tovo.gestion.rh.utilisateur.entity.Role;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TransitionRegistry {

    private final Map<TransitionKey, StatutDemande> transitions = Map.of(
            new TransitionKey(StatutDemande.EN_ATTENTE_RH, EtapeValidation.RH, DecisionValidation.DEMANDE_ANALYSE),
            StatutDemande.ANALYSE_FINANCE,
            new TransitionKey(StatutDemande.EN_ATTENTE_RH, EtapeValidation.RH, DecisionValidation.ACCEPTE),
            StatutDemande.EN_ATTENTE_DIRECTION,
            new TransitionKey(StatutDemande.EN_ATTENTE_RH, EtapeValidation.RH, DecisionValidation.A_CORRIGER),
            StatutDemande.A_CORRIGER,
            new TransitionKey(StatutDemande.EN_ATTENTE_RH, EtapeValidation.RH, DecisionValidation.REJETE),
            StatutDemande.REJETE,
            new TransitionKey(StatutDemande.ANALYSE_FINANCE, EtapeValidation.FINANCE, DecisionValidation.ACCEPTE),
            StatutDemande.EN_ATTENTE_DIRECTION,
            new TransitionKey(StatutDemande.ANALYSE_FINANCE, EtapeValidation.FINANCE, DecisionValidation.A_CORRIGER),
            StatutDemande.A_CORRIGER,
            new TransitionKey(StatutDemande.ANALYSE_FINANCE, EtapeValidation.FINANCE, DecisionValidation.REJETE),
            StatutDemande.REJETE,
            new TransitionKey(StatutDemande.EN_ATTENTE_DIRECTION, EtapeValidation.DIRECTION, DecisionValidation.ACCEPTE),
            StatutDemande.VALIDE,
            new TransitionKey(StatutDemande.EN_ATTENTE_DIRECTION, EtapeValidation.DIRECTION, DecisionValidation.A_CORRIGER),
            StatutDemande.A_CORRIGER,
            new TransitionKey(StatutDemande.EN_ATTENTE_DIRECTION, EtapeValidation.DIRECTION, DecisionValidation.REJETE),
            StatutDemande.REJETE
    );

    private final Map<StatutDemande, EtapeValidation> etapesParStatut = Map.of(
            StatutDemande.EN_ATTENTE_RH, EtapeValidation.RH,
            StatutDemande.ANALYSE_FINANCE, EtapeValidation.FINANCE,
            StatutDemande.EN_ATTENTE_DIRECTION, EtapeValidation.DIRECTION
    );

    public StatutDemande statutSuivant(
            StatutDemande statutCourant,
            String role,
            DecisionValidation decision
    ) {
        EtapeValidation etape = etapePour(statutCourant)
                .orElseThrow(() -> new TransitionInterditeException(
                        "La demande n'est pas dans une étape de validation"
                ));

        if (!etape.name().equals(role)) {
            throw new TransitionInterditeException(
                    "Le rôle " + role + " ne peut pas traiter l'étape " + etape
            );
        }

        return Optional.ofNullable(transitions.get(new TransitionKey(statutCourant, etape, decision)))
                .orElseThrow(() -> new TransitionInterditeException(
                        "La décision " + decision + " est interdite depuis le statut " + statutCourant
                ));
    }

    public EtapeValidation etapePourRole(String role) {
        return Set.of(EtapeValidation.values()).stream()
                .filter(etape -> etape.name().equals(role))
                .findFirst()
                .orElseThrow(() -> new TransitionInterditeException(
                        "Le rôle " + role + " n'est pas habilité à valider une demande"
                ));
    }

    public Optional<EtapeValidation> etapePour(StatutDemande statut) {
        return Optional.ofNullable(etapesParStatut.get(statut));
    }

    public record TransitionKey(
            StatutDemande statutCourant,
            EtapeValidation etape,
            DecisionValidation decision
    ) {
    }
}
