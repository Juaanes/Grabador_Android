package campalans.dam.grabador_android

import android.Manifest.permission
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import campalans.dam.grabador_android.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var mRecorder: MediaRecorder? = null
    private var mPlayer: MediaPlayer? = null
    var mFileName: File? = null
    private var isPlaying = false
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnRecord.setOnClickListener {
            startRecording()
        }

        binding.btnStop.setOnClickListener {
            pauseRecording()
        }

        binding.btnPlay.setOnClickListener {
            if (mFileName != null) {
                if (isPlaying) {
                    pauseAudio()
                } else {
                    playAudio()
                }
            } else {
                Toast.makeText(applicationContext, "No hi ha grabació que reproduir", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStopPlay.setOnClickListener {
            restartAudio()
        }
    }

    private fun startRecording() {
        if (CheckPermissions()) {
            mFileName = File(getExternalFilesDir("")?.absolutePath, "Record.3gp")

            var n = 0
            while (mFileName!!.exists()) {
                n++
                mFileName = File(getExternalFilesDir("")?.absolutePath, "Record$n.3gp")
            }

            mRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(mFileName)
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e("TAG", "prepare() failed")
                }
                start()
            }
            binding.idTVstatus.text = "Recording in progress"
        } else {
            RequestPermissions()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_AUDIO_PERMISSION_CODE -> if (grantResults.isNotEmpty()) {
                val permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (permissionToRecord && permissionToStore) {
                    Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun CheckPermissions(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, permission.WRITE_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, permission.RECORD_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun RequestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(permission.RECORD_AUDIO, permission.WRITE_EXTERNAL_STORAGE), REQUEST_AUDIO_PERMISSION_CODE)
    }

    private fun playAudio() {
        if (!isPlaying) {
            binding.btnPlay.setBackgroundResource(R.drawable.button_pause)
            mPlayer = MediaPlayer().apply {
                try {
                    setDataSource(mFileName.toString())
                    prepare()
                    start()
                    binding.idTVstatus.text = "Listening recording"
                    startRotationAnimation()
                    this@MainActivity.isPlaying = true
                    setOnCompletionListener {
                        stopAudioPlayback()
                    }
                } catch (e: IOException) {
                    Log.e("TAG", "prepare() failed")
                }
            }
        }
    }
    private fun stopAudioPlayback() {
        binding.btnPlay.setBackgroundResource(R.drawable.btn_rec_play)
        binding.idTVstatus.text = "Recording finished"
        stopRotationAnimation()
        isPlaying = false
        mPlayer?.release()
        mPlayer = null
    }

    private fun pauseRecording() {
        if (mFileName == null) {
            Toast.makeText(applicationContext, "Registration not started", Toast.LENGTH_LONG).show()
        } else {
            mRecorder?.stop()
            val savedUri = Uri.fromFile(mFileName)
            val msg = "File saved: ${savedUri.lastPathSegment}"
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
            mRecorder?.release()
            mRecorder = null
            binding.idTVstatus.text = "Recording interrupted"
        }
    }

    private fun pauseAudio() {
        mPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                binding.btnPlay.setBackgroundResource(R.drawable.btn_rec_play)
                binding.idTVstatus.text = "Recording paused"
                stopRotationAnimation()
                isPlaying = false
            }
        }
    }

    private fun restartAudio() {
        mPlayer?.release()
        mPlayer = null
        binding.btnPlay.setBackgroundResource(R.drawable.btn_rec_play)
        isPlaying = false
        playAudio()
    }

    private fun startRotationAnimation() {
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate)
        binding.idTVstatus.startAnimation(rotate)
    }

    private fun stopRotationAnimation() {
        binding.idTVstatus.clearAnimation()
    }

    companion object {
        const val REQUEST_AUDIO_PERMISSION_CODE = 1
    }
}
