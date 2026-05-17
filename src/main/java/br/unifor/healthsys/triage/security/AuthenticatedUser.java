package br.unifor.healthsys.triage.security;

public record AuthenticatedUser(
        Long userId,
        String username,
        String role,
        String email,
        String nome
) {
}
