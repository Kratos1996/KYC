@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util


import android.content.Context
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import java.io.File

actual class VideoRecorder(private val context: Context) {
    private var recording: Recording? = null
    private var savedVideoPath: String? = null
    private val videoCapture: VideoCapture<Recorder> by lazy {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        VideoCapture.withOutput(recorder)
    }

    actual fun startRecording(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val videoFile = File(context.filesDir, "chaintech_face_video_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            onError("Recording error: ${event.error}")
                        } else {
                            savedVideoPath = videoFile.absolutePath
                            onResult(videoFile.absolutePath)
                        }
                    }
                }
            }
    }

    actual fun stopRecording() {
        recording?.stop()
        recording = null
    }

    actual fun getVideoUri(): String? = savedVideoPath

    actual fun release() {
        recording?.stop()
        recording = null
    }
}
