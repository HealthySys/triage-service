package br.unifor.healthsys.triage.repository;

import br.unifor.healthsys.triage.model.TriageEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriageRepository extends JpaRepository<TriageEntry, Long> {

    List<TriageEntry> findByPatientId(Long patientId);

    List<TriageEntry> findByRiskClassificationOrderByTriageDateAsc(TriageEntry.RiskClassification classification);

    @Query("""
            SELECT t FROM TriageEntry t
            WHERE t.status = :status
            ORDER BY
              CASE t.riskClassification
                WHEN br.unifor.healthsys.triage.model.TriageEntry.RiskClassification.VERMELHO THEN 1
                WHEN br.unifor.healthsys.triage.model.TriageEntry.RiskClassification.LARANJA THEN 2
                WHEN br.unifor.healthsys.triage.model.TriageEntry.RiskClassification.AMARELO THEN 3
                WHEN br.unifor.healthsys.triage.model.TriageEntry.RiskClassification.VERDE THEN 4
                ELSE 5
              END ASC,
              t.triageDate ASC
            """)
    List<TriageEntry> findWaitingQueueByPriority(@Param("status") TriageEntry.TriageStatus status);
}
