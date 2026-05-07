package br.unifor.healthsys.triage.messaging;

import br.unifor.healthsys.triage.model.TriageEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class TriageEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TriageEventProducer.class);
    private static final String TRIAGE_TOPIC = "triagem-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TriageEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTriageEvent(TriageEntry entry) {
        Map<String, Object> event = buildTriageEvent(entry);
        sendAndAwait(entry.getCorrelationId(), event,
                "Evento de triagem publicado - correlationId: %s | classificacao: %s"
                        .formatted(entry.getCorrelationId(), entry.getRiskClassification()));
    }

    public void publishAttendanceStarted(TriageEntry entry, Long medicoId, String medicoName) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "ATENDIMENTO_INICIADO");
        event.put("eventId", UUID.randomUUID().toString());
        event.put("correlationId", entry.getCorrelationId());
        event.put("triageId", entry.getId());
        event.put("patientId", entry.getPatientId());
        event.put("patientName", entry.getPatientName());
        event.put("medicoId", medicoId);
        event.put("medicoName", medicoName);
        event.put("startedAt", java.time.LocalDateTime.now().toString());
        event.put("version", 1);
        sendAndAwait(entry.getCorrelationId(), event,
                "Evento ATENDIMENTO_INICIADO publicado - correlationId: %s | triageId: %s | medico: %s"
                        .formatted(entry.getCorrelationId(), entry.getId(), medicoName));
    }

    public void publishPatientForwarded(Long patientId, String patientName, Long forwardedById, String forwardedByName) {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Object> event = new HashMap<>();
        event.put("type", "PACIENTE_ENCAMINHADO");
        event.put("eventId", UUID.randomUUID().toString());
        event.put("correlationId", correlationId);
        event.put("patientId", patientId);
        event.put("patientName", patientName);
        event.put("forwardedById", forwardedById);
        event.put("forwardedByName", forwardedByName);
        event.put("forwardedAt", java.time.LocalDateTime.now().toString());
        event.put("version", 1);
        sendAndAwait(correlationId, event,
                "Evento PACIENTE_ENCAMINHADO publicado - correlationId: %s | patientId: %s | por: %s"
                        .formatted(correlationId, patientId, forwardedByName));
    }

    private Map<String, Object> buildTriageEvent(TriageEntry entry) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "TRIAGEM_CLASSIFICADA");
        event.put("eventId", UUID.randomUUID().toString());
        event.put("correlationId", entry.getCorrelationId());
        event.put("triageId", entry.getId());
        event.put("patientId", entry.getPatientId());
        event.put("patientName", entry.getPatientName());
        event.put("riskClassification", entry.getRiskClassification().name());
        event.put("nurseId", entry.getNurseId());
        event.put("chiefComplaint", entry.getChiefComplaint());
        event.put("observations", entry.getObservations());
        event.put("vitalSigns", entry.getVitalSigns());
        event.put("classifiedAt", entry.getTriageDate().toString());
        event.put("version", 1);
        return event;
    }

    private void sendAndAwait(String key, Map<String, Object> event, String successLog) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(TRIAGE_TOPIC, key, event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Falha ao publicar evento Kafka. key={} type={}", key, event.get("type"), ex);
            } else {
                log.info(successLog);
            }
        });
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao confirmar publicacao Kafka em ate 5 segundos.", ex);
        }
    }
}
