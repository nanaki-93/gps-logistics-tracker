package com.github.nanaki93.logisticsservice.domain.util

import java.util.UUID

fun String.toUuid() = UUID.fromString(this)
