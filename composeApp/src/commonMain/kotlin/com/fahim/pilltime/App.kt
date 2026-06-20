package com.fahim.pilltime

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.fahim.pilltime.core.navigation.AppNavGraph
import com.fahim.pilltime.core.ui.PillTimeTheme

@Composable
@Preview
fun App() {
    PillTimeTheme {
        AppNavGraph()
    }
}
