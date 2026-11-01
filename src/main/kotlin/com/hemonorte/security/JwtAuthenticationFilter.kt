package com.hemonorte.security

import com.hemonorte.model.enums.TipoUsuario
import com.hemonorte.repository.DoadorRepository
import com.hemonorte.repository.FuncionarioRepository
import com.hemonorte.service.JwtService
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val doadorRepository: DoadorRepository,
    private val funcionarioRepository: FuncionarioRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header == null) {
            filterChain.doFilter(request, response)
            return
        }
        if (!header.startsWith(BEARER_PREFIX) || header.length == BEARER_PREFIX.length) {
            naoAutorizado(response)
            return
        }

        try {
            val claims = jwtService.validarToken(header.substring(BEARER_PREFIX.length))
            val id = UUID.fromString(claims.subject)
            val tipo = TipoUsuario.valueOf(claims.get("tipo", String::class.java))
            val (principal, authorities) = when (tipo) {
                TipoUsuario.DOADOR -> {
                    if (!doadorRepository.existsById(id)) error("Doador inexistente")
                    DoadorAutenticado(id) to listOf(SimpleGrantedAuthority("TIPO_DOADOR"))
                }
                TipoUsuario.FUNCIONARIO -> {
                    val funcionario = funcionarioRepository.findById(id).orElse(null)
                        ?: error("Funcionário inexistente")
                    if (!funcionario.ativo) error("Funcionário inativo")
                    FuncionarioAutenticado(id, funcionario.perfil) to listOf(
                        SimpleGrantedAuthority("TIPO_FUNCIONARIO"),
                        SimpleGrantedAuthority("ROLE_${funcionario.perfil.name}"),
                    )
                }
            }
            val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = authentication
            SecurityContextHolder.setContext(context)
            filterChain.doFilter(request, response)
        } catch (_: JwtException) {
            SecurityContextHolder.clearContext()
            naoAutorizado(response)
        } catch (_: IllegalArgumentException) {
            SecurityContextHolder.clearContext()
            naoAutorizado(response)
        } catch (_: IllegalStateException) {
            SecurityContextHolder.clearContext()
            naoAutorizado(response)
        }
    }

    private fun naoAutorizado(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write("""{"erro":"Token ausente ou inválido"}""")
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}
