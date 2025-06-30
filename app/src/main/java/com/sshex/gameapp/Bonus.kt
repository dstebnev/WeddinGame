package com.sshex.gameapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.random.Random

class Bonus(context: Context, screenWidth: Int, gameView: GameView) {

    private val type = Random.nextInt(1, 10)
    private val bitmap = gameView.getCachedBonusBitmap(type)
    private val width = bitmap.width
    private val height = bitmap.height

    private var x = screenWidth.toFloat()
    private val y = Player.FLOOR_Y - 200f
    private val speed = 10f

    fun update() {
        x -= speed
    }

    fun isOffScreen(): Boolean = x + width < 0

    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y - height, null)
    }

    fun getRect(): RectF {
        val margin = 10
        return RectF(x + margin, y - height + margin, x + width - margin, y - margin)
    }
}
