package com.boyprovod.player

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.boyprovod.player.databinding.ActivityMainBinding
import java.net.HttpURLConnection
import java.net.URL

data class IptvItem(val name: String, val url: String)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val items = mutableListOf<IptvItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnInicio.requestFocus()
        binding.btnTestPlayer.setOnClickListener { openPlayer(DEMO_URL, "Canal Demo") }
        binding.btnAddList.setOnClickListener { showAddListDialog() }

        binding.btnInicio.setOnClickListener { showSection("Início", "Sua TV, do seu jeito.") }
        binding.btnLive.setOnClickListener { showSection("AO VIVO", "Canais ao vivo") }
        binding.btnMovies.setOnClickListener { showSection("FILMES", "Seus filmes") }
        binding.btnSeries.setOnClickListener { showSection("SÉRIES", "Suas séries") }
        binding.btnGames.setOnClickListener { showSection("JOGOS DO DIA", "Esportes em destaque") }
        binding.btnFavorites.setOnClickListener { showSection("FAVORITOS", "Seus conteúdos favoritos") }

        val saved = getSharedPreferences("boyprovod", MODE_PRIVATE).getString("m3u_url", null)
        if (!saved.isNullOrBlank()) {
            binding.importStatus.text = "Lista salva. Selecione + Adicionar Lista para atualizar."
        }
    }

    private fun showSection(label: String, title: String) {
        binding.sectionEyebrow.text = label
        binding.sectionTitle.text = title
    }

    private fun showAddListDialog() {
        val input = EditText(this).apply {
            hint = "https://servidor.com/lista.m3u"
            setSingleLine(true)
            setPadding(32, 18, 32, 18)
        }
        AlertDialog.Builder(this)
            .setTitle("Adicionar lista M3U")
            .setMessage("Cole a URL da sua lista autorizada.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Importar") { _, _ ->
                val value = input.text.toString().trim()
                if (value.startsWith("http://") || value.startsWith("https://")) importM3u(value)
                else Toast.makeText(this, "Informe uma URL válida.", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun importM3u(url: String) {
        binding.importStatus.text = "Importando lista..."
        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 25000
                conn.setRequestProperty("User-Agent", "BOYPROVOD-Player/1.0")
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseM3u(text)
                runOnUiThread {
                    items.clear()
                    items.addAll(parsed)
                    getSharedPreferences("boyprovod", MODE_PRIVATE).edit().putString("m3u_url", url).apply()
                    binding.importStatus.text = "${items.size} itens importados"
                    renderChannels()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.importStatus.text = "Erro ao importar: ${e.message ?: "falha de conexão"}"
                }
            }
        }.start()
    }

    private fun parseM3u(text: String): List<IptvItem> {
        val lines = text.lines()
        val result = mutableListOf<IptvItem>()
        var pendingName: String? = null
        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF", true)) {
                pendingName = line.substringAfterLast(",").trim().ifBlank { "Canal" }
            } else if ((line.startsWith("http://") || line.startsWith("https://")) && pendingName != null) {
                result += IptvItem(pendingName!!, line)
                pendingName = null
                if (result.size >= 5000) break
            }
        }
        return result
    }

    private fun renderChannels() {
        binding.emptyChannels.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        while (binding.channelContainer.childCount > 2) {
            binding.channelContainer.removeViewAt(2)
        }
        items.take(50).forEach { item ->
            val button = Button(this).apply {
                text = "▶  ${item.name}"
                isAllCaps = false
                setTextColor(getColor(android.R.color.white))
                setBackgroundResource(R.drawable.button_tv)
                setPadding(20, 14, 20, 14)
                isFocusable = true
                setOnClickListener { openPlayer(item.url, item.name) }
            }
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 10
            binding.channelContainer.addView(button, lp)
        }
    }

    private fun openPlayer(url: String, name: String) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("url", url)
            putExtra("name", name)
        })
    }

    companion object {
        const val DEMO_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
    }
}