package com.github.nanaki93.logisticsservice.domain.parcel

import java.util.UUID

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

    fun toEntity(
        statusHistoryDto: StatusHistoryDto,
        parcelUid: UUID,
    ): StatusHistory =
        StatusHistory(
            parcelUid = parcelUid,
            status = statusHistoryDto.status,
            oldStatus = statusHistoryDto.oldStatus,
            tsFrom = statusHistoryDto.tsFrom,
            tsTo = statusHistoryDto.tsTo,
            reason = statusHistoryDto.reason,
        )
}
