package com.hemonorte

fun cpfDeTeste(numero: Long): String {
    val digitos = "%09d".format(numero).map { it - '0' }.toMutableList()
    repeat(2) {
        val tamanho = digitos.size
        digitos += digitos.mapIndexed { i, d -> d * (tamanho + 1 - i) }.sum() * 10 % 11 % 10
    }
    return digitos.joinToString("")
}
