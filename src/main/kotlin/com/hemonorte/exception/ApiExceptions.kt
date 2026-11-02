package com.hemonorte.exception

class ConflitoException(message: String) : RuntimeException(message)

class RegraNegocioException(message: String) : RuntimeException(message)

class AutenticacaoException : RuntimeException("Credenciais inválidas")
