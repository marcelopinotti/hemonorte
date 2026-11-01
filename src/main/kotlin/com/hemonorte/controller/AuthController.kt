package com.hemonorte.controller

import com.hemonorte.dto.LoginDoadorRequest
import com.hemonorte.dto.LoginFuncionarioRequest
import com.hemonorte.dto.TokenResponse
import com.hemonorte.service.AuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/login")
class AuthController(private val authService: AuthService) {

    @PostMapping("/doador")
    fun loginDoador(@Valid @RequestBody request: LoginDoadorRequest): TokenResponse =
        authService.autenticarDoador(request.cpf, request.senha)

    @PostMapping("/funcionario")
    fun loginFuncionario(@Valid @RequestBody request: LoginFuncionarioRequest): TokenResponse =
        authService.autenticarFuncionario(request.matricula, request.senha)
}
