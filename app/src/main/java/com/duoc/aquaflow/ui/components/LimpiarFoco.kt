package com.duoc.aquaflow.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

fun Modifier.deseleccionarAlTocarODeslizar(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Quita el foco en cuanto el usuario arrastra el dedo para hacer scroll
    val detectarDeslizamiento = remember(focusManager, keyboardController) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
                return Offset.Zero
            }
        }
    }

    this
        .nestedScroll(detectarDeslizamiento)
        // Quita el foco al tocar cualquier zona vacía, texto o tarjeta no interactiva
        .pointerInput(focusManager, keyboardController) {
            detectTapGestures(
                onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            )
        }
}