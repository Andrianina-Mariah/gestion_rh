package tovo.gestion.rh.workflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.workflow.entity.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByDestinataireIdOrderByLueAscDateCreationDesc(Integer destinataireId);

    long countByDestinataireIdAndLueFalse(Integer destinataireId);
}
