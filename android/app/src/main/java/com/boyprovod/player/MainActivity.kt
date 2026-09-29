package com.boyprovod.player

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.boyprovod.player.databinding.ActivityMainBinding
import java.net.HttpURLConnection
import java.net.URL

enum class IptvKind(val label: String) {
    LIVE("AO VIVO"), MOVIE("FILME"), SERIES("SÉRIE"), SPORTS("ESPORTE")
}

data class IptvItem(
    val name: String,
    val url: String,
    val logo: String = "",
    val group: String = "",
    val kind: IptvKind = IptvKind.LIVE
)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val allItems = mutableListOf<IptvItem>()
    private lateinit var adapter: MediaAdapter
    private var selectedKind: IptvKind? = null
    private var favoritesOnly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MediaAdapter(emptyList()) { item -> openPlayer(item) }
        binding.contentGrid.layoutManager = GridLayoutManager(this, 5)
        binding.contentGrid.adapter = adapter
        binding.contentGrid.setHasFixedSize(true)

        binding.btnInicio.setOnClickListener { selectSection(null, false, "Todo o conteúdo") }
        binding.btnLive.setOnClickListener { selectSection(IptvKind.LIVE, false, "Ao Vivo") }
        binding.btnMovies.setOnClickListener { selectSection(IptvKind.MOVIE, false, "Filmes") }
        binding.btnSeries.setOnClickListener { selectSection(IptvKind.SERIES, false, "Séries") }
        binding.btnGames.setOnClickListener { selectSection(IptvKind.SPORTS, false, "Esportes") }
        binding.btnFavorites.setOnClickListener { selectSection(null, true, "Favoritos") }
        binding.btnAddList.setOnClickListener { showAddListDialog() }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = applyFilter()
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnInicio.requestFocus()

        val savedUrl = prefs().getString("m3u_url", null)
        if (!savedUrl.isNullOrBlank()) {
            binding.importStatus.text = "Atualizando lista..."
            importM3u(savedUrl, silent = true)
        }
    }

    private fun selectSection(kind: IptvKind?, favorites: Boolean, title: String) {
        selectedKind = kind
        favoritesOnly = favorites
        binding.sectionTitle.text = title
        applyFilter()
    }

    private fun applyFilter() {
        val query = binding.searchInput.text?.toString()?.trim()?.lowercase().orEmpty()
        val favoriteUrls = prefs().getStringSet("favorites", emptySet()) ?: emptySet()
        val filtered = allItems.asSequence()
            .filter { selectedKind == null || it.kind == selectedKind }
            .filter { !favoritesOnly || it.url in favoriteUrls }
            .filter {
                query.isBlank() ||
                    it.name.lowercase().contains(query) ||
                    it.group.lowercase().contains(query)
            }
            .toList()
        adapter.submit(filtered)
        binding.resultCount.text = "${filtered.size} itens"
    }

    private fun showAddListDialog() {
        val input = EditText(this).apply {
            hint = "https://servidor.com/get.php?... ou lista.m3u"
            setSingleLine(true)
            setPadding(32, 18, 32, 18)
            setText(prefs().getString("m3u_url", ""))
            selectAll()
        }
        AlertDialog.Builder(this)
            .setTitle("Adicionar lista")
            .setMessage("Cole uma URL M3U/M3U8 autorizada. O app usará os nomes, grupos e logos reais enviados pela lista.")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Importar") { _, _ ->
                val value = input.text.toString().trim()
                if (value.startsWith("http://") || value.startsWith("https://")) {
                    importM3u(value, silent = false)
                } else {
                    Toast.makeText(this, "Informe uma URL válida.", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun importM3u(url: String, silent: Boolean) {
        binding.importStatus.text = "Importando..."
        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 40000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "BOYPROVOD-Player/0.2")
                conn.setRequestProperty("Accept", "*/*")
                val code = conn.responseCode
                if (code !in 200..299) throw IllegalStateException("Servidor respondeu HTTP $code")
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseM3u(text)
                if (parsed.isEmpty()) throw IllegalStateException("Nenhum item M3U encontrado")
                runOnUiThread {
                    allItems.clear()
                    allItems.addAll(parsed)
                    prefs().edit().putString("m3u_url", url).apply()
                    binding.importStatus.text = "${allItems.size} itens carregados"
                    binding.sectionTitle.text = "Todo o conteúdo"
                    selectedKind = null
                    favoritesOnly = false
                    applyFilter()
                    if (!silent) Toast.makeText(this, "Lista importada com sucesso.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.importStatus.text = "Falha ao carregar"
                    if (!silent) {
                        AlertDialog.Builder(this)
                            .setTitle("Não foi possível importar")
                            .setMessage(e.message ?: "Falha de conexão")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }.start()
    }

    private fun parseM3u(text: String): List<IptvItem> {
        val result = ArrayList<IptvItem>()
        var pendingInfo: String? = null

        fun attr(info: String, key: String): String {
            val regex = Regex("""$key=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
            return regex.find(info)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        }

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF", true)) {
                pendingInfo = line
                continue
            }
            if ((line.startsWith("http://") || line.startsWith("https://")) && pendingInfo != null) {
                val info = pendingInfo!!
                val name = info.substringAfterLast(",").trim().ifBlank { "Sem nome" }
                val logo = attr(info, "tvg-logo")
                val group = attr(info, "group-title")
                result += IptvItem(
                    name = name,
                    url = line,
                    logo = logo,
                    group = group,
                    kind = detectKind(name, group, line)
                )
                pendingInfo = null
                if (result.size >= 15000) break
            }
        }
        return result
    }

    private fun detectKind(name: String, group: String, url: String): IptvKind {
        val hay = "$name $group $url".lowercase()
        if (listOf("sport", "esporte", "futebol", "jogo", "premiere", "espn").any { it in hay }) {
            return IptvKind.SPORTS
        }
        if (Regex("""s\d{1,2}e\d{1,3}""", RegexOption.IGNORE_CASE).containsMatchIn(name) ||
            listOf("series", "séries", "serie", "temporada").any { it in hay }) {
            return IptvKind.SERIES
        }
        if (listOf(".mp4", ".mkv", ".avi", "movie", "filme", "vod").any { it in hay }) {
            return IptvKind.MOVIE
        }
        return IptvKind.LIVE
    }

    private fun openPlayer(item: IptvItem) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("url", item.url)
            putExtra("name", item.name)
            putExtra("group", item.group)
            putExtra("logo", item.logo)
        })
    }

    private fun prefs() = getSharedPreferences("boyprovod", MODE_PRIVATE)
}