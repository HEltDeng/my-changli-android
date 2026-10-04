package com.mychangli.app.settings.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.mychangli.app.R
import com.mychangli.app.base.FullScreenActivity
import com.mychangli.app.common.data.local.mmkv.UserInfoManager
import com.mychangli.app.common.redux.action.UserAction
import com.mychangli.app.common.redux.store.UserStore
import com.mychangli.app.core.PlanetApplication
import com.mychangli.app.core.Route
import com.mychangli.app.core.privacy.ReleaseDocumentType
import com.mychangli.app.databinding.ActivityBindingUserBinding
import com.mychangli.app.utils.event.AppEventBus
import com.mychangli.app.utils.event.FinishEvent
import com.mychangli.app.widget.view.CustomToast
import com.dcelysia.csust_spider.core.RetrofitUtils
import com.mychangli.app.widget.dialog.SSOWebviewDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.atomic.AtomicLong

/**
 * 绑定用户类
 */
class BindingUserActivity : FullScreenActivity<ActivityBindingUserBinding>() {
    private val username: TextView by lazy { binding.etStudentId }
    private val password: TextView by lazy { binding.etStudentPassword }
    private val back: ImageView by lazy { binding.bindingBack }
    private val save: MaterialButton by lazy { binding.saveUser }
    private val webLoginBtn: MaterialButton by lazy { binding.webLoginBtn }

    private val store = UserStore()
    private var bindingRequestId: Long = 0L

    /**
     * 当前是否为"切换学号"场景。该值决定绑定成功后是否清理旧账号的内容缓存 / 过期用户信息。
     */
    private var isSwitchAccount: Boolean = false

    companion object {
        const val EXTRA_IS_SWITCH_ACCOUNT = "extra_is_switch_account"
        private const val STATE_BINDING_REQUEST_ID = "binding_request_id"
        private val requestSequence = AtomicLong(0L)
    }

