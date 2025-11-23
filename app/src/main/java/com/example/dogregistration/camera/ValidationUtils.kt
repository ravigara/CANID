package com.example.dogregistration.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.max

/**
 * Validates that the given URI is a dog nose image by:
 *  1) running ML Kit Object Detection (single-image mode, classification enabled)
 *  2) requiring either a detected object with label containing "dog" OR
 *     a detected object with bounding box area ratio > minObjectAreaRatio
 *  3) running noseDetector.isNoseFromUri(uri) and checking classifierThreshold
 *
 * Returns Pair(accepted:Boolean, score:Float). Score is the classifier score (lower -> more nose-like).
 */
suspend fun validateDogNoseUri(
    context: Context,
    uri: Uri,
    noseDetector: NoseDetector,
    classifierThreshold: Float = 0.20f,
    minObjectAreaRatio: Float = 0.01f
): Pair<Boolean, Float> = withContext(Dispatchers.IO) {
    try {
        // Build ML Kit detector (single image)
        val image = InputImage.fromFilePath(context, uri)
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .build()
        val detector = ObjectDetection.getClient(options)

        val detectedObjects: List<DetectedObject> = try {
            detector.process(image).await()
        } catch (e: Exception) {
            Log.w("ValidationUtils", "Object detection failed: ${e.message}")
            emptyList()
        } finally {
            try { detector.close() } catch (_: Exception) {}
        }

        // Load bitmap to compute image area for object area ratio
        val bmp = loadBitmapDownsample(context, uri, maxDim = 1024)
        val imgArea = bmp?.let { max(1, it.width * it.height) } ?: 1

        var hasDogLabel = false
        var hasLargeObject = false
        for (obj in detectedObjects) {
            if (obj.labels.any { lbl -> lbl.text.contains("dog", ignoreCase = true) }) {
                hasDogLabel = true
            }
            val bb = obj.boundingBox
            val area = bb.width() * bb.height()
            val ratio = area.toFloat() / imgArea.toFloat()
            if (ratio >= minObjectAreaRatio) hasLargeObject = true
        }

        // Run the nose classifier (returns Pair<Boolean, Float>) - must be implemented in your NoseDetector
        val (isNose, score) = try {
            noseDetector.isNoseFromUri(uri)
        } catch (e: Exception) {
            Log.w("ValidationUtils", "Classifier run failed: ${e.message}")
            Pair(false, 1.0f)
        }

        val passesObjectCheck = hasDogLabel || hasLargeObject
        val passesClassifier = isNose && score <= classifierThreshold
        val accept = passesObjectCheck && passesClassifier

        Pair(accept, score)
    } catch (e: Exception) {
        Log.e("ValidationUtils", "validate error", e)
        Pair(false, 1.0f)
    }
}

/** Downsample large images to avoid OOM and to make downstream detectors stable. */
suspend fun loadBitmapDownsample(context: Context, uri: Uri, maxDim: Int = 1200): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = max(1, (maxOf(w, h) / maxDim))
                val targetW = (w / scale)
                val targetH = (h / scale)
                decoder.setTargetSize(targetW, targetH)
            }
        } else {
            @Suppress("DEPRECATION")
            val bmp = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val w = bmp.width
            val h = bmp.height
            if (maxOf(w, h) <= maxDim) return@withContext bmp
            val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
            Bitmap.createScaledBitmap(bmp, (w * ratio).toInt(), (h * ratio).toInt(), true)
        }
    } catch (e: IOException) {
        Log.w("ValidationUtils", "loadBitmapDownsample failed: ${e.message}")
        null
    } catch (e: Exception) {
        Log.w("ValidationUtils", "decode error: ${e.message}")
        null
    }
}

/**
 * Robust processing wrapper: validates and computes embedding with retries.
 * Returns Result.success(Pair(embedding, score)) on success, or Result.failure on error.
 */
suspend fun processUriSafely(
    context: Context,
    uri: Uri,
    noseDetector: NoseDetector,
    recognizerFunc: suspend (Uri) -> FloatArray?, // e.g. { uri -> recognizer.getEmbedding(uri) }
    maxAttempts: Int = 2,
    classifierThreshold: Float = 0.20f,
    minObjectAreaRatio: Float = 0.01f
): Result<Pair<FloatArray, Float>> = withContext(Dispatchers.IO) {
    var lastErr: Exception? = null
    for (attempt in 1..maxAttempts) {
        try {
            // best-effort persistable permission
            try {
                val flags = (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (permEx: Exception) {
                Log.w("processUriSafely", "takePersistableUriPermission failed: ${permEx.message}")
            }

            // Pre-decode (ensures file readable)
            val bmp = loadBitmapDownsample(context, uri, maxDim = 1200)
            bmp?.recycle()

            // Validate
            val (accepted, score) = validateDogNoseUri(context, uri, noseDetector, classifierThreshold, minObjectAreaRatio)
            if (!accepted) return@withContext Result.failure(IllegalStateException("Validation failed (score=${"%.3f".format(score)})"))

            // Embedding
            val emb = recognizerFunc(uri)
            if (emb == null) return@withContext Result.failure(IllegalStateException("Embedding computation returned null"))

            return@withContext Result.success(Pair(emb, score))
        } catch (e: Exception) {
            lastErr = e
            Log.w("processUriSafely", "attempt $attempt failed: ${e.message}")
        }
    }
    Result.failure(lastErr ?: IllegalStateException("Unknown processing failure"))
}
