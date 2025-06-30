package com.sshex.gameapp

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager

class GameActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Полноэкранный режим
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Устанавливаем кастомное игровое представление
        val gameView = GameView(this)
        setContentView(gameView)
    }
}
