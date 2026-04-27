package br.unifor.healthsys.triage.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "triage_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 50)
    private String correlationId;

    @Column(name = "patient_name", length = 200)
    private String patientName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_classification", nullable = false)
    private RiskClassification riskClassification;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "vital_signs", columnDefinition = "TEXT")
    private String vitalSigns;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "nurse_id")
    private String nurseId;

    @Column(name = "nurse_name", length = 200)
    private String nurseName;

    @Column(name = "triage_date", nullable = false)
    private LocalDateTime triageDate;

    @Enumerated(EnumType.STRING)
    private TriageStatus status;

    @Column(name = "event_published")
    private boolean eventPublished = false;

    @PrePersist
    protected void onCreate() {
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        triageDate = LocalDateTime.now();
        status = TriageStatus.AGUARDANDO_ATENDIMENTO;
    }

    /**
     * Classificacao de Risco - Protocolo Manchester
     */
    public enum RiskClassification {
        VERMELHO("Emergencia", 0),
        LARANJA("Muito Urgente", 10),
        AMARELO("Urgente", 60),
        VERDE("Pouco Urgente", 120),
        AZUL("Nao Urgente", 240);

        private final String descricao;
        private final int tempoEsperaMinutos;

        RiskClassification(String descricao, int tempoEsperaMinutos) {
            this.descricao = descricao;
            this.tempoEsperaMinutos = tempoEsperaMinutos;
        }

        public String getDescricao() { return descricao; }
        public int getTempoEsperaMinutos() { return tempoEsperaMinutos; }
    }

    public enum TriageStatus {
        AGUARDANDO_ATENDIMENTO,
        EM_ATENDIMENTO,
        ATENDIDO,
        TRANSFERIDO,
        ALTA_ADMINISTRATIVA
    }
}
