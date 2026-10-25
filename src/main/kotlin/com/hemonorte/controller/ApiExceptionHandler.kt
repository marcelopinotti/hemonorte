package com.hemonorte.controller

import com.hemonorte.exception.ConflitoException
import com.hemonorte.exception.RegraNegocioException
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(val erro: String)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun naoEncontrado(exception: NoSuchElementException) = ApiError(exception.message ?: "Recurso não encontrado")

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun argumentoInvalido(exception: IllegalArgumentException) =
        ApiError(exception.message ?: "Entrada inválida")

    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        HttpMessageNotReadableException::class,
        ConstraintViolationException::class,
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun entradaInvalida() = ApiError("Entrada inválida")

    @ExceptionHandler(ConflitoException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun conflito(exception: ConflitoException) = ApiError(exception.message ?: "Conflito")

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun conflitoIntegridade() = ApiError("Conflito de integridade")

    @ExceptionHandler(RegraNegocioException::class)
    fun regraNegocio(exception: RegraNegocioException) =
        ApiError(exception.message ?: "Regra de negócio não atendida")
}
