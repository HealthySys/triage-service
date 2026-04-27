package br.unifor.healthsys.triage.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("JWT ausente ou invalido no triage-service. path={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7);
            AuthenticatedUser user = jwtService.parse(token);
            response.setHeader("X-Auth-Debug-Role", String.valueOf(user.role()));
            log.info("JWT aceito no triage-service. path={} user={} role={}",
                    request.getRequestURI(), user.username(), user.role());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    token,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            response.setHeader("X-Auth-Debug-Error", ex.getClass().getSimpleName());
            log.error("Falha ao processar JWT no triage-service. path={} erro={}",
                    request.getRequestURI(), ex.getClass().getSimpleName(), ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
