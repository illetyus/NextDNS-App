package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

fun String.toHex(): String = toByteArray().joinToString("") { "%02x".format(it) }

@Composable
fun FaviconImage(domain: String, modifier: Modifier = Modifier) {
    val hexDomain = domain.toHex()
    val nextDnsUrl = "https://favicons.nextdns.io/hex:$hexDomain@1x.png"
    
    SubcomposeAsyncImage(
        model = nextDnsUrl,
        contentDescription = null,
        modifier = modifier
    ) {
        val state = painter.state
        if (state is AsyncImagePainter.State.Success) {
            SubcomposeAsyncImageContent()
        }
    }
}
