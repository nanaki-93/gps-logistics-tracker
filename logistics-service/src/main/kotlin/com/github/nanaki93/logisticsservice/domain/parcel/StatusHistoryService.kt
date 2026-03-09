package com.github.nanaki93.logisticsservice.domain.parcel

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class StatusHistoryService(
    val statusHistoryRepository: StatusHistoryRepository,
) {
    fun createHistory(parcel: Parcel) {
        val statusHistory = StatusHistory(parcelUid = parcel.parcelUid, status = parcel.status, tsFrom = Instant.now())
        statusHistoryRepository.save(statusHistory)
    }

    fun updateHistory(
        parcel: Parcel,
        reason: String? = null,
    ) {
        val activeStatusHistory = getActiveStatusHistoryByParcelUid(parcel.parcelUid)
        statusHistoryRepository.save(activeStatusHistory.close())
        val newStatusHistory =
            StatusHistory(
                parcelUid = parcel.parcelUid,
                status = parcel.status,
                oldStatus = activeStatusHistory.status,
                tsFrom = Instant.now(),
                reason = reason,
            )
        statusHistoryRepository.save(newStatusHistory)
    }

    fun getHistoryByParcelUid(parcelUid: UUID): List<StatusHistory> = statusHistoryRepository.findByParcelUid(parcelUid)

    fun getActiveStatusHistoryByParcelUid(parcelUid: UUID): StatusHistory =
        statusHistoryRepository
            .findByParcelUidAndTsToIsNull(parcelUid)
            .orElseThrow { IllegalArgumentException("Active status history not found for parcel with uid $parcelUid") }
}
