package com.example.workouttimer

import android.animation.ObjectAnimator
import android.os.CountDownTimer
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import java.util.concurrent.TimeUnit

class Timer(millisInFuture: Long, countDownInterval: Long, private var text: TextView, private var progressBar: ProgressBar, private val initialRestTime: Long, private var isSlow: Boolean
) :
    CountDownTimer(millisInFuture, countDownInterval) {

    private var millisLeft: Long = 0
    private var previousProgress = 1000


    fun getTime(): Long {
        return millisLeft
    }

    override fun onFinish() {
        println("Done!")
        progressBar.visibility = View.INVISIBLE
        println("Progress?")
    }

    override fun onTick(millisUntilFinished: Long) {
        if (progressBar.visibility == View.INVISIBLE) {
            progressBar.visibility = View.VISIBLE
        }
        millisLeft = millisUntilFinished
        updateScreen()


    }

    fun updateScreen() {
        val currentProgress = ((millisLeft.toDouble() / initialRestTime.toDouble())*1000).toInt()

        println(((millisLeft.toDouble() / initialRestTime.toDouble())*1000).toInt())

        if (!isSlow) {
            val animation =
                ObjectAnimator.ofInt(progressBar, "progress", currentProgress)
            previousProgress = currentProgress
            animation.duration = 100
            animation.interpolator = LinearInterpolator()
            animation.start()
        } else {
            progressBar.progress = currentProgress
        }

        text.text = String.format("%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millisLeft)%60,
            TimeUnit.MILLISECONDS.toSeconds(millisLeft)%60)
    }

    fun createNewTimer(timeToAdd: Long, createSlow: Boolean): Timer {
        val interval: Long = if (createSlow) 1000 else {
            100
        }
        return when {
            createSlow -> {
                Timer(this.getTime(), interval, text, progressBar, initialRestTime, createSlow)
            }
            getTime() + timeToAdd > initialRestTime -> {
                Timer(initialRestTime, interval, text, progressBar, initialRestTime, false)
            }
            else -> {
                Timer(getTime() + timeToAdd, interval, text, progressBar, initialRestTime, false)
            }
        }
    }

}