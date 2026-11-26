package com.duoc.aquaflow.backend;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

@Configuration
@SecurityScheme(name = "cuenta", type = SecuritySchemeType.HTTP, scheme = "basic")
class Seguridad {
    @Bean
    InMemoryUserDetailsManager usuarios(
            @Value("${AQUAFLOW_SUPERVISOR_PIN}") String supervisorPin,
            @Value("${AQUAFLOW_JEFE_PIN}") String jefePin) {
        Assert.hasText(supervisorPin, "Configura AQUAFLOW_SUPERVISOR_PIN.");
        Assert.hasText(jefePin, "Configura AQUAFLOW_JEFE_PIN.");
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(
                User.withUsername("supervisor").password(encoder.encode(supervisorPin)).roles("SUPERVISOR").build(),
                User.withUsername("jefe").password(encoder.encode(jefePin)).roles("JEFE").build());
    }

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("SUPERVISOR", "JEFE")
                        .requestMatchers(HttpMethod.POST, "/api/faenas/*/decision").hasRole("JEFE")
                        .requestMatchers(HttpMethod.PUT, "/api/faenas/*/bitacora/alertas/*/revision").hasRole("JEFE")
                        .requestMatchers("/api/buzos", "/api/buzos/**").hasRole("JEFE")
                        .requestMatchers("/api/faenas", "/api/faenas/**").hasRole("SUPERVISOR")
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
