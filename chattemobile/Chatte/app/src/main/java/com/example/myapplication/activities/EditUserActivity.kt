package com.example.myapplication.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityEditUserBinding

class EditUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityEditUserBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        binding.imageBackDados.setOnClickListener {
            navegar("telaDados")
        }

    }

    private fun navegar(nomeTela: String){
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("telaDados", nomeTela)
        startActivity(intent)
    }


}