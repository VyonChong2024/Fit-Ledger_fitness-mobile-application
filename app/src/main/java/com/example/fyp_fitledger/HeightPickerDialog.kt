package com.example.fyp_fitledger

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class HeightPickerDialog(
    private val isCm: Boolean,
    private val currentValue: Int,
    private val onHeightSelected: (Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val recyclerView = RecyclerView(requireContext()).apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        val values = if (isCm) {
            (50..250).toList() // cm
        } else {
            (30..80).toList()  // feet *10 (like 30 = 3.0, 80 = 8.0)
        }

        val adapter = HeightAdapter(values, currentValue) { selected ->
            onHeightSelected(selected)
            dismiss()
        }
        recyclerView.adapter = adapter

        // Snap to center
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        return AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setTitle("Select Height")
            .setView(recyclerView)
            .setNegativeButton("Cancel", null)
            .create()
    }
}