package tovo.gestion.rh.workflow.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Integer id;
    private String message;
    private String lien;
    private Boolean lue;
    private LocalDateTime dateCreation;
}
