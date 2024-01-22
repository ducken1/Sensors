package com.example.poraprojekt


import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poraprojekt.databinding.ActivityHistoryBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter // Create this adapter

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadDataFromFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val readingsCollection = firestore.collection("Readings")

        readingsCollection.orderBy("absoluteTime") // Add this line to order by time
            .get()
            .addOnSuccessListener { documents ->
                val historyList = mutableListOf<HistoryItem>()

                for (document in documents) {
                    val sensorType = document.getString("sensorType") ?: ""
                    val sensorData = document.getString("sensorData") ?: "0.0"
                    val absoluteTime = document.getString("absoluteTime") ?: ""
                    val location = document.getString("location") ?: ""
                    val casTrajanja = document.getLong("casTrajanja") ?: 0

                    val historyItem = HistoryItem(
                        sensorType,
                        sensorData,
                        absoluteTime,
                        location,
                        casTrajanja
                    )
                    historyList.add(historyItem)
                }

                // Sort the list by time before setting it to the adapter
                val sortedList = historyList.sortedByDescending { historyItem ->
                    // Convert time string to milliseconds since epoch for sorting
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val instant = LocalDateTime.parse(historyItem.absoluteTime, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                    instant.toEpochMilli()
                }

                adapter.setItems(sortedList)
            }
            .addOnFailureListener { e ->
                // Handle failure if needed
            }
    }

}