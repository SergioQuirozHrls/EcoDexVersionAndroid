package com.example.ecodexandroid

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MAX_PAGES = 5
    }

    private lateinit var etQuery: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnClearSearch: Button
    private lateinit var btnToggleFavorites: Button
    private lateinit var tvMode: TextView
    private lateinit var rvSpecies: RecyclerView
    private lateinit var btnLoadMore: Button
    private lateinit var btnLogout: Button


    private lateinit var adapter: SpeciesAdapter

    // estado principal
    private val species = mutableListOf<Species>()
    private val favoritePlants = mutableListOf<Species>()
    private val favoriteIds = mutableSetOf<Int>() // ids numéricos

    private var isSearching = false
    private var showFavoritesOnly = false
    private var page = 1
    private var hasMore = true
    private var loading = false

    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var favoritesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etQuery = findViewById(R.id.etQuery)
        btnSearch = findViewById(R.id.btnSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        btnToggleFavorites = findViewById(R.id.btnToggleFavorites)
        tvMode = findViewById(R.id.tvMode)
        rvSpecies = findViewById(R.id.rvSpecies)
        btnLoadMore = findViewById(R.id.btnLoadMore)


        btnLogout = findViewById(R.id.btnLogout)

        btnLogout.setOnClickListener {
        logout()
        }



        rvSpecies.layoutManager = LinearLayoutManager(this)
        adapter = SpeciesAdapter(
            items = emptyList(),
            isFavorite = { species -> favoriteIds.contains(species.id) },
            onToggleFavorite = { plant -> toggleFavorite(plant) },
            onItemClick = { plant -> openDetails(plant) }
        )
        rvSpecies.adapter = adapter

        btnSearch.setOnClickListener { handleSearch() }
        btnClearSearch.setOnClickListener { clearSearch() }
        btnToggleFavorites.setOnClickListener { toggleFavoritesView() }
        btnLoadMore.setOnClickListener { loadMore() }

        // escuchar cambios de auth igual que en la web
        val user = auth.currentUser
        if (user != null) {
            subscribeFavorites(user.uid)
        }

        // primera página de exploración
        loadBrowsePage(page)
    }

    override fun onDestroy() {
        super.onDestroy()
        favoritesListener?.remove()
    }

    // ----- Firestore favoritos (misma ruta que la web) -----

    private fun subscribeFavorites(uid: String) {
        favoritesListener?.remove()
        val favsRef = db.collection("users")
            .document(uid)
            .collection("plantFavorites")

        favoritesListener = favsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            favoritePlants.clear()
            favoriteIds.clear()

            snapshot?.forEach { doc ->
                val idNum = doc.id.toIntOrNull()
                val data = doc.data
                val plant = Species(
                    id = idNum ?: 0,
                    commonName = data["common_name"] as? String,
                    scientificName = data["scientific_name"] as? String,
                    imageUrl = data["image_url"] as? String,
                    family = data["family"] as? String,
                    genus = null,
                    year = null,
                    author = null,
                    status = null
                )
                favoritePlants.add(plant)
                if (idNum != null) favoriteIds.add(idNum)
            }

            refreshVisibleList()
        }
    }

    private fun isFavorite(plant: Species): Boolean =
        favoriteIds.contains(plant.id)

    private fun toggleFavorite(plant: Species) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Debes iniciar sesión para usar favoritos", Toast.LENGTH_SHORT).show()
            return
        }

        val favRef = db.collection("users")
            .document(user.uid)
            .collection("plantFavorites")
            .document(plant.id.toString())

        if (isFavorite(plant)) {
            favRef.delete()
        } else {
            val data = mapOf(
                "common_name" to plant.commonName,
                "scientific_name" to plant.scientificName,
                "image_url" to plant.imageUrl,
                "family" to plant.family,
                "addedAt" to System.currentTimeMillis()
            )
            favRef.set(data)
        }
    }


private fun logout() {
    auth.signOut()

    // Limpia listas
    favoriteIds.clear()
    favoritePlants.clear()
    species.clear()
    adapter.updateData(emptyList())

    // Detiene listeners
    favoritesListener?.remove()
    favoritesListener = null

    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

    // Regresar a LoginActivity
    startActivity(
        Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    )
    finish()
}



    // ----- Carga de datos (explorar / buscar) -----

    private fun loadBrowsePage(p: Int) {
        if (p > MAX_PAGES) {
            hasMore = false
            updateLoadMoreState()
            return
        }
        loading = true
        updateLoadMoreState()

        lifecycleScope.launch {
            try {
                val res = TrefleClient.api.listSpecies(p)
                val cleaned = res.data.filter { it.commonName != null && it.imageUrl != null }

                if (p == 1) {
                    species.clear()
                }
                species.addAll(cleaned)

                hasMore = res.links?.next != null && p < MAX_PAGES
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                loading = false
                updateLoadMoreState()
                refreshVisibleList()
            }
        }
    }

    private fun loadSearchPage(p: Int, q: String) {
        loading = true
        updateLoadMoreState()

        lifecycleScope.launch {
            try {
                val res = TrefleClient.api.searchSpecies(q, p)
                val cleaned = res.data.filter { it.commonName != null && it.imageUrl != null }

                if (p == 1) {
                    species.clear()
                }
                species.addAll(cleaned)

                hasMore = res.links?.next != null
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error búsqueda: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                loading = false
                updateLoadMoreState()
                refreshVisibleList()
            }
        }
    }

    private fun loadMore() {
        if (loading || !hasMore || showFavoritesOnly) return
        page += 1
        if (isSearching) {
            loadSearchPage(page, etQuery.text.toString().trim())
        } else {
            loadBrowsePage(page)
        }
    }

    private fun handleSearch() {
        val q = etQuery.text.toString().trim()
        if (q.isEmpty()) {
            isSearching = false
            page = 1
            hasMore = true
            loadBrowsePage(page)
            return
        }

        showFavoritesOnly = false
        isSearching = true
        page = 1
        hasMore = true
        loadSearchPage(page, q)
    }

    private fun clearSearch() {
        etQuery.setText("")
        isSearching = false
        page = 1
        hasMore = true
        loadBrowsePage(page)
    }

    private fun toggleFavoritesView() {
        showFavoritesOnly = !showFavoritesOnly
        refreshVisibleList()
    }

    private fun refreshVisibleList() {
        val visible = if (showFavoritesOnly) favoritePlants else species
        adapter.updateData(visible.toList())

        tvMode.text = when {
            showFavoritesOnly -> "Mostrando tus plantas favoritas"
            isSearching -> "Mostrando resultados de búsqueda"
            else -> "Explorando especies populares"
        }

        btnToggleFavorites.text = if (showFavoritesOnly) {
            "Ver todas"
        } else {
            "Ver favoritos (${favoritePlants.size})"
        }

        updateLoadMoreState()
    }

    private fun updateLoadMoreState() {
        btnLoadMore.isEnabled = !loading && !showFavoritesOnly && hasMore
        btnLoadMore.text = when {
            showFavoritesOnly -> "Solo favoritos (sin paginación)"
            loading -> "Cargando..."
            !hasMore -> "No hay más resultados"
            else -> "Cargar más plantas"
        }
    }

    // ----- Detalles -----

    private fun openDetails(plant: Species) {
        lifecycleScope.launch {
            try {
                val res = TrefleClient.api.getSpecies(plant.id)
                val full = res.data

                val msg = buildString {
                    appendLine(full.commonName ?: "Sin nombre común")
                    appendLine(full.scientificName ?: "")
                    if (!full.family.isNullOrEmpty()) appendLine("Familia: ${full.family}")
                    if (!full.genus.isNullOrEmpty()) appendLine("Género: ${full.genus}")
                    if (full.year != null) appendLine("Año: ${full.year}")
                    if (!full.author.isNullOrEmpty()) appendLine("Autor: ${full.author}")
                    if (!full.status.isNullOrEmpty()) appendLine("Estatus: ${full.status}")
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Detalles de la planta")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .setNegativeButton(
                        if (isFavorite(full)) "Quitar de favoritos" else "Agregar a favoritos"
                    ) { _, _ -> toggleFavorite(full) }
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al cargar detalles", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
