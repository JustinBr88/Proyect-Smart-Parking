package com.example.proyectosmartparking

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class Pkview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var estado: Map<Int, Map<Int, Int>> = mapOf()
        set(value) {
            field = value
            invalidate()
        }
    var modoSimulacion: Boolean = false

    private val MAX_POR_FILA = 5  // Como su nombre indica establece el número máximo de cajones por fila se puede cambiar para mas o menos y el programa debe adaptarse
    //Aqui se comienza a dibujar todo
    private val margenSuperior = 150
    private val celdaWidth = 700
    private val celdaHeight = 700
    private val cajonWidth = 650
    private val cajonHeight = 650
    private val carroMaxWidth = 500
    private val carroMaxHeight = 500
    private val cajonMargin = 25

    //se busca la imagen de carro para dibujarla
    private val carroBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.carro)
    }

    // Pan + límites
    private var offsetX = 0f
    private var offsetY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var maxOffsetX = 0f
    private var maxOffsetY = 0f
    private var minOffsetX = 0f
    private var minOffsetY = 0f

    init {
        // Estado simulado inicial para pruebas (si no se conecta con el API  quitalo pero no deberia afectar)
        if (estado.isEmpty()) {
            estado = mapOf(
                1 to mapOf(1 to 1, 2 to 0, 3 to 1, 4 to 0, 5 to 1),
                2 to mapOf(1 to 1, 2 to 1, 3 to 0, 4 to 0, 5 to 1)
            )
        }
    }
    //Ajusta el tamaño de la vista y la tabla para los cuadros
    private fun computeScrollBounds(cajonesCount: Int) {
        val filasVisuales = (cajonesCount + MAX_POR_FILA - 1) / MAX_POR_FILA
        val gridWidth = MAX_POR_FILA * celdaWidth
        val gridHeight = filasVisuales * celdaHeight + margenSuperior

        minOffsetX = -(gridWidth - width).coerceAtLeast(0).toFloat()
        minOffsetY = -(gridHeight - height).coerceAtLeast(0).toFloat()
        maxOffsetX = 0f
        maxOffsetY = 0f

        offsetX = offsetX.coerceIn(minOffsetX, maxOffsetX)
        offsetY = offsetY.coerceIn(minOffsetY, maxOffsetY)
    }
    //Operación para dibujar con canvas
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paintLibre = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val paintOcupado = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val paintTexto = Paint().apply {
            color = Color.BLACK
            textSize = 46f
            isFakeBoldText = true
        }
        val paintSimulacion = Paint().apply {
            color = Color.BLUE
            textSize = 40f
        }
        val paintCelda = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        if (modoSimulacion) {
            canvas.drawText(
                "Modo simulación (esperando conexión a API)",
                50f, 80f, paintSimulacion
            )
        }

        val cajones = mutableListOf<Int>()      // Acumulador total de ocupación, solo el estado
        estado.forEach { (_, lugares) ->
            lugares.forEach { (_, ocupado) ->
                cajones.add(ocupado)
            }
        }
        if (cajones.isEmpty()) return

        computeScrollBounds(cajones.size)
        val filasVisuales = (cajones.size + MAX_POR_FILA - 1) / MAX_POR_FILA

        // Dibuja cada celda/tab
        for(idx in cajones.indices) {
            val ocupado = cajones[idx]
            val col = idx % MAX_POR_FILA
            val row = idx / MAX_POR_FILA

            val letra = 'A' + row
            val numero = col + 1
            val nombre = "$letra$numero"

            val left = offsetX + col * celdaWidth
            val top = offsetY + row * celdaHeight + margenSuperior
            val right = left + celdaWidth
            val bottom = top + celdaHeight

            // Celda blanca de fondo
            canvas.drawRect(left, top, right, bottom, paintCelda)

            // Dibuja rectángulo del estacionamiento
            val cajonLeft = left + cajonMargin
            val cajonTop = top + cajonMargin
            val cajonRight = cajonLeft + cajonWidth
            val cajonBottom = cajonTop + cajonHeight

            val path = Path()
            path.moveTo(cajonLeft, cajonBottom)
            path.lineTo(cajonLeft, cajonTop)
            path.lineTo(cajonRight, cajonTop)
            path.lineTo(cajonRight, cajonBottom)
            canvas.drawPath(path, if (ocupado == 1) paintOcupado else paintLibre)

            // Dibuja auto si está ocupado
            if (ocupado == 1) {
                val scaleW = carroMaxWidth.toFloat() / cajonWidth
                val scaleH = carroMaxHeight.toFloat() / cajonHeight
                val scaleFinal = minOf(1f, scaleW, scaleH, 0.7f)
                val imgW = (cajonWidth * scaleFinal).toInt()
                val imgH = (cajonHeight * scaleFinal).toInt()
                val imgLeft = cajonLeft + (cajonWidth - imgW) / 2
                val imgTop = cajonTop + (cajonHeight - imgH) / 2
                val carroEscalado = Bitmap.createScaledBitmap(carroBitmap, imgW, imgH, false)
                canvas.drawBitmap(carroEscalado, imgLeft, imgTop, null)
            }

            // Dibuja el nombre
            canvas.drawText(
                nombre,
                cajonLeft + cajonWidth / 2f - paintTexto.measureText(nombre) / 2f,
                cajonTop - 16f,  // fija el texto por encima del cajón
                paintTexto
            )
        }
    }
 // Calcula el tamaño de la vista
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalCajones = estado.values.sumOf { it.size }
        val filasVisuales = if (totalCajones > 0) (totalCajones + MAX_POR_FILA - 1) / MAX_POR_FILA else 1
        val width = (if (totalCajones > 0) MAX_POR_FILA else 1) * celdaWidth
        val height = filasVisuales * celdaHeight + margenSuperior

        val realWidth = resolveSize(width, widthMeasureSpec)
        val realHeight = resolveSize(height, heightMeasureSpec)
        setMeasuredDimension(realWidth, realHeight)
    }
// Maneja los gestos de arrastre de pantalla poniendo limites para que no se pueda salir de la pantalla
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        when(event.action){
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                offsetX = (offsetX + dx).coerceIn(minOffsetX, maxOffsetX)
                offsetY = (offsetY + dy).coerceIn(minOffsetY, maxOffsetY)
                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
            }
        }
        return true
    }
}