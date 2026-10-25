package com.v2ray.ang.ui.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.ui.compose.AppTheme
import com.v2ray.ang.ui.compose.resolveDarkTheme

abstract class BaseComponentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.onActivityCreated(this)
        enableEdgeToEdge()
        setContent {
            AppTheme(darkTheme = if (alwaysDarkTheme) true else resolveDarkTheme()) {
                ScreenContent()
            }
        }
    }

    /** True for a screen drawn in the brand's dark look whatever the app theme (the main screen). */
    protected open val alwaysDarkTheme: Boolean = false

    @Composable
    protected abstract fun ScreenContent()
}