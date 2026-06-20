package com.fahim.pilltime

import androidx.compose.ui.window.ComposeUIViewController
import com.fahim.pilltime.core.di.doInitKoin

fun MainViewController() = ComposeUIViewController {
    doInitKoin()
    App()
}
