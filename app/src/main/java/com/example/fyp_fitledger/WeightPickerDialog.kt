package com.example.fyp_fitledger

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class WeightPickerDialog(
    private val isKg: Boolean,
    private val currentValue: Int,
    private val onWeightSelected: (Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_picker, null)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewWeight)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val values = if (isKg) (30..200).toList() else (60..440).toList()

        val adapter = WeightAdapter(values)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // set default selection to currentValue
        adapter.selectedPosition = values.indexOf(currentValue)
        recyclerView.scrollToPosition(adapter.selectedPosition)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(rv.layoutManager)
                    val pos = rv.getChildAdapterPosition(centerView!!)
                    if (pos != RecyclerView.NO_POSITION) {
                        adapter.selectedPosition = pos
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        })

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        btnOk.setOnClickListener {
            onWeightSelected(values[adapter.selectedPosition])
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        val itemHeight = resources.getDimensionPixelSize(R.dimen.picker_item_height) // define in dimens.xml (e.g. 80dp)

        // Wait until RecyclerView is laid out
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val padding = recyclerView.height / 2 - itemHeight / 2
                recyclerView.setPadding(0, padding, 0, padding)
                recyclerView.clipToPadding = false
            }
        })


        return dialog
    }
}
