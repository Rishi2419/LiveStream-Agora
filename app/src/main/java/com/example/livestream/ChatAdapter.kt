package com.example.livestream

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<Pair<String, Boolean>>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Define view types as constants
    companion object {
        private const val VIEW_TYPE_USER = 1  // Right side (user message)
        private const val VIEW_TYPE_BOT = 0   // Left side (bot message)
    }

    // Create separate ViewHolder classes for each message type
    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.tvMessage)
    }

    class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.tvMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_right, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_left, parent, false)
            BotMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position].first
        when (holder) {
            is UserMessageViewHolder -> {
                holder.textMessage.text = message
                holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_RTL // Aligns to right
            }
            is BotMessageViewHolder -> {
                holder.textMessage.text = message
                holder.itemView.layoutDirection = View.LAYOUT_DIRECTION_LTR // Aligns to left
            }
        }
    }



    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].second) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }
}