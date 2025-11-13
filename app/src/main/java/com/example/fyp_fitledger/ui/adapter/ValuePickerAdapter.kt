package com.example.fyp_fitledger.ui.adapter

import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp_fitledger.R
import kotlin.math.abs

class ValuePickerAdapter(
    private val values: List<Int>
) : RecyclerView.Adapter<ValuePickerAdapter.ValueViewHolder>() {

    var selectedPosition: Int = values.size / 2

    inner class ValueViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(value: Int, position: Int) {
            textView.text = value.toString()
            val distance = abs(position - selectedPosition)

            when (distance) {
                0 -> {
                    textView.textSize = 38f
                    textView.setTypeface(null, Typeface.BOLD)
                    textView.setTextColor(
                        ContextCompat.getColor(textView.context, R.color.black)
                    )
                    textView.alpha = 1.0f
                }
                1 -> {
                    textView.textSize = 26f
                    textView.setTypeface(null, Typeface.NORMAL)
                    textView.setTextColor(
                        ContextCompat.getColor(textView.context, R.color.gray)
                    )
                    textView.alpha = 0.75f
                }
                2 -> {
                    textView.textSize = 20f
                    textView.setTypeface(null, Typeface.NORMAL)
                    textView.setTextColor(
                        ContextCompat.getColor(textView.context, R.color.soft_gray)
                    )
                    textView.alpha = 0.5f
                }
                else -> {
                    textView.textSize = 16f
                    textView.setTypeface(null, Typeface.NORMAL)
                    textView.setTextColor(
                        ContextCompat.getColor(textView.context, R.color.grayish_blue)
                    )
                    textView.alpha = 0.3f
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValueViewHolder {
        val tv = TextView(parent.context).apply {
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                150 // same as picker_item_height
            )
        }
        return ValueViewHolder(tv)
    }

    override fun getItemCount() = values.size

    override fun onBindViewHolder(holder: ValueViewHolder, position: Int) {
        holder.bind(values[position], position)
    }
}