package com.focusvolution.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.focusvolution.app.R

@Composable
fun AppCharacter(
    level: Int,
    modifier: Modifier = Modifier
) {
    val safeLevel = level.coerceIn(1, 10)

    val imageRes = when (safeLevel) {
        1 -> R.drawable.cerebro_1
        2 -> R.drawable.cerebro_2
        3 -> R.drawable.cerebro_3
        4 -> R.drawable.cerebro_4
        5 -> R.drawable.cerebro_5
        6 -> R.drawable.cerebro_6
        7 -> R.drawable.cerebro_7
        8 -> R.drawable.cerebro_8
        9 -> R.drawable.cerebro_9
        10 -> R.drawable.cerebro_10
        else -> R.drawable.cerebro_1
    }

    Crossfade(
        targetState = imageRes,
        animationSpec = tween(400),
        modifier = modifier.fillMaxSize()
    ) { res ->
        Image(
            painter = painterResource(res),
            contentDescription = "Nivel $safeLevel",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
