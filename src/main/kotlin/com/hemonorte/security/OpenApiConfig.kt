package com.hemonorte.security

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun hemonorteOpenApi(): OpenAPI = OpenAPI().info(
        Info().title("Hemonorte API").description(
            "JWT Bearer com claims sub (UUID), tipo (DOADOR/FUNCIONARIO), " + "perfil (funcionário), iat, exp e iss=hemonorte.",
        ),
    ).components(
        Components().addSecuritySchemes(
            BEARER_AUTH,
            SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                .description("Authorization: Bearer {token}"),
        ),
    )

    @Bean
    fun securityDocumentation(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.paths?.forEach { (path, item) ->
            item.readOperationsMap().forEach { (method, operation) ->
                if (!isPublic(method.name, path)) {
                    operation.addSecurityItem(SecurityRequirement().addList(BEARER_AUTH))
                    operation.responses.addApiResponse(
                        "401", ApiResponse().description("Token ausente, inválido ou expirado")
                    ).addApiResponse("403", ApiResponse().description("Usuário autenticado sem permissão"))
                }
            }
        }
    }

    private fun isPublic(method: String, path: String): Boolean =
        (method == "POST" && (path == "/api/doador" || path.startsWith("/auth/login/"))) || (method == "GET" && (path == "/slots" || path.startsWith("/comprovantes/")))

    private companion object {
        const val BEARER_AUTH = "bearerAuth"
    }
}
