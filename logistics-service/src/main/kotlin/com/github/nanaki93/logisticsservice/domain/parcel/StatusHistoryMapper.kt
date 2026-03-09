package com.github.nanaki93.logisticsservice.domain.parcel

import com.github.nanaki93.logisticsservice.domain.util.toUuid

object StatusHistoryMapper {
    fun toDto(
        statusHistory: StatusHistory,
        parcelDto: ParcelDto,
    ): StatusHistoryDto =
        StatusHistoryDto(
            parcel = parcelDto,
            status = statusHistory.status,
            oldStatus = statusHistory.oldStatus,
            tsFrom = statusHistory.tsFrom,
            tsTo = statusHistory.tsTo,
            reason = statusHistory.reason,
        )

    fun toEntity(statusHistoryDto: StatusHistoryInsertDto): StatusHistory =
        StatusHistory(
            parcelUid = statusHistoryDto.parcelId.toUuid(),
            status = statusHistoryDto.status,
            oldStatus = statusHistoryDto.oldStatus,
            tsFrom = statusHistoryDto.tsFrom,
            tsTo = statusHistoryDto.tsTo,
            reason = statusHistoryDto.reason,
        )
}
