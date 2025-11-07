package com.example.fyp_fitledger

import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class WeightAdapter(
    private val values: List<Int>
) : RecyclerView.Adapter<WeightAdapter.WeightViewHolder>() {

    var selectedPosition: Int = values.size / 2 // default center

    inner class WeightViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(value: Int, position: Int) {
            textView.text = value.toString()

            if (position == selectedPosition) {
                // Center item style
                textView.textSize = 38f
                textView.setTypeface(null, Typeface.BOLD)
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.black))
            } else {
                // Non-selected style
                textView.textSize = 18f
                textView.setTypeface(null, Typeface.NORMAL)
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray))
            }
        }

        fun bind(value: Int, position: Int, selectedPos: Int) {
            val distance = kotlin.math.abs(position - selectedPos)

            when (distance) {
                0 -> { // center
                    textView.textSize = 38f
                    textView.setTypeface(null, Typeface.BOLD)
                    textView.setTextColor(ContextCompat.getColor(textView.context, R.color.black))
                }
                1 -> { // one above/below
                    textView.textSize = 22f
                    textView.setTypeface(null, Typeface.NORMAL)
                    textView.setTextColor(ContextCompat.getColor(textView.context, R.color.gray))
                }
                2 -> { // two above/below
                    textView.textSize = 18f
                    textView.setTextColor(ContextCompat.getColor(textView.context, R.color.soft_gray))
                }
                else -> { // everything else
                    textView.textSize = 16f
                    textView.setTextColor(ContextCompat.getColor(textView.context, R.color.grayish_blue))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeightViewHolder {
        val tv = TextView(parent.context).apply {
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                150 // each row height
            )
        }
        return WeightViewHolder(tv)
    }

    override fun getItemCount() = values.size

    override fun onBindViewHolder(holder: WeightViewHolder, position: Int) {
        holder.bind(values[position], position)
    }
}