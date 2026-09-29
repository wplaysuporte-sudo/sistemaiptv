package com.boyprovod.player

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.boyprovod.player.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.channelName.text = intent.getStringExtra("name") ?: "BOYPROVOD"
    }

    override fun onStart() {
        super.onStart()
        val url = intent.getStringExtra("url") ?: return
        player = ExoPlayer.Builder(this).build().also { exo ->
            binding.playerView.player = exo
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    binding.statusText.text = when (state) {
                        Player.STATE_BUFFERING -> "Carregando..."
                        Player.STATE_READY -> "BOYPROVOD • conectado"
                        Player.STATE_ENDED -> "Transmissão finalizada"
                        else -> ""
                    }
                }
            })
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    override fun onStop() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }
}