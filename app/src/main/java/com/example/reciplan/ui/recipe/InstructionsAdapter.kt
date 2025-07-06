package com.example.reciplan.ui.recipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.reciplan.R

class InstructionsAdapter : ListAdapter<String, InstructionsAdapter.InstructionViewHolder>(InstructionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InstructionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_instruction, parent, false)
        return InstructionViewHolder(view)
    }

    override fun onBindViewHolder(holder: InstructionViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    class InstructionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textStepNumber: TextView = itemView.findViewById(R.id.textStepNumber)
        private val textInstruction: TextView = itemView.findViewById(R.id.textInstruction)

        fun bind(instruction: String, stepNumber: Int) {
            textStepNumber.text = stepNumber.toString()
            textInstruction.text = instruction
        }
    }
}

class InstructionDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
} 