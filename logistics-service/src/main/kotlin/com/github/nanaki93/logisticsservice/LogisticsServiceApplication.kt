package com.github.nanaki93.logisticsservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableCaching
@EnableAspectJAutoProxy
class LogisticsServiceApplication

fun main(args: Array<String>) {
    runApplication<LogisticsServiceApplication>(*args)
}
