package br.unifor.healthsys.triage.controller;

import br.unifor.healthsys.triage.model.TriageEntry;
import br.unifor.healthsys.triage.security.AuthenticatedUser;
import br.unifor.healthsys.triage.service.TriageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/triage")
public class TriageController {

    private final TriageService triageService;

    public TriageController(TriageService triageService) {
        this.triageService = triageService;
    }

    /**
     * Endpoint de teletriagem / triagem presencial
     */
    @PostMapping
    public ResponseEntity<TriageEntry> performTriage(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody TriageEntry entry,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        requireAnyRole(authenticatedUser, "ENFERMEIRO");
        if (correlationId != null && !correlationId.isBlank()) {
            entry.setCorrelationId(correlationId);
        }
        entry.setNurseId(authenticatedUser.userId());
        entry.setNurseName(authenticatedUser.nome());
        return ResponseEntity.status(HttpStatus.CREATED).body(triageService.performTriage(entry));
    }

    @GetMapping
    public ResponseEntity<List<TriageEntry>> findAll(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        requireAnyRole(authenticatedUser, "MEDICO", "ENFERMEIRO", "ADMIN");
        return ResponseEntity.ok(triageService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TriageEntry> findById(@PathVariable Long id,
                                                @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        requireAnyRole(authenticatedUser, "MEDICO", "ENFERMEIRO", "ADMIN");
        return ResponseEntity.ok(triageService.findById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<TriageEntry>> findByPatientId(@PathVariable Long patientId,
                                                             @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        requireAnyRole(authenticatedUser, "MEDICO", "ENFERMEIRO", "ADMIN");
        return ResponseEntity.ok(triageService.findByPatientId(patientId));
    }

    @GetMapping("/queue")
    public ResponseEntity<List<TriageEntry>> getWaitingQueue(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        requireAnyRole(authenticatedUser, "MEDICO", "ENFERMEIRO", "ADMIN");
        return ResponseEntity.ok(triageService.findWaitingQueue());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TriageEntry> update(@PathVariable Long id,
                                              @Valid @RequestBody TriageEntry entry,
                                              @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        requireAnyRole(authenticatedUser, "MEDICO", "ENFERMEIRO");
        return ResponseEntity.ok(triageService.update(id, entry));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TriageEntry> updateStatus(@PathVariable Long id,
                                                     @RequestParam TriageEntry.TriageStatus status,
                                                     @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        requireAnyRole(authenticatedUser, "MEDICO", "ENFERMEIRO");
        return ResponseEntity.ok(triageService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        requireAnyRole(authenticatedUser, "ADMIN");
        triageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAnyRole(AuthenticatedUser authenticatedUser, String... roles) {
        if (authenticatedUser == null || authenticatedUser.role() == null || authenticatedUser.role().isBlank()) {
            throw new AccessDeniedException("Usuario autenticado sem perfil valido para acesso a triagem.");
        }

        for (String role : roles) {
            if (role.equalsIgnoreCase(authenticatedUser.role())) {
                return;
            }
        }

        throw new AccessDeniedException("Perfil sem permissao para acessar o recurso de triagem.");
    }
}
