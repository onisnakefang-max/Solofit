package com.solofit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Синее «окно Системы» с неоновой рамкой. */
@Composable
fun SystemWindow(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = Sys.Blue,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = accent, spotColor = accent)
            .clip(shape)
            .background(Sys.Panel)
            .border(1.dp, accent.copy(alpha = 0.85f), shape)
            .padding(14.dp)
    ) {
        Text(
            "[ $title ]",
            color = accent,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = accent.copy(alpha = 0.4f))
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun NeonBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Int = 8) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Sys.Track)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color)
        )
    }
}

@Composable
fun SysButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Sys.Blue,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, if (enabled) accent else Sys.Dim),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent, disabledContentColor = Sys.Dim)
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SysText(text: String, modifier: Modifier = Modifier, color: Color = Sys.Text, size: Int = 14, bold: Boolean = false) {
    Text(
        text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
fun SystemDialog(
    title: String,
    accent: Color,
    buttonText: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        SystemWindow(title = title, accent = accent) {
            content()
            Spacer(Modifier.height(14.dp))
            SysButton(buttonText, onClose, Modifier.fillMaxWidth(), accent)
        }
    }
}

fun Modifier.screenPadding(): Modifier = this.padding(horizontal = 16.dp, vertical = 12.dp)
