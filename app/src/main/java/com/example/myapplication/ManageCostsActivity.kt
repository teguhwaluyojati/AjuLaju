package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.CostAdapter
import kotlinx.coroutines.launch

class ManageCostsActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: CostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manage_costs)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCosts)
        val textEmpty = findViewById<TextView>(R.id.textEmptyCosts)

        adapter = CostAdapter(
            onEdit = { cost ->
                val intent = Intent(this, AddCostActivity::class.java)
                intent.putExtra(AddCostActivity.EXTRA_COST_ID, cost.id)
                startActivity(intent)
            },
            onDelete = { cost ->
                AlertDialog.Builder(this)
                    .setTitle("Hapus Biaya")
                    .setMessage("Hapus catatan ${cost.type} senilai Rp ${cost.amount}?")
                    .setPositiveButton("Hapus") { _, _ ->
                        lifecycleScope.launch {
                            db.costDao().deleteById(cost.id)
                            Toast.makeText(this@ManageCostsActivity, "Data dihapus", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        db.costDao().getAllCostsWithVehicle().asLiveData().observe(this) { costs ->
            adapter.submitList(costs)
            textEmpty.visibility = if (costs.isNullOrEmpty()) View.VISIBLE else View.GONE
        }
    }
}
