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

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TRIAGE_TOPIC, entry.getCorrelationId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Falha ao publicar evento de triagem correlationId={}: {}",
                        entry.getCorrelationId(), ex.getMessage());
            } else {
                log.info("Evento de triagem publicado - correlationId: {} | classificacao: {}",
                        entry.getCorrelationId(), entry.getRiskClassification());
            }
        });
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao confirmar publicacao Kafka em ate 5 segundos.", ex);
        }
    }

    private Map<String, Object> buildTriageEvent(TriageEntry entry) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", java.util.UUID.randomUUID().toString());
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
}
