package com.hemonorte.exception

import com.hemonorte.model.ResultadoAptidao

class ConflitoException(message: String) : RuntimeException(message)

class RegraNegocioException(message: String) : RuntimeException(message)

class InaptidaoDoacaoException(val resultado: ResultadoAptidao) :
    RuntimeException(resultado.motivo)

class AutenticacaoException : RuntimeException("Credenciais inválidas")
