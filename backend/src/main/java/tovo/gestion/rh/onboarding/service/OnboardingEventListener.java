package tovo.gestion.rh.onboarding.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tovo.gestion.rh.workflow.event.EmployeCreeEvent;

@Component
@RequiredArgsConstructor
public class OnboardingEventListener {
    private final OnboardingService onboardingService;

    @EventListener
    public void onEmployeCree(EmployeCreeEvent event) {
        onboardingService.creerDepuisEmploye(event);
    }
}
