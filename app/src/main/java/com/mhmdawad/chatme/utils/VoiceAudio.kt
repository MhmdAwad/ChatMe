package com.mhmdawad.chatme.utils

import android.Manifest
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.SoundPool
import android.util.Log
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import com.mhmdawad.chatme.R
import java.io.File
import java.io.IOException

class VoiceAudio {

    private var mediaPlayer: MediaPlayer = MediaPlayer()
    private var recorder: MediaRecorder? = null

    fun stopRecordPlayer() {
        Log.d("tty", "stopRecord")
        if (recorder != null) {
            try {
                recorder?.release()
                recorder = null
            } catch (e: RuntimeException) {
            }
        }
    }


    private fun playRecord(voiceLink: String, recordPlay: ImageButton) {
        if (mediaPlayer.isPlaying)
            mediaPlayer.reset()

        try {
            mediaPlayer.setDataSource(voiceLink)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                mediaPlayer.start()
                recordPlay.setImageResource(R.drawable.ic_pause_record)
                Log.d("Duration", "${it.duration}")
            }
        } catch (e: IOException) {
            Log.d("error", "$e")
        }
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.reset()
            recordPlay.setImageResource(R.drawable.ic_play_record)
        }
    }


    fun voiceRecord(fileName: String) {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            Thread {
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e("LOG_TAG", "prepare() failed")
                }
                start()
            }
        }
    }
}