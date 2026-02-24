package eu.ottop.yamlauncher.views

import android.animation.AnimatorSet
import android.animation.ValueAnimator
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

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var selectedIndex = -1
    private var previousSelectedIndex = -1
    private var availableLetters: Set<String> = emptySet()
    private var onLetterSelectedListener: ((String) -> Unit)? = null
    
    private var animatedRadius = 0f
    private var animatedAlpha = 0
    private var animatorSet: AnimatorSet? = null
    private var fixedHeight: Int = 0

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
    
    private fun calculateFixedWidth(): Int {
        textPaint.textSize = maxTextSize
        val maxWidth = letters.maxOf { textPaint.measureText(it) }
        return (maxWidth + paddingLeft + paddingRight + 16).toInt()
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
        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (height == 0 || width == 0) return

        val letterHeight = height.toFloat() / letters.size
        val calculatedTextSize = letterHeight * 0.8f
        val textSize = calculatedTextSize.coerceIn(minTextSize, maxTextSize)
        textPaint.textSize = textSize

        val centerX = width / 2f

        letters.forEachIndexed { index, letter ->
            val y = letterHeight * (index + 0.5f)
            val isAvailable = availableLetters.contains(letter)
            
            if (index == selectedIndex || (index == previousSelectedIndex && animatedAlpha > 0)) {
                val radius = if (index == selectedIndex) {
                    textSize * 0.7f
                } else {
                    animatedRadius
                }
                val alpha = if (index == selectedIndex) {
                    40
                } else {
                    animatedAlpha
                }
                bgPaint.color = Color.argb(alpha, Color.red(highlightColor), Color.green(highlightColor), Color.blue(highlightColor))
                canvas.drawCircle(centerX, y, radius, bgPaint)
            }
            
            textPaint.color = when {
                index == selectedIndex -> highlightColor
                isAvailable -> textColor
                else -> Color.argb(80, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            }
            
            canvas.drawText(letter, centerX, y + textPaint.textSize / 3, textPaint)
        }
    }

    private fun animateSelectionEnd() {
        animatorSet?.cancel()
        
        val textSize = textPaint.textSize
        val startRadius = textSize * 0.7f
        val endRadius = textSize * 1.2f
        
        val radiusAnimator = ValueAnimator.ofFloat(startRadius, endRadius).apply {
            duration = 150
            addUpdateListener { animation ->
                animatedRadius = animation.animatedValue as Float
                invalidate()
            }
        }
        
        val alphaAnimator = ValueAnimator.ofInt(40, 0).apply {
            duration = 150
            addUpdateListener { animation ->
                animatedAlpha = animation.animatedValue as Int
                invalidate()
            }
        }
        
        animatorSet = AnimatorSet().apply {
            playTogether(radiusAnimator, alphaAnimator)
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val letterHeight = height.toFloat() / letters.size
        val y = event.y
        val index = (y / letterHeight).toInt().coerceIn(0, letters.lastIndex)
        val letter = letters[index]

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (selectedIndex != index) {
                    if (selectedIndex >= 0) {
                        previousSelectedIndex = selectedIndex
                        animateSelectionEnd()
                    }
                    selectedIndex = index
                    if (availableLetters.contains(letter)) {
                        onLetterSelectedListener?.invoke(letter)
                    }
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (selectedIndex >= 0) {
                    previousSelectedIndex = selectedIndex
                    animateSelectionEnd()
                }
                selectedIndex = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val fixedWidth = calculateFixedWidth()
        
        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (fixedHeight == 0 && measuredHeight > 0) {
            fixedHeight = measuredHeight
        }
        
        val height = if (fixedHeight > 0) fixedHeight else measuredHeight
        setMeasuredDimension(fixedWidth, height)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animatorSet?.cancel()
    }
}
