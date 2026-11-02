package com.hemonorte.service

import com.hemonorte.model.enums.PerfilFuncionario
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") jwtSecret: String,
    @Value("\${jwt.expiration}") private val jwtExpiration: Long,
) {

    private val signingKey: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))

    fun gerarToken(id: UUID, tipo: String, perfil: PerfilFuncionario? = null): String {
        require(tipo == TIPO_DOADOR || tipo == TIPO_FUNCIONARIO) { "Tipo de usuário inválido: $tipo" }
        require(tipo == TIPO_FUNCIONARIO || perfil == null) { "Token de doador não pode conter perfil" }
        require(tipo == TIPO_DOADOR || perfil != null) { "Token de funcionário deve conter perfil" }

        val emitidoEm = Date()
        val builder = Jwts.builder()
            .subject(id.toString())
            .claim(CLAIM_TIPO, tipo)
            .issuer(ISSUER)
            .issuedAt(emitidoEm)
            .expiration(Date(emitidoEm.time + jwtExpiration))

        if (perfil != null) {
            builder.claim(CLAIM_PERFIL, perfil.name)
        }

        return builder.signWith(signingKey, Jwts.SIG.HS512).compact()
    }

    fun validarToken(token: String): Claims {
        val jwt = Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(ISSUER)
            .build()
            .parseSignedClaims(token)
        if (jwt.header.algorithm != Jwts.SIG.HS512.id) {
            throw MalformedJwtException("Algoritmo de assinatura deve ser HS512")
        }

        val claims = jwt.payload
        validarClaimsObrigatorios(claims)
        return claims
    }

    fun extrairId(token: String): UUID = UUID.fromString(validarToken(token).subject)

    fun extrairTipo(token: String): String = validarToken(token).get(CLAIM_TIPO, String::class.java)

    fun extrairPerfil(token: String): PerfilFuncionario? = validarToken(token)
        .get(CLAIM_PERFIL, String::class.java)
        ?.let(PerfilFuncionario::valueOf)

    val expiresIn: Long
        get() = jwtExpiration

    private fun validarClaimsObrigatorios(claims: Claims) {
        val tipo = claims.get(CLAIM_TIPO, String::class.java)
            ?: throw MalformedJwtException("Claim tipo é obrigatória")
        val perfil = claims.get(CLAIM_PERFIL, String::class.java)

        if (claims.subject == null || claims.issuedAt == null || claims.expiration == null) {
            throw MalformedJwtException("Claims sub, iat e exp são obrigatórias")
        }
        try {
            UUID.fromString(claims.subject)
        } catch (exception: IllegalArgumentException) {
            throw MalformedJwtException("Claim sub deve ser um UUID", exception)
        }
        if (tipo != TIPO_DOADOR && tipo != TIPO_FUNCIONARIO) {
            throw MalformedJwtException("Claim tipo inválida")
        }
        if (tipo == TIPO_DOADOR && perfil != null) {
            throw MalformedJwtException("Token de doador não pode conter perfil")
        }
        if (tipo == TIPO_FUNCIONARIO && perfil == null) {
            throw MalformedJwtException("Token de funcionário deve conter perfil")
        }
        if (perfil != null && PerfilFuncionario.entries.none { it.name == perfil }) {
            throw MalformedJwtException("Claim perfil inválida")
        }
    }

    private companion object {
        const val ISSUER = "hemonorte"
        const val CLAIM_TIPO = "tipo"
        const val CLAIM_PERFIL = "perfil"
        const val TIPO_DOADOR = "DOADOR"
        const val TIPO_FUNCIONARIO = "FUNCIONARIO"
    }
}
