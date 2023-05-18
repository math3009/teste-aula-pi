package com.example.myapplication.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityProfileBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.bottom_settings
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_settings -> true
                R.id.bottom_calendar -> {
                    startActivity(Intent(applicationContext, CalendarSheduleActivity::class.java))
                    finish()
                    true
                }
                R.id.bottom_chat-> {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        binding.btDados.setOnClickListener {
            navegar(nomeTela = "telaEditar")
        }



    }

    private fun navegar(nomeTela: String){
        val intent = Intent(this, EditUserActivity::class.java)
        intent.putExtra("telaEditar", nomeTela)
        startActivity(intent)
    }


}