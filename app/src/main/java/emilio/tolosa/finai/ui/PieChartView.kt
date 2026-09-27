package emilio.tolosa.finai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var datos: List<Pair<String, Double>> = emptyList()
    private val colores = listOf(
        "#4F5BFF", "#22A06B", "#F5A623", "#E5484D", "#9B59B6", "#1ABC9C", "#E67E22"
    ).map { Color.parseColor(it) }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setDatos(nuevos: List<Pair<String, Double>>) {
        datos = nuevos.filter { it.second > 0 }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (datos.isEmpty()) return

        val total = datos.sumOf { it.second }
        if (total <= 0) return

        val diametro = minOf(width, height * 2 / 3).toFloat()
        val rect = RectF(16f, 16f, diametro, diametro)

        var anguloInicio = -90f
        datos.forEachIndexed { i, (_, valor) ->
            val barrido = (valor / total * 360).toFloat()
            paint.color = colores[i % colores.size]
            canvas.drawArc(rect, anguloInicio, barrido, true, paint)
            anguloInicio += barrido
        }

        // Leyenda debajo del círculo
        paint.textSize = 28f
        var y = diametro + 50f
        datos.forEachIndexed { i, (nombre, valor) ->
            paint.color = colores[i % colores.size]
            canvas.drawRect(16f, y - 24f, 46f, y + 4f, paint)
            paint.color = Color.DKGRAY
            canvas.drawText("$nombre: $${"%.0f".format(valor)}", 60f, y, paint)
            y += 40f
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val ancho = MeasureSpec.getSize(widthMeasureSpec)
        val alturaDeseada = (ancho * 0.6f + datos.size * 40 + 80).toInt()
        setMeasuredDimension(ancho, alturaDeseada)
    }
}