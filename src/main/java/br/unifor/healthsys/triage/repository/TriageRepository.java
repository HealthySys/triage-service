package br.unifor.healthsys.triage.repository;

import br.unifor.healthsys.triage.model.TriageEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriageRepository extends JpaRepository<TriageEntry, Long> {

    List<TriageEntry> findByPatientId(Long patientId);

    List<TriageEntry> findByRiskClassificationOrderByTriageDateAsc(TriageEntry.RiskClassification classification);

    List<TriageEntry> findByStatusOrderByRiskClassificationAscTriageDateAsc(TriageEntry.TriageStatus status);
}
