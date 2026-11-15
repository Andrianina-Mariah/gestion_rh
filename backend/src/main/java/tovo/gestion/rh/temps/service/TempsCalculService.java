package tovo.gestion.rh.temps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import tovo.gestion.rh.temps.event.AbsenceATraiterEvent;

@Service @RequiredArgsConstructor
public class TempsCalculService {
    private static final ZoneId ZONE=ZoneId.of("Indian/Antananarivo");
    private final JdbcTemplate jdbc;
    private final NotificationAnomalieService notifications;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Map<String,Object> recalculer(long employeId,LocalDate date,boolean cloturer){
        jdbc.queryForObject("SELECT id FROM employe WHERE id=? FOR UPDATE",Long.class,employeId);
        Integer periodStatus=jdbc.queryForObject("SELECT COALESCE((SELECT CASE statut WHEN 'OUVERTE' THEN 0 ELSE 1 END FROM periode_temps WHERE annee=? AND mois=?),0)",Integer.class,date.getYear(),date.getMonthValue());
        if(periodStatus!=null&&periodStatus>0)throw new IllegalStateException("La période est verrouillée ; réouverture requise avant recalcul");

        var rules=jdbc.queryForMap("SELECT * FROM regle_temps WHERE id=1");
        LocalTime nightStart=time(rules.get("nuit_debut")),nightEnd=time(rules.get("nuit_fin"));
        int maxDay=((Number)rules.get("duree_max_journaliere_minutes")).intValue();
        List<Map<String,Object>> planned=jdbc.queryForList("SELECT heure_debut,heure_fin,pause_minutes,type FROM planning WHERE employe_id=? AND date=? ORDER BY heure_debut",employeId,date);
        long scheduled=0; int pause=0; LocalDateTime planStart=null,planEnd=null; boolean absence=false;
        for(var p:planned){LocalTime start=time(p.get("heure_debut")),end=time(p.get("heure_fin")); LocalDateTime s=date.atTime(start),e=date.atTime(end);if(!end.isAfter(start))e=e.plusDays(1);int pmin=((Number)p.get("pause_minutes")).intValue();pause+=pmin;
            if(Set.of("CONGE","ABSENCE").contains(p.get("type"))){absence=true;continue;}
            scheduled+=Math.max(0,Duration.between(s,e).toMinutes()-pmin);if(planStart==null||s.isBefore(planStart))planStart=s;if(planEnd==null||e.isAfter(planEnd))planEnd=e;
        }
        if(absence&&scheduled==0){Integer dow=date.getDayOfWeek().getValue();Long theoretical=jdbc.queryForObject("SELECT COALESCE(SUM(GREATEST(0,EXTRACT(EPOCH FROM (h.heure_fin-h.heure_debut+CASE WHEN h.heure_fin<=h.heure_debut THEN INTERVAL '1 day' ELSE INTERVAL '0 day' END))/60-h.pause_minutes)),0) FROM employe_regime er JOIN horaire_type h ON h.regime_travail_id=er.regime_travail_id WHERE er.employe_id=? AND er.date_debut<=? AND (er.date_fin IS NULL OR er.date_fin>=?) AND h.jour_semaine=? AND h.travaille=true",Long.class,employeId,date,date,dow);scheduled=theoretical==null?0:theoretical;}
        OffsetDateTime from=date.minusDays(1).atStartOfDay(ZONE).toOffsetDateTime(),to=date.plusDays(2).atStartOfDay(ZONE).toOffsetDateTime();
        List<Map<String,Object>> events=jdbc.queryForList("SELECT p.id,p.date_heure,p.sens FROM pointage p WHERE p.employe_id=? AND p.date_heure>=? AND p.date_heure<? AND NOT EXISTS(SELECT 1 FROM pointage c WHERE c.pointage_origine_id=p.id) ORDER BY p.date_heure,p.id",employeId,from,to);
        OffsetDateTime open=null,firstArrival=null,lastDeparture=null; List<Interval> intervals=new ArrayList<>();
        for(var event:events){OffsetDateTime at=asOffsetDateTime(event.get("date_heure"));String sens=(String)event.get("sens");if("ENTREE".equals(sens)){if(open==null)open=at;}else if(open!=null){if(open.atZoneSameInstant(ZONE).toLocalDate().equals(date)){intervals.add(new Interval(open,at));if(firstArrival==null||open.isBefore(firstArrival))firstArrival=open;if(lastDeparture==null||at.isAfter(lastDeparture))lastDeparture=at;}open=null;}}
        if(open!=null&&open.atZoneSameInstant(ZONE).toLocalDate().equals(date)&& (firstArrival==null||open.isBefore(firstArrival)))firstArrival=open;
        long actual=intervals.stream().mapToLong(i->Math.max(0,Duration.between(i.start.toInstant(),i.end.toInstant()).toMinutes())).sum();
        if(intervals.size()==1)actual=Math.max(0,actual-pause); // Une seule paire signifie que la pause n'a pas été pointée.
        long late=0,early=0;
        if(firstArrival!=null&&planStart!=null){long raw=Duration.between(planStart.atZone(ZONE).toInstant(),firstArrival.toInstant()).toMinutes();int tolerance=jdbc.queryForObject("SELECT COALESCE((SELECT r.tolerance_retard_min FROM employe_regime er JOIN regime_travail r ON r.id=er.regime_travail_id WHERE er.employe_id=? AND er.date_debut<=? AND (er.date_fin IS NULL OR er.date_fin>=?) ORDER BY er.date_debut DESC LIMIT 1),0)",Integer.class,employeId,date,date);late=Math.max(0,raw-Math.max(0,tolerance));}
        if(lastDeparture!=null&&planEnd!=null)early=Math.max(0,Duration.between(lastDeparture.toInstant(),planEnd.atZone(ZONE).toInstant()).toMinutes());
        long normal=Math.min(actual,scheduled),overtime=Math.max(0,actual-scheduled),night=0,sundayHoliday=0;
        for(Interval i:intervals){night+=overlapMinutes(i.start,i.end,nightStart,nightEnd);sundayHoliday+=sundayHolidayMinutes(i.start,i.end);}
        long approved=jdbc.queryForObject("SELECT COALESCE(SUM(minutes),0) FROM heure_supplementaire WHERE employe_id=? AND date=? AND statut='VALIDEE'",Long.class,employeId,date);
        int approvedMinutes=(int)Math.min(overtime,approved);
        boolean excessive=actual>maxDay;
        if(excessive)anomalie(employeId,date,"DUREE_EXCESSIVE","Durée travaillée supérieure au maximum quotidien configuré");
        if(planned.isEmpty()&&actual>0)anomalie(employeId,date,"HORS_PLANNING","Pointage et temps travaillé sans créneau planifié");
        if(scheduled>0&&actual==0&&firstArrival==null)anomalie(employeId,date,"ENTREE_MANQUANTE","Aucun pointage d'entrée sur une journée planifiée");
        if(cloturer&&open!=null&&open.atZoneSameInstant(ZONE).toLocalDate().equals(date))anomalie(employeId,date,"SORTIE_MANQUANTE","Entrée sans sortie lors de la clôture");
        String status=hasOpenAnomaly(employeId,date)?"EN_ANOMALIE":cloturer?"CLOTUREE":"OUVERTE";
        int absenceMinutes=absence?0:(int)Math.min(Integer.MAX_VALUE,Math.max(0,scheduled-actual));
        if(absenceMinutes>0){long anomalyId=anomalie(employeId,date,"ABSENCE_SANS_CONGE",actual==0?"Aucun pointage sur une journée planifiée, sans congé validé":"Temps planifié non travaillé sans congé validé");boolean newItem=!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM absence_a_qualifier WHERE anomalie_id=?)",Boolean.class,anomalyId));jdbc.update("INSERT INTO absence_a_qualifier(anomalie_id,employe_id,date,minutes) VALUES(?,?,?,?) ON CONFLICT(anomalie_id) DO UPDATE SET minutes=EXCLUDED.minutes WHERE absence_a_qualifier.statut='A_QUALIFIER'",anomalyId,employeId,date,absenceMinutes);if(newItem)eventPublisher.publishEvent(new AbsenceATraiterEvent(anomalyId,employeId,date,absenceMinutes));}
        jdbc.update("INSERT INTO journee_travail(employe_id,date,heure_arrivee,heure_depart,minutes_travaillees,minutes_retard,minutes_depart_anticipe,minutes_sup,minutes_normales,minutes_nuit,minutes_dimanche_ferie,minutes_absence,minutes_sup_validees,statut) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(employe_id,date) DO UPDATE SET heure_arrivee=EXCLUDED.heure_arrivee,heure_depart=EXCLUDED.heure_depart,minutes_travaillees=EXCLUDED.minutes_travaillees,minutes_retard=EXCLUDED.minutes_retard,minutes_depart_anticipe=EXCLUDED.minutes_depart_anticipe,minutes_sup=EXCLUDED.minutes_sup,minutes_normales=EXCLUDED.minutes_normales,minutes_nuit=EXCLUDED.minutes_nuit,minutes_dimanche_ferie=EXCLUDED.minutes_dimanche_ferie,minutes_absence=EXCLUDED.minutes_absence,minutes_sup_validees=EXCLUDED.minutes_sup_validees,statut=EXCLUDED.statut",employeId,date,firstArrival,lastDeparture,actual,late,early,overtime,normal,night,sundayHoliday,absenceMinutes,approvedMinutes,status);
        verifierQuota(employeId,date);
        String finalStatus=hasOpenAnomaly(employeId,date)?"EN_ANOMALIE":cloturer?"CLOTUREE":"OUVERTE";
        if(!finalStatus.equals(status))jdbc.update("UPDATE journee_travail SET statut=? WHERE employe_id=? AND date=?",finalStatus,employeId,date);
        rafraichirVariables(employeId,date);
        Map<String,Object> result=new LinkedHashMap<>();result.put("employeId",employeId);result.put("date",date);result.put("minutesTravaillees",actual);result.put("minutesNormales",normal);result.put("minutesRetard",late);result.put("minutesDepartAnticipe",early);result.put("minutesSupplementaires",overtime);result.put("minutesSupplementairesValidees",approvedMinutes);result.put("minutesNuit",night);result.put("minutesDimancheFerie",sundayHoliday);result.put("minutesAbsenceNonJustifiee",absenceMinutes);result.put("statut",finalStatus);return result;
    }

