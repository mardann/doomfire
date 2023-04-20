package dev.adambennett.doomcompose

import android.util.Log
import android.view.Choreographer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.adambennett.doomcompose.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.random.Random
import kotlin.math.absoluteValue
import kotlin.math.max

data class DoomState(var pixels: List<Int> = emptyList())

@Composable
fun DoomCompose(
    state: DoomState = DoomState(),
    scope: CoroutineScope = rememberCoroutineScope()
    
) {
    var innerState by remember {
        mutableStateOf(state)
    }
    DoomCanvas(innerState) { canvas ->
        setupFireView(scope, canvas, windDirection = WindDirection.Right, updateDoomState = {
            innerState = it
        })
    }
}

@Composable
fun DoomCanvas(
    state: DoomState,
    measurements: (CanvasMeasurements) -> Unit
) {
    val innerState: DoomState by remember(state) {
        mutableStateOf(state)
    }
    
    val paint = remember { Paint() }
    var measured by remember {
        mutableStateOf(false)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasState = CanvasMeasurements(
            size.width.absoluteValue.toInt(),
            size.height.absoluteValue.toInt()
        )

        if (!measured) {
            measured = true
            measurements(canvasState)
        }

        if (innerState.pixels.isNotEmpty()) {
            renderFire(
                paint,
                state.pixels,
                canvasState.heightPixel,
                canvasState.widthPixel,
                canvasState.pixelSize
            )
        }
    }
}

fun DrawScope.renderFire(
    paint: Paint,
    firePixels: List<Int>,
    heightPixels: Int,
    widthPixels: Int,
    pixelSize: Int
                        ) {
    Log.d("DoomCanvas", "renderFire: ")
    for (column in 0 until widthPixels) {
        for (row in 0 until heightPixels - 1) {
            val currentPixelIndex = column + (widthPixels * row)
            val currentPixel = firePixels[currentPixelIndex]
            val color = fireColors[currentPixel]
            drawRect(
                color = color,
                    topLeft = Offset((column * pixelSize).toFloat(),(row * pixelSize).toFloat()),
                    Size(((column + 1) * pixelSize).toFloat(), ((row + 1) * pixelSize).toFloat())
                    )
        }
    }
}

private fun setupFireView(
    scope: CoroutineScope,
    canvas: CanvasMeasurements,
    updateDoomState: (DoomState) -> Unit,
    windDirection: WindDirection = WindDirection.Left
) {
    val arraySize = canvas.widthPixel * canvas.heightPixel

    val pixelArray = IntArray(arraySize) { 0 }
        .apply { createFireSource(this, canvas) }
    
    scope.launch {
        while (true) {
            calculateFirePropagation(pixelArray, canvas, windDirection)
            updateDoomState(DoomState(pixelArray.toList()))
            Log.d("setupFireView", "doFrame: ")
            delay(60)
        }
    }
    
}

private fun createFireSource(firePixels: IntArray, canvas: CanvasMeasurements) {
    val overFlowFireIndex = canvas.widthPixel * canvas.heightPixel

    for (column in 0 until canvas.widthPixel) {
        val pixelIndex = (overFlowFireIndex - canvas.widthPixel) + column
        firePixels[pixelIndex] = (fireColors.size - 1).also {
            Log.d("createFireSource", "createFireSource: pixel index: $pixelIndex, value: $it")
        }
    }
}

private fun calculateFirePropagation(
    firePixels: IntArray,
    canvasMeasurements: CanvasMeasurements,
    windDirection: WindDirection
) {
    for (column in 0 until canvasMeasurements.widthPixel) {
        for (row in 1 until canvasMeasurements.heightPixel) {
            val currentPixelIndex = column + (canvasMeasurements.widthPixel * row)
            updateFireIntensityPerPixel(
                currentPixelIndex,
                firePixels,
                canvasMeasurements,
                windDirection
            )
        }
    }
}

private fun updateFireIntensityPerPixel(
    currentPixelIndex: Int,
    firePixels: IntArray,
    measurements: CanvasMeasurements,
    windDirection: WindDirection
) {
    val bellowPixelIndex = currentPixelIndex + measurements.widthPixel
    if (bellowPixelIndex >= measurements.widthPixel * measurements.heightPixel) return

    val offset = if (measurements.tallerThanWide) 2 else 3
    val decay = floor(Random.nextDouble() * offset).toInt()
    val bellowPixelFireIntensity = firePixels[bellowPixelIndex]
    
    val newFireIntensity = max(bellowPixelFireIntensity - decay,0)

    val newPosition = when (windDirection) {
        WindDirection.Right -> max(currentPixelIndex - decay, 0)
        WindDirection.Left -> max(currentPixelIndex + decay,0)
        WindDirection.None -> currentPixelIndex
    }

    firePixels[newPosition] = newFireIntensity
}


