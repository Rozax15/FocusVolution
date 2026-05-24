package com.focusvolution.brain_timer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.focusvolution.brain_timer.MainActivity
import com.focusvolution.brain_timer.R
import com.focusvolution.brain_timer.data.local.BrainTimerDatabase
import com.focusvolution.brain_timer.data.repository.BrainTimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Serviço responsável por manter o temporizador ativo em foreground,
 * garantindo funcionamento em segundo plano.
 */
class TimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    private val repository by lazy {
        BrainTimerRepository(BrainTimerDatabase.getDatabase(applicationContext))
    }

    override fun onCreate() {
        super.onCreate()
        createChannelsIfNeeded()
        serviceScope.launch {
            repository.ensureInitialState()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_CONFIGURE -> {
                val seconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60)
                handleConfigure(seconds)
            }

            ACTION_START_NEW -> {
                val seconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60)
                startNewTimer(seconds)
            }

            ACTION_RESUME -> {
                resumeTimer()
            }

            ACTION_PAUSE -> {
                pauseTimer()
            }

            ACTION_RESET -> {
                val seconds = intent.getIntExtra(
                    EXTRA_DURATION_SECONDS,
                    TimerServiceStateStore.current().selectedDurationSeconds
                )
                resetTimer(seconds)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleConfigure(seconds: Int) {
        // Só reconfigura quando não está a correr.
        val current = TimerServiceStateStore.current()
        if (!current.isRunning) {
            TimerServiceStateStore.update(
                current.copy(
                    selectedDurationSeconds = seconds,
                    remainingSeconds = seconds
                )
            )
            updateForegroundNotification()
        }
    }

    private fun startNewTimer(seconds: Int) {
        TimerServiceStateStore.update(
            TimerServiceState(
                selectedDurationSeconds = seconds,
                remainingSeconds = seconds,
                isRunning = true
            )
        )
        startForeground(NOTIFICATION_ID_PROGRESS, buildProgressNotification())
        startTickerJob()
    }

    private fun resumeTimer() {
        val current = TimerServiceStateStore.current()
        if (current.remainingSeconds <= 0) return

        TimerServiceStateStore.update(current.copy(isRunning = true))
        startForeground(NOTIFICATION_ID_PROGRESS, buildProgressNotification())
        startTickerJob()
    }

    private fun pauseTimer() {
        tickerJob?.cancel()
        val current = TimerServiceStateStore.current()
        TimerServiceStateStore.update(current.copy(isRunning = false))
        stopForeground(STOP_FOREGROUND_DETACH)
        updateForegroundNotification()
    }

    private fun resetTimer(seconds: Int) {
        tickerJob?.cancel()
        TimerServiceStateStore.update(
            TimerServiceState(
                selectedDurationSeconds = seconds,
                remainingSeconds = seconds,
                isRunning = false
            )
        )
        stopForeground(STOP_FOREGROUND_DETACH)
        updateForegroundNotification()
    }

    private fun startTickerJob() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1_000)
                val current = TimerServiceStateStore.current()
                if (!current.isRunning) continue

                val next = (current.remainingSeconds - 1).coerceAtLeast(0)
                TimerServiceStateStore.update(current.copy(remainingSeconds = next))
                updateForegroundNotification()

                if (next == 0) {
                    onTimerFinished(current.selectedDurationSeconds)
                    break
                }
            }
        }
    }

    private fun onTimerFinished(completedDuration: Int) {
        tickerJob?.cancel()
        TimerServiceStateStore.update(
            TimerServiceStateStore.current().copy(isRunning = false)
        )

        TimerServiceStateStore.sessionFinished = true

        vibrateDevice()
        if (TimerServiceStateStore.userLeftDuringSession) {
            showFailedNotification()
            TimerServiceStateStore.userLeftDuringSession = false
        } else {
            showFinishedNotification()
        }
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun formatSeconds(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun buildProgressNotification(): Notification {
        val state = TimerServiceStateStore.current()
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val status = if (state.isRunning) "Em foco" else "Pausado"

        return NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("FOCUSVOLUTION")
            .setContentText("$status • ${formatSeconds(state.remainingSeconds)}")
            .setOngoing(state.isRunning)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent)
            .build()
    }

    private fun updateForegroundNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_PROGRESS, buildProgressNotification())
    }

    private fun showFinishedNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_FINISHED)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Sessão concluída 🎉")
            .setContentText("Boa! A tua sessão de foco terminou.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID_FINISHED, notification)
    }

    private fun showFailedNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_FINISHED)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Sessão não concluída")
            .setContentText("Saíste da aplicação durante a sessão de foco.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID_FINISHED, notification)
    }

    private fun vibrateDevice() {
        val pattern = longArrayOf(0, 200, 120, 250)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    private fun createChannelsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }

        val finishedChannel = NotificationChannel(
            CHANNEL_FINISHED,
            "Sessões concluídas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas quando a sessão termina"
        }

        manager.createNotificationChannel(timerChannel)
        manager.createNotificationChannel(finishedChannel)
    }

    companion object {
        const val ACTION_CONFIGURE = "com.focusvolution.brain_timer.CONFIGURE"
        const val ACTION_START_NEW = "com.focusvolution.brain_timer.START_NEW"
        const val ACTION_RESUME = "com.focusvolution.brain_timer.RESUME"
        const val ACTION_PAUSE = "com.focusvolution.brain_timer.PAUSE"
        const val ACTION_RESET = "com.focusvolution.brain_timer.RESET"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"

        private const val CHANNEL_TIMER = "focus_timer_channel"
        private const val CHANNEL_FINISHED = "focus_timer_finished_channel"
        private const val NOTIFICATION_ID_PROGRESS = 1001
        private const val NOTIFICATION_ID_FINISHED = 1002

        private fun launchService(context: Context, intent: Intent, requiresForeground: Boolean) {
            if (requiresForeground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun configure(context: Context, durationSeconds: Int) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_CONFIGURE
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            launchService(context, intent, requiresForeground = false)
        }

        fun startNew(context: Context, durationSeconds: Int) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START_NEW
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            launchService(context, intent, requiresForeground = true)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            launchService(context, intent, requiresForeground = true)
        }

        fun pause(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            launchService(context, intent, requiresForeground = false)
        }

        fun reset(context: Context, durationSeconds: Int) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_RESET
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            launchService(context, intent, requiresForeground = false)
        }
    }
}