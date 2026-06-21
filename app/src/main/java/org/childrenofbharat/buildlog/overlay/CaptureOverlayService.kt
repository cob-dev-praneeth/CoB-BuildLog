package org.childrenofbharat.buildlog.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.childrenofbharat.buildlog.BuildLogApplication
import org.childrenofbharat.buildlog.MainActivity
import kotlin.math.abs

class CaptureOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var bubble: View? = null
    private var captureCard: View? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var bubbleX: Int? = null
    private var bubbleY: Int? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        if (Settings.canDrawOverlays(this)) showBubble()
    }

    private fun showBubble() {
        if (bubble != null || captureCard != null || !Settings.canDrawOverlays(this)) return

        val size = dp(64)
        val view = TextView(this).apply {
            text = "+"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(INK)
            background = roundedBackground(ACID, size / 2f, INK, dp(2))
            elevation = dp(12).toFloat()
            contentDescription = "Open CoB Build Log quick capture"
        }
        bubbleParams = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleX ?: (resources.displayMetrics.widthPixels - size)
            y = bubbleY ?: (resources.displayMetrics.heightPixels / 3)
        }

        var downX = 0
        var downY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = bubbleParams.x
                    downY = bubbleParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    moved = moved || abs(dx) > dp(4) || abs(dy) > dp(4)
                    bubbleParams.x = downX + dx
                    bubbleParams.y = downY + dy
                    windowManager.updateViewLayout(view, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) dockToEdge(view) else showCaptureCard()
                    true
                }
                else -> false
            }
        }

        bubble = view
        windowManager.addView(view, bubbleParams)
    }

    private fun dockToEdge(view: View) {
        val screenWidth = resources.displayMetrics.widthPixels
        bubbleParams.x = if (bubbleParams.x + view.width / 2 < screenWidth / 2) {
            0
        } else {
            screenWidth - view.width
        }
        bubbleX = bubbleParams.x
        bubbleY = bubbleParams.y
        windowManager.updateViewLayout(view, bubbleParams)
    }

    private fun showCaptureCard() {
        if (captureCard != null) return
        bubbleX = bubbleParams.x
        bubbleY = bubbleParams.y
        removeBubble()

        val count = TextView(this).apply {
            text = "0 characters"
            textSize = 12f
            setTextColor(MUTED)
            gravity = Gravity.END
        }
        val send = Button(this).apply {
            text = "Send"
            isAllCaps = false
            isEnabled = false
            setTextColor(INK)
            backgroundTintList = ColorStateList.valueOf(ACID)
        }
        val input = EditText(this).apply {
            hint = "What are you building?"
            textSize = 17f
            setTextColor(PAPER)
            setHintTextColor(MUTED)
            gravity = Gravity.TOP or Gravity.START
            minLines = 4
            maxLines = 7
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground(SURFACE_VARIANT, dp(14).toFloat(), OUTLINE, dp(1))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, countValue: Int) {
                    val length = text?.length ?: 0
                    count.text = if (length == 1) "1 character" else "$length characters"
                    send.isEnabled = !text.isNullOrBlank()
                }
                override fun afterTextChanged(text: Editable?) = Unit
            })
        }
        val cancel = Button(this).apply {
            text = "Cancel"
            isAllCaps = false
            setTextColor(PAPER)
            backgroundTintList = ColorStateList.valueOf(SURFACE_VARIANT)
            setOnClickListener { closeCaptureCard() }
        }
        send.setOnClickListener {
            val content = input.text.toString()
            if (content.isBlank()) return@setOnClickListener
            send.isEnabled = false
            cancel.isEnabled = false
            input.isEnabled = false
            send.text = "Saving…"
            saveNote(content, send, cancel, input)
        }

        val title = TextView(this).apply {
            text = "QUICK CAPTURE"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.12f
            setTextColor(ACID)
        }
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            addView(cancel, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                marginEnd = dp(8)
            })
            addView(send, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(14))
            background = roundedBackground(SURFACE, dp(22).toFloat(), OUTLINE, dp(1))
            elevation = dp(18).toFloat()
            addView(title, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(input, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = dp(12)
            })
            addView(count, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = dp(6)
            })
            addView(actionRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = dp(8)
            })
        }

        val width = minOf(resources.displayMetrics.widthPixels - dp(32), dp(420))
        val cardParams = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(56)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        captureCard = card
        windowManager.addView(card, cardParams)
        input.requestFocus()
        input.postDelayed({
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, KEYBOARD_DELAY_MS)
    }

    private fun saveNote(content: String, send: Button, cancel: Button, input: EditText) {
        serviceScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    (application as BuildLogApplication).repository.captureNote(
                        content = content,
                        projectId = null,
                        tags = emptyList()
                    )
                }
                closeCaptureCard()
            } catch (error: Exception) {
                send.text = "Send"
                send.isEnabled = true
                cancel.isEnabled = true
                input.isEnabled = true
                Toast.makeText(this@CaptureOverlayService, "Could not save note", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun closeCaptureCard() {
        captureCard?.let {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken, 0)
            windowManager.removeView(it)
        }
        captureCard = null
        showBubble()
    }

    private fun removeBubble() {
        bubble?.let { windowManager.removeView(it) }
        bubble = null
    }

    private fun roundedBackground(
        fillColor: Int,
        cornerRadius: Float,
        strokeColor: Int? = null,
        strokeWidth: Int = 0
    ) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fillColor)
        this.cornerRadius = cornerRadius
        if (strokeColor != null && strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun notification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(org.childrenofbharat.buildlog.R.drawable.ic_launcher_foreground)
        .setContentTitle("Build Log is ready")
        .setContentText("Tap the floating + to capture")
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Floating capture",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    override fun onDestroy() {
        captureCard?.let { windowManager.removeView(it) }
        captureCard = null
        removeBubble()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "capture_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val KEYBOARD_DELAY_MS = 120L
        private const val MATCH_PARENT = LinearLayout.LayoutParams.MATCH_PARENT
        private const val WRAP_CONTENT = LinearLayout.LayoutParams.WRAP_CONTENT

        private val INK = Color.rgb(17, 18, 14)
        private val PAPER = Color.rgb(246, 246, 238)
        private val ACID = Color.rgb(233, 255, 112)
        private val MUTED = Color.rgb(168, 170, 158)
        private val SURFACE = Color.rgb(27, 28, 23)
        private val SURFACE_VARIANT = Color.rgb(41, 42, 36)
        private val OUTLINE = Color.rgb(69, 70, 63)

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CaptureOverlayService::class.java)
            )
        }
    }
}
