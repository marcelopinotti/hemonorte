package com.hemonorte

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<HemonorteApplication>().with(TestcontainersConfiguration::class).run(*args)
}
