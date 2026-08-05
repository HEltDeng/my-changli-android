package com.mychangli.app.feature.sports.ui

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychangli.app.base.ComposeActivity
import com.mychangli.app.core.network.campustools.CampusToolsSession
import com.mychangli.app.core.network.campustools.ReservationContext
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.feature.common.compose_ui.FeatureAvailabilityGate
import com.mychangli.app.feature.common.compose_ui.ImeCompatibleTextField
import com.mychangli.app.feature.sports.data.SportsConnectionState
import com.mychangli.app.feature.sports.data.SportsReservationRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SportsReservationActivity : ComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!com.mychangli.app.core.account.AccountAccess.isSignedIn) {
            com.mychangli.app.core.account.AccountAccess.openOrRequestLogin(
                this,
                android.content.Intent(this, SportsReservationActivity::class.java),
            )
            finish()
            return
        }
        setComposeContent {
            FeatureAvailabilityGate(
                featureKey = "sports_reservation",
                title = "场地预约",
                onBack = ::finish,
            ) {
                SportsReservationRoute(onBack = ::finish)
            }
        }
    }
}

enum class SportType(val label: String, val unitLabel: String, val count: Int, val priceYuan: Int) {
    BADMINTON("羽毛球", "场", 8, 20),
    TABLE_TENNIS("乒乓球", "台", 10, 10),
}

enum class ReservationStatus { ACTIVE, CANCELLED }

data class LocalReservation(
    val id: String,
    val sport: SportType,
    val date: String,
    val timeSlot: String,
    val courtNumber: Int,
    val priceYuan: Int,
    val status: ReservationStatus = ReservationStatus.ACTIVE,
    val simulatedPaymentComplete: Boolean = true,
    val cancelledBy: String? = null,
    val cancelReason: String? = null,
    val canCancel: Boolean = false,
    val cancelUnavailableReason: String = "",
    val nextCancellationCount: Int? = null,
    val remainingCancellationsAfterCancel: Int? = null,
    val reservationStudentIdSnapshot: String? = null,
    val reservationNameSnapshot: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

/** All availability and payment values are local demo data; no venue or payment endpoint is used. */
object SportsReservationLocalStore {
    const val LOCAL_NOTICE = "本地预约模拟，以场馆实际安排为准"
    val timeSlots = listOf("08:00-09:00", "09:00-10:00", "10:00-11:00", "14:00-15:00", "15:00-16:00", "16:00-17:00", "19:00-20:00", "20:00-21:00")
    private const val CACHE_ID = "sports_reservation_local_v1"
    private const val KEY_RESERVATIONS = "reservations_v1"
    private const val KEY_ACCOUNT_ID = "active_account_id_v1"
    private val gson = Gson()
    private val type = object : TypeToken<List<LocalReservation>>() {}.type
    private val mmkv by lazy { MMKV.mmkvWithID(CACHE_ID) }
    private val lock = Any()
    private val _reservations = MutableStateFlow(load())
    val reservations: StateFlow<List<LocalReservation>> = _reservations.asStateFlow()

    fun stateFor(sport: SportType, date: String, time: String, court: Int): String = when {
        reservations.value.any { it.sport == sport && it.date == date && it.timeSlot == time && it.courtNumber == court && it.status == ReservationStatus.ACTIVE } -> "已被本机预约"
        SportsReservationRules.isPastSlot(date, time) -> "模拟不可用"
        isSimulatedUnavailable(sport, date, time, court) -> "模拟不可用"
        else -> "可预约"
    }

    fun reserve(
        sport: SportType,
        date: String,
        time: String,
        court: Int,
    ): LocalReservation? = synchronized(lock) {
        if (!SportsReservationRules.isValidSelection(sport, date, time, court)) return null
        if (stateFor(sport, date, time, court) != "可预约") return null
        val next = LocalReservation(
            UUID.randomUUID().toString(),
            sport,
            date,
            time,
            court,
            sport.priceYuan,
        )
        save(listOf(next) + reservations.value)
        next
    }

    fun cancel(id: String) {
        synchronized(lock) {
            val next = reservations.value.map {
                if (
                    it.id == id &&
                    it.status == ReservationStatus.ACTIVE &&
                    !SportsReservationRules.isExpired(it)
                ) {
                    it.copy(status = ReservationStatus.CANCELLED)
                } else {
                    it
                }
            }
            if (next != reservations.value) save(next)
        }
    }

    private fun isSimulatedUnavailable(sport: SportType, date: String, time: String, court: Int): Boolean =
        (sport.ordinal * 11 + date.hashCode() + time.hashCode() + court) % 17 == 0

    private fun load(): List<LocalReservation> = runCatching {
        gson.fromJson<List<LocalReservation>>(mmkv.decodeString(KEY_RESERVATIONS), type).orEmpty()
            .filter { it.courtNumber in 1..it.sport.count && it.date.isNotBlank() && it.timeSlot in timeSlots }
    }.getOrDefault(emptyList())

    private fun save(value: List<LocalReservation>) {
        val normalized = value
            .distinctBy(LocalReservation::id)
            .sortedByDescending(LocalReservation::createdAtMillis)
        check(mmkv.encode(KEY_RESERVATIONS, gson.toJson(normalized)))
        _reservations.value = normalized
    }

    fun replaceCached(value: List<LocalReservation>) {
        synchronized(lock) { save(value) }
    }

    fun activateAccount(userId: String): List<LocalReservation> = synchronized(lock) {
        val previous = mmkv.decodeString(KEY_ACCOUNT_ID).orEmpty()
        if (previous.isNotBlank() && previous != userId) {
            save(emptyList())
        }
        check(mmkv.encode(KEY_ACCOUNT_ID, userId))
        reservations.value
    }
}

object SportsReservationRules {
    fun isValidSelection(
        sport: SportType,
        date: String,
        time: String,
        court: Int,
        now: LocalDateTime = LocalDateTime.now(),
    ): Boolean {
        val selectedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        val today = now.toLocalDate()
        return selectedDate in today..today.plusDays(6) &&
            time in SportsReservationLocalStore.timeSlots &&
            court in 1..sport.count &&
            !isPastSlot(date, time, now)
    }

    fun isPastSlot(
        date: String,
        time: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): Boolean {
        val selectedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return true
        val endTime = runCatching {
            LocalTime.parse(time.substringAfter('-'), DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull() ?: return true
        return selectedDate.isBefore(now.toLocalDate()) ||
            (selectedDate == now.toLocalDate() && !endTime.isAfter(now.toLocalTime()))
    }

    fun isExpired(
        reservation: LocalReservation,
        now: LocalDateTime = LocalDateTime.now(),
    ): Boolean = reservation.status == ReservationStatus.ACTIVE &&
        isPastSlot(reservation.date, reservation.timeSlot, now)

    fun displayStatus(
        reservation: LocalReservation,
        now: LocalDateTime = LocalDateTime.now(),
    ): String = when {
        reservation.status == ReservationStatus.CANCELLED && reservation.cancelledBy == "ADMIN" ->
            "已取消 · 管理员取消"
        reservation.status == ReservationStatus.CANCELLED -> "已取消"
        isExpired(reservation, now) -> "已过期"
        else -> "已预约 · 模拟支付成功"
    }
}

@Composable
private fun SportsReservationRoute(onBack: () -> Unit) {
    val reservations by SportsReservationRepository.reservations.collectAsState()
    val reservationContext by SportsReservationRepository.context.collectAsState()
    val connectionState by SportsReservationRepository.connectionState.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var page by remember { mutableStateOf<SportsPage>(SportsPage.Home) }
    var selection by remember { mutableStateOf<BookingSelection?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var profilePromptDismissed by remember { mutableStateOf(false) }
    var profileStudentId by remember { mutableStateOf("") }
    var profileName by remember { mutableStateOf("") }
    var profileConfirmed by remember { mutableStateOf(false) }
    var profileSaving by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        SportsReservationRepository.refreshAll { it?.let { text -> message = text } }
    }
    LaunchedEffect(page) {
        SportsReservationRepository.refreshContext()
    }
    LaunchedEffect(
        reservationContext?.profile?.studentId,
        reservationContext?.profile?.name,
    ) {
        reservationContext?.profile?.let { profile ->
            if (profileStudentId.isBlank()) profileStudentId = profile.studentId.orEmpty()
            if (profileName.isBlank()) profileName = profile.name.orEmpty()
        }
    }
    when (val current = page) {
        SportsPage.Home -> SportsHome(
            onBack,
            reservationContext,
            onSport = { sport -> selection = BookingSelection(sport); page = SportsPage.Booking },
            onMyReservations = { page = SportsPage.MyReservations },
        )
        SportsPage.Booking -> SportsBooking(
            selection ?: BookingSelection(SportType.BADMINTON),
            reservationContext,
            onBack = { page = SportsPage.Home },
            onContinue = { selection = it; page = SportsPage.Confirm },
        )
        SportsPage.Confirm -> SportsConfirm(
            selection!!,
            reservationContext,
            onBack = { page = SportsPage.Booking },
            onSubmit = { page = SportsPage.Payment },
        )
        SportsPage.Payment -> SportsPayment(selection!!, onBack = { page = SportsPage.Confirm }, onPay = {
            SportsReservationRepository.reserve(
                it.sport,
                it.date,
                it.timeSlot,
                it.courtNumber,
            ) { result, resultMessage ->
                message = resultMessage
                page = if (result == null) SportsPage.Booking else SportsPage.MyReservations
            }
        })
        SportsPage.MyReservations -> SportsMyReservations(
            reservations,
            onBack = { page = SportsPage.Home },
            onCancel = { id ->
                SportsReservationRepository.cancel(id) { resultMessage ->
                    message = resultMessage
                }
            },
            notice = message,
            isOffline = connectionState == SportsConnectionState.OFFLINE,
        )
    }
    val shouldPromptProfile = reservationContext?.let {
        it.rules.reservationProfileMode != "OFF" &&
            (
                it.profile.studentId.isNullOrBlank() ||
                    it.profile.name.isNullOrBlank() ||
                    it.profile.consentedAt == null
                ) &&
            !profilePromptDismissed
    } == true
    if (shouldPromptProfile) {
        val required = reservationContext?.rules?.reservationProfileMode == "REQUIRED"
        AlertDialog(
            onDismissRequest = {
                if (!required) profilePromptDismissed = true
            },
            title = { Text("预约资料补充") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "请填写真实姓名和预约识别学号，用于场馆预约核验、账号查找及后续如适用的退款处理。若资料不真实、不完整或无法核验，可能影响预约服务、人工核验、账户查找及退款处理。",
                        lineHeight = 20.sp,
                    )
                    Text(
                        "该信息仅用于体育预约服务，不会登录学校教务系统，不会读取成绩、课表或其他教务数据。",
                        color = AppTheme.colors.secondaryTextColor,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    ImeCompatibleTextField(
                        value = profileName,
                        onValueChange = {
                            profileName = it.take(40)
                            profileConfirmed = false
                        },
                        label = "真实姓名",
                        placeholder = "请填写用于预约核验的真实姓名",
                    )
                    ImeCompatibleTextField(
                        value = profileStudentId,
                        onValueChange = {
                            profileStudentId = it
                                .filter { char -> char.isLetterOrDigit() || char == '_' || char == '-' }
                                .take(32)
                            profileConfirmed = false
                        },
                        label = "预约识别学号",
                        placeholder = "由你自主填写",
                    )
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = profileConfirmed,
                            onCheckedChange = { profileConfirmed = it },
                        )
                        Text(
                            "我已确认所填写的姓名和学号真实、准确，并同意仅用于预约核验、账号查找及相关订单服务。",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    if (required) {
                        Text(
                            "当前场馆规则要求姓名和预约识别学号均填写并确认后，才能创建预约。",
                            color = AppTheme.colors.commonColor,
                            fontSize = 12.sp,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = profileName.trim().length >= 2 &&
                        profileStudentId.length >= 6 &&
                        profileConfirmed &&
                        !profileSaving,
                    onClick = {
                        profileSaving = true
                        scope.launch {
                            runCatching {
                                CampusToolsSession.updateReservationProfile(
                                    profileStudentId,
                                    profileName,
                                )
                            }.onSuccess {
                                profilePromptDismissed = true
                                profileConfirmed = false
                                SportsReservationRepository.refreshAll()
                                message = "预约资料已保存"
                            }.onFailure {
                                message = it.message ?: "预约资料保存失败"
                            }
                            profileSaving = false
                        }
                    },
                ) {
                    Text(if (profileSaving) "正在保存…" else "明确同意并保存")
                }
            },
            dismissButton = {
                if (!required) {
                    OutlinedButton(onClick = { profilePromptDismissed = true }) {
                        Text("暂不填写并继续")
                    }
                }
            },
        )
    }
}

private sealed interface SportsPage { data object Home : SportsPage; data object Booking : SportsPage; data object Confirm : SportsPage; data object Payment : SportsPage; data object MyReservations : SportsPage }
private data class BookingSelection(
    val sport: SportType,
    val date: String = LocalDate.now().toString(),
    val timeSlot: String = "",
    val courtNumber: Int = 0,
    val priceYuan: Int = sport.priceYuan,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SportsScaffold(title: String, onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    val colors = AppTheme.colors
    Scaffold(containerColor = colors.overviewPageBackgroundColor, topBar = { TopAppBar(
        title = { Column { Text(title, fontWeight = FontWeight.Bold, color = colors.primaryTextColor); Text(SportsReservationLocalStore.LOCAL_NOTICE, color = colors.secondaryTextColor, fontSize = 11.sp) } },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "返回", tint = colors.primaryTextColor) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.overviewPageBackgroundColor)
    ) }) { padding -> content(Modifier.padding(padding).padding(horizontal = 16.dp)) }
}

@Composable
private fun SportsHome(
    onBack: () -> Unit,
    context: ReservationContext?,
    onSport: (SportType) -> Unit,
    onMyReservations: () -> Unit,
) = SportsScaffold("场地预约", onBack) { modifier ->
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("仅用于本地流程演示，价格、状态和预约结果均不代表场馆实际安排。", color = AppTheme.colors.secondaryTextColor, lineHeight = 20.sp)
        ReservationRulesCard(context)
        SportCard(
            SportType.BADMINTON,
            "${SportsReservationRepository.courtCount(SportType.BADMINTON)} 片场地",
            onSport,
        )
        SportCard(
            SportType.TABLE_TENNIS,
            "${SportsReservationRepository.courtCount(SportType.TABLE_TENNIS)} 张球台",
            onSport,
        )
        OutlinedButton(
            onClick = onMyReservations,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.primaryTextColor)
        ) { Text("我的预约") }
    }
}

@Composable private fun SportCard(sport: SportType, sub: String, onClick: (SportType) -> Unit) = Card(
    modifier = Modifier.fillMaxWidth().clickable { onClick(sport) }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AppTheme.colors.bgCardColor)
) { Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.SportsTennis, null, tint = AppTheme.colors.commonColor); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(sport.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.primaryTextColor); Text("$sub · 选择场地与时段", color = AppTheme.colors.secondaryTextColor) }; Text("进入", color = AppTheme.colors.commonColor) } }

@Composable
private fun SportsBooking(
    initial: BookingSelection,
    context: ReservationContext?,
    onBack: () -> Unit,
    onContinue: (BookingSelection) -> Unit,
) {
    var selected by remember(initial.sport) { mutableStateOf(initial) }
    val days = remember { (0L..6L).map { LocalDate.now().plusDays(it) } }
    val connectionState by SportsReservationRepository.connectionState.collectAsState()
    val availability by SportsReservationRepository.availability.collectAsState()
    availability.size
    LaunchedEffect(selected.sport, selected.date) {
        SportsReservationRepository.refreshAvailability(selected.sport, selected.date)
    }
    SportsScaffold("${selected.sport.label}预约", onBack) { modifier -> Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReservationRulesCard(context, compact = true)
        Text("1. 选择日期", fontWeight = FontWeight.Bold, color = AppTheme.colors.primaryTextColor)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(days) { day -> FilterChip(selected = selected.date == day.toString(), onClick = { selected = selected.copy(date = day.toString(), courtNumber = 0) }, label = { Text(day.format(DateTimeFormatter.ofPattern("E MM/dd", Locale.SIMPLIFIED_CHINESE))) }) } }
        Text("2. 选择时间段", fontWeight = FontWeight.Bold, color = AppTheme.colors.primaryTextColor)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(SportsReservationRepository.timeSlots(selected.sport)) { time -> val past = SportsReservationRules.isPastSlot(selected.date, time); FilterChip(selected = selected.timeSlot == time, enabled = !past, onClick = { selected = selected.copy(timeSlot = time, courtNumber = 0) }, label = { Text(if (past) "$time · 已结束" else time) }) } }
        Text("3. 选择${selected.sport.unitLabel}位", fontWeight = FontWeight.Bold, color = AppTheme.colors.primaryTextColor)
        if (selected.timeSlot.isEmpty()) Text("请先选择时间段", color = AppTheme.colors.secondaryTextColor) else LazyVerticalGrid(columns = GridCells.Fixed(if (selected.sport == SportType.BADMINTON) 4 else 2), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(SportsReservationRepository.courtCount(selected.sport)) { index -> val number = index + 1; val state = SportsReservationRepository.stateFor(selected.sport, selected.date, selected.timeSlot, number); val available = state == "可预约"; Surface(modifier = Modifier.height(if (selected.sport == SportType.BADMINTON) 84.dp else 66.dp).clip(RoundedCornerShape(14.dp)).clickable(enabled = available) { selected = selected.copy(courtNumber = number) }, color = when { selected.courtNumber == number -> AppTheme.colors.commonColor; available -> AppTheme.colors.bgCardColor; else -> AppTheme.colors.bgButtonLowlightColor }) { Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("$number 号${selected.sport.unitLabel}", color = if (selected.courtNumber == number) AppTheme.colors.textButtonColor else AppTheme.colors.primaryTextColor, fontWeight = FontWeight.Bold); Text(state, color = if (selected.courtNumber == number) AppTheme.colors.textButtonColor else AppTheme.colors.secondaryTextColor, fontSize = 11.sp, textAlign = TextAlign.Center) } } } }
        if (connectionState == SportsConnectionState.OFFLINE) Text("服务暂时不可用，当前显示本机缓存状态", color = AppTheme.colors.secondaryTextColor, fontSize = 12.sp)
        Button(onClick = { onContinue(selected.copy(priceYuan = SportsReservationRepository.priceYuan(selected.sport))) }, enabled = selected.timeSlot.isNotEmpty() && selected.courtNumber > 0, modifier = Modifier.fillMaxWidth()) { Text("确认选择 · 模拟价格 ${SportsReservationRepository.priceYuan(selected.sport)} 元/小时") }
    } }
}

@Composable private fun SportsConfirm(selection: BookingSelection, context: ReservationContext?, onBack: () -> Unit, onSubmit: () -> Unit) = SportsScaffold("确认预约", onBack) { modifier -> ReservationSummary(modifier, selection, "提交预约", context = context, onAction = onSubmit) }
@Composable private fun SportsPayment(selection: BookingSelection, onBack: () -> Unit, onPay: (BookingSelection) -> Unit) = SportsScaffold("模拟支付", onBack) { modifier -> var submitting by remember { mutableStateOf(false) }; ReservationSummary(modifier, selection, if (submitting) "正在保存…" else "确认模拟支付", actionEnabled = !submitting) { if (!submitting) { submitting = true; onPay(selection) } } }
@Composable private fun ReservationSummary(modifier: Modifier, s: BookingSelection, action: String, actionEnabled: Boolean = true, context: ReservationContext? = null, onAction: () -> Unit) = Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) { context?.let { ReservationRulesCard(it, compact = true) }; Card(colors = CardDefaults.cardColors(containerColor = AppTheme.colors.bgCardColor)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(s.sport.label, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("日期：${s.date}"); Text("时段：${s.timeSlot}"); Text("场地：${s.courtNumber} 号${s.sport.unitLabel}"); context?.profile?.studentId?.let { Text("预约识别学号：$it"); context.profile.name?.let { name -> Text("真实姓名：$name") } }; Text("模拟金额：${s.priceYuan} 元", fontWeight = FontWeight.Bold); Text("模拟价格，不代表场馆实际收费", color = AppTheme.colors.secondaryTextColor, fontSize = 12.sp) } }; Button(onClick = onAction, enabled = actionEnabled, modifier = Modifier.fillMaxWidth()) { Text(action) }; Text("不会连接支付平台、不会生成真实订单。", color = AppTheme.colors.secondaryTextColor, fontSize = 12.sp) }

@Composable
private fun ReservationRulesCard(
    context: ReservationContext?,
    compact: Boolean = false,
) {
    if (context == null) {
        Surface(
            color = AppTheme.colors.bgSecondaryColor,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                "正在读取最新预约规则…",
                color = AppTheme.colors.secondaryTextColor,
                modifier = Modifier.padding(14.dp),
            )
        }
        return
    }
    val rules = context.rules
    val restriction = context.restriction
    Surface(
        color = AppTheme.colors.bgSecondaryColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(if (compact) 12.dp else 15.dp),
        ) {
            Text("预约规则", fontWeight = FontWeight.Bold, color = AppTheme.colors.primaryTextColor)
            Text(
                "每日最多 ${rules.maxActivePerDay} 条有效预约；最多主动取消 ${rules.maxUserCancellationsPerDay} 次；开场前 ${rules.cancelCutoffMinutes} 分钟不可自行取消。",
                color = AppTheme.colors.secondaryTextColor,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Text(
                "今日已预约 ${context.today.activeReservations} 次，剩余 ${context.today.remainingReservations} 次；已主动取消 ${context.today.userCancellations} 次，剩余 ${context.today.remainingCancellations} 次。",
                color = AppTheme.colors.primaryTextColor,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Text(
                when (rules.reservationProfileMode) {
                    "REQUIRED" -> "当前规则：创建预约前必须填写并确认真实姓名和预约识别学号。"
                    "OFF" -> "当前规则：无需填写预约资料。"
                    else -> "当前规则：预约资料可选，可稍后在“账号与安全”中补充。"
                },
                color = if (rules.reservationProfileMode == "REQUIRED") {
                    AppTheme.colors.commonColor
                } else {
                    AppTheme.colors.secondaryTextColor
                },
                fontSize = 12.sp,
            )
            if (restriction.reservationBlocked || restriction.cancellationBlocked) {
                val until = listOfNotNull(
                    restriction.reservationBlockedUntil,
                    restriction.cancellationBlockedUntil,
                ).maxOrNull()?.let {
                    java.text.SimpleDateFormat("MM-dd HH:mm", Locale.SIMPLIFIED_CHINESE)
                        .format(java.util.Date(it))
                }
                Text(
                    "账号限制：${restriction.reason.ifBlank { "请联系管理员处理" }}${until?.let { "（至 $it）" }.orEmpty()}",
                    color = AppTheme.colors.errorRedColor,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            } else {
                Text(
                    "多次取消或违规可能触发临时预约限制。",
                    color = AppTheme.colors.secondaryTextColor,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable private fun SportsMyReservations(reservations: List<LocalReservation>, onBack: () -> Unit, onCancel: (String) -> Unit, notice: String?, isOffline: Boolean) = SportsScaffold("我的预约", onBack) { modifier ->
    var pendingCancel by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        notice?.let { item { Text(it, color = AppTheme.colors.commonColor) } }
        if (isOffline) item { Text("服务暂时不可用，正在显示最近缓存", color = AppTheme.colors.secondaryTextColor) }
        if (reservations.isEmpty()) item { Text("暂无预约记录", color = AppTheme.colors.secondaryTextColor) } else items(reservations, key = LocalReservation::id) { r -> val expired = SportsReservationRules.isExpired(r); Card(colors = CardDefaults.cardColors(containerColor = AppTheme.colors.bgCardColor)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${r.sport.label} · ${r.courtNumber} 号${r.sport.unitLabel}", fontWeight = FontWeight.Bold); Text("${r.date} ${r.timeSlot}"); Text(SportsReservationRules.displayStatus(r), color = AppTheme.colors.secondaryTextColor, fontSize = 12.sp); r.reservationNameSnapshot?.let { Text("真实姓名：$it", color = AppTheme.colors.secondaryTextColor, fontSize = 11.sp) }; r.reservationStudentIdSnapshot?.let { Text("预约识别学号：$it", color = AppTheme.colors.secondaryTextColor, fontSize = 11.sp) }; if (r.status == ReservationStatus.ACTIVE && !r.canCancel && r.cancelUnavailableReason.isNotBlank()) Text("不可取消：${r.cancelUnavailableReason}", color = AppTheme.colors.errorRedColor, fontSize = 11.sp) }; if (r.status == ReservationStatus.ACTIVE && !expired && !isOffline) OutlinedButton(enabled = r.canCancel, onClick = { pendingCancel = r.id }) { Text(if (r.canCancel) "可取消" else "不可取消") } } } }
        if (reservations.any { it.status == ReservationStatus.ACTIVE && !SportsReservationRules.isExpired(it) }) item { Text("取消会同步释放服务端测试库存，不涉及真实退款。", color = AppTheme.colors.secondaryTextColor, fontSize = 12.sp) }
    }
    pendingCancel?.let { id ->
        val reservation = reservations.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { pendingCancel = null },
            title = { Text("取消预约？") },
            text = {
                Text(
                    "本次取消将计入今日第 ${reservation?.nextCancellationCount ?: "—"} 次主动取消，取消后剩余 ${reservation?.remainingCancellationsAfterCancel ?: "—"} 次。场地将恢复可预约，不涉及真实退款。"
                )
            },
            confirmButton = {
                Button(onClick = { onCancel(id); pendingCancel = null }) {
                    Text("确认取消")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingCancel = null }) { Text("保留预约") }
            },
        )
    }
}
