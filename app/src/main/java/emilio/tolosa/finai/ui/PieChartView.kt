package emilio.tolosa.finai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import emilio.tolosa.finai.R

class PieChartView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var datos: List<Pair<String, Double>> = emptyList()

    // Colores oficiales de FinAI
    private val colores: List<Int>
        get() = listOf(
            ContextCompat.getColor(context, R.color.primary),
            ContextCompat.getColor(context, R.color.income),
            ContextCompat.getColor(context, R.color.warning),
            ContextCompat.getColor(context, R.color.expense)
        )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setDatos(nuevos: List<Pair<String, Double>>) {
        datos = nuevos.filter { it.second > 0 }
        invalidate()
        requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (datos.isEmpty()) return

        val total = datos.sumOf { it.second }

        if (total <= 0) return

        // Tamaño del círculo
        val diametro = width * 0.55f

        // Centrar horizontalmente
        val izquierda = (width - diametro) / 2f
        val arriba = 16f

        val rect = RectF(
            izquierda,
            arriba,
            izquierda + diametro,
            arriba + diametro
        )

        var anguloInicio = -90f

        datos.forEachIndexed { i, (_, valor) ->

            val barrido =
                (valor / total * 360).toFloat()

            paint.color =
                colores[i % colores.size]

            canvas.drawArc(
                rect,
                anguloInicio,
                barrido,
                true,
                paint
            )

            anguloInicio += barrido
        }

        // LEYENDA
        paint.textSize = 28f

        val leyenda = datos.map { (nombre, valor) ->
            "$nombre: $${"%.0f".format(valor)}"
        }

        // Calcular cuánto ocupa la leyenda más larga
        val anchoMaximoTexto =
            leyenda.maxOfOrNull {
                paint.measureText(it)
            } ?: 0f

        val anchoLeyenda =
            44f + anchoMaximoTexto

        val inicioLeyenda =
            (width - anchoLeyenda) / 2f

        var y =
            arriba + diametro + 45f

        datos.forEachIndexed { i, (nombre, valor) ->

            // Cuadro de color
            paint.color =
                colores[i % colores.size]

            canvas.drawRect(
                inicioLeyenda,
                y - 22f,
                inicioLeyenda + 28f,
                y + 4f,
                paint
            )

            // Texto
            paint.color =
                Color.DKGRAY

            canvas.drawText(
                "$nombre: $${"%.0f".format(valor)}",
                inicioLeyenda + 42f,
                y,
                paint
            )

            y += 40f
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {

        val ancho =
            MeasureSpec.getSize(widthMeasureSpec)

        val diametro =
            ancho * 0.55f

        val alturaDeseada =
            (
                    diametro +
                            datos.size * 40 +
                            90
                    ).toInt()

        setMeasuredDimension(
            ancho,
            alturaDeseada
        )
    }
}