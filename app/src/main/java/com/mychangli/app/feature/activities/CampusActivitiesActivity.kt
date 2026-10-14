package com.mychangli.app.feature.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.shadow
import com.mychangli.app.core.Route
import com.mychangli.app.overview.ui.compose.SectionTitle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.mychangli.app.base.ComposeActivity
import com.mychangli.app.core.theme.AppTheme
import com.mychangli.app.common.data.local.mmkv.StudentInfoManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ActivityBlue = Color(0xFF3978E8)
private fun activityTime(value: Long) = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(value))

class CampusActivitiesActivity : ComposeActivity() {
    private val model: ActivitiesViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = intent.data?.takeIf { it.scheme == "mychangli" && it.host == "activity" }?.lastPathSegment
            ?: intent.getStringExtra("activity_id")
        if (key != null && key.matches(Regex("[a-zA-Z0-9-]{1,80}"))) model.open(key)
        setComposeContent { ActivityCenter(model, ::finish) }
    }
    companion object {
        fun open(context: Context, id: String? = null) = context.startActivity(Intent(context, CampusActivitiesActivity::class.java).apply { id?.let { putExtra("activity_id", it) } })
    }
}

class ActivitiesViewModel : ViewModel() {
    var isBound by mutableStateOf(StudentInfoManager.hasCompleteBinding); private set
    var items by mutableStateOf<List<CampusActivity>>(emptyList()); private set
    var selected by mutableStateOf<CampusActivity?>(null); private set
    var selectedId by mutableStateOf<String?>(null); private set
    var mine by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var identity by mutableStateOf<ActivityIdentity?>(null); private set
    var phoneActivity by mutableStateOf<CampusActivity?>(null); private set
    var savedPhone by mutableStateOf(""); private set
    var success by mutableStateOf<CampusActivity?>(null); private set
    var pending by mutableStateOf<CampusActivity?>(null); private set
    private fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true;error = null
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "网络异常，请重新加载" }
            finally { busy = false }
        }
    }
    fun refresh() {
        isBound = StudentInfoManager.hasCompleteBinding
        if (!isBound && selectedId == null) { items = emptyList(); error = null; return }
        run {
            val loaded = ActivityRepository.list(mine)
            isBound = StudentInfoManager.hasCompleteBinding
            items = if (isBound) loaded else emptyList()
            selectedId?.let { selected = ActivityRepository.detail(it) }
        }
    }
    fun open(id: String) { selectedId = id;run { selected = ActivityRepository.detail(id) } }
    fun back() { selectedId = null;selected = null;refresh() }
    fun tab(value: Boolean) { mine=value;refresh() }
    fun enroll(a: CampusActivity) = run {
        pending = a
        val current = ActivityRepository.identity()
        if (current == null || current.student_number != StudentInfoManager.studentId) {
            identity = ActivityRepository.readSchoolIdentity()
        } else if (a.requires_phone) { savedPhone=current.phone.orEmpty();phoneActivity=a }
        else complete(a)
    }
    private suspend fun complete(a: CampusActivity, phone: String? = null) {
        val result = ActivityRepository.register(a.id, phone)
        if (selectedId != null) selected=result
        success=result;pending=null;phoneActivity=null
        items=ActivityRepository.list(mine)
    }
    fun acceptIdentity() = run {
        val value=identity ?: return@run
        ActivityRepository.confirmIdentity(value);identity=null
        val a=pending
        if (a == null) { items=ActivityRepository.list(mine); return@run }
        if (a.requires_phone) { savedPhone="";phoneActivity=a } else complete(a)
    }
    fun submitPhone(phone: String) = run { complete(phoneActivity ?: return@run,phone) }
    fun cancel(a: CampusActivity) = run {
        val result=ActivityRepository.cancel(a.id)
        if(selectedId!=null) selected=result
        items=ActivityRepository.list(mine)
    }
    fun dismiss() { identity=null;phoneActivity=null;success=null;pending=null }
}

@Composable private fun ActivityResume(model: ActivitiesViewModel) {
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner,model) {
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) model.refresh() }
        owner.lifecycle.addObserver(observer);onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(model) { model.refresh() }
    LaunchedEffect(model) { while(true) { delay(15000);if(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) model.refresh() } }
}

@Composable private fun ActivityCard(a: CampusActivity, onOpen: ()->Unit, onEnroll: ()->Unit, busy: Boolean, compact: Boolean = false) {
    Surface(shape=RoundedCornerShape(if(compact)28.dp else 18.dp),color=Color.White,border=BorderStroke(0.5.dp,Color(0xFFE2ECFA)),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.fillMaxWidth().clickable(onClick=onOpen),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text(a.title,fontSize=if(compact)16.sp else 18.sp,fontWeight=FontWeight.Bold,color=AppTheme.colors.primaryTextColor)
                Text(if(compact) a.organizer_name else "${a.category} · ${a.organizer_name}",fontSize=13.sp,color=ActivityBlue)
                Text("${activityTime(a.start_at)} · ${a.location}",fontSize=14.sp, maxLines=if(compact)1 else Int.MAX_VALUE, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if(!compact) Text("报名截止 ${activityTime(a.signup_end_at)}",fontSize=12.sp,color=AppTheme.colors.secondaryTextColor)
                Text("已报名 ${a.registered_count} / ${a.capacity ?: "不限"} · ${a.state_label}"+(if(a.registration_status=="ACTIVE")" · 我已报名" else if(a.registration_status=="CANCELLED")" · 我已取消" else ""),fontSize=13.sp)
            }
            if(a.can_register) Button(onClick=onEnroll,enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=ActivityBlue),modifier=if(compact) Modifier.align(Alignment.End) else Modifier.fillMaxWidth()) { Text("我要报名") }
        }
    }
}

@Composable private fun RegistrationDialogs(model: ActivitiesViewModel) {
    model.identity?.let { p ->
        AlertDialog(containerColor=Color.White, onDismissRequest={if(!model.busy)model.dismiss()},title={Text("确认报名身份")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${p.student_name}\n${p.student_number}\n${p.college_id}")
            Text("以上资料自动来自当前学校账号，不能在报名表中修改。确认后用于活动报名，并向本次活动有权限的老师提供姓名、学号、学院。",fontSize=14.sp)
            model.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        }},confirmButton={TextButton(onClick=model::acceptIdentity,enabled=!model.busy){Text("确认并继续")}},dismissButton={TextButton(onClick=model::dismiss,enabled=!model.busy){Text("暂不报名")}})
    }
    model.phoneActivity?.let { a ->
        var phone by remember(a.id) { mutableStateOf(model.savedPhone) }
        AlertDialog(containerColor=Color.White, onDismissRequest={if(!model.busy)model.dismiss()},title={Text("确认联系电话")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${a.title} 需要联系电话。请确认当前号码正确，同意提供给本次活动负责老师。")
            OutlinedTextField(value=phone,onValueChange={phone=it},singleLine=true,label={Text("联系电话")},keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Phone))
            model.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        }},confirmButton={TextButton(onClick={model.submitPhone(phone)},enabled=!model.busy&&phone.matches(Regex("1[3-9]\\d{9}"))){Text("确认并报名")}},dismissButton={TextButton(onClick=model::dismiss,enabled=!model.busy){Text("取消")}})
    }
    model.success?.let { a ->
        AlertDialog(containerColor=Color.White, onDismissRequest=model::dismiss,title={Text("✓ 报名成功")},text={Text("${a.title}\n\n${activityTime(a.start_at)}\n${a.location}\n\n已加入“我的活动”")},confirmButton={TextButton(onClick=model::dismiss){Text("完成")}})
    }
}

@Composable private fun ActivityCenter(model: ActivitiesViewModel,onBack:()->Unit) {
    ActivityResume(model)
    val context = LocalContext.current
    var cancel by remember { mutableStateOf<CampusActivity?>(null) }
    var filter by remember { mutableStateOf("待参加") }
    BackHandler(model.selectedId!=null) { model.back() }
    Column(Modifier.fillMaxSize().background(AppTheme.colors.overviewPageBackgroundColor).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick={if(model.selectedId!=null)model.back() else onBack()}) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回") }
            Text(if(model.selectedId==null)"校园活动" else "活动详情",fontSize=20.sp,fontWeight=FontWeight.Bold)
            Spacer(Modifier.weight(1f));TextButton(onClick=model::refresh,enabled=!model.busy){Text("刷新", color=ActivityBlue)}
        }
        if(model.busy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth(),color=ActivityBlue)
        model.error?.let { message ->
            Column(Modifier.padding(16.dp)) { Text(message,color=MaterialTheme.colorScheme.error);TextButton(onClick=model::refresh,enabled=!model.busy){Text("重新加载")} }
        }
        if(model.selectedId==null) {
            Row(Modifier.fillMaxWidth().padding(top=12.dp, bottom=16.dp), horizontalArrangement=Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                ActivityTab("发现活动", !model.mine) { model.tab(false) }
                ActivityTab("我的活动", model.mine) { model.tab(true) }
            }
            if(!model.isBound) TextButton(
                onClick={Route.goBindingUser(context)},
                modifier=Modifier.align(Alignment.CenterHorizontally)
            ) { Text("先绑定学号，查看本学院活动", color=ActivityBlue, fontSize=14.sp) }
            if(model.mine && model.items.isNotEmpty()) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                listOf("待参加","已完成","已取消").forEach { name -> ActivityTab(name, filter==name) { filter=name } }
            }
            val visible=model.items.filter { a -> !model.mine || when(filter) {
                "已取消" -> a.registration_status=="CANCELLED" || a.state=="CANCELLED"
                "已完成" -> a.registration_status=="ACTIVE" && a.state=="ENDED"
                else -> a.registration_status=="ACTIVE" && a.state !in setOf("ENDED","CANCELLED")
            } }
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if(visible.isEmpty()&&!model.busy && model.error==null) item {
                    ActivityEmptyState(
                        title=if(!model.mine) "暂无可报名活动" else if(model.items.isEmpty()) "还没有报名的活动" else "暂无${filter}的活动",
                        subtitle=if(!model.mine) "新活动发布后会显示在这里" else if(model.items.isEmpty()) "报名成功后会显示在这里" else ""
                    )
                }
                items(visible,key={it.id}) { a -> ActivityCard(a,{model.open(a.id)},{model.enroll(a)},model.busy) }
            }
        } else model.selected?.let { a ->
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                item { Text(a.title,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("${a.category} · ${a.state_label}",color=ActivityBlue,modifier=Modifier.padding(top=10.dp)) }
                item { Surface(shape=RoundedCornerShape(16.dp),color=Color.White,border=BorderStroke(1.dp,Color(0xFFE2ECFA))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text("主办单位：${a.organizer_name}");Text("发布学院：${a.college_id}");Text("地点：${a.location}")
                        Text("开始：${activityTime(a.start_at)}");Text("结束：${activityTime(a.end_at)}");Text("时长：约 ${a.duration_minutes} 分钟")
                        Text("报名截止：${activityTime(a.signup_end_at)}");Text("已报名 ${a.registered_count} / ${a.capacity ?: "不限"}")
                    }
                } }
                if(a.latest_update.isNotBlank()) item { Surface(color=Color(0xFFEAF3FF),shape=RoundedCornerShape(12.dp)) { Text("最新情况 / 补充通知\n${a.latest_update}",Modifier.fillMaxWidth().padding(16.dp),color=ActivityBlue) } }
                item { Text(a.summary,fontWeight=FontWeight.Medium);Spacer(Modifier.height(16.dp));Text(a.description,lineHeight=25.sp) }
                if(a.contact.isNotBlank()) item { Text("联系方式：${a.contact}") }
            }
            Surface(color=Color.White) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if(a.registration_status=="ACTIVE") Text("已报名",fontWeight=FontWeight.Bold,color=ActivityBlue)
                    if(a.can_cancel) OutlinedButton(onClick={cancel=a},enabled=!model.busy,modifier=Modifier.fillMaxWidth()) { Text("取消报名") }
                    else Button(onClick={model.enroll(a)},enabled=a.can_register&&!model.busy,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=ActivityBlue)) {
                        Text(if(a.registration_status=="ACTIVE")"已报名" else when(a.state){"OPEN"->"我要报名";"FULL"->"名额已满";"CLOSED"->"报名已截止";"ENDED"->"活动已结束";"CANCELLED"->"活动已取消";else->a.state_label})
                    }
                }
            }
        }
    }
    cancel?.let { a -> AlertDialog(containerColor=Color.White, onDismissRequest={cancel=null},title={Text("确定取消本次活动报名吗？")},text={Text("取消后将释放该活动名额。")},confirmButton={TextButton(onClick={cancel=null;model.cancel(a)}){Text("取消报名")}},dismissButton={TextButton(onClick={cancel=null}){Text("继续参加")}}) }
    RegistrationDialogs(model)
}

@Composable private fun ActivityTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick=onClick, shape=RoundedCornerShape(50),
        color=if(selected) Color(0xFFE4F0FC) else Color.White,
        border=if(selected) null else BorderStroke(0.5.dp, Color(0xFFE4E8EE))
    ) {
        Text(title, modifier=Modifier.padding(horizontal=20.dp, vertical=11.dp),
            fontSize=14.sp, fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Normal,
            color=if(selected) ActivityBlue else AppTheme.colors.secondaryTextColor)
    }
}

@Composable private fun ActivityEmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top=64.dp, bottom=32.dp), horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize=16.sp, color=Color(0xFF727984), textAlign=TextAlign.Center)
        if(subtitle.isNotBlank()) Text(subtitle, fontSize=13.sp, color=AppTheme.colors.secondaryTextColor, textAlign=TextAlign.Center)
    }
}

@Composable fun RecentActivities(onBindClick: () -> Unit) {
    val context=LocalContext.current
    val model: ActivitiesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key="recent_activities")
    ActivityResume(model)
    val changes by ActivityRepository.changes.collectAsState()
    LaunchedEffect(changes) { model.refresh() }
    Column(verticalArrangement=Arrangement.spacedBy(18.dp)) {
        SectionTitle("近期活动", "全部") { CampusActivitiesActivity.open(context) }
        val recent=model.items.filter { it.can_register }.take(1)
        if(!model.isBound || model.error!=null || recent.isEmpty()) {
            val label=when {
                !model.isBound -> "先绑定学号"
                model.error!=null -> "活动暂时无法加载 · 重新加载"
                model.busy -> "正在加载活动"
                else -> "暂无可报名活动"
            }
            Surface(
                onClick={ when {
                    !model.isBound -> onBindClick()
                    model.error!=null -> model.refresh()
                    else -> CampusActivitiesActivity.open(context)
                } },
                shape=RoundedCornerShape(28.dp), color=AppTheme.colors.bgCardColor,
                modifier=Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(28.dp), ambientColor=Color(0x227B9FCB), spotColor=Color(0x227B9FCB))
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal=22.dp, vertical=26.dp), verticalAlignment=Alignment.CenterVertically) {
                    Text(label, fontSize=if(model.isBound)16.sp else 18.sp,
                        fontWeight=if(model.isBound)FontWeight.Normal else FontWeight.Bold,
                        color=if(model.isBound)AppTheme.colors.secondaryTextColor else AppTheme.colors.primaryTextColor,
                        modifier=Modifier.weight(1f))
                    Text("›", fontSize=24.sp, color=AppTheme.colors.secondaryTextColor)
                }
            }
        } else recent.forEach { a -> ActivityCard(a,{CampusActivitiesActivity.open(context,a.id)},{model.enroll(a)},model.busy,compact=true) }
    }
    RegistrationDialogs(model)
}
