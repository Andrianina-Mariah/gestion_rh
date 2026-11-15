package tovo.gestion.rh.candidature.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class CandidatureFileStorage {
    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final Set<String> CV_TYPES = Set.of("application/pdf");
    private static final Set<String> LETTER_TYPES = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final Path root;

    public CandidatureFileStorage(@Value("${application.storage.candidatures-dir:uploads/candidatures}") String directory) {
        root = Paths.get(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, boolean cv) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Chaque fichier ne doit pas dépasser 5 Mo");
        }
        Set<String> accepted = cv ? CV_TYPES : LETTER_TYPES;
        if (!accepted.contains(file.getContentType())) {
            throw new IllegalArgumentException(cv
                    ? "Le CV doit être au format PDF"
                    : "La lettre doit être au format PDF, DOC ou DOCX");
        }
        String filename = UUID.randomUUID() + extensionFor(file.getContentType());
        try {
            Files.createDirectories(root);
            Path destination = root.resolve(filename).normalize();
            if (!destination.getParent().equals(root)) {
                throw new IllegalArgumentException("Nom de fichier invalide");
            }
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/api/candidatures/files/" + filename;
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'enregistrer le fichier", ex);
        }
    }

    public Resource load(String filename) {
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }
        try {
            Resource resource = new UrlResource(root.resolve(filename).normalize().toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Fichier introuvable");
            }
            return resource;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Fichier introuvable", ex);
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            default -> "";
        };
    }
}
