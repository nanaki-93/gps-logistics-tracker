package com.github.nanaki93.logisticsservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.data.web.config.EnableSpringDataWebSupport

@SpringBootApplication
@EnableCaching
@EnableAspectJAutoProxy
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class LogisticsServiceApplication

fun main(args: Array<String>) {
    runApplication<LogisticsServiceApplication>(*args)
}
