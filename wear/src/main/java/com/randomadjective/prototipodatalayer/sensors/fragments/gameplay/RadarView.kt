package com.randomadjective.prototipodatalayer.sensors.fragments.gameplay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.randomadjective.prototipodatalayer.sensors.models.RadarPOI
import com.randomadjective.prototipodatalayer.sensors.models.RadarZoomLevel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val GRID_SIZE = 7
        private const val CENTER_CELL_INDEX = GRID_SIZE / 2

        private const val PULSE_INTERVAL_MS = 2200L
        private const val PULSE_DURATION_MS = 1800L

        private const val CENTRAL_CELL_RADIUS_FACTOR = 0.5f
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    private var userAccuracy: Float = 0f

    private var headingDegrees: Float = 0f
    private var hasHeading: Boolean = false

    private var currentZoom = RadarZoomLevel.FAR

    private var pulseStartTime: Long = 0L
    private var pulseId: Int = 0

    private var foundMessage: String? = null
    private var lastFeedbackMessage: String? = null

    private var onZoomChanged: ((RadarZoomLevel) -> Unit)? = null
    private var onStatusChanged: ((String) -> Unit)? = null
    private var onPoiFound: ((RadarPOI) -> Unit)? = null

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

    private val pois = mutableListOf<RadarPOI>()

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 24, 0)
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(40, 120, 40)
        strokeWidth = 1.5f
        alpha = 140
        style = Paint.Style.STROKE
    }

    private val gridStrongPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(100, 220, 100)
        strokeWidth = 2.2f
        alpha = 180
        style = Paint.Style.STROKE
    }

    private val cardinalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(190, 255, 190)
        textAlign = Paint.Align.CENTER
        textSize = 20f
        isFakeBoldText = true
        style = Paint.Style.FILL
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(160, 255, 160)
        strokeWidth = 3.5f
        alpha = 190
        style = Paint.Style.STROKE
    }

    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 70, 70)
        style = Paint.Style.FILL
    }

    private val poiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 240, 90)
        style = Paint.Style.FILL
    }

    private val poiDetectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(190, 255, 190)
        textAlign = Paint.Align.CENTER
        textSize = 24f
        style = Paint.Style.FILL
    }

    private val foundTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 255, 160)
        textAlign = Paint.Align.CENTER
        textSize = 28f
        isFakeBoldText = true
        style = Paint.Style.FILL
    }

    private val pulseRunnable = object : Runnable {
        override fun run() {
            startPulse()
            mainHandler.postDelayed(this, PULSE_INTERVAL_MS)
        }
    }

    init {
        isClickable = true
        isFocusable = true
        startRadarPulseLoop()
    }

    fun setCallbacks(
        onZoomChanged: ((RadarZoomLevel) -> Unit)? = null,
        onStatusChanged: ((String) -> Unit)? = null,
        onPoiFound: ((RadarPOI) -> Unit)? = null
    ) {
        this.onZoomChanged = onZoomChanged
        this.onStatusChanged = onStatusChanged
        this.onPoiFound = onPoiFound
    }

    fun setPOIs(newPois: List<RadarPOI>) {
        pois.clear()
        pois.addAll(newPois)
        invalidate()
    }

    fun updateUserLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ) {
        userLatitude = latitude
        userLongitude = longitude
        userAccuracy = accuracy

        if (foundMessage == null) {
            onStatusChanged?.invoke("Radar activo")
        }

        invalidate()
    }

    fun updateHeading(heading: Float) {
        headingDegrees = normalizeDegrees(heading)
        hasHeading = true
        invalidate()
    }

    fun getCurrentZoom(): RadarZoomLevel {
        return currentZoom
    }

    private fun startRadarPulseLoop() {
        mainHandler.removeCallbacks(pulseRunnable)
        startPulse()
        mainHandler.postDelayed(pulseRunnable, PULSE_INTERVAL_MS)
    }

    private fun startPulse() {
        pulseStartTime = System.currentTimeMillis()
        pulseId += 1
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mainHandler.removeCallbacks(pulseRunnable)
        toneGenerator.release()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radarSize = min(width, height).toFloat()
        val left = (width - radarSize) / 2f
        val top = (height - radarSize) / 2f
        val right = left + radarSize
        val bottom = top + radarSize

        val centerX = width / 2f
        val centerY = height / 2f
        val cellSize = radarSize / GRID_SIZE
        val radarRadius = radarSize / 2f

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        //drawGrid(canvas, left, top, right, bottom, cellSize)
        //drawRotatedGrid(canvas, centerX, centerY, radarSize, cellSize)
        drawCardinalPoints(canvas, centerX, centerY, radarRadius)
        //drawPulse(canvas, centerX, centerY, radarRadius)
        drawPlayer(canvas, centerX, centerY, cellSize)
        //drawPOIs(canvas, centerX, centerY, cellSize)

        foundMessage?.let {
            canvas.drawText(it, centerX, centerY - radarRadius * 0.62f, foundTextPaint)
        }

        lastFeedbackMessage?.let {
            canvas.drawText(it, centerX, centerY + radarRadius * 0.72f, textPaint)
        }

        if (userLatitude == null || userLongitude == null) {
            canvas.drawText("Buscando GPS", centerX, centerY + radarRadius * 0.62f, textPaint)
        }

        val elapsed = System.currentTimeMillis() - pulseStartTime
        if (elapsed < PULSE_DURATION_MS) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawGrid(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        cellSize: Float
    ) {
        for (i in 0..GRID_SIZE) {
            val x = left + i * cellSize
            val y = top + i * cellSize

            val paint = if (i == CENTER_CELL_INDEX || i == CENTER_CELL_INDEX + 1) {
                gridStrongPaint
            } else {
                gridPaint
            }

            canvas.drawLine(x, top, x, bottom, paint)
            canvas.drawLine(left, y, right, y, paint)
        }
    }

    private fun drawRotatedGrid(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radarSize: Float,
        cellSize: Float
    ) {
        val halfSize = radarSize / 2f

        canvas.save()

        // Rota la cuadrícula para que pertenezca al mismo "mundo" que los POIs.
        // Si el usuario mira al Este, el mundo gira visualmente en sentido contrario.
        canvas.rotate(-headingDegrees, centerX, centerY)

        val left = centerX - halfSize
        val top = centerY - halfSize
        val right = centerX + halfSize
        val bottom = centerY + halfSize

        for (i in 0..GRID_SIZE) {
            val x = left + i * cellSize
            val y = top + i * cellSize

            val paint = if (i == CENTER_CELL_INDEX || i == CENTER_CELL_INDEX + 1) {
                gridStrongPaint
            } else {
                gridPaint
            }

            canvas.drawLine(x, top, x, bottom, paint)
            canvas.drawLine(left, y, right, y, paint)
        }

        canvas.restore()
    }

    private fun drawCardinalPoints(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radarRadius: Float
    ) {
        val labelRadius = radarRadius * 0.86f

        val cardinals = listOf(
            "N" to 0f,
            "NE" to 45f,
            "E" to 90f,
            "SE" to 135f,
            "S" to 180f,
            "SO" to 225f,
            "O" to 270f,
            "NO" to 315f
        )

        for ((label, bearing) in cardinals) {
            val relativeBearing = normalizeDegrees(bearing - headingDegrees)
            val radians = Math.toRadians(relativeBearing.toDouble())

            val x = centerX + (sin(radians) * labelRadius).toFloat()
            val y = centerY - (cos(radians) * labelRadius).toFloat() + cardinalPaint.textSize / 3f

            canvas.drawText(label, x, y, cardinalPaint)
        }
    }

    private fun drawPulse(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radarRadius: Float
    ) {
        val elapsed = System.currentTimeMillis() - pulseStartTime
        if (elapsed < 0 || elapsed > PULSE_DURATION_MS) return

        val progress = elapsed.toFloat() / PULSE_DURATION_MS.toFloat()
        val radius = radarRadius * progress

        pulsePaint.alpha = ((1f - progress) * 190f).toInt().coerceIn(30, 190)
        canvas.drawCircle(centerX, centerY, radius, pulsePaint)
    }

    private fun drawPlayer(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        cellSize: Float
    ) {
        val size = cellSize * 0.32f

        val path = Path().apply {
            moveTo(centerX, centerY - size)
            lineTo(centerX - size * 0.8f, centerY + size)
            lineTo(centerX + size * 0.8f, centerY + size)
            close()
        }

        canvas.drawPath(path, playerPaint)
    }

    private fun drawPOIs(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        cellSize: Float
    ) {
        val lat = userLatitude
        val lng = userLongitude

        if (lat == null || lng == null) return

        val visibleRangeMeters = currentZoom.metersPerCell * (GRID_SIZE / 2f)

        for (poi in pois) {
            if (poi.isFound) continue

            val distance = distanceBetweenMeters(
                lat,
                lng,
                poi.latitude,
                poi.longitude
            )

            if (distance > visibleRangeMeters) continue

            val absoluteBearing = bearingBetweenDegrees(
                lat,
                lng,
                poi.latitude,
                poi.longitude
            )

            val relativeBearing = normalizeDegrees(absoluteBearing - headingDegrees)

            val distanceInCells = distance / currentZoom.metersPerCell
            val distanceInPixels = distanceInCells * cellSize

            val radians = Math.toRadians(relativeBearing.toDouble())

            val poiX = centerX + (sin(radians) * distanceInPixels).toFloat()
            val poiY = centerY - (cos(radians) * distanceInPixels).toFloat()

            val pulseTouched = isPulseTouchingDistance(distance, visibleRangeMeters)

            if (pulseTouched && poi.lastDetectedPulseId != pulseId) {
                poi.lastDetectedPulseId = pulseId
                playPoiDetectedFeedback(distance, visibleRangeMeters)
            }

            val radius = if (pulseTouched) cellSize * 0.16f else cellSize * 0.11f
            val paint = if (pulseTouched) poiDetectedPaint else poiPaint

            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 240, 90)
                alpha = if (pulseTouched) 120 else 50
                style = Paint.Style.FILL
            }

            canvas.drawCircle(poiX, poiY, radius * 2.2f, glowPaint)
            canvas.drawCircle(poiX, poiY, radius, paint)
        }
    }

    private fun isPulseTouchingDistance(
        poiDistanceMeters: Float,
        visibleRangeMeters: Float
    ): Boolean {
        val elapsed = System.currentTimeMillis() - pulseStartTime
        if (elapsed < 0 || elapsed > PULSE_DURATION_MS) return false

        val progress = elapsed.toFloat() / PULSE_DURATION_MS.toFloat()
        val pulseDistanceMeters = visibleRangeMeters * progress

        val toleranceMeters = currentZoom.metersPerCell * 0.25f

        return abs(pulseDistanceMeters - poiDistanceMeters) <= toleranceMeters
    }

    private fun playPoiDetectedFeedback(
        distance: Float,
        visibleRangeMeters: Float
    ) {
        val normalizedCloseness = 1f - (distance / visibleRangeMeters).coerceIn(0f, 1f)

        val tone = if (normalizedCloseness > 0.66f) {
            ToneGenerator.TONE_PROP_BEEP
        } else {
            ToneGenerator.TONE_PROP_ACK
        }

        val duration = if (normalizedCloseness > 0.66f) 120 else 80

        toneGenerator.startTone(tone, duration)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            handleRadarTap()
            return true
        }

        return true
    }

    private fun handleRadarTap() {
        val lat = userLatitude
        val lng = userLongitude

        if (lat == null || lng == null) {
            showTemporaryFeedback("Sin ubicación")
            return
        }

        val centralPoi = getClosestPOIInsideCentralCell(lat, lng)

        if (centralPoi == null) {
            showTemporaryFeedback("Acércate al POI")
            toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 100)
            return
        }

        val nextZoom = currentZoom.nextOrNull()

        if (nextZoom != null) {
            currentZoom = nextZoom
            showTemporaryFeedback(currentZoom.label)
            onZoomChanged?.invoke(currentZoom)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 100)
            invalidate()
        } else {
            centralPoi.isFound = true
            foundMessage = "POI ENCONTRADO"
            showTemporaryFeedback(centralPoi.name)
            onPoiFound?.invoke(centralPoi)
            onStatusChanged?.invoke("POI encontrado: ${centralPoi.name}")
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            invalidate()
        }
    }

    private fun getClosestPOIInsideCentralCell(
        userLat: Double,
        userLng: Double
    ): RadarPOI? {
        var closestPoi: RadarPOI? = null
        var closestDistance = Float.MAX_VALUE

        val centralCellRadiusMeters = currentZoom.metersPerCell * CENTRAL_CELL_RADIUS_FACTOR

        for (poi in pois) {
            if (poi.isFound) continue

            val distance = distanceBetweenMeters(
                userLat,
                userLng,
                poi.latitude,
                poi.longitude
            )

            if (distance <= centralCellRadiusMeters && distance < closestDistance) {
                closestPoi = poi
                closestDistance = distance
            }
        }

        return closestPoi
    }

    private fun showTemporaryFeedback(message: String) {
        lastFeedbackMessage = message
        invalidate()

        mainHandler.postDelayed({
            if (lastFeedbackMessage == message) {
                lastFeedbackMessage = null
                invalidate()
            }
        }, 1200L)
    }

    private fun distanceBetweenMeters(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float {
        val result = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, result)
        return result[0]
    }

    private fun bearingBetweenDegrees(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float {
        val startLocation = Location("start").apply {
            latitude = startLat
            longitude = startLng
        }

        val endLocation = Location("end").apply {
            latitude = endLat
            longitude = endLng
        }

        return normalizeDegrees(startLocation.bearingTo(endLocation))
    }

    private fun normalizeDegrees(value: Float): Float {
        var result = value % 360f
        if (result < 0f) result += 360f
        return result
    }
}