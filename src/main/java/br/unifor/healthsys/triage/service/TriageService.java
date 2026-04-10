package br.unifor.healthsys.triage.service;

import br.unifor.healthsys.triage.messaging.TriageEventProducer;
import br.unifor.healthsys.triage.model.TriageEntry;
import br.unifor.healthsys.triage.repository.TriageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TriageService {

    private final TriageRepository triageRepository;
    private final TriageEventProducer eventProducer;

    public TriageService(TriageRepository triageRepository, TriageEventProducer eventProducer) {
        this.triageRepository = triageRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public TriageEntry performTriage(TriageEntry entry) {
        TriageEntry saved = triageRepository.save(entry);
        eventProducer.publishTriageEvent(saved);
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
        return triageRepository.findByStatusOrderByRiskClassificationAscTriageDateAsc(
                TriageEntry.TriageStatus.AGUARDANDO_ATENDIMENTO);
    }

    @Transactional
    public TriageEntry update(Long id, TriageEntry updated) {
        TriageEntry existing = findById(id);
        existing.setPatientId(updated.getPatientId());
        existing.setPatientName(updated.getPatientName());
        existing.setRiskClassification(updated.getRiskClassification());
        existing.setChiefComplaint(updated.getChiefComplaint());
        existing.setVitalSigns(updated.getVitalSigns());
        existing.setObservations(updated.getObservations());
        existing.setNurseId(updated.getNurseId());
        existing.setNurseName(updated.getNurseName());

        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }

        return triageRepository.save(existing);
    }

    @Transactional
    public TriageEntry updateStatus(Long id, TriageEntry.TriageStatus newStatus) {
        TriageEntry entry = findById(id);
        entry.setStatus(newStatus);
        return triageRepository.save(entry);
    }

    @Transactional
    public void delete(Long id) {
        triageRepository.delete(findById(id));
    }
}
