package com.example.lifemaxx.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifemaxx.R
import com.example.lifemaxx.repository.SupplementRepository

class MainActivity : AppCompatActivity() {
    private val repository = SupplementRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Fetch supplements
        repository.getSupplements("USER_ID") { supplements ->
            recyclerView.adapter = SupplementsAdapter(supplements)
        }

        findViewById<View>(R.id.addSupplementButton).setOnClickListener {
            startActivity(Intent(this, AddSupplementActivity::class.java))
        }
    }
}
