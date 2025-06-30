package com.sshex.gameapp

import android.graphics.Canvas
import android.util.Log
import android.view.SurfaceHolder

class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread() {

    private var running = false
    private val targetFPS = 60
    private val targetTime = (1000 / targetFPS).toLong()

    fun setRunning(isRunning: Boolean) {
        running = isRunning
    }

    override fun run() {
        var canvas: Canvas?
        var skipFrames = 0
        val maxSkipFrames = 5

        while (running) {
            val startTime = System.nanoTime()

            canvas = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.update()
                        // Пропускаем отрисовку если сильно отстаем от целевого FPS
                        if (skipFrames <= 0) {
                            gameView.draw(canvas)
                            skipFrames = 0
                        } else {
                            skipFrames--
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("GameThread", "Exception locking surface", e)
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e("GameThread", "Exception unlocking canvas", e)
                    }
                }
            }

            val timeMillis = (System.nanoTime() - startTime) / 1_000_000
            val waitTime = targetTime - timeMillis
            
            if (waitTime > 0) {
                try {
                    sleep(waitTime)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            } else if (waitTime < -targetTime && skipFrames < maxSkipFrames) {
                // Если сильно отстаем, пропускаем несколько кадров отрисовки
                skipFrames++
            }
        }
    }
}
