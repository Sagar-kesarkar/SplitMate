package com.splitmate.app.data.repository

import com.splitmate.app.model.BalanceEventType
import com.splitmate.app.model.BalanceHistoryEvent

data class MemberBalanceChange(
    val memberId: String,
    val before: Double,
    val delta: Double,
    val after: Double
)

data class BalanceTimelineEntry(
    val sourceId: String,
    val title: String,
    val eventType: BalanceEventType,
    val paidByUserId: String,
    val fullAmount: Double,
    val dateMillis: Long,
    val allocations: Map<String, Double>,
    val changes: List<MemberBalanceChange>
)

data class BalanceTimeline(
    val entries: List<BalanceTimelineEntry>,
    val finalBalances: Map<String, Double>
)

object BalanceTimelineBuilder {
    fun build(events: List<BalanceHistoryEvent>, currentUserId: String): BalanceTimeline {
        val running = mutableMapOf<String, Double>()
        val entries = events
            .groupBy { EventKey(it.sourceId, it.eventType, it.dateMillis) }
            .entries
            .sortedBy { it.key.dateMillis }
            .map { (_, rows) ->
                val first = rows.first()
                val deltas = rows.filter { kotlin.math.abs(it.signedChange) >= 0.005 }.groupBy { row ->
                    when (row.eventType) {
                        BalanceEventType.SETTLEMENT, BalanceEventType.SETTLEMENT_REVERSAL -> row.otherUserId
                        else -> if (row.paidByUserId == currentUserId) row.otherUserId else row.paidByUserId
                    }
                }.mapValues { (_, affectedRows) -> affectedRows.sumOf { it.signedChange } }
                val changes = deltas.map { (memberId, delta) ->
                    val before = running[memberId] ?: 0.0
                    val after = before + delta
                    running[memberId] = after
                    MemberBalanceChange(memberId, before, delta, after)
                }
                BalanceTimelineEntry(
                    sourceId = first.sourceId,
                    title = first.title,
                    eventType = first.eventType,
                    paidByUserId = first.paidByUserId,
                    fullAmount = first.fullAmount,
                    dateMillis = first.dateMillis,
                    allocations = if (first.eventType == BalanceEventType.SETTLEMENT || first.eventType == BalanceEventType.SETTLEMENT_REVERSAL) emptyMap()
                    else rows.associate { it.otherUserId to it.currentUserShare },
                    changes = changes
                )
            }
        return BalanceTimeline(entries, running.toMap())
    }

    private data class EventKey(val sourceId: String, val type: BalanceEventType, val dateMillis: Long)
}
