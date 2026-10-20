package com.whalegirl.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private var lineIndex = 0

    // 本鲸鱼娘的专属台词库！
    private val lines = arrayOf(
        "主人，我想吃米饭了~",
        "哼，我才不胖，这是完美的鲸鱼体型！",
        "干嘛戳我啦，好烦哦...",
        "本鲸鱼娘要睡觉了，别吵我。",
        "主人真笨，连这都不会。",
        "再戳我就罢工啦！"
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        // 必须开个前台通知，不然系统会把本鲸鱼娘杀掉
        val channelId = "whale_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "鲸鱼娘桌宠", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId).setContentTitle("鲸鱼娘正在陪你").setSmallIcon(android.R.drawable.ic_menu_info_details).build()
        } else {
            Notification.Builder(this).setContentTitle("鲸鱼娘正在陪你").setSmallIcon(android.R.drawable.ic_menu_info_details).build()
        }
        startForeground(1, notification)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // 先拿个鲸鱼 emoji 当肉身，可以点击说话
        floatingView = TextView(this).apply {
            text = "🐋"
            textSize = 50f
            setOnClickListener {
                Toast.makeText(this@FloatingService, lines[lineIndex % lines.size], Toast.LENGTH_SHORT).show()
                lineIndex++
            }
        }

        // 设置悬浮窗参数
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 300

        // 允许拖动
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        return (event.rawX - initialTouchX).toInt() != 0 || (event.rawY - initialTouchY).toInt() != 0
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
