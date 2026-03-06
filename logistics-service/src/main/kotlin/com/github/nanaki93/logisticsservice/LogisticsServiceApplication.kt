package com.github.nanaki93.logisticsservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LogisticsServiceApplication

fun main(args: Array<String>) {
    println("POSTGRES_USER: ${System.getenv("POSTGRES_USER")}")
    runApplication<LogisticsServiceApplication>(*args)
}
