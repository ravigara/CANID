package com.example.dogregistration.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NoseDetector(private val context: Context, private val modelFileName: String = "dog_nose_classifier.tflite") {

    private var interpreter: Interpreter? = null
    private val loadLock = ReentrantLock()
    private val runLock = ReentrantLock()

    private val inputWidth = 224
    private val inputHeight = 224

    private val imageProcessor: ImageProcessor by lazy {
        ImageProcessor.Builder()
            // 1. Resize to 224x224
            .add(ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
            // 2. Convert integer pixels (0-255) to Float (0.0-255.0)
            // We do NOT normalize here because the model has a Rescaling layer built-in.
            .add(CastOp(DataType.FLOAT32))
            .build()
    }

    init {
        loadModel()
    }

    private fun loadModel() {
        loadLock.withLock {
            try {
                val modelBuffer: ByteBuffer = FileUtil.loadMappedFile(context, modelFileName)
                val options = Interpreter.Options().apply { setNumThreads(2) }
                interpreter = Interpreter(modelBuffer, options)
                Log.i("NoseDetector", "Loaded $modelFileName")
            } catch (e: Exception) {
                Log.e("NoseDetector", "Failed to load model", e)
            }
        }
    }

    fun close() {
        runLock.withLock {
            interpreter?.close()
            interpreter = null
        }
    }

    // Returns (IsNose, Score)
    // 0.0 = Nose, 1.0 = Not Nose
    fun isNoseFromUri(uri: Uri, threshold: Float = 0.5f): Pair<Boolean, Float> {
        val score = scoreFromUri(uri) ?: return Pair(false, 1.0f)
        return Pair(score <= threshold, score)
    }

    fun scoreFromUri(uri: Uri): Float? {
        val localInterp = interpreter ?: return null
        val bmp = loadBitmapFromUri(uri) ?: return null

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bmp)

        val processed = imageProcessor.process(tensorImage)

        // Output: [1, 1]
        val outputBuffer = Array(1) { FloatArray(1) }

        runLock.withLock {
            return try {
                localInterp.run(processed.buffer, outputBuffer)
                // Raw output from Sigmoid (0.0 to 1.0)
                outputBuffer[0][0]
            } catch (e: Exception) {
                Log.e("NoseDetector", "Inference error", e)
                null
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                    decoder.isMutableRequired = false
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e("NoseDetector", "Bitmap load failed", e)
            null
        }
    }
}