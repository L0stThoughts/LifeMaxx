package com.example.lifemaxx.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.lifemaxx.R
import com.example.lifemaxx.models.Supplement
import com.example.lifemaxx.repository.SupplementRepository
import java.util.UUID

class AddSupplementActivity : AppCompatActivity() {
    private val repository = SupplementRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_supplement)

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val name = findViewById<EditText>(R.id.nameInput).text.toString()
            val dosePerDay = findViewById<EditText>(R.id.doseInput).text.toString().toInt()
            val totalStock = findViewById<EditText>(R.id.stockInput).text.toString().toInt()

            val supplement = Supplement(
                id = UUID.randomUUID().toString(),
                name = name,
                dosePerDay = dosePerDay,
                totalStock = totalStock,
                reminders = listOf("08:00", "20:00") // Example times
            )

            repository.addSupplement("USER_ID", supplement)
            finish()
        }
    }
}
