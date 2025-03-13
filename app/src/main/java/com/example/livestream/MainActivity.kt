package com.example.livestream

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.livestream.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnJoinAsHost.setOnClickListener {
            navigateToCallActivity(true)  // Pass true for host
        }

        binding.btnJoinAsAudience.setOnClickListener {
            navigateToCallActivity(false)  // Pass false for audience
        }
    }

    private fun navigateToCallActivity(isHost: Boolean) {
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra("isHost", isHost)
        startActivity(intent)
    }
}
