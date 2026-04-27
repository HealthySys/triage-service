package br.unifor.healthsys.triage.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class InternalPatientClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final RestTemplate restTemplate;
    private final String patientServiceBaseUrl;
    private final String internalServiceToken;

    public InternalPatientClient(RestTemplate restTemplate,
                                 @Value("${internal.patient-service.base-url}") String patientServiceBaseUrl,
                                 @Value("${internal.auth.token}") String internalServiceToken) {
        this.restTemplate = restTemplate;
        this.patientServiceBaseUrl = patientServiceBaseUrl;
        this.internalServiceToken = internalServiceToken;
    }

    public InternalPatientSummaryResponse fetchRequiredPatient(Long patientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<InternalPatientSummaryResponse> response = restTemplate.exchange(
                    patientServiceBaseUrl + "/api/internal/patients/{id}/summary",
                    HttpMethod.GET,
                    entity,
                    InternalPatientSummaryResponse.class,
                    patientId
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalArgumentException("Paciente nao encontrado para triagem: " + patientId);
        }
    }

    public record InternalPatientSummaryResponse(Long id, String nome, boolean ativo, String email) {
    }
}
