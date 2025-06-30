package com.sshex.gameapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.scale
import kotlin.collections.forEach
import kotlin.collections.getOrPut
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.removeAll
import kotlin.collections.set
import kotlin.math.max
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val thread: GameThread
    private val player = Player(context)
    private val paint = Paint().apply {
        isAntiAlias = false // Disable antialiasing for better performance
        isFilterBitmap = false
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = 48f
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val textInterfacePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = 52f
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private var startTime = 0L
    private val gameDuration = 30_000L
    private var score = 0
    private var jumpPressed = false
    private var isGameOver = false
    private var hasWon = false
    private var gameStarted = false
    private var iconStar: Bitmap
    private var iconClock: Bitmap

    private val obstacles = mutableListOf<Obstacle>()
    private val bonuses = mutableListOf<Bonus>()
    private var obstacleTimer = 0
    private var bonusTimer = 0
    private var nextObstacleTime = Random.nextInt(40, 80)
    private var nextBonusTime = Random.nextInt(110, 140)

    // Кэш для битмапов препятствий и бонусов
    private val obstacleBitmaps = mutableMapOf<Int, Bitmap>()
    private val bonusBitmaps = mutableMapOf<Int, Bitmap>()

    // Параллакс фон
    private val rawBgBitmap: Bitmap = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.backgorund_new,
        BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
    )
    private lateinit var bgBitmap: Bitmap
    private var bgX1 = 0f
    private var bgX2: Float = .0f
    private val bgScrollSpeed = 5f
    private var gameOverTime = 0L

    // Кэшированные значения времени
    private var lastFrameTime = 0L

    // ==== FPS ====
    private var fps = 0
    private var framesThisSecond = 0
    private var lastFpsTime = 0L

    init {
        holder.addCallback(this)
        isFocusable = true
        thread = GameThread(holder, this)

        // Загрузка иконок
        iconStar = BitmapFactory.decodeResource(context.resources, R.drawable.ic_star).scale(52, 52)
        iconClock =
            BitmapFactory.decodeResource(context.resources, R.drawable.ic_clock).scale(52, 52)

        // Предзагрузка битмапов препятствий и бонусов
        preloadBitmaps(context)
    }

    private fun preloadBitmaps(context: Context) {
        for (i in 1..3) {
            val rawBitmap = BitmapFactory.decodeResource(
                context.resources,
                context.resources.getIdentifier("obstacle_$i", "drawable", context.packageName)
            )
            val aspectRatio = rawBitmap.height.toFloat() / rawBitmap.width
            val scaledWidth = 80
            val scaledHeight = (scaledWidth * aspectRatio).toInt()
            obstacleBitmaps[i] = rawBitmap.scale(scaledWidth, scaledHeight)
        }

        for (i in 1..9) {
            val rawBitmap = BitmapFactory.decodeResource(
                context.resources,
                context.resources.getIdentifier("bonus_$i", "drawable", context.packageName)
            )
            val aspectRatio = rawBitmap.width.toFloat() / rawBitmap.height
            val scaledHeight = 100
            val scaledWidth = (scaledHeight * aspectRatio).toInt()
            bonusBitmaps[i] = rawBitmap.scale(scaledWidth, scaledHeight)
        }
    }

    fun getCachedObstacleBitmap(type: Int): Bitmap = obstacleBitmaps[type]!!
    fun getCachedBonusBitmap(type: Int): Bitmap = bonusBitmaps[type]!!

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Оптимизированное масштабирование фона
        if (!::bgBitmap.isInitialized) {
            bgBitmap = rawBgBitmap.scale(width, height)
        }

        bgX1 = 0f
        bgX2 = width.toFloat()

        thread.setRunning(true)
        if (!thread.isAlive) {
            thread.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread.setRunning(false)
        try {
            thread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        cleanupBitmaps()
    }

    private fun cleanupBitmaps() {
        obstacleBitmaps.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        bonusBitmaps.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        obstacleBitmaps.clear()
        bonusBitmaps.clear()

        if (::bgBitmap.isInitialized && !bgBitmap.isRecycled) {
            bgBitmap.recycle()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            if (!gameStarted) {
                startGame()
            } else if (isGameOver && System.currentTimeMillis() - gameOverTime > 1000L) {
                restartGame()
            } else {
                jumpPressed = true
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!gameStarted && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            startGame()
            return true
        }

        if (isGameOver && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (System.currentTimeMillis() - gameOverTime > 1000L) {
                restartGame()
            }
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            jumpPressed = true
        }

        return super.onKeyDown(keyCode, event)
    }

    fun update() {
        if (!gameStarted || isGameOver) return

        // Кэшируем время для текущего кадра
        lastFrameTime = System.currentTimeMillis()

        // === FPS (только debug) ===
        if (BuildConfig.DEBUG) {
            val now = lastFrameTime
            if (now - lastFpsTime >= 1000L) {
                fps = framesThisSecond
                framesThisSecond = 0
                lastFpsTime = now
            }
        }

        // Прокрутка фона
        bgX1 -= bgScrollSpeed
        bgX2 -= bgScrollSpeed
        if (bgX1 + bgBitmap.width < 0) bgX1 = bgX2 + bgBitmap.width
        if (bgX2 + bgBitmap.width < 0) bgX2 = bgX1 + bgBitmap.width

        if (lastFrameTime - startTime >= gameDuration) {
            isGameOver = true
            gameOverTime = lastFrameTime
        }

        if (jumpPressed) {
            player.jump()
            jumpPressed = false
        }

        player.update()

        // Спавн препятствий
        obstacleTimer++
        if (obstacleTimer >= nextObstacleTime) {
            obstacles.add(Obstacle(context, width, this))
            obstacleTimer = 0
            nextObstacleTime = Random.nextInt(40, 80)
        }

        // Спавн бонусов (может быть пропущен)
        bonusTimer++
        if (bonusTimer >= nextBonusTime) {
            if (Random.nextInt(100) < 95) { // 95% chance for reliable spawning
                bonuses.add(Bonus(context, width, this))
            }
            bonusTimer = 0
            nextBonusTime = Random.nextInt(110, 140)
        }

        obstacles.forEach { it.update() }
        obstacles.removeAll { obstacle ->
            obstacle.isOffScreen() || run {
                if (RectF.intersects(player.getRect(), obstacle.getRect())) {
                    isGameOver = true
                    gameOverTime = lastFrameTime
                    true
                } else false
            }
        }

        bonuses.forEach { it.update() }
        bonuses.removeAll { bonus ->
            bonus.isOffScreen() || run {
                if (RectF.intersects(player.getRect(), bonus.getRect())) {
                    score += 5
                    // Check for win condition
                    if (score >= 50) {
                        hasWon = true
                        isGameOver = true
                        gameOverTime = lastFrameTime
                    }
                    true
                } else false
            }
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (BuildConfig.DEBUG) {
            framesThisSecond++ // Считаем кадры
        }

        // Фон
        canvas.drawBitmap(bgBitmap, bgX1, 0f, paint)
        canvas.drawBitmap(bgBitmap, bgX2, 0f, paint)

        // До старта игры — титульный экран
        if (!gameStarted) {
            textPaint.textSize = 60f
            val title = "Smart Home Runner"
            val titleX = (width - textPaint.measureText(title)) / 2
            canvas.drawText(title, titleX, height / 2f - 60f, textPaint)

            textPaint.textSize = 40f
            val prompt = "Нажми ОК, чтобы начать"
            val promptX = (width - textPaint.measureText(prompt)) / 2
            canvas.drawText(prompt, promptX, height / 2f, textPaint)
            return
        }

        // Игрок и объекты
        player.draw(canvas)
        obstacles.forEach { it.draw(canvas) }
        bonuses.forEach { it.draw(canvas) }

        // ==== UI: Баллы и таймер ====

        val iconSize = 55
        val padding = 45f
        val spacing = 25f
        val blockHeight = 95f
        val blockWidth = 180f
        val blockRadius = 20f
        val iconMargin = 10f
        val textOffsetY = (blockHeight + textInterfacePaint.textSize) / 2f - 10f

        val blockPaint = Paint().apply {
            isAntiAlias = true
        }

// ==== Плашка Баллы ====
        val scoreText = String.format("%02d", score)
        val scoreBlockX = width - blockWidth - padding
        val scoreBlockY = padding

        blockPaint.color = Color.argb(170, 0, 0, 0) // более прозрачный чёрный
        canvas.drawRoundRect(
            RectF(scoreBlockX, scoreBlockY, scoreBlockX + blockWidth, scoreBlockY + blockHeight),
            blockRadius, blockRadius, blockPaint
        )

        canvas.drawBitmap(
            iconStar,
            scoreBlockX + iconMargin,
            scoreBlockY + (blockHeight - iconSize) / 2f,
            null
        )
        canvas.drawText(
            scoreText,
            scoreBlockX + iconSize + spacing,
            scoreBlockY + textOffsetY,
            textInterfacePaint
        )


// ==== Плашка Таймер ====
        val remaining = max(0, (gameDuration - (lastFrameTime - startTime)) / 1000)
        val timeText = String.format("%02d", remaining)
        val timeBlockX = padding
        val timeBlockY = scoreBlockY

        blockPaint.color = Color.argb(180, 255, 215, 0) // более прозрачный жёлтый
        canvas.drawRoundRect(
            RectF(timeBlockX, timeBlockY, timeBlockX + blockWidth, timeBlockY + blockHeight),
            blockRadius, blockRadius, blockPaint
        )

        canvas.drawBitmap(
            iconClock,
            timeBlockX + iconMargin,
            timeBlockY + (blockHeight - iconSize) / 2f,
            null
        )
        canvas.drawText(
            timeText,
            timeBlockX + iconSize + spacing,
            timeBlockY + textOffsetY,
            textInterfacePaint
        )

        // ==== Конец игры ====
        if (isGameOver) {
            textPaint.textSize = 80f
            val gameOverText = if (hasWon) "Победа!" else "Game Over!"
            val x1 = (width - textPaint.measureText(gameOverText)) / 2
            canvas.drawText(gameOverText, x1, height / 2f - 80f, textPaint)

            textPaint.textSize = 50f
            val scoreLine = "Баллы: $score"
            val xScore = (width - textPaint.measureText(scoreLine)) / 2
            canvas.drawText(scoreLine, xScore, height / 2f - 20f, textPaint)

            // Показываем кнопку рестарта только через 1 сек после окончания
            if (System.currentTimeMillis() - gameOverTime > 1000L) {
                textPaint.textSize = 40f
                val restartText = "Нажми ОК, чтобы начать сначала"
                val x2 = (width - textPaint.measureText(restartText)) / 2
                canvas.drawText(restartText, x2, height / 2f + 40f, textPaint)
            }
        }

        // ==== FPS (только debug) ====
        if (BuildConfig.DEBUG) {
            textPaint.textSize = 32f
            val fpsText = "FPS: $fps"
            canvas.drawText(fpsText, width - 160f, height - 40f, textPaint)
        }
    }

    private fun startGame() {
        gameStarted = true
        startTime = System.currentTimeMillis()
    }

    private fun restartGame() {
        isGameOver = false
        hasWon = false
        score = 0
        obstacles.clear()
        bonuses.clear()
        obstacleTimer = 0
        bonusTimer = 0
        nextObstacleTime = Random.nextInt(40, 80)
        nextBonusTime = Random.nextInt(110, 140)
        startTime = System.currentTimeMillis()
    }
}
