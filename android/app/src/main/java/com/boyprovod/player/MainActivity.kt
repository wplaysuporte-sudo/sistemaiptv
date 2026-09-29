package com.boyprovod.player

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.boyprovod.player.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null

    private val demoUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.playerView.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { exo ->
                binding.playerView.player = exo
                exo.setMediaItem(MediaItem.fromUri(demoUrl))
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        binding.statusText.text = when (state) {
                            Player.STATE_BUFFERING -> "Carregando transmissão..."
                            Player.STATE_READY -> "BOYPROVOD • conectado"
                            Player.STATE_ENDED -> "Transmissão finalizada"
                            else -> "BOYPROVOD Player"
                        }
                    }
                })
                exo.prepare()
                exo.playWhenReady = true
            }
        }
    }

    override fun onStop() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }
}