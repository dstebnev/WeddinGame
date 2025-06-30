package com.sshex.gameapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.random.Random

class Obstacle(context: Context, screenWidth: Int, gameView: GameView) {

    private val type = Random.nextInt(1, 4)
    private val bitmap = gameView.getCachedObstacleBitmap(type)
    private val width = bitmap.width
    private val height = bitmap.height

    private var x = screenWidth.toFloat()
    private val y = Player.FLOOR_Y
    private val speed = 10f

    fun update() {
        x -= speed
    }

    fun isOffScreen(): Boolean = x + width < 0

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y - height, null)
    }

    fun getRect(): RectF {
        val margin = 20
        return RectF(x + margin, y - height + margin, x + width - margin, y - margin)
    }
}
