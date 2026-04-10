package br.unifor.healthsys.triage.controller;

import br.unifor.healthsys.triage.model.TriageEntry;
import br.unifor.healthsys.triage.service.TriageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<TriageEntry> performTriage(@Valid @RequestBody TriageEntry entry) {
        return ResponseEntity.status(HttpStatus.CREATED).body(triageService.performTriage(entry));
    }

    @GetMapping
    public ResponseEntity<List<TriageEntry>> findAll() {
        return ResponseEntity.ok(triageService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TriageEntry> findById(@PathVariable Long id) {
        return ResponseEntity.ok(triageService.findById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<TriageEntry>> findByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(triageService.findByPatientId(patientId));
    }

    @GetMapping("/queue")
    public ResponseEntity<List<TriageEntry>> getWaitingQueue() {
        return ResponseEntity.ok(triageService.findWaitingQueue());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TriageEntry> update(@PathVariable Long id,
                                              @Valid @RequestBody TriageEntry entry) {
        return ResponseEntity.ok(triageService.update(id, entry));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TriageEntry> updateStatus(@PathVariable Long id,
                                                     @RequestParam TriageEntry.TriageStatus status) {
        return ResponseEntity.ok(triageService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        triageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
