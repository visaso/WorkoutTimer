package com.example.workouttimer

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit


class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    private lateinit var timer: Timer

    private var isAmbient: Boolean = false


    private lateinit var progressBar: ProgressBar
    private val initialRestTime: Long = 90000
    private val timeToAdd: Long = 30000
    private var set: Int = 1


    private lateinit var ambientController: AmbientModeSupport.AmbientController


    private val TAG = "MainActivity"
    private val MSG_UPDATE_SCREEN = 0

    private val ACTIVE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1)

    private val AMBIENT_INTERVAL_MS = TimeUnit.SECONDS.toMillis(3)


    private val AMBIENT_UPDATE_ACTION = "com.example.workouttimer.action.AMBIENT_UPDATE"
    private lateinit var ambientUpdateAlarmManager: AlarmManager
    private lateinit var ambientUpdatePendingIntent: PendingIntent
    private lateinit var ambientUpdateBroadcastReceiver: BroadcastReceiver


    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback = MyAmbientCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ambientController = AmbientModeSupport.attach(this)

        ambientUpdateAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        ambientUpdatePendingIntent = Intent(AMBIENT_UPDATE_ACTION).let { ambientUpdateIntent ->
            PendingIntent.getBroadcast(
                this,
                0,
                ambientUpdateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        ambientUpdateBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                refreshDisplayAndSetNextUpdate()
                Log.d("Hey", "Updated")
            }
        }


        progressBar = timerBar
        setNumber.text = String.format(resources.getString(R.string.set_1_3), set)

        timer = Timer(initialRestTime, 1000, text, progressBar, initialRestTime, false)
        timer.start()

        button.setOnClickListener {
            addRestTime()
        }
    }

    private fun refreshDisplayAndSetNextUpdate() {
        timer.updateScreen()
        if (isAmbient) {
            println("Ambient")
        } else {
            println("Not ambient")
        }
        val timeMs: Long = System.currentTimeMillis()
        // Schedule a new alarm
        if (ambientController.isAmbient) {
            val delayMs = AMBIENT_INTERVAL_MS - (timeMs % AMBIENT_INTERVAL_MS);
            val triggerTimeMs = timeMs + delayMs;

            ambientUpdateAlarmManager.setExact(
                AlarmManager.RTC_WAKEUP, triggerTimeMs, ambientUpdatePendingIntent);
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(AMBIENT_UPDATE_ACTION)
        registerReceiver(ambientUpdateBroadcastReceiver, filter)

        refreshDisplayAndSetNextUpdate()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(ambientUpdateBroadcastReceiver);

        ambientUpdateAlarmManager.cancel(ambientUpdatePendingIntent);
    }

    override fun onDestroy() {
        ambientUpdateAlarmManager.cancel(ambientUpdatePendingIntent)
        super.onDestroy()
    }

    private fun ambientTimer(toggle: Boolean) {
        timer.cancel()
        timer = timer.createNewTimer(0, toggle)
        timer.start()
    }


    private fun addRestTime() {
        timer.cancel()
        timer = timer.createNewTimer(timeToAdd, false)
        timer.start()
    }

    private fun resetTimer() {
        timer.cancel()
        timer = timer.createNewTimer(initialRestTime, false)
        timer.start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_STEM_1 -> {
                    println("HEYAA")
                    true
                }
                KeyEvent.KEYCODE_STEM_2 -> {
                    if (set < 3) {
                        set++
                        setNumber.text = String.format(resources.getString(R.string.set_1_3), set)
                        resetTimer()
                    } else {
                        Toast.makeText(this, "Sets full", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                KeyEvent.KEYCODE_STEM_3 -> {
                    set = 1
                    setNumber.text = String.format(resources.getString(R.string.set_1_3), set)
                    resetTimer()
                    true
                }
                else -> {
                    super.onKeyDown(keyCode, event)
                }
            }
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    private inner class MyAmbientCallback(val activity: FragmentActivity) : AmbientModeSupport.AmbientCallback() {

        override fun onEnterAmbient(ambientDetails: Bundle?) {
            isAmbient = true
            refreshDisplayAndSetNextUpdate()
            ambientTimer(true)
            val opacityAnimation = AlphaAnimation(1.0f, 0.0f)
            opacityAnimation.apply {
                duration = 700
                interpolator = LinearInterpolator()
            }
            activity.button.startAnimation(opacityAnimation)
            activity.setNumber.startAnimation(opacityAnimation)

            animateColorShift(activity.text, false)
            activity.text.paint.isAntiAlias = false
            activity.button.visibility = View.INVISIBLE
            activity.setNumber.visibility = View.INVISIBLE
            super.onEnterAmbient(ambientDetails)
            Log.d("Enter ambient", "Enter Ambient")
        }

        override fun onExitAmbient() {
            ambientUpdateAlarmManager.cancel(ambientUpdatePendingIntent)
            isAmbient = false
            ambientTimer(false)
            activity.text.paint.isAntiAlias = true
            activity.button.visibility = View.VISIBLE
            activity.setNumber.visibility = View.VISIBLE
            val opacityOnAnimation = AlphaAnimation(0.0f, 1.0f)
            opacityOnAnimation.apply {
                duration = 700
                interpolator = LinearInterpolator()
                startOffset = 200
            }
            animateColorShift(activity.text, true)
            activity.parentView.startAnimation(opacityOnAnimation)
            activity.button.startAnimation(opacityOnAnimation)
            activity.setNumber.startAnimation(opacityOnAnimation)
            super.onExitAmbient()
            Log.d("Exit ambient", "Exit Ambient")
        }

        override fun onUpdateAmbient() {
            super.onUpdateAmbient()
            refreshDisplayAndSetNextUpdate()
        }

        fun animateColorShift(text: TextView, reverse: Boolean) {
            val start: Int
            val finish: Int
            if (reverse) {
                start = Color.WHITE
                finish = activity.getColor(R.color.blue)
            } else {
                start = activity.getColor(R.color.blue)
                finish = Color.WHITE
            }
            val anim = ObjectAnimator.ofInt(text, "textColor", start, finish).apply {
                interpolator = LinearInterpolator()
                duration = 700
                repeatCount = 0
                setEvaluator(ArgbEvaluator())
            }
            anim.start()
        }
    }


}
