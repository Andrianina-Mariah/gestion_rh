package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.TypeDocument;
import java.util.Optional;
import java.util.List;

public interface TypeDocumentRepository extends JpaRepository<TypeDocument, Long> {
    Optional<TypeDocument> findByCode(String code);
    List<TypeDocument> findByObligatoireTrue();
}
