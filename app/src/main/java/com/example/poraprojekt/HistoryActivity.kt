package com.example.poraprojekt


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poraprojekt.databinding.ActivityHistoryBinding
import com.google.firebase.firestore.FirebaseFirestore

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter // Create this adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)


        adapter = HistoryAdapter()
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = adapter

        loadDataFromFirestore()

        binding.homeButton.setOnClickListener {
            // Handle the "Home" button click, e.g., navigate back to MainActivity
            finish() // This will close the HistoryActivity and go back to MainActivity
        }
    }

    private fun loadDataFromFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val readingsCollection = firestore.collection("Readings")

        readingsCollection.get()
            .addOnSuccessListener { documents ->
                val historyList = mutableListOf<HistoryItem>()

                for (document in documents) {
                    val sensorType = document.getString("sensorType") ?: ""
                    val sensorData = document.getString("sensorData") ?: 0.0
                    val absoluteTime = document.getString("absoluteTime") ?: ""
                    val location = document.getString("location") ?: ""
                    val casTrajanja = document.getLong("casTrajanja") ?: 0

                    val historyItem = HistoryItem(
                        sensorType,
                        sensorData.toString(),
                        absoluteTime,
                        location,
                        casTrajanja
                    )
                    historyList.add(historyItem)
                }

                adapter.setItems(historyList)
            }
            .addOnFailureListener { e ->
                // Handle failure if needed
            }
    }
}