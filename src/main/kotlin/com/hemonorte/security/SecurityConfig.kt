package com.hemonorte.security

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig(private val jwtAuthenticationFilter: JwtAuthenticationFilter) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers(HttpMethod.POST, "/api/doador", "/auth/login/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/slots", "/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers(
                    "/api/funcionario/**",
                    "/api/doador/**",
                    "/agendamentos/**",
                    "/doadores/**",
                    "/triagens/**",
                    "/coletas/**",
                    "/estoque/**",
                    "/exames-sorologicos/**",
                    "/slots/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                ).authenticated()
                .anyRequest().denyAll()
        }
        .exceptionHandling {
            it.authenticationEntryPoint { _, response, _ -> responder(response, 401, "Token ausente ou inválido") }
                .accessDeniedHandler { _, response, _ -> responder(response, 403, "Acesso negado") }
        }
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()

    private fun responder(response: HttpServletResponse, status: Int, erro: String) {
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write("""{"erro":"$erro"}""")
    }
}
