package com.john.waveview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

// y=Asin(ωx+φ)+k
internal class Wave @JvmOverloads constructor(context: Context?, attrs: AttributeSet?, defStyle: Int = R.attr.waveViewStyle) : View(context, attrs, defStyle) {
    private val WAVE_HEIGHT_LARGE = 16
    private val WAVE_HEIGHT_MIDDLE = 8
    private val WAVE_HEIGHT_LITTLE = 5
    private val WAVE_LENGTH_MULTIPLE_LARGE = 1.5f
    private val WAVE_LENGTH_MULTIPLE_MIDDLE = 1f
    private val WAVE_LENGTH_MULTIPLE_LITTLE = 0.5f
    private val WAVE_HZ_FAST = 0.13f
    private val WAVE_HZ_NORMAL = 0.09f
    private val WAVE_HZ_SLOW = 0.05f
    val DEFAULT_ABOVE_WAVE_ALPHA = 50
    val DEFAULT_BLOW_WAVE_ALPHA = 30
    private val X_SPACE = 20f
    private val PI2 = 2 * Math.PI
    private val mAboveWavePath = Path()
    private val mBlowWavePath = Path()
    val aboveWavePaint = Paint()
    val blowWavePaint = Paint()
    private var mAboveWaveColor = 0
    private var mBlowWaveColor = 0
    private var mWaveMultiple = 0f
    private var mWaveLength = 0f
    private var mWaveHeight = 0
    private var mMaxRight = 0f
    private var mWaveHz = 0f

    // wave animation
    private var mAboveOffset = 0.0f
    private var mBlowOffset = 0f
    private var mRefreshProgressRunnable: RefreshProgressRunnable? = null
    private var mleft = 0
    private var mright = 0
    private var mbottom = 0

    // ω
    private var omega = 0.0
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(mBlowWavePath, blowWavePaint)
        canvas.drawPath(mAboveWavePath, aboveWavePaint)
    }

    fun setAboveWaveColor(aboveWaveColor: Int) {
        mAboveWaveColor = aboveWaveColor
    }

    fun setBlowWaveColor(blowWaveColor: Int) {
        mBlowWaveColor = blowWaveColor
    }

    fun initializeWaveSize(waveMultiple: Int, waveHeight: Int, waveHz: Int) {
        mWaveMultiple = getWaveMultiple(waveMultiple)
        mWaveHeight = getWaveHeight(waveHeight)
        mWaveHz = getWaveHz(waveHz)
        mBlowOffset = mWaveHeight * 0.4f
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                mWaveHeight * 2)
        layoutParams = params
    }

    fun initializePainters() {
        aboveWavePaint.color = mAboveWaveColor
        aboveWavePaint.alpha = DEFAULT_ABOVE_WAVE_ALPHA
        aboveWavePaint.style = Paint.Style.FILL
        aboveWavePaint.isAntiAlias = true
        blowWavePaint.color = mBlowWaveColor
        blowWavePaint.alpha = DEFAULT_BLOW_WAVE_ALPHA
        blowWavePaint.style = Paint.Style.FILL
        blowWavePaint.isAntiAlias = true
    }

    private fun getWaveMultiple(size: Int): Float {
        when (size) {
            WaveView.LARGE -> return WAVE_LENGTH_MULTIPLE_LARGE
            WaveView.MIDDLE -> return WAVE_LENGTH_MULTIPLE_MIDDLE
            WaveView.LITTLE -> return WAVE_LENGTH_MULTIPLE_LITTLE
        }
        return 0f
    }

    private fun getWaveHeight(size: Int): Int {
        when (size) {
            WaveView.LARGE -> return WAVE_HEIGHT_LARGE
            WaveView.MIDDLE -> return WAVE_HEIGHT_MIDDLE
            WaveView.LITTLE -> return WAVE_HEIGHT_LITTLE
        }
        return 0
    }

    private fun getWaveHz(size: Int): Float {
        when (size) {
            WaveView.LARGE -> return WAVE_HZ_FAST
            WaveView.MIDDLE -> return WAVE_HZ_NORMAL
            WaveView.LITTLE -> return WAVE_HZ_SLOW
        }
        return 0f
    }

    /**
     * calculate wave track
     */
    private fun calculatePath() {
        mAboveWavePath.reset()
        mBlowWavePath.reset()
        waveOffset
        var y: Float
        mAboveWavePath.moveTo(left.toFloat(), bottom.toFloat())
        run {
            var x = 0f
            while (x <= mMaxRight) {
                y = (mWaveHeight * Math.sin(omega * x + mAboveOffset) + mWaveHeight).toFloat()
                mAboveWavePath.lineTo(x, y)
                x += X_SPACE
            }
        }
        mAboveWavePath.lineTo(right.toFloat(), bottom.toFloat())
        mBlowWavePath.moveTo(left.toFloat(), bottom.toFloat())
        var x = 0f
        while (x <= mMaxRight) {
            y = (mWaveHeight * Math.sin(omega * x + mBlowOffset) + mWaveHeight).toFloat()
            mBlowWavePath.lineTo(x, y)
            x += X_SPACE
        }
        mBlowWavePath.lineTo(right.toFloat(), bottom.toFloat())
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (GONE == visibility) {
            removeCallbacks(mRefreshProgressRunnable)
        } else {
            removeCallbacks(mRefreshProgressRunnable)
            mRefreshProgressRunnable = RefreshProgressRunnable()
            post(mRefreshProgressRunnable)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            if (mWaveLength == 0f) {
                startWave()
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (mWaveLength == 0f) {
            startWave()
        }
    }

    private fun startWave() {
        if (width != 0) {
            val width = width
            mWaveLength = width * mWaveMultiple
            mleft = left
            mright = right
            mbottom = bottom + 2
            mMaxRight = right + X_SPACE
            omega = PI2 / mWaveLength
        }
    }

    private val waveOffset: Unit
        private get() {
            if (mBlowOffset > Float.MAX_VALUE - 100) {
                mBlowOffset = 0f
            } else {
                mBlowOffset += mWaveHz
            }
            if (mAboveOffset > Float.MAX_VALUE - 100) {
                mAboveOffset = 0f
            } else {
                mAboveOffset += mWaveHz
            }
        }

    private inner class RefreshProgressRunnable : Runnable {
        override fun run() {
            synchronized(this@Wave) {
                val start = System.currentTimeMillis()
                calculatePath()
                invalidate()
                val gap = 16 - (System.currentTimeMillis() - start)
                postDelayed(this, if (gap < 0) 0 else gap)
            }
        }
    }
}