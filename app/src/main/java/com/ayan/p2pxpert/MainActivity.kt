package com.ayan.p2pxpert

import LoadingDialog
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.common.api.Releasable
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST


class MainActivity : AppCompatActivity() {
    private lateinit var loginBtn: Button
    private lateinit var togglePassword: ImageButton
    private lateinit var editTextPassword: EditText
    private lateinit var loginEmailGroup: LinearLayout
    private lateinit var showSignupBtn: Button
    private lateinit var backFromSignupBtn: ImageButton
    private lateinit var loginScreen: View
    private lateinit var OtpScreen: View
    private lateinit var SplashScreen: View
    private lateinit var signupScreen: View
    private lateinit var forgotPasswordLink: Button
    private lateinit var forgetScreen: View
    private lateinit var backFromForgetBtn: ImageButton
    private lateinit var loginEmail: EditText
    private lateinit var loginEmailError: TextView
    private lateinit var loginPasswordError: TextView
    private lateinit var otpError: TextView

    // Signup fields
    private lateinit var signupPassword: EditText
    private lateinit var passwordStrengthBar: LinearProgressIndicator
    private lateinit var toggleSignupPassword: ImageButton
    private lateinit var passwordStrengthText: TextView

    // Login layout
    private lateinit var loginEmailLayout: RelativeLayout
    private lateinit var loginPasswordLayout: RelativeLayout

    // Signup layout
    private lateinit var SignupNameLayout: RelativeLayout
    private lateinit var SignupEmailLayout: RelativeLayout
    private lateinit var SignupPasswordLayout: RelativeLayout
    private lateinit var SignupPasswordConfirmLayout: RelativeLayout
    private lateinit var SignupName: EditText
    private lateinit var SignupEmail: EditText
    private lateinit var SignupPhone: EditText
    private lateinit var SignupPassword: EditText
    private lateinit var SignupPasswordConfirm: EditText
    private lateinit var SignupRefer: EditText

    // Forget
    private lateinit var ForgetLayout: RelativeLayout
    private lateinit var Forget: EditText

    // Track first time for each screen
    private var isSignupFirstTime = true
    private var isForgetFirstTime = true

    // Loading Dialog
    private lateinit var loadingDialog: LoadingDialog

    // Networking
    private lateinit var api: P2PXpertApi
    private var currentOtpId: String? = null
    private var currentAuthFlow: AuthFlow = AuthFlow.LOGIN

    // Reset password: after verifying OTP we get a new otpId + otp to use for change-password
    private var resetPasswordOtpId: String? = null
    private var resetPasswordOtp: String? = null
    private var resetEmail: String? = null

    // Buttons (additional screens)
    private lateinit var signupBtn: Button
    private lateinit var resetRequestBtn: Button
    private lateinit var verifyOtpBtn: Button
    private lateinit var resendOtpBtn: Button

    // Change password screen
    private lateinit var changePasswordScreen: View
    private lateinit var changePasswordBack: ImageButton
    private lateinit var changePassword: EditText
    private lateinit var changePasswordConfirm: EditText
    private lateinit var changePasswordBtn: Button
    private lateinit var toggleChangePassword: ImageButton
    private lateinit var toggleChangePasswordConfirm: ImageButton
    private lateinit var changePasswordError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Screens containers Initialize
        loginScreen = findViewById(R.id.login_screen)
        signupScreen = findViewById(R.id.signup_screen)
        SplashScreen = findViewById(R.id.splash_screen)
        OtpScreen = findViewById(R.id.otp_screen)

        SplashScreen.postDelayed({
            SplashScreen.animate()
                .alpha(0f)
                .setDuration(200) // fade-out duration
                .withEndAction {
                    SplashScreen.visibility = View.GONE
                    SplashScreen.alpha = 1f

                    // Show login screen with fade-in
                    loginScreen.visibility = View.VISIBLE
                    loginScreen.alpha = 0f
                    loginScreen.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }, 1000)

        initializeViews()
        setupOtpAutoFocus()
        setupPasswordStrength()
        setupClickListeners()
        setAutoFocus()
        setupApi()

        OneSignal.initWithContext(this, "497ba797-e67a-4b06-b115-8a084bc32901")
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(true)
        }

    }


    private fun setupClickListeners() {
        // Login password toggle
        togglePassword.setOnClickListener {
            togglePasswordVisibility(editTextPassword, togglePassword)
        }

        // Signup password toggle
        toggleSignupPassword.setOnClickListener {
            togglePasswordVisibility(signupPassword, toggleSignupPassword)
        }

        // Show signup screen
        showSignupBtn.setOnClickListener {
            showSignupScreen()
        }

        // Back to login from signup
        backFromSignupBtn.setOnClickListener {
            showLoginScreen()
        }

        // Forgot password link
        forgotPasswordLink.setOnClickListener {
            showForgetScreen()
        }

        // Back from forget screen
        backFromForgetBtn.setOnClickListener {
            showLoginScreen()
        }

        loginBtn.setOnClickListener {
            // Handle login button click
            val email = loginEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            clearLoginErrors()

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                loginEmailError.text = "Please enter a valid email"
                loginEmailError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password.length < 6) {
                loginPasswordError.text = "Password must be at least 6 characters"
                loginPasswordError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            performLogin(email, password)
        }

        signupBtn.setOnClickListener {
            val name = SignupName.text.toString().trim()
            val email = SignupEmail.text.toString().trim()
            val password = SignupPassword.text.toString().trim()
            val confirmPassword = SignupPasswordConfirm.text.toString().trim()

            SignupName.error = null
            SignupEmail.error = null
            SignupPassword.error = null
            SignupPasswordConfirm.error = null

            if (name.isEmpty()) {
                SignupName.error = "Please enter your name"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                SignupEmail.error = "Please enter a valid email"
                return@setOnClickListener
            }

            if (password.length < 6) {
                SignupPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                SignupPasswordConfirm.error = "Passwords do not match"
                return@setOnClickListener
            }

            performSignup(name, email, password, confirmPassword)
        }

        resetRequestBtn.setOnClickListener {
            val identifier = Forget.text.toString().trim()
            Forget.error = null

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                Forget.error = "Please enter a valid email"
                return@setOnClickListener
            }

            performResetPassword(identifier)
        }

        verifyOtpBtn.setOnClickListener {
            clearOtpError()

            val otp = listOf(
                findViewById<EditText>(R.id.otp_1).text.toString().trim(),
                findViewById<EditText>(R.id.otp_2).text.toString().trim(),
                findViewById<EditText>(R.id.otp_3).text.toString().trim(),
                findViewById<EditText>(R.id.otp_4).text.toString().trim(),
                findViewById<EditText>(R.id.otp_5).text.toString().trim(),
                findViewById<EditText>(R.id.otp_6).text.toString().trim()
            ).joinToString("")

            if (otp.length != 6) {
                otpError.text = "Please enter the 6-digit code"
                otpError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            currentOtpId?.let { otpId ->
                performVerifyOtp(otpId, otp)
            } ?: run {
                otpError.text = "OTP session expired. Please retry."
                otpError.visibility = View.VISIBLE
            }
        }

        resendOtpBtn.setOnClickListener {
            currentOtpId?.let { otpId ->
                performResendOtp(otpId)
            } ?: run {
                Toast.makeText(this, "OTP session expired. Please retry.", Toast.LENGTH_SHORT).show()
            }
        }

        // Change password screen actions
        changePasswordBack.setOnClickListener {
            showLoginScreen()
        }

        toggleChangePassword.setOnClickListener {
            togglePasswordVisibility(changePassword, toggleChangePassword)
        }

        toggleChangePasswordConfirm.setOnClickListener {
            togglePasswordVisibility(changePasswordConfirm, toggleChangePasswordConfirm)
        }

        changePasswordBtn.setOnClickListener {
            clearChangePasswordError()

            val newPassword = changePassword.text.toString().trim()
            val confirm = changePasswordConfirm.text.toString().trim()

            if (newPassword.length < 6) {
                changePasswordError.text = "Password must be at least 6 characters"
                changePasswordError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (newPassword != confirm) {
                changePasswordError.text = "Passwords do not match"
                changePasswordError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            performChangePassword(newPassword, confirm)
        }
    }


    override fun onBackPressed() {
        when {
            signupScreen.visibility == View.VISIBLE -> showLoginScreen()
            forgetScreen.visibility == View.VISIBLE -> showLoginScreen()
            OtpScreen.visibility == View.VISIBLE -> showLoginScreen()
            else -> super.onBackPressed()
        }
    }

    private fun setFocusTransition(editText: View, layout: ViewGroup) {
        val transition = layout.background as TransitionDrawable
        editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) transition.startTransition(200)
            else transition.reverseTransition(200)
        }
    }

    private fun setAutoFocus() {
        setFocusTransition(loginEmail, loginEmailLayout)
        setFocusTransition(editTextPassword, loginPasswordLayout)
        setFocusTransition(SignupName, SignupNameLayout)
        setFocusTransition(SignupEmail, SignupEmailLayout)
        setFocusTransition(SignupPassword, SignupPasswordLayout)
        setFocusTransition(SignupPasswordConfirm, SignupPasswordConfirmLayout)
        setFocusTransition(Forget, ForgetLayout)
    }


    private fun initializeViews() {
        loadingDialog = LoadingDialog(this)
        // Login screen views

        loginEmailGroup = findViewById(R.id.login_email_group)
        editTextPassword = findViewById(R.id.login_password)
        togglePassword = findViewById(R.id.toggle_login_password)
        showSignupBtn = findViewById(R.id.show_signup)
        loginBtn = findViewById(R.id.login_btn)
        loginEmail = findViewById(R.id.login_email)

        loginEmailLayout = findViewById(R.id.login_email_layout)
        loginPasswordLayout = findViewById(R.id.login_password_layout)

        // Signup screen views
        SignupNameLayout = findViewById(R.id.signup_name_layout)
        SignupEmailLayout = findViewById(R.id.signup_email_layout)
        SignupPasswordLayout = findViewById(R.id.signup_password_layout)
        SignupPasswordConfirmLayout = findViewById(R.id.signup_confirm_password_layout)
        SignupName = findViewById(R.id.signup_name)
        SignupEmail = findViewById(R.id.signup_email)
        SignupPassword = findViewById(R.id.signup_password)
        SignupPasswordConfirm = findViewById(R.id.signup_confirm_password)

        // Forget
        ForgetLayout = findViewById(R.id.reset_identifier_layout)
        Forget = findViewById(R.id.reset_identifier)

        // Signup screen views
        backFromSignupBtn = findViewById(R.id.back_from_signup)
        signupPassword = findViewById(R.id.signup_password)
        passwordStrengthBar = findViewById(R.id.password_strength_bar)
        toggleSignupPassword = findViewById(R.id.toggle_signup_password)
        passwordStrengthText = findViewById(R.id.password_strength_text)
        forgotPasswordLink = findViewById(R.id.forgot_password_link)
        forgetScreen = findViewById(R.id.forget_screen)
        backFromForgetBtn = findViewById(R.id.back_btn_forget)

        // Additional action buttons
        signupBtn = findViewById(R.id.signup_btn)
        resetRequestBtn = findViewById(R.id.reset_request_btn)
        verifyOtpBtn = findViewById(R.id.verify_otp_btn)
        resendOtpBtn = findViewById(R.id.resend_otp_btn)

        // Change password screen
        changePasswordScreen = findViewById(R.id.change_password_screen)
        changePasswordBack = findViewById(R.id.change_password_back)
        changePassword = findViewById(R.id.change_password)
        changePasswordConfirm = findViewById(R.id.change_password_confirm)
        changePasswordBtn = findViewById(R.id.change_password_btn)
        toggleChangePassword = findViewById(R.id.toggle_change_password)
        toggleChangePasswordConfirm = findViewById(R.id.toggle_change_password_confirm)
        changePasswordError = findViewById(R.id.change_password_error)

        // Error labels
        loginEmailError = findViewById(R.id.login_email_error)
        loginPasswordError = findViewById(R.id.login_password_error)
        otpError = findViewById(R.id.otp_error)

        clearLoginErrors()
        clearOtpError()
    }

    private fun clearLoginErrors() {
        loginEmailError.visibility = View.GONE
        loginPasswordError.visibility = View.GONE
    }

    private fun clearOtpError() {
        otpError.visibility = View.GONE
    }

    private fun clearChangePasswordError() {
        changePasswordError.visibility = View.GONE
    }

    private fun showFieldError(field: String, message: String) {
        when (field.lowercase()) {
            "email", "loginemail", "login_email" -> {
                loginEmailError.text = message
                loginEmailError.visibility = View.VISIBLE
            }
            "password", "loginpassword", "login_password" -> {
                loginPasswordError.text = message
                loginPasswordError.visibility = View.VISIBLE
            }
            "otp", "otp_id", "otpid", "otp_id" -> {
                otpError.text = message
                otpError.visibility = View.VISIBLE
            }
            else -> {
                // fallback
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showApiErrors(errors: Map<String, Any>) {
        errors.forEach { (key, value) ->
            showFieldError(key, value.toString())
        }
    }

    // OTP auto-focus logic Kotlin में
    private fun setupOtpAutoFocus() {
        val otp1 = findViewById<EditText>(R.id.otp_1)
        val otp2 = findViewById<EditText>(R.id.otp_2)
        val otp3 = findViewById<EditText>(R.id.otp_3)
        val otp4 = findViewById<EditText>(R.id.otp_4)
        val otp5 = findViewById<EditText>(R.id.otp_5)
        val otp6 = findViewById<EditText>(R.id.otp_6)

        val otpFields = listOf(otp1, otp2, otp3, otp4, otp5, otp6)

        setupOtpField(otp1, null, otp2, otpFields, 0)
        setupOtpField(otp2, otp1, otp3, otpFields, 1)
        setupOtpField(otp3, otp2, otp4, otpFields, 2)
        setupOtpField(otp4, otp3, otp5, otpFields, 3)
        setupOtpField(otp5, otp4, otp6, otpFields, 4)
        setupOtpField(otp6, otp5, null, otpFields, 5)
    }

    private fun setupOtpField(
        current: EditText,
        previous: EditText?,
        next: EditText?,
        allFields: List<EditText>,
        index: Int
    ) {
        current.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""

                // Handle paste (e.g. user pastes 6 digits into one box)
                if (text.length > 1) {
                    val digits = text.filter { it.isDigit() }
                    if (digits.isNotEmpty()) {
                        current.setText(digits[0].toString())
                        for (i in 1 until digits.length) {
                            val targetIndex = index + i
                            if (targetIndex in allFields.indices) {
                                allFields[targetIndex].setText(digits[i].toString())
                            }
                        }
                        val focusIndex = (index + digits.length).coerceAtMost(allFields.size - 1)
                        allFields[focusIndex].requestFocus()
                    }
                    return
                }

                if (text.length == 1 && next != null) {
                    next.requestFocus()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        current.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL &&
                event.action == KeyEvent.ACTION_DOWN &&
                current.text.isEmpty() &&
                previous != null) {
                previous.requestFocus()
                true
            } else if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                // Pressing enter should try to verify if all digits are entered
                if (allFields.all { it.text.length == 1 }) {
                    verifyOtpBtn.performClick()
                }
                true
            } else {
                false
            }
        }
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }


    private fun togglePasswordVisibility(editText: EditText, toggleBtn: ImageButton) {
        val isCurrentlyHidden =
            editText.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val currentTypeface = editText.typeface
        val cursorPosition = editText.selectionStart

        val animationRes = if (isCurrentlyHidden) {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            R.drawable.anim_eye_to_slash
        } else {
            editText.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            R.drawable.anim_slash_to_eye
        }

        toggleBtn.setImageResource(animationRes)
        val drawable = toggleBtn.drawable
        if (drawable is Animatable) {
            drawable.start()
        }

        editText.typeface = currentTypeface
        editText.setSelection(cursorPosition)
    }

    private var currentColor = "#1e2329"

    private fun setupPasswordStrength() {
        signupPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrength(s.toString())
            }
        })
    }

    private fun updatePasswordStrength(password: String) {
        val (progress, level) = calculatePasswordLevel(password)
        passwordStrengthBar.setProgressCompat(progress, true)
        updateStrengthUI(level)
    }

    private fun calculatePasswordLevel(password: String): Pair<Int, String> {
        if (password.isEmpty()) return 0 to "empty"

        var types = 0
        if (password.any { it.isLowerCase() }) types++
        if (password.any { it.isUpperCase() }) types++
        if (password.any { it.isDigit() }) types++
        if (password.any { !it.isLetterOrDigit() }) types++

        return when {
            password.length < 4 -> 20 to "weak"
            password.length < 8 && types >= 2 -> 55 to "medium"
            password.length >= 8 && types >= 3 -> 100 to "strong"
            else -> 35 to "weak"
        }
    }

    private fun updateStrengthUI(level: String) {
        val (text, textColor, progressColor) = when (level) {
            "empty" -> Triple("Password strength", "#848e9c", "#1e2329")
            "weak" -> Triple("Weak password", "#f6465d", "#f6465d")
            "medium" -> Triple("Medium password", "#f0b90b", "#f0b90b")
            "strong" -> Triple("Strong password", "#02c076", "#02c076")
            else -> Triple("Password strength", "#848e9c", "#1e2329")
        }

        passwordStrengthText.text = text
        passwordStrengthText.setTextColor(Color.parseColor(textColor))

        if (currentColor != progressColor) {
            val colorFrom = Color.parseColor(currentColor)
            val colorTo = Color.parseColor(progressColor)
            ValueAnimator.ofArgb(colorFrom, colorTo).apply {
                duration = 200
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    passwordStrengthBar.setIndicatorColor(animator.animatedValue as Int)
                }
                start()
            }
            currentColor = progressColor
        }
    }

    private fun showSignupScreen() {
        currentFocus?.hideKeyboard()

        if (isSignupFirstTime) {
            // First time for signup - fade animation
            loginScreen.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction {
                    loginScreen.visibility = View.GONE
                    loginScreen.alpha = 1f

                    signupScreen.visibility = View.VISIBLE
                    signupScreen.alpha = 0f
                    signupScreen.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            isSignupFirstTime = false
        } else {
            // SAME ANIMATION: Login slides LEFT, Signup comes from RIGHT
            loginScreen.animate()
                .translationX(-loginScreen.width.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    loginScreen.visibility = View.GONE
                    loginScreen.translationX = 0f
                    loginScreen.alpha = 1f
                }
                .start()

            signupScreen.visibility = View.VISIBLE
            signupScreen.translationX = signupScreen.width.toFloat()
            signupScreen.alpha = 0f

            signupScreen.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun showForgetScreen() {
        currentFocus?.hideKeyboard()

        if (isForgetFirstTime) {
            // First time for forget - fade animation
            loginScreen.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction {
                    loginScreen.visibility = View.GONE
                    loginScreen.alpha = 1f
                    forgetScreen.visibility = View.VISIBLE
                    forgetScreen.alpha = 0f
                    forgetScreen.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            isForgetFirstTime = false
        } else {
            // OPPOSITE ANIMATION: Login slides RIGHT, Forget comes from LEFT
            loginScreen.animate()
                .translationX(loginScreen.width.toFloat()) // Login slides RIGHT
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    loginScreen.visibility = View.GONE
                    loginScreen.translationX = 0f
                    loginScreen.alpha = 1f
                }
                .start()

            forgetScreen.visibility = View.VISIBLE
            forgetScreen.translationX = -forgetScreen.width.toFloat() // Forget starts from LEFT
            forgetScreen.alpha = 0f

            forgetScreen.animate()
                .translationX(0f) // Forget slides in from left to center
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun showLoginScreen() {
        currentFocus?.hideKeyboard()

        clearLoginErrors()
        clearOtpError()
        clearChangePasswordError()
        changePasswordScreen.visibility = View.GONE
        resetPasswordOtpId = null
        resetPasswordOtp = null
        resetEmail = null

        val currentVisibleScreen = when {
            signupScreen.visibility == View.VISIBLE -> signupScreen
            forgetScreen.visibility == View.VISIBLE -> forgetScreen
            OtpScreen.visibility == View.VISIBLE -> OtpScreen
            changePasswordScreen.visibility == View.VISIBLE -> changePasswordScreen
            else -> null
        }

        currentVisibleScreen?.let { screen ->
            if (screen == signupScreen) {
                // SAME ANIMATION for signup -> login
                screen.animate()
                    .translationX(screen.width.toFloat()) // Signup slides RIGHT out
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        screen.visibility = View.GONE
                        screen.translationX = 0f
                        screen.alpha = 1f
                    }
                    .start()

                loginScreen.visibility = View.VISIBLE
                loginScreen.translationX = -loginScreen.width.toFloat() // Login starts from LEFT
                loginScreen.alpha = 0f

                loginScreen.animate()
                    .translationX(0f) // Login slides in from left to center
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            } else if (screen == forgetScreen) {
                // OPPOSITE ANIMATION for forget -> login
                screen.animate()
                    .translationX(-screen.width.toFloat()) // Forget slides LEFT out
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        screen.visibility = View.GONE
                        screen.translationX = 0f
                        screen.alpha = 1f
                    }
                    .start()

                loginScreen.visibility = View.VISIBLE
                loginScreen.translationX = screen.width.toFloat() // Login starts from RIGHT
                loginScreen.alpha = 0f

                loginScreen.animate()
                    .translationX(0f) // Login slides in from right to center
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            } else {
                // OTP -> login
                screen.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        screen.visibility = View.GONE
                        screen.alpha = 1f

                        loginScreen.visibility = View.VISIBLE
                        loginScreen.alpha = 0f
                        loginScreen.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            }
        }
    }

    private fun showOtpScreen() {
        currentFocus?.hideKeyboard()

        // Hide other screens and show OTP screen
        loginScreen.visibility = View.GONE
        signupScreen.visibility = View.GONE
        forgetScreen.visibility = View.GONE
        changePasswordScreen.visibility = View.GONE

        OtpScreen.visibility = View.VISIBLE
        OtpScreen.alpha = 0f
        OtpScreen.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun showChangePasswordScreen() {
        currentFocus?.hideKeyboard()

        // Hide other screens and show change password screen
        loginScreen.visibility = View.GONE
        signupScreen.visibility = View.GONE
        forgetScreen.visibility = View.GONE
        OtpScreen.visibility = View.GONE

        changePasswordError.visibility = View.GONE
        changePassword.setText("")
        changePasswordConfirm.setText("")

        changePasswordScreen.visibility = View.VISIBLE
        changePasswordScreen.alpha = 0f
        changePasswordScreen.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun setupApi() {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logger).build()

        api = Retrofit.Builder()
            .baseUrl("https://super.p2pxpert.com")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(P2PXpertApi::class.java)
    }

    private fun performLogin(email: String, password: String) {
        loadingDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.login(mapOf("email" to email, "password" to password))
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.success) {
                        currentOtpId = response.data?.otpId?.trim()
                        currentAuthFlow = AuthFlow.LOGIN
                        currentOtpId?.let { Log.d("MainActivity", "Received otpId: $it") }
                        Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                        showOtpScreen()
                    } else {
                        if (!response.errors.isNullOrEmpty()) {
                            showApiErrors(response.errors)
                        } else if (!response.field.isNullOrBlank()) {
                            showFieldError(response.field, response.message)
                        } else {
                            Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performSignup(name: String, email: String, password: String, confirmPassword: String) {
        loadingDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.register(
                    mapOf(
                        "name" to name,
                        "email" to email,
                        "password" to password,
                        "confirmPassword" to confirmPassword
                    )
                )
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.success) {
                        currentOtpId = response.data?.otpId?.trim()
                        currentAuthFlow = AuthFlow.SIGNUP
                        currentOtpId?.let { Log.d("MainActivity", "Received otpId: $it") }
                        Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                        showOtpScreen()
                    } else {
                        if (!response.errors.isNullOrEmpty()) {
                            showApiErrors(response.errors)
                        } else if (!response.field.isNullOrBlank()) {
                            showFieldError(response.field, response.message)
                        } else {
                            Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Signup failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performResetPassword(email: String) {
        resetEmail = email
        loadingDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.resetPassword(mapOf("email" to email))
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.success) {
                        currentOtpId = response.data?.otpId?.trim()
                        currentAuthFlow = AuthFlow.RESET_PASSWORD
                        currentOtpId?.let { Log.d("MainActivity", "Received otpId: $it") }
                        Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                        showOtpScreen()
                    } else {
                        if (!response.errors.isNullOrEmpty()) {
                            showApiErrors(response.errors)
                        } else if (!response.field.isNullOrBlank()) {
                            showFieldError(response.field, response.message)
                        } else {
                            Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Reset request failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performVerifyOtp(otpId: String, otp: String) {
        val cleanOtpId = otpId.trim()
        loadingDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.verifyOtp(mapOf("otpId" to cleanOtpId, "otp" to otp))
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.success) {
                        Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()

                        if (currentAuthFlow == AuthFlow.RESET_PASSWORD) {
                            // The reset flow expects a new otpId/otp to be returned and used for change-password
                            resetPasswordOtpId = response.data?.otpId?.trim()
                            // The server may return newOtp or otp field.
                            resetPasswordOtp = response.data?.newOtp?.trim() ?: response.data?.otp?.trim()

                            // Switch to change password screen
                            showChangePasswordScreen()
                        } else {
                            // On successful OTP verification (login/signup), navigate to app home
                            startActivity(android.content.Intent(this@MainActivity, HomeActivity::class.java))
                            finish()
                        }
                    } else {
                        if (!response.errors.isNullOrEmpty()) {
                            showApiErrors(response.errors)
                        } else if (!response.field.isNullOrBlank()) {
                            showFieldError(response.field, response.message)
                        } else {
                            Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@MainActivity, "OTP verification failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performChangePassword(newPassword: String, confirmPassword: String) {
        val otpId = resetPasswordOtpId?.trim()
        val newOtp = resetPasswordOtp?.trim()

        if (otpId.isNullOrBlank() || newOtp.isNullOrBlank()) {
            Toast.makeText(this, "Unable to change password. Please restart the reset flow.", Toast.LENGTH_LONG).show()
            showLoginScreen()
            return
        }

        loadingDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.changePassword(
                    mapOf(
                        "otpId" to otpId,
                        "otp" to newOtp,
                        "newOtp" to newOtp,
                        "password" to newPassword,
                        "confirmPassword" to confirmPassword
                    )
                )
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.success) {
                        Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                        // After password reset success, show forget screen so user can log in again
                        resetEmail?.let { Forget.setText(it) }
                        showForgetScreen()
                    } else {
                        if (!response.errors.isNullOrEmpty()) {
                            showApiErrors(response.errors)
                        } else if (!response.field.isNullOrBlank()) {
                            showFieldError(response.field, response.message)
                        } else {
                            Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Change password failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performResendOtp(otpId: String) {
        val cleanOtpId = otpId.trim()
        loadingDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.resendOtp(mapOf("otpId" to cleanOtpId))
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    if (response.success) {
                        Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Resend OTP failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Fix for first time width measurement
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            loginScreen.translationX = 0f
            signupScreen.translationX = 0f
            forgetScreen.translationX = 0f
        }
    }
}

private enum class AuthFlow {
    LOGIN,
    SIGNUP,
    RESET_PASSWORD
}

private data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val errors: Map<String, Any>? = null,
    val field: String? = null
)

private data class AuthPayload(
    val userid: String?,
    val name: String?,
    val email: String?,
    val jwt_token: String?,
    val otpId: String? = null,
    @SerializedName("newOtp")
    val newOtp: String? = null,
    val otp: String? = null
)

private data class OtpResponse(
    val otpId: String?
)

private interface P2PXpertApi {

    @POST("/api/auth/register")
    suspend fun register(@Body body: Map<String, String>): ApiResponse<OtpResponse>

    @POST("/api/auth/login")
    suspend fun login(@Body body: Map<String, String>): ApiResponse<OtpResponse>

    @POST("/api/auth/verify-otp")
    suspend fun verifyOtp(@Body body: Map<String, String>): ApiResponse<AuthPayload>

    @POST("/api/auth/resend-otp")
    suspend fun resendOtp(@Body body: Map<String, String>): ApiResponse<Unit>

    @POST("/api/auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): ApiResponse<OtpResponse>

    @POST("/api/auth/change-password")
    suspend fun changePassword(@Body body: Map<String, String>): ApiResponse<Unit>

    @GET("/api/auth/auth")
    suspend fun authCheck(@Header("Authorization") authorization: String): ApiResponse<AuthPayload>
}
