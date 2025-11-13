package com.example.fyp_fitledger.ui.dialog

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
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.ui.adapter.ValuePickerAdapter

class ValuePickerDialog(
    private val title: String,
    private val values: List<Int>,
    private val currentValue: Int,
    private val onValueSelected: (Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_picker, null)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val adapter = ValuePickerAdapter(values)
        recyclerView.adapter = adapter
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // Default selection
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

            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val centerView = snapHelper.findSnapView(rv.layoutManager) ?: return
                val pos = rv.getChildAdapterPosition(centerView)
                if (pos != RecyclerView.NO_POSITION && pos != adapter.selectedPosition) {
                    adapter.selectedPosition = pos
                    adapter.notifyDataSetChanged()
                }
            }
        })

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .create()

        btnOk.setOnClickListener {
            onValueSelected(values[adapter.selectedPosition])
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val itemHeight =
                    resources.getDimensionPixelSize(R.dimen.picker_item_height)
                val padding = recyclerView.height / 2 - itemHeight / 2
                recyclerView.setPadding(0, padding, 0, padding)
                recyclerView.clipToPadding = false

                val index = values.indexOf(currentValue)
                recyclerView.scrollToPosition(index)

                // Center align the selected item
                recyclerView.post {
                    val view = recyclerView.layoutManager?.findViewByPosition(index)
                    if (view != null) {
                        val centerY = recyclerView.height / 2
                        val viewCenterY = (view.top + view.bottom) / 2
                        val offset = viewCenterY - centerY
                        recyclerView.scrollBy(0, offset)
                    }
                }
            }
        })

        return dialog
    }
}
