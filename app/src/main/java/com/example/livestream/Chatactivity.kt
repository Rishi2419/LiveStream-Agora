package com.example.livestream

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.livestream.databinding.ActivityChatactivityBinding

class Chatactivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatactivityBinding
    private val messages = mutableListOf<Pair<String, Boolean>>() // Pair (Message, isUser)
    private lateinit var adapter: ChatAdapter
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChatactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up RecyclerView
        adapter = ChatAdapter(messages)
        binding.recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Ensures messages appear from the bottom
        }

        binding.recyclerView.adapter = adapter

        // Send message on button click
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (!TextUtils.isEmpty(message)) {
                sendMessage(message)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun sendMessage(msg: String) {
        messages.add(Pair(msg, true)) // User message
        adapter.notifyItemInserted(messages.size - 1)
        binding.recyclerView.scrollToPosition(messages.size - 1)

        // Simulated bot response after 3 seconds
        handler.postDelayed({
            messages.add(Pair("Hello", false)) // Bot message
            adapter.notifyItemInserted(messages.size - 1)
            binding.recyclerView.scrollToPosition(messages.size - 1)
        }, 3000)
    }

}
