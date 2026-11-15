package tovo.gestion.rh.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object id) {
        super(String.format("%s introuvable avec l'id : %s", resourceName, id));
    }
}