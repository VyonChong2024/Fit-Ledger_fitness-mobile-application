package com.example.fyp_fitledger

import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HeightAdapter(
    private val values: List<Int>,
    private val currentValue: Int,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<HeightAdapter.HeightViewHolder>() {

    private var selectedPosition = values.indexOf(currentValue).takeIf { it >= 0 } ?: (values.size / 2)

    inner class HeightViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(value: Int, isSelected: Boolean) {
            textView.text = if (value < 10) value.toString() else value.toString()
            textView.textSize = if (isSelected) 24f else 16f
            textView.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            textView.alpha = if (isSelected) 1.0f else 0.5f

            textView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position != selectedPosition) {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)   // only update old item
                    notifyItemChanged(selectedPosition) // only update new item
                    onItemClick(value)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeightViewHolder {
        val tv = TextView(parent.context).apply {
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                200,
                150 // item height for spacing
            )
        }
        return HeightViewHolder(tv)
    }

    override fun getItemCount() = values.size

    override fun onBindViewHolder(holder: HeightViewHolder, position: Int) {
        holder.bind(values[position], position == selectedPosition)
    }
}
