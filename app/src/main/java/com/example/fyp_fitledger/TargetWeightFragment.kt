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
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import java.util.Locale

class TargetWeightFragment : Fragment() {
    private val workoutPlanViewModel: WorkoutPlanViewModel by activityViewModels()

    private lateinit var tvTargetWeight: TextView
    private lateinit var btnMinusWeight: Button
    private lateinit var btnPlusWeight: Button
    private lateinit var radioKg: RadioButton
    private lateinit var radioLbs: RadioButton

    private var weightKg = 65.0
    private var weightLbs = 143.3

    private var isKg = true

    private lateinit var userViewModel: UserViewModel

    private val handler = Handler(Looper.getMainLooper())
    private var weightIncrementRunnable: Runnable? = null
    private var weightDecrementRunnable: Runnable? = null
    private var isLongClickWeight = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_target_weight, container, false)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        tvTargetWeight = view.findViewById(R.id.tvTargetWeight)
        btnMinusWeight = view.findViewById(R.id.btnMinusWeight)
        btnPlusWeight = view.findViewById(R.id.btnPlusWeight)
        radioKg = view.findViewById(R.id.radioKg)
        radioLbs = view.findViewById(R.id.radioLbs)
        val btnNext = view.findViewById<Button>(R.id.btnNext)

        // Pre-fill default weight from ViewModel
        var weight = (userViewModel.weight)?.format(2)
        if (weight != null) {
            tvTargetWeight.setText("$weight kg")
            weightKg = weight.format(2)
            weightLbs = kgtolbs(weight).format(2)
        }

        btnPlusWeight.setOnClickListener { changeWeight(0.1) }
        btnMinusWeight.setOnClickListener { changeWeight(-0.1) }

        btnPlusWeight.setOnLongClickListener {
            isLongClickWeight = true
            startIncrementWeight()
            true
        }
        btnPlusWeight.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isLongClickWeight = false
                stopIncrementWeight()
            }
            false
        }

        //Long Click Minus Weight
        btnMinusWeight.setOnLongClickListener {
            isLongClickWeight = true
            startDecrementWeight()
            true
        }
        btnMinusWeight.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isLongClickWeight = false
                stopDecrementWeight()
            }
            false
        }

        radioKg.setOnCheckedChangeListener { _, isChecked ->
            isKg = isChecked
            updateWeightDisplay()
        }

        btnNext.setOnClickListener {
            val targetWeight = weightKg.format(2)

            workoutPlanViewModel.updateTargetWeight(targetWeight)
            val w = workoutPlanViewModel.targetWeight

            (activity as? DemographicActivity)?.addFragment("TargetBodyFatFragment")
            (activity as? DemographicActivity)?.nextPage()
        }
        return view
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this).toDouble()

    private fun changeWeight(delta: Double) {
        if (isKg) {
            weightKg += delta
            weightLbs = kgtolbs(weightKg)
        } else {
            weightLbs += delta
            weightKg = lbstokg(weightLbs)
        }
        updateWeightDisplay()
    }

    private fun kgtolbs(kg: Double): Double {
        return kg * 2.205
    }
    private fun lbstokg(lbs: Double): Double {
        return lbs / 2.205
    }

    private fun updateWeightDisplay() {
        val displayWeight = if (isKg) weightKg else weightLbs
        val unit = if (isKg) "kg" else "lbs"
        tvTargetWeight.text = String.format(Locale.getDefault(), "%.1f %s", displayWeight, unit)
    }

    private fun startIncrementWeight() {
        if (weightIncrementRunnable == null) {
            Log.d("WeightControl", "start Increment Weight")
            weightIncrementRunnable = Runnable {
                changeWeight(0.1)
                handler.postDelayed(weightIncrementRunnable!!, 70)
            }
            handler.post(weightIncrementRunnable!!)
        }
    }
    private fun stopIncrementWeight() {
        Log.d("WeightControl", "Stop Incrementing weight")
        weightIncrementRunnable?.let { handler.removeCallbacks(it) }
        weightIncrementRunnable = null
    }

    private fun startDecrementWeight() {
        if (weightDecrementRunnable == null) {
            Log.d("WeightControl", "start Decrement Weight")
            weightDecrementRunnable = Runnable {
                changeWeight(-0.1)
                handler.postDelayed(weightDecrementRunnable!!, 70)
            }
            handler.post(weightDecrementRunnable!!)
        }
    }
    private fun stopDecrementWeight() {
        Log.d("WeightControl", "Stop Decrementing weight")
        weightDecrementRunnable?.let { handler.removeCallbacks(it) }
        weightDecrementRunnable = null
    }
}
