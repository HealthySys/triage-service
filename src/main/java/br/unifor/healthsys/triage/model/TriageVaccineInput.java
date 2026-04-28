package br.unifor.healthsys.triage.model;

import java.time.LocalDate;

public record TriageVaccineInput(
        String nomeVacina,
        LocalDate dataAplicacao,
        String lote,
        String profissionalResp
) {
}