    @Transactional(readOnly=true)
    public Map<String,Object> recap(long employeId,YearMonth month){
        LocalDate from=month.atDay(1),to=month.atEndOfMonth();
        Map<String,Object> totals=jdbc.queryForMap("SELECT COALESCE(SUM(minutes_travaillees),0) minutes_travaillees,COALESCE(SUM(minutes_normales),0) minutes_normales,COALESCE(SUM(minutes_retard),0) minutes_retard,COALESCE(SUM(minutes_depart_anticipe),0) minutes_depart_anticipe,COALESCE(SUM(minutes_sup),0) minutes_sup,COALESCE(SUM(minutes_nuit),0) minutes_nuit,COALESCE(SUM(minutes_dimanche_ferie),0) minutes_dimanche_ferie,COALESCE(SUM(minutes_absence),0) minutes_absence,COALESCE(SUM(minutes_sup_validees),0) minutes_sup_validees FROM journee_travail WHERE employe_id=? AND date BETWEEN ? AND ?",employeId,from,to);
        totals.put("mois",month.toString());
        totals.put("heuresSupEnAttente",jdbc.queryForObject("SELECT COALESCE(SUM(minutes),0) FROM heure_supplementaire WHERE employe_id=? AND date BETWEEN ? AND ? AND statut='DEMANDEE'",Long.class,employeId,from,to));
        totals.put("heuresSupRefusees",jdbc.queryForObject("SELECT COALESCE(SUM(minutes),0) FROM heure_supplementaire WHERE employe_id=? AND date BETWEEN ? AND ? AND statut='REFUSEE'",Long.class,employeId,from,to));
        totals.put("variables",jdbc.queryForList("SELECT type,COALESCE(SUM(quantite_minutes),0) quantite_minutes FROM variable_paie v JOIN periode_temps p ON p.id=v.periode_id WHERE v.employe_id=? AND p.annee=? AND p.mois=? GROUP BY type ORDER BY type",employeId,month.getYear(),month.getMonthValue()));
        totals.put("jours",jdbc.queryForList("SELECT date,heure_arrivee,heure_depart,minutes_travaillees,minutes_normales,minutes_retard,minutes_depart_anticipe,minutes_sup,minutes_sup_validees,minutes_nuit,minutes_dimanche_ferie,minutes_absence,statut FROM journee_travail WHERE employe_id=? AND date BETWEEN ? AND ? ORDER BY date",employeId,from,to));
        return totals;
    }

