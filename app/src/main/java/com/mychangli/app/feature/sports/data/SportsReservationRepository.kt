package com.mychangli.app.feature.sports.data

import com.mychangli.app.core.network.campustools.CreateReservationRequest
import com.mychangli.app.core.network.campustools.RemoteReservation
import com.mychangli.app.core.network.campustools.RemoteVenue
import com.mychangli.app.core.network.campustools.ReservationContext
import com.mychangli.app.core.network.campustools.authenticatedCall
import com.mychangli.app.feature.sports.ui.LocalReservation
import com.mychangli.app.feature.sports.ui.ReservationStatus
import com.mychangli.app.feature.sports.ui.SportType
import com.mychangli.app.feature.sports.ui.SportsReservationLocalStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SportsConnectionState { IDLE, LOADING, ONLINE, OFFLINE }

object SportsReservationRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _reservations = MutableStateFlow(SportsReservationLocalStore.reservations.value)
    val reservations: StateFlow<List<LocalReservation>> = _reservations.asStateFlow()
    private val _venues = MutableStateFlow<Map<SportType, RemoteVenue>>(emptyMap())
    private val _context = MutableStateFlow<ReservationContext?>(null)
    val context: StateFlow<ReservationContext?> = _context.asStateFlow()
    private val _availability = MutableStateFlow<Map<String, String>>(emptyMap())
    val availability: StateFlow<Map<String, String>> = _availability.asStateFlow()
    private val _connectionState = MutableStateFlow(SportsConnectionState.IDLE)
    val connectionState: StateFlow<SportsConnectionState> = _connectionState.asStateFlow()
    private val loadedAvailability = mutableSetOf<String>()
    private var activeAccountId: String? = null

    @Synchronized
    fun activateAccount(userId: String) {
        if (activeAccountId == userId) return
        activeAccountId = userId
        _reservations.value = SportsReservationLocalStore.activateAccount(userId)
        _venues.value = emptyMap()
        _context.value = null
        _availability.value = emptyMap()
        loadedAvailability.clear()
        _connectionState.value = SportsConnectionState.IDLE
    }

    fun refreshAll(onMessage: (String?) -> Unit = {}) {
        scope.launch {
            _connectionState.value = SportsConnectionState.LOADING
            runCatching {
                val venues = authenticatedCall { it.venues() }
                val reservations = authenticatedCall { it.reservations() }
                val context = authenticatedCall { it.reservationContext() }
                _venues.value = venues.mapNotNull { venue ->
                    venue.sport.toSportTypeOrNull()?.let { it to venue }
                }.toMap()
                val mapped = reservations.mapNotNull(RemoteReservation::toLocal)
                _reservations.value = mapped
                SportsReservationLocalStore.replaceCached(mapped)
                _context.value = context
                _connectionState.value = SportsConnectionState.ONLINE
            }.onFailure {
                _reservations.value = SportsReservationLocalStore.reservations.value
                _connectionState.value = SportsConnectionState.OFFLINE
                withContext(Dispatchers.Main) {
                    onMessage("服务暂时不可用，正在显示本机缓存")
                }
            }
        }
    }

    fun refreshContext(onResult: (ReservationContext?, String?) -> Unit = { _, _ -> }) {
        scope.launch {
            runCatching {
                authenticatedCall { it.reservationContext() }
            }.onSuccess {
                _context.value = it
                withContext(Dispatchers.Main) { onResult(it, null) }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    onResult(_context.value, it.message ?: "预约规则读取失败")
                }
            }
        }
    }

    fun timeSlots(sport: SportType): List<String> =
        _venues.value[sport]?.timeSlots.orEmpty().ifEmpty {
            SportsReservationLocalStore.timeSlots
        }

    fun priceYuan(sport: SportType): Int = _venues.value[sport]?.priceYuan ?: sport.priceYuan
    fun courtCount(sport: SportType): Int = _venues.value[sport]?.courtCount ?: sport.count

    fun stateFor(sport: SportType, date: String, time: String, court: Int): String {
        val remote = _availability.value[inventoryKey(sport, date, time, court)]
        return when {
            remote == "AVAILABLE" -> "可预约"
            remote == "RESERVED" -> "已被预约"
            remote == "UNAVAILABLE" -> "不可用"
            availabilityKey(sport, date) in loadedAvailability -> "不可用"
            _connectionState.value == SportsConnectionState.OFFLINE ->
                SportsReservationLocalStore.stateFor(sport, date, time, court)
            else -> "加载中"
        }
    }

    fun refreshAvailability(sport: SportType, date: String) {
        scope.launch {
            runCatching {
                val venue = venueFor(sport)
                val result = authenticatedCall { it.availability(venue.id, date) }
                val additions = buildMap {
                    result.slots.forEach { slot ->
                        slot.courts.forEach { court ->
                            put(
                                inventoryKey(sport, date, slot.timeSlot, court.courtNumber),
                                court.status,
                            )
                        }
                    }
                }
                _availability.value = _availability.value
                    .filterKeys { !it.startsWith("${sport.name}|$date|") } + additions
                loadedAvailability += availabilityKey(sport, date)
                _connectionState.value = SportsConnectionState.ONLINE
            }.onFailure {
                _connectionState.value = SportsConnectionState.OFFLINE
            }
        }
    }

    fun reserve(
        sport: SportType,
        date: String,
        time: String,
        court: Int,
        onResult: (LocalReservation?, String) -> Unit,
    ) {
        scope.launch {
            runCatching {
                val venue = venueFor(sport)
                authenticatedCall {
                    it.createReservation(
                        CreateReservationRequest(venue.id, date, time, court)
                    )
                }.toLocal() ?: error("服务端返回了不支持的运动类型")
            }.onSuccess { reservation ->
                val updated = (listOf(reservation) + _reservations.value)
                    .distinctBy(LocalReservation::id)
                _reservations.value = updated
                SportsReservationLocalStore.replaceCached(updated)
                refreshContext()
                refreshAvailability(sport, date)
                withContext(Dispatchers.Main) {
                    onResult(reservation, "服务端预约已保存；当前仍为模拟支付，未产生真实付款")
                }
            }.onFailure { error ->
                refreshAvailability(sport, date)
                withContext(Dispatchers.Main) {
                    onResult(null, error.message ?: "预约失败，请刷新后重试")
                }
            }
        }
    }

    fun cancel(id: String, onResult: (String) -> Unit) {
        scope.launch {
            runCatching {
                authenticatedCall { it.cancelReservation(id) }.toLocal()
                    ?: error("预约状态无效")
            }.onSuccess { reservation ->
                val updated = _reservations.value.map {
                    if (it.id == reservation.id) reservation else it
                }
                _reservations.value = updated
                SportsReservationLocalStore.replaceCached(updated)
                refreshContext()
                refreshAvailability(reservation.sport, reservation.date)
                withContext(Dispatchers.Main) {
                    onResult("预约已取消，场地已恢复可预约")
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    onResult(it.message ?: "取消失败，请稍后重试")
                }
            }
        }
    }

    private suspend fun venueFor(sport: SportType): RemoteVenue {
        _venues.value[sport]?.let { return it }
        val venues = authenticatedCall { it.venues() }
        _venues.value = venues.mapNotNull { venue ->
            venue.sport.toSportTypeOrNull()?.let { it to venue }
        }.toMap()
        return _venues.value[sport] ?: error("服务端未开放${sport.label}")
    }

    private fun availabilityKey(sport: SportType, date: String) = "${sport.name}|$date"
    private fun inventoryKey(
        sport: SportType,
        date: String,
        time: String,
        court: Int,
    ) = "${sport.name}|$date|$time|$court"
}

private fun String.toSportTypeOrNull(): SportType? =
    runCatching { SportType.valueOf(this) }.getOrNull()

private fun RemoteReservation.toLocal(): LocalReservation? {
    val type = sport.toSportTypeOrNull() ?: return null
    return LocalReservation(
        id = id,
        sport = type,
        date = date,
        timeSlot = timeSlot,
        courtNumber = courtNumber,
        priceYuan = priceYuan,
        status = if (status == "CANCELLED") ReservationStatus.CANCELLED else ReservationStatus.ACTIVE,
        simulatedPaymentComplete = paymentStatus == "SIMULATED",
        cancelledBy = cancelledBy,
        cancelReason = cancelReason,
        canCancel = canCancel,
        cancelUnavailableReason = cancelUnavailableReason,
        nextCancellationCount = nextCancellationCount,
        remainingCancellationsAfterCancel = remainingCancellationsAfterCancel,
        reservationStudentIdSnapshot = reservationStudentIdSnapshot,
        reservationNameSnapshot = reservationNameSnapshot,
        createdAtMillis = createdAtMillis,
    )
}