    override fun createViewBinding(): ActivityBindingUserBinding =
        ActivityBindingUserBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingRequestId = savedInstanceState
            ?.getLong(STATE_BINDING_REQUEST_ID)
            ?.takeIf { it > 0L }
            ?: requestSequence.incrementAndGet()
        isSwitchAccount = intent.getBooleanExtra(EXTRA_IS_SWITCH_ACCOUNT, false)
        initView()
        initListener()
        store.dispatch(UserAction.initilaize())
        observeState()
    }

    private fun showLoading() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.bindingUserLayout.visibility = View.GONE
        save.isEnabled = false
        webLoginBtn.isEnabled = false
    }

    fun hideLoading() {
        binding.loadingLayout.visibility = View.GONE
        binding.bindingUserLayout.visibility = View.VISIBLE
        webLoginBtn.isEnabled = true
        updateFormState()
    }

    private fun observeState() {
        disposables.add(
            store.state()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { state ->
                    if (state.userStats.studentNumber.isNotBlank()) {
                        username.text = state.userStats.studentNumber
                    }
                    if (!state.uiForLoading) {
                        hideLoading()
                    }
                }
        )
    }

    private fun initView() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppEventBus.finishEvent.collect { finishEvent ->
                    if (
                        finishEvent.name == "bindingUser" &&
                        finishEvent.requestId == bindingRequestId
                    ) {
                        if (isSwitchAccount) {
                            PlanetApplication.clearContentCache()
                            UserInfoManager.clearStaleProfile()
                        }
                        showMessage("学号和密码保存成功！")
                        Route.goHomeForcibly(this@BindingUserActivity)
                        finish()
                    }
                }
            }
        }
        back.imageTintList = android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.color_primary_blue))
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(
                view.paddingLeft,
                insets.top + (10 * resources.displayMetrics.density).toInt(),
                view.paddingRight,
                view.paddingBottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun initListener() {
        username.doAfterTextChanged {
            binding.studentIdInputLayout.error = null
            updateInputHints()
            updateFormState()
        }
        password.doAfterTextChanged {
            binding.passwordInputLayout.error = null
            updateInputHints()
            updateFormState()
        }
        username.setOnFocusChangeListener { _, hasFocus ->
            updateInputHints()
            if (!hasFocus && username.text.isNullOrBlank()) {
                binding.studentIdInputLayout.error = getString(R.string.binding_student_id_required)
            }
        }
        password.setOnFocusChangeListener { _, hasFocus ->
            updateInputHints()
            if (!hasFocus && password.text.isNullOrBlank()) {
                binding.passwordInputLayout.error = getString(R.string.binding_password_required)
            }
        }
        username.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                password.requestFocus()
                true
            } else {
                false
            }
        }
        password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (validateForm(showErrors = true)) {
                    getSystemService(InputMethodManager::class.java)
                        ?.hideSoftInputFromWindow(password.windowToken, 0)
                    saveUserInfo()
                }
                true
            } else {
                false
            }
        }
        save.setOnClickListener { saveUserInfo() }
        back.setOnClickListener { finish() }
        webLoginBtn.setOnClickListener { webLogin() }
        binding.bindingPrivacyPolicyLink.setOnClickListener {
            Route.goReleaseInformation(this, ReleaseDocumentType.PRIVACY)
        }
        updateInputHints()
        updateFormState()
    }

    /**
     * Material 的浮动标签与字段内提示共用同一个 hint 槽，避免两层文字重叠。
     * 空白且未聚焦时展示完整输入提示；聚焦或已有内容时切换为简短字段标签。
     */
    private fun updateInputHints() {
        updateInputHint(
            inputLayout = binding.studentIdInputLayout,
            input = username,
            labelRes = R.string.binding_student_id_label,
            placeholderRes = R.string.binding_student_id_hint
        )
        updateInputHint(
            inputLayout = binding.passwordInputLayout,
            input = password,
            labelRes = R.string.binding_password_label,
            placeholderRes = R.string.binding_password_hint
        )
    }

    private fun updateInputHint(
        inputLayout: TextInputLayout,
        input: TextView,
        @StringRes labelRes: Int,
        @StringRes placeholderRes: Int
    ) {
        val hintRes = if (input.hasFocus() || input.text.isNotEmpty()) {
            labelRes
        } else {
            placeholderRes
        }
        val nextHint = getString(hintRes)
        if (inputLayout.hint != nextHint) {
            inputLayout.hint = nextHint
        }
    }

    private fun webLogin() {
        save.isEnabled = false
        webLoginBtn.isEnabled = false
        SSOWebviewDialog(
            loginResult = { account, stPassword, loginMode, url, cookies ->
                if (url.isNotEmpty() && cookies.isNotEmpty()) {
                    RetrofitUtils.totalCookieJar.saveFromResponse(url.toHttpUrl(), cookies)
                    CustomToast.showMessage(this, "cookies保存成功！")
                } else {
                    CustomToast.showMessage(this, "cookies保存失败")
                }
                if (account.isNotEmpty() && stPassword.isNotEmpty() && loginMode == "Username") {
                    store.dispatch(
                        UserAction.WebLoginSuccess(
                            this,
                            bindingRequestId,
                            account,
                            stPassword
                        )
                    )
                }
            },
            onDismissCallback = {
                webLoginBtn.isEnabled = true
                updateFormState()
            },
        ).show(supportFragmentManager, "SSOWebDialog")
    }

    private fun saveUserInfo() {
        val studentId = username.text.toString()
        val studentPassword = password.text.toString()
        if (!validateForm(showErrors = true)) {
            showMessage("请完整填写学号和密码")
            return
        }
        store.dispatch(
            UserAction.BindingStudentNumber(
                this,
                bindingRequestId,
                studentId,
                studentPassword
            ) { webLogin() }
        )
        showLoading()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_BINDING_REQUEST_ID, bindingRequestId)
        super.onSaveInstanceState(outState)
    }

    private fun updateFormState() {
        save.isEnabled = username.text.isNotBlank() &&
            password.text.isNotBlank() &&
            binding.loadingLayout.visibility != View.VISIBLE
    }

    private fun validateForm(showErrors: Boolean): Boolean {
        val hasStudentId = username.text.isNotBlank()
        val hasPassword = password.text.isNotBlank()
        if (showErrors) {
            binding.studentIdInputLayout.error = if (hasStudentId) {
                null
            } else {
                getString(R.string.binding_student_id_required)
            }
            binding.passwordInputLayout.error = if (hasPassword) {
                null
            } else {
                getString(R.string.binding_password_required)
            }
        }
        updateFormState()
        return hasStudentId && hasPassword
    }

    private fun showMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).apply {
            val cardView = CardView(applicationContext).apply {
                radius = 25f
                cardElevation = 8f
                setCardBackgroundColor(getColor(R.color.color_base_white))
                useCompatPadding = true
            }

            val textView = TextView(applicationContext).apply {
                text = message
                textSize = 17f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(80, 40, 80, 40)
            }

            cardView.addView(textView)
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 140)
            view = cardView
            show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
