package br.unifor.healthsys.triage.messaging;

import br.unifor.healthsys.triage.model.TriageEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class TriageEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TriageEventProducer.class);
    private static final String TRIAGE_TOPIC = "healthsys.triage.events";
    private static final String NOTIFICATION_TOPIC = "healthsys.notifications";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TriageEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTriageEvent(TriageEntry entry) {
        Map<String, Object> event = buildTriageEvent(entry);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TRIAGE_TOPIC, entry.getPatientId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Falha ao publicar evento de triagem para paciente {}: {}",
                        entry.getPatientId(), ex.getMessage());
            } else {
                log.info("Evento de triagem publicado - paciente: {} | classificacao: {}",
                        entry.getPatientId(), entry.getRiskClassification());
            }
        });

        // Notificacao de emergencia para classificacoes criticas
        if (entry.getRiskClassification() == TriageEntry.RiskClassification.VERMELHO
                || entry.getRiskClassification() == TriageEntry.RiskClassification.LARANJA) {
            publishEmergencyNotification(entry);
        }
    }

    private void publishEmergencyNotification(TriageEntry entry) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "EMERGENCY_ALERT");
        notification.put("patientId", entry.getPatientId());
        notification.put("patientName", entry.getPatientName());
        notification.put("classification", entry.getRiskClassification().name());
        notification.put("classificationDesc", entry.getRiskClassification().getDescricao());
        notification.put("triageId", entry.getId());
        notification.put("message", String.format("ATENCAO: Paciente %s classificado como %s - %s",
                entry.getPatientName(),
                entry.getRiskClassification().name(),
                entry.getRiskClassification().getDescricao()));

        kafkaTemplate.send(NOTIFICATION_TOPIC, "emergency", notification);
        log.warn("Alerta de emergencia publicado para paciente: {}", entry.getPatientName());
    }

    private Map<String, Object> buildTriageEvent(TriageEntry entry) {
        Map<String, Object> event = new HashMap<>();
        event.put("triageId", entry.getId());
        event.put("patientId", entry.getPatientId());
        event.put("patientName", entry.getPatientName());
        event.put("riskClassification", entry.getRiskClassification().name());
        event.put("chiefComplaint", entry.getChiefComplaint());
        event.put("observations", entry.getObservations());
        event.put("vitalSigns", entry.getVitalSigns());
        event.put("triageDate", entry.getTriageDate().toString());
        return event;
    }
}
