package com.example.fyp_fitledger

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import java.util.Locale

class TargetBodyFatFragment : Fragment() {
    private val workoutPlanViewModel: WorkoutPlanViewModel by activityViewModels()

    private lateinit var tvTargetBodyFat: TextView
    private lateinit var btnMinus: Button
    private lateinit var btnPlus: Button
    private lateinit var btnNext: Button

    private var bodyFat = 0.0

    private lateinit var userViewModel: UserViewModel

    private val handler = Handler(Looper.getMainLooper())
    private var incrementRunnable: Runnable? = null
    private var decrementRunnable: Runnable? = null
    private var isLongClick = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_target_body_fat, container, false)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        tvTargetBodyFat = view.findViewById(R.id.tvTargetBodyFat)
        btnMinus = view.findViewById(R.id.btnMinusBodyFat)
        btnPlus = view.findViewById(R.id.btnPlusBodyFat)
        btnNext = view.findViewById(R.id.btnNext)

        // Pre-fill default body fat %
        bodyFat = userViewModel.bodyFatPercent!!
        if (bodyFat != 0.0) {
            tvTargetBodyFat.setText(bodyFat.format(1).toString())
        }

        btnPlus.setOnClickListener { changeBodyFat(0.1) }
        btnMinus.setOnClickListener { changeBodyFat(-0.1) }

        btnPlus.setOnLongClickListener {
            isLongClick = true
            startIncrement()
            true
        }
        btnPlus.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isLongClick = false
                stopIncrement()
            }
            false
        }

        //Long Click Minus Weight
        btnMinus.setOnLongClickListener {
            isLongClick = true
            startDecrement()
            true
        }
        btnMinus.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isLongClick = false
                stopDecrement()
            }
            false
        }


        btnNext.setOnClickListener {
            val targetBodyFat = tvTargetBodyFat.text.toString().toDoubleOrNull()
            if (targetBodyFat != null) {
                workoutPlanViewModel.updateTargetBodyFat(targetBodyFat)

                (activity as? DemographicActivity)?.addFragment("ActivityLevelFrequencyFragment")
                (activity as? DemographicActivity)?.nextPage()
                //findNavController().navigate(R.id.action_targetBodyFatFragment_to_activityLevelFragment)
            }
        }
        return view
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this).toDouble()

    private fun changeBodyFat(delta: Double) {
        bodyFat += delta
        updateWeightDisplay()
    }

    private fun updateWeightDisplay() {
        tvTargetBodyFat.text = String.format(Locale.getDefault(), "%.1f", bodyFat)
    }


    private fun startIncrement() {
        if (incrementRunnable == null) {
            Log.d("WeightControl", "start Increment Weight")
            incrementRunnable = Runnable {
                changeBodyFat(0.1)
                handler.postDelayed(incrementRunnable!!, 70)
            }
            handler.post(incrementRunnable!!)
        }
    }
    private fun stopIncrement() {
        Log.d("WeightControl", "Stop Incrementing weight")
        incrementRunnable?.let { handler.removeCallbacks(it) }
        incrementRunnable = null
    }

    private fun startDecrement() {
        if (decrementRunnable == null) {
            Log.d("WeightControl", "start Decrement Weight")
            decrementRunnable = Runnable {
                changeBodyFat(-0.1)
                handler.postDelayed(decrementRunnable!!, 70)
            }
            handler.post(decrementRunnable!!)
        }
    }
    private fun stopDecrement() {
        Log.d("WeightControl", "Stop Decrementing weight")
        decrementRunnable?.let { handler.removeCallbacks(it) }
        decrementRunnable = null
    }
}