    public void verifierQuota(long employeId,LocalDate date){
        var r=jdbc.queryForMap("SELECT quota_sup_hebdo_minutes,quota_sup_annuel_minutes FROM regle_temps WHERE id=1");
        LocalDate monday=date.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Long week=jdbc.queryForObject("SELECT COALESCE(SUM(minutes_sup),0) FROM journee_travail WHERE employe_id=? AND date BETWEEN ? AND ?",Long.class,employeId,monday,monday.plusDays(6));
        LocalDate year=date.withDayOfYear(1);Long annual=jdbc.queryForObject("SELECT COALESCE(SUM(minutes_sup),0) FROM journee_travail WHERE employe_id=? AND date BETWEEN ? AND ?",Long.class,employeId,year,date);
        if(week>((Number)r.get("quota_sup_hebdo_minutes")).longValue())anomalie(employeId,date,"DUREE_EXCESSIVE","Quota hebdomadaire d'heures supplémentaires dépassé");
        if(annual>((Number)r.get("quota_sup_annuel_minutes")).longValue())anomalie(employeId,date,"DUREE_EXCESSIVE","Quota annuel d'heures supplémentaires dépassé");
    }

    public Map<String,Object> regles(){return jdbc.queryForMap("SELECT nuit_debut AS \"nuitDebut\",nuit_fin AS \"nuitFin\",taux_sup_30 AS \"tauxSup30\",taux_sup_50 AS \"tauxSup50\",seuil_sup_50_minutes AS \"seuilSup50Minutes\",quota_sup_hebdo_minutes AS \"quotaSupHebdoMinutes\",quota_sup_annuel_minutes AS \"quotaSupAnnuelMinutes\",duree_max_journaliere_minutes AS \"dureeMaxJournaliereMinutes\" FROM regle_temps WHERE id=1");}

