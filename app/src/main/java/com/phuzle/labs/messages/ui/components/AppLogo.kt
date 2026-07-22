package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phuzle.labs.messages.R

/**
 * The launcher icon's background+foreground vectors, layered by hand. `painterResource` can't
 * load the `<adaptive-icon>` mipmap XML directly (only plain VectorDrawables/rasters), so the two
 * layers are composited here instead.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 40.dp, cornerRadius: Dp = 12.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(cornerRadius))) {
        Image(painter = painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize())
        Image(painter = painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}
