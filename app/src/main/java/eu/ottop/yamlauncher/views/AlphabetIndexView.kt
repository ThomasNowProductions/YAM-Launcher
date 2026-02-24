package eu.ottop.yamlauncher.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.withStyledAttributes
import eu.ottop.yamlauncher.R

class AlphabetIndexView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val letters = arrayOf(
        "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    )

    private var textColor: Int = Color.WHITE
    private var highlightColor: Int = Color.CYAN
    private var minTextSize: Float = 32f
    private var maxTextSize: Float = 56f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private var selectedIndex = -1
    private var availableLetters: Set<String> = emptySet()
    private var onLetterSelectedListener: ((String) -> Unit)? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.AlphabetIndexView, defStyleAttr, 0) {
            textColor = getColor(R.styleable.AlphabetIndexView_indexTextColor, Color.WHITE)
            highlightColor = getColor(R.styleable.AlphabetIndexView_indexHighlightColor, Color.CYAN)
            val attrTextSize = getDimension(R.styleable.AlphabetIndexView_indexTextSize, -1f)
            if (attrTextSize > 0) {
                minTextSize = attrTextSize
                maxTextSize = attrTextSize * 1.75f
            }
        }
    }

    fun setAvailableLetters(letters: Set<String>) {
        availableLetters = letters.map { it.uppercase() }.toSet()
        invalidate()
    }

    fun setOnLetterSelectedListener(listener: (String) -> Unit) {
        onLetterSelectedListener = listener
    }

    fun setTextColor(color: Int) {
        textColor = color
        invalidate()
    }

    fun setHighlightColor(color: Int) {
        highlightColor = color
        invalidate()
    }

    fun setTextSize(size: Float) {
        minTextSize = size
        maxTextSize = size * 1.75f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (height == 0 || width == 0) return

        val letterHeight = height.toFloat() / letters.size
        val calculatedTextSize = letterHeight * 0.8f
        val textSize = calculatedTextSize.coerceIn(minTextSize, maxTextSize)
        paint.textSize = textSize

        val centerX = width / 2f

        letters.forEachIndexed { index, letter ->
            val y = letterHeight * (index + 0.5f)
            val isAvailable = availableLetters.contains(letter)
            
            if (index == selectedIndex) {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = setAlpha(highlightColor, 40)
                    style = Paint.Style.FILL
                }
                val radius = textSize * 0.7f
                canvas.drawCircle(centerX, y, radius, bgPaint)
            }
            
            paint.color = when {
                index == selectedIndex -> highlightColor
                isAvailable -> textColor
                else -> setAlpha(textColor, 80)
            }
            
            canvas.drawText(letter, centerX, y + paint.textSize / 3, paint)
        }
    }

    private fun setAlpha(color: Int, alpha: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(alpha, r, g, b)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val letterHeight = height.toFloat() / letters.size
        val y = event.y
        val index = (y / letterHeight).toInt().coerceIn(0, letters.lastIndex)
        val letter = letters[index]

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (selectedIndex != index) {
                    selectedIndex = index
                    if (availableLetters.contains(letter)) {
                        onLetterSelectedListener?.invoke(letter)
                    }
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                selectedIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val letterHeight = height.toFloat() / letters.size
        val calculatedTextSize = letterHeight * 0.8f
        val textSize = calculatedTextSize.coerceIn(minTextSize, maxTextSize)

        val desiredWidth = (textSize + paddingLeft + paddingRight + 16).toInt()

        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desiredWidth, MeasureSpec.getSize(widthMeasureSpec))
            else -> desiredWidth
        }

        setMeasuredDimension(width, height)
    }
}
