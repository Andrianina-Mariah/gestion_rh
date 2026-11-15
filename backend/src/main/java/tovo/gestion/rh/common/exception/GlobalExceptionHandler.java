package tovo.gestion.rh.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import tovo.gestion.rh.workflow.TransitionInterditeException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "Erreur de validation", errors.toString(), LocalDateTime.now()
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                404, "Ressource introuvable", ex.getMessage(), LocalDateTime.now()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
                403, "Accès refusé", ex.getMessage(), LocalDateTime.now()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "Requête invalide", ex.getMessage(), LocalDateTime.now()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                409, "Conflit", ex.getMessage(), LocalDateTime.now()
        ));
    }

    @ExceptionHandler(TransitionInterditeException.class)
    public ResponseEntity<ErrorResponse> handleTransitionInterdite(TransitionInterditeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                409, "Transition interdite", ex.getMessage(), LocalDateTime.now()
        ));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                409,
                "Conflit de concurrence",
                "La demande a été modifiée par un autre utilisateur. Rechargez-la avant de réessayer.",
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                500, "Erreur interne du serveur", ex.getMessage(), LocalDateTime.now()
        ));
    }

    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {}
}