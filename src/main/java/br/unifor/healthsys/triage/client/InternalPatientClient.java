package br.unifor.healthsys.triage.client;

import br.unifor.healthsys.triage.model.TriageAllergyInput;
import br.unifor.healthsys.triage.model.TriageVaccineInput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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
        HttpEntity<Void> entity = new HttpEntity<>(buildAuthHeaders(MediaType.APPLICATION_JSON));

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

    public void addAllergies(Long patientId, List<TriageAllergyInput> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return;
        }
        HttpEntity<List<TriageAllergyInput>> entity = new HttpEntity<>(allergies, buildAuthHeaders(MediaType.APPLICATION_JSON));
        restTemplate.exchange(
                patientServiceBaseUrl + "/api/internal/patients/{id}/allergies",
                HttpMethod.POST,
                entity,
                Void.class,
                patientId
        );
    }

    public void addVaccines(Long patientId, List<TriageVaccineInput> vaccines) {
        if (vaccines == null || vaccines.isEmpty()) {
            return;
        }
        HttpEntity<List<TriageVaccineInput>> entity = new HttpEntity<>(vaccines, buildAuthHeaders(MediaType.APPLICATION_JSON));
        restTemplate.exchange(
                patientServiceBaseUrl + "/api/internal/patients/{id}/vaccines",
                HttpMethod.POST,
                entity,
                Void.class,
                patientId
        );
    }

    private HttpHeaders buildAuthHeaders(MediaType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(INTERNAL_TOKEN_HEADER, internalServiceToken);
        headers.setContentType(contentType);
        return headers;
    }

    public record InternalPatientSummaryResponse(Long id, String nome, boolean ativo, String email) {
    }
}
