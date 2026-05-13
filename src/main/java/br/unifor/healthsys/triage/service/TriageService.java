package br.unifor.healthsys.triage.service;

import br.unifor.healthsys.triage.client.InternalPatientClient;
import br.unifor.healthsys.triage.exception.ConflictException;
import br.unifor.healthsys.triage.messaging.TriageEventProducer;
import br.unifor.healthsys.triage.model.TriageEntry;
import br.unifor.healthsys.triage.repository.TriageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final TriageRepository triageRepository;
    private final TriageEventProducer eventProducer;
    private final InternalPatientClient internalPatientClient;

    public TriageService(TriageRepository triageRepository,
                         TriageEventProducer eventProducer,
                         InternalPatientClient internalPatientClient) {
        this.triageRepository = triageRepository;
        this.eventProducer = eventProducer;
        this.internalPatientClient = internalPatientClient;
    }

    @Transactional
    public TriageEntry performTriage(TriageEntry entry) {
        InternalPatientClient.InternalPatientSummaryResponse patient =
                internalPatientClient.fetchRequiredPatient(entry.getPatientId());
        if (!patient.ativo()) {
            throw new IllegalArgumentException("Paciente inativo nao pode ser encaminhado para triagem.");
        }
        if (!triageRepository.findOpenTriagesByPatientId(entry.getPatientId()).isEmpty()) {
            throw new ConflictException(
                    "Paciente ja possui um atendimento em andamento. Finalize-o antes de registrar uma nova triagem."
            );
        }
        if (entry.getPatientName() == null || entry.getPatientName().isBlank()) {
            entry.setPatientName(patient.nome());
        }

        internalPatientClient.addAllergies(entry.getPatientId(), entry.getAlergiasReportadas());
        internalPatientClient.addVaccines(entry.getPatientId(), entry.getVacinasReportadas());

        long startedAt = System.currentTimeMillis();
        TriageEntry saved = triageRepository.save(entry);
        eventProducer.publishTriageEvent(saved);
        long processingMs = System.currentTimeMillis() - startedAt;
        log.info("triageProcessingMs={} correlationId={} triageId={}",
                processingMs, saved.getCorrelationId(), saved.getId());
        if (processingMs > 5000) {
            log.warn("Tempo de processamento da triagem acima do alvo (5s). correlationId={} triageId={} processingMs={}",
                    saved.getCorrelationId(), saved.getId(), processingMs);
        }
        saved.setEventPublished(true);
        return triageRepository.save(saved);
    }

    public List<TriageEntry> findAll() {
        return triageRepository.findAll();
    }

    public TriageEntry findById(Long id) {
        return triageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Triagem nao encontrada: " + id));
    }

    public List<TriageEntry> findByPatientId(Long patientId) {
        return triageRepository.findByPatientId(patientId);
    }

    public List<TriageEntry> findWaitingQueue() {
        return triageRepository.findWaitingQueueByPriority(
                TriageEntry.TriageStatus.AGUARDANDO_ATENDIMENTO);
    }

    @Transactional
    public TriageEntry update(Long id, TriageEntry updated) {
        InternalPatientClient.InternalPatientSummaryResponse patient =
                internalPatientClient.fetchRequiredPatient(updated.getPatientId());
        if (!patient.ativo()) {
            throw new IllegalArgumentException("Paciente inativo nao pode ser associado a triagem.");
        }

        TriageEntry existing = findById(id);
        existing.setPatientId(updated.getPatientId());
        existing.setPatientName(
                updated.getPatientName() == null || updated.getPatientName().isBlank()
                        ? patient.nome()
                        : updated.getPatientName()
        );
        existing.setRiskClassification(updated.getRiskClassification());
        existing.setChiefComplaint(updated.getChiefComplaint());
        existing.setVitalSigns(updated.getVitalSigns());
        existing.setObservations(updated.getObservations());

        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }

        return triageRepository.save(existing);
    }

    @Transactional
    public TriageEntry updateStatus(Long id, TriageEntry.TriageStatus newStatus, Long medicoId, String medicoName) {
        TriageEntry entry = findById(id);
        TriageEntry.TriageStatus previousStatus = entry.getStatus();
        entry.setStatus(newStatus);
        TriageEntry saved = triageRepository.save(entry);

        if (newStatus == TriageEntry.TriageStatus.EM_ATENDIMENTO
                && previousStatus != TriageEntry.TriageStatus.EM_ATENDIMENTO) {
            eventProducer.publishAttendanceStarted(saved, medicoId, medicoName);
        }

        return saved;
    }

    @Transactional
    public void delete(Long id) {
        triageRepository.delete(findById(id));
    }

    public void forwardPatientToTriage(Long patientId, Long forwardedById, String forwardedByName) {
        InternalPatientClient.InternalPatientSummaryResponse patient =
                internalPatientClient.fetchRequiredPatient(patientId);
        if (!patient.ativo()) {
            throw new IllegalArgumentException("Paciente inativo nao pode ser encaminhado para triagem.");
        }
        eventProducer.publishPatientForwarded(patient.id(), patient.nome(), forwardedById, forwardedByName);
    }
}
