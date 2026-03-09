package com.github.nanaki93.logisticsservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class LogisticsServiceApplication

fun main(args: Array<String>) {
    runApplication<LogisticsServiceApplication>(*args)
}
