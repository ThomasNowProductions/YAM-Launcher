package eu.ottop.yamlauncher.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.withStyledAttributes
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import eu.ottop.yamlauncher.R

class AlphabetIndexView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val letters = listOf(
        "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    )

    private var textColor: Int = Color.WHITE
    private var highlightColor: Int = Color.CYAN
    private var minTextSize: Float = 32f
    private var maxTextSize: Float = 56f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var selectedIndex = -1
    private var availableLetters: Set<String> = emptySet()
    private var availableIndexes: IntArray = intArrayOf()
    private var onLetterSelectedListener: ((String) -> Unit)? = null
    private var letterHeight = 0f
    private var contentCenterX = 0f
    private var extraWidthPx = 0f

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
        extraWidthPx = resources.displayMetrics.density * 8f
    }

    fun setAvailableLetters(letters: Set<String>) {
        availableLetters = letters.map { it.uppercase() }.toSet()
        availableIndexes = this.letters.mapIndexedNotNull { index, letter ->
            if (availableLetters.contains(letter)) index else null
        }.toIntArray()
        invalidate()
    }

    fun hasAvailableLetters(): Boolean {
        return availableIndexes.isNotEmpty()
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
        requestLayout()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateMetrics(w, h)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val resolvedHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> heightSize
            else -> max(suggestedMinimumHeight, (minTextSize * letters.size).roundToInt() + paddingTop + paddingBottom)
        }

        val textSize = calculateTextSize(resolvedHeight)
        textPaint.textSize = textSize
        val maxLetterWidth = letters.maxOf { textPaint.measureText(it) }
        val desiredWidth = (paddingLeft + paddingRight + maxLetterWidth + extraWidthPx).roundToInt()

        val resolvedWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        setMeasuredDimension(resolvedWidth, resolvedHeight)
        updateMetrics(resolvedWidth, resolvedHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (letterHeight <= 0f) return

        val fontMetrics = textPaint.fontMetrics
        letters.forEachIndexed { index, letter ->
            val centerY = paddingTop + letterHeight * (index + 0.5f)
            val baseline = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
            val isAvailable = availableLetters.contains(letter)

            if (index == selectedIndex) {
                highlightPaint.color = Color.argb(50, Color.red(highlightColor), Color.green(highlightColor), Color.blue(highlightColor))
                canvas.drawCircle(contentCenterX, centerY, textPaint.textSize * 0.7f, highlightPaint)
            }

            textPaint.color = when {
                index == selectedIndex -> highlightColor
                isAvailable -> textColor
                else -> Color.argb(80, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            }
            canvas.drawText(letter, contentCenterX, baseline, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || letterHeight <= 0f) return false

        val index = ((event.y - paddingTop) / letterHeight).toInt().coerceIn(0, letters.lastIndex)
        val resolvedIndex = resolveAvailableIndex(index)
        val letter = letters[resolvedIndex]

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (selectedIndex != resolvedIndex) {
                    selectedIndex = resolvedIndex
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
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun updateMetrics(width: Int, height: Int) {
        val contentWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val contentHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0)
        contentCenterX = paddingLeft + contentWidth / 2f
        letterHeight = if (letters.isNotEmpty()) contentHeight.toFloat() / letters.size else 0f
        textPaint.textSize = calculateTextSize(height)
    }

    private fun calculateTextSize(height: Int): Float {
        val contentHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0)
        val raw = if (letters.isNotEmpty()) contentHeight.toFloat() / letters.size * 0.75f else 0f
        return raw.coerceIn(minTextSize, maxTextSize)
    }

    private fun resolveAvailableIndex(index: Int): Int {
        if (availableIndexes.isEmpty()) return index
        if (availableLetters.contains(letters[index])) return index

        var closest = availableIndexes[0]
        var minDistance = abs(closest - index)
        for (i in 1 until availableIndexes.size) {
            val candidate = availableIndexes[i]
            val distance = abs(candidate - index)
            if (distance < minDistance) {
                minDistance = distance
                closest = candidate
            }
        }
        return closest
    }
}
