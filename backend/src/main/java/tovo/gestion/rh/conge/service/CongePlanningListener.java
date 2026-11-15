package tovo.gestion.rh.conge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import tovo.gestion.rh.conge.event.*;
import java.time.LocalDate;

@Component @RequiredArgsConstructor
public class CongePlanningListener {
    private final JdbcTemplate jdbc;

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void congeValide(CongeValideEvent event){
        for(LocalDate d=event.dateDebut();!d.isAfter(event.dateFin());d=d.plusDays(1)){
            String marker="Demande de congé #"+event.demandeId();
            int changed=jdbc.update("UPDATE planning SET type_avant_conge=type,source_avant_conge=source,motif_avant_conge=motif,type='CONGE',source='CONGE',motif=? WHERE employe_id=? AND date=? AND type<>'CONGE'",marker,event.employeId(),d);
            Integer rows=jdbc.queryForObject("SELECT COUNT(*) FROM planning WHERE employe_id=? AND date=?",Integer.class,event.employeId(),d);
            if(changed==0&&(rows==null||rows==0))jdbc.update("INSERT INTO planning(employe_id,date,heure_debut,heure_fin,type,source,motif,genere_pour_conge) VALUES(?,?,TIME '00:00',TIME '23:59','CONGE','CONGE',?,TRUE)",event.employeId(),d,marker);
        }
    }
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void congeAnnule(CongeAnnuleEvent event){
        for(LocalDate d=event.dateDebut();!d.isAfter(event.dateFin());d=d.plusDays(1)){
            String marker="Demande de congé #"+event.demandeId();
            jdbc.update("DELETE FROM planning WHERE employe_id=? AND date=? AND type='CONGE' AND motif=? AND genere_pour_conge=true",event.employeId(),d,marker);
            jdbc.update("UPDATE planning SET type=type_avant_conge,source=source_avant_conge,motif=motif_avant_conge,type_avant_conge=NULL,source_avant_conge=NULL,motif_avant_conge=NULL WHERE employe_id=? AND date=? AND type='CONGE' AND motif=?",event.employeId(),d,marker);
        }
    }
}