    @Transactional public Map<String,Object> modifierRegles(tovo.gestion.rh.temps.dto.RegleTempsRequest r){
        if(r.nuitDebut().equals(r.nuitFin()))throw new IllegalArgumentException("La plage de nuit doit avoir une durée positive");
        jdbc.update("UPDATE regle_temps SET nuit_debut=?,nuit_fin=?,taux_sup_30=?,taux_sup_50=?,seuil_sup_50_minutes=?,quota_sup_hebdo_minutes=?,quota_sup_annuel_minutes=?,duree_max_journaliere_minutes=? WHERE id=1",r.nuitDebut(),r.nuitFin(),r.tauxSup30(),r.tauxSup50(),r.seuilSup50Minutes(),r.quotaSupHebdoMinutes(),r.quotaSupAnnuelMinutes(),r.dureeMaxJournaliereMinutes());
        return regles();
    }

    private void rafraichirVariables(long emp,LocalDate date){
        int year=date.getYear(),month=date.getMonthValue();
        jdbc.update("INSERT INTO periode_temps(annee,mois) VALUES(?,?) ON CONFLICT(annee,mois) DO NOTHING",year,month);
        List<Long> openPeriods=jdbc.query("SELECT id FROM periode_temps WHERE annee=? AND mois=? AND statut='OUVERTE'",(rs,n)->rs.getLong(1),year,month);
        if(openPeriods.isEmpty())return;
        Long period=openPeriods.getFirst();
        jdbc.update("DELETE FROM variable_paie WHERE employe_id=? AND periode_id=?",emp,period);
        LocalDate from=date.withDayOfMonth(1),to=date.withDayOfMonth(date.lengthOfMonth());
        insertVariable(emp,period,"HEURES_NORMALES","SELECT COALESCE(SUM(minutes_normales),0) FROM journee_travail WHERE employe_id=? AND date BETWEEN ? AND ?",from,to);
        insertVariable(emp,period,"NUIT","SELECT COALESCE(SUM(minutes_nuit),0) FROM journee_travail WHERE employe_id=? AND date BETWEEN ? AND ?",from,to);
        insertVariable(emp,period,"DIMANCHE_FERIE","SELECT COALESCE(SUM(minutes_dimanche_ferie),0) FROM journee_travail WHERE employe_id=? AND date BETWEEN ? AND ?",from,to);
        insertVariable(emp,period,"RETARD","SELECT COALESCE(SUM(minutes_retard),0) FROM journee_travail WHERE employe_id=? AND date BETWEEN ? AND ?",from,to);
        insertVariable(emp,period,"ABSENCE","SELECT COALESCE(SUM(minutes_absence),0) FROM journee_travail WHERE employe_id=? AND date BETWEEN ? AND ?",from,to);
        insertVariable(emp,period,"HEURES_SUP_30","SELECT COALESCE(SUM(minutes),0) FROM heure_supplementaire WHERE employe_id=? AND date BETWEEN ? AND ? AND statut='VALIDEE' AND taux<40",from,to);
        insertVariable(emp,period,"HEURES_SUP_50","SELECT COALESCE(SUM(minutes),0) FROM heure_supplementaire WHERE employe_id=? AND date BETWEEN ? AND ? AND statut='VALIDEE' AND taux>=40",from,to);
    }
    private void insertVariable(long emp,long period,String type,String sql,LocalDate from,LocalDate to){Long q=jdbc.queryForObject(sql,Long.class,emp,from,to);if(q!=null&&q>0)jdbc.update("INSERT INTO variable_paie(employe_id,periode_id,type,quantite_minutes,source) VALUES(?,?,?,?, 'CALCUL_TEMPS')",emp,period,type,q);}
    private boolean hasOpenAnomaly(long emp,LocalDate date){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM anomalie_pointage WHERE employe_id=? AND date=? AND statut='OUVERTE')",Boolean.class,emp,date));}
    private long anomalie(long emp,LocalDate date,String type,String comment){List<Long> existing=jdbc.query("SELECT id FROM anomalie_pointage WHERE employe_id=? AND date=? AND type=? AND commentaire=? ORDER BY id DESC LIMIT 1",(rs,n)->rs.getLong(1),emp,date,type,comment);if(!existing.isEmpty())return existing.getFirst();Long id=jdbc.queryForObject("INSERT INTO anomalie_pointage(employe_id,date,type,statut,commentaire) VALUES(?,?,?,'OUVERTE',?) RETURNING id",Long.class,emp,date,type,comment);notifications.notifier(id,emp,date,type);return id;}
    private boolean hasSpecificAnomaly(long emp,LocalDate date,String text){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM anomalie_pointage WHERE employe_id=? AND date=? AND statut='OUVERTE' AND commentaire=?)",Boolean.class,emp,date,text));}
    private static LocalTime time(Object o){return o instanceof Time t?t.toLocalTime():(LocalTime)o;}
    private long sundayHolidayMinutes(OffsetDateTime start,OffsetDateTime end){long total=0;LocalDate d=start.atZoneSameInstant(ZONE).toLocalDate(),last=end.atZoneSameInstant(ZONE).toLocalDate();for(;!d.isAfter(last);d=d.plusDays(1)){if(d.getDayOfWeek()==DayOfWeek.SUNDAY||Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM jour_ferie WHERE date=?)",Boolean.class,d))){Instant a=d.atStartOfDay(ZONE).toInstant(),b=d.plusDays(1).atStartOfDay(ZONE).toInstant();total+=overlap(start.toInstant(),end.toInstant(),a,b);}}return total;}
    private long overlapMinutes(OffsetDateTime start,OffsetDateTime end,LocalTime nightStart,LocalTime nightEnd){long total=0;LocalDate d=start.atZoneSameInstant(ZONE).toLocalDate().minusDays(1),last=end.atZoneSameInstant(ZONE).toLocalDate();for(;!d.isAfter(last);d=d.plusDays(1)){LocalDateTime a=d.atTime(nightStart),b=d.atTime(nightEnd);if(!nightEnd.isAfter(nightStart))b=b.plusDays(1);total+=overlap(start.toInstant(),end.toInstant(),a.atZone(ZONE).toInstant(),b.atZone(ZONE).toInstant());}return total;}
    private static long overlap(Instant a,Instant b,Instant c,Instant d){Instant s=a.isAfter(c)?a:c,e=b.isBefore(d)?b:d;return e.isAfter(s)?Duration.between(s,e).toMinutes():0;}
    private record Interval(OffsetDateTime start,OffsetDateTime end){}
    private static OffsetDateTime asOffsetDateTime(Object value){
        if(value instanceof OffsetDateTime dateTime)return dateTime;
        if(value instanceof Timestamp timestamp)return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        if(value instanceof LocalDateTime dateTime)return dateTime.atZone(ZONE).toOffsetDateTime();
        if(value instanceof Instant instant)return instant.atOffset(ZoneOffset.UTC);
        throw new IllegalStateException("Type d'horodatage de pointage non pris en charge : "+(value==null?"null":value.getClass().getName()));
    }
}
