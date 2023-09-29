package com.project.openanyfiles

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.project.openanyfiles.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var REQUEST_CODE = 1
    private var selectedFileUri: Uri? = null
    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var mediaPlayer: MediaPlayer? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage = 0
    private var currentAudioPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.MainFloatingPlay.setOnClickListener {
            playAudio()
        }

        binding.MainFloatingPause.setOnClickListener {
            pauseAudio()
        }

        binding.MainFloatingStop.setOnClickListener {
            stopAudio()
        }

        binding.MainFab.setOnClickListener {
            openFile()
        }
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedFileUri = uri

                if (isImageFile(uri)) {
                    binding.MainImg.visibility = ImageView.VISIBLE
                    binding.MainVideo.visibility = VideoView.GONE
                    binding.MainImg.setImageURI(uri)
                } else if (isVideoFile(uri)) {
                    playVideo(uri)
                } else if (isAudioFile(uri)) {
                    showAudioPlaybackUI(true)
                } // في داخل onActivityResult
                else if (isPdfFile(uri)) {
//                    // إذا كان الملف PDF، قم بعرضه في WebView
//                    binding.MainPdfWebView.visibility = WebView.VISIBLE
//                    binding.MainImg.visibility = ImageView.GONE
//                    binding.MainVideo.visibility = VideoView.GONE
//
//                    // قم بتحديد WebView الذي تريد استخدامه
//                    val webView: WebView = findViewById(R.id.MainPdfWebView)
//                    webView.settings.javaScriptEnabled = true
//
//                    webView.loadUrl("file:///android_asset/pdfjs/web/viewer.html")
//                    webView.setWebViewClient(object : WebViewClient() {
//                        override fun onPageFinished(view: WebView?, url: String?) {
//                            // قم بتحميل ملف الـ PDF في WebView
//                            webView.loadUrl("javascript:loadPdf('$uri');")
//                        }
//                    })
                } else {
                    // Handle other file types as needed
                }
            }
        }
    }

    private fun showAudioPlaybackUI(show: Boolean) {
        if (show) {
            binding.MainSeekBar.visibility = View.VISIBLE
            binding.MainVideo.visibility = VideoView.GONE
            binding.MainImg.visibility = ImageView.GONE
            binding.MainLinearIcons.visibility = LinearLayout.VISIBLE
            binding.MainLinearDurationTv.visibility = LinearLayout.VISIBLE
        } else {
            binding.MainSeekBar.visibility = View.GONE
            binding.MainFloatingPlay.visibility = View.GONE
            binding.MainFloatingPause.visibility = View.GONE
            binding.MainFloatingStop.visibility = View.GONE
        }
    }

    private fun isImageFile(uri: Uri): Boolean {
        val type = contentResolver.getType(uri)
        return type?.startsWith("image/") == true
    }

    private fun isVideoFile(uri: Uri): Boolean {
        val type = contentResolver.getType(uri)
        return type?.startsWith("video/") == true
    }

    private fun isAudioFile(uri: Uri): Boolean {
        val type = contentResolver.getType(uri)
        return type?.startsWith("audio/") == true

    }

    private fun playVideo(uri: Uri) {
        binding.MainVideo.visibility = VideoView.VISIBLE
        binding.MainImg.visibility = ImageView.GONE
        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.MainVideo)
        binding.MainVideo.setMediaController(mediaController)
        binding.MainVideo.setVideoURI(uri)
        binding.MainVideo.requestFocus()
        binding.MainVideo.start()
    }

    private fun playAudio() {
        selectedFileUri?.let { uri ->
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(this, uri)
                mediaPlayer?.prepare()
                binding.MainSeekBar.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) mediaPlayer?.seekTo(progress)
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    //Notification that the user has finished a touch gesture
                    override fun onStopTrackingTouch(p0: SeekBar?) {
                    }

                })
            }

            if (!mediaPlayer?.isPlaying!!) {
                // إعادة تعيين موقع الصوت إلى البداية
                currentAudioPosition = 0

                mediaPlayer?.start()

                binding.MainSeekBar.visibility = View.VISIBLE
                binding.MainSeekBar.max = mediaPlayer?.duration ?: 0

                handler = Handler(Looper.getMainLooper())
                runnable = object : Runnable {
                    override fun run() {
                        mediaPlayer?.currentPosition?.let {
                            binding.MainSeekBar.progress = it
                        }
//                        val currentTime = mediaPlayer?.currentPosition ?: 0
//                        val totalTime = mediaPlayer?.duration ?: 0
//                        binding.MainTvDue.text = "${totalTime / 1000} sec"
//                        binding.MainTvPlayer.text = "${currentTime / 1000} sec"
                        val currentTime = mediaPlayer?.currentPosition ?: 0
                        val totalTime = mediaPlayer?.duration ?: 0

                        val currentSeconds = currentTime / 1000
                        val totalSeconds = totalTime / 1000

                        val minutes = currentSeconds / 60
                        val seconds = currentSeconds % 60

                        val totalMinutes = totalSeconds / 60
                        val totalSecondsRemaining = totalSeconds % 60

                        binding.MainTvDue.text =
                            String.format("%02d:%02d", totalMinutes, totalSecondsRemaining)
                        binding.MainTvPlayer.text = String.format("%02d:%02d", minutes, seconds)


                        handler.postDelayed(this, 1000) // Update every 1 second
                    }
                }
                handler.postDelayed(runnable, 0)
            }
        }
    }

    private fun pauseAudio() {
        if (mediaPlayer?.isPlaying!!) {
            mediaPlayer?.pause()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacks(runnable)
        binding.MainSeekBar.progress = 0
        binding.MainTvDue.text = "0 sec"
        binding.MainTvPlayer.text = "0 sec"
    }

    private fun isPdfFile(uri: Uri): Boolean {
        val type = contentResolver.getType(uri)
        return type?.startsWith("application/pdf") == true
    }

}



