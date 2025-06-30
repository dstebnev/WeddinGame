package com.sshex.gameapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.scale

/**
 * Представляет игрового персонажа.
 * Управляет положением игрока, движением (включая прыжки) и отрисовкой.
 */
class Player(context: Context) {

    private val rawBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.player_cat)
    private val bitmap: Bitmap = rawBitmap.scale(120, 120)
    private val width = bitmap.width
    private val height = bitmap.height

    private var x = 100f
    private var y = FLOOR_Y
    private var yVelocity = 0f
    private val gravity = 1.2f
    private val jumpStrength = -25f
    private var isJumping = false

    companion object {
        /** Y-координата, представляющая уровень пола. Игрок не может опуститься ниже этого уровня. */
        const val FLOOR_Y = 885f
    }

    /**
     * Обновляет состояние игрока, обрабатывая физику прыжка.
     * Метод вызывается в каждом кадре игрового цикла.
     */
    fun update() {

    }

    /**
     * Инициирует прыжок для игрока, если он еще не прыгает.
     * Метод вызывается при нажатии на кнопку "ОК".
     */
    fun jump() {

    }

    /**
     * Отрисовывает персонажа игрока на предоставленном холсте.
     */
    fun draw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, x, y - height, null)
    }

    /**
     * Возвращает ограничивающий прямоугольник для игрока, используемый для обнаружения столкновений.
     */
    fun getRect(): RectF {
        val margin = 20
        return RectF(x + margin, y - height + margin, x + width - margin, y - margin)
    }
}
