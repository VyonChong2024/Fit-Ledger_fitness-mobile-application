package com.example.fyp_fitledger

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.text.format

class HeightWeightFragment : Fragment() {

    private lateinit var tvHeight: TextView
    private lateinit var btnMinusHeight: Button
    private lateinit var btnPlusHeight: Button
    private lateinit var radioCm: RadioButton
    private lateinit var radioFeet: RadioButton

    private lateinit var tvWeight: TextView
    private lateinit var btnMinusWeight: Button
    private lateinit var btnPlusWeight: Button
    private lateinit var radioKg: RadioButton
    private lateinit var radioLbs: RadioButton

    private var isLongClickWeight = false
    private var isLongClickPlusHeight = false
    private var isLongClickMinusHeight = false

    private lateinit var btnNext: Button

    private var heightCm = 170.0
    private var heightFeet = 5.6
    private var weightKg = 65.0
    private var weightLbs = 143.3

    private var isCm = true
    private var isKg = true

    private lateinit var userViewModel: UserViewModel

    private val handler = Handler(Looper.getMainLooper())
    private var heightIncrementRunnable: Runnable? = null
    private var heightDecrementRunnable: Runnable? = null
    private var weightIncrementRunnable: Runnable? = null
    private var weightDecrementRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_height_weight, container, false)
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        tvHeight = view.findViewById(R.id.tvHeight)
        btnMinusHeight = view.findViewById(R.id.btnMinusHeight)
        btnPlusHeight = view.findViewById(R.id.btnPlusHeight)
        radioCm = view.findViewById(R.id.radioCm)
        radioFeet = view.findViewById(R.id.radioFeet)

        tvWeight = view.findViewById(R.id.tvWeight)
        btnMinusWeight = view.findViewById(R.id.btnMinusWeight)
        btnPlusWeight = view.findViewById(R.id.btnPlusWeight)
        radioKg = view.findViewById(R.id.radioKg)
        radioLbs = view.findViewById(R.id.radioLbs)

        btnNext = view.findViewById(R.id.btnNext)

        btnPlusHeight.setOnClickListener { changeHeight(0.1) }
        btnMinusHeight.setOnClickListener { changeHeight(-0.1) }
        btnPlusWeight.setOnClickListener { changeWeight(0.1) }
        btnMinusWeight.setOnClickListener { changeWeight(-0.1) }

        //Height
        //Long Click Minus Height
        btnMinusHeight.setOnLongClickListener {
            isLongClickMinusHeight = true
            startDecrementHeight()
            true
        }
        btnMinusHeight.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isLongClickMinusHeight = false
                stopDecrementHeight()
            }
            false
        }
        //Long Click Plus Height
        btnPlusHeight.setOnLongClickListener {
            isLongClickPlusHeight = true
            startIncrementHeight()
            true
        }
        btnPlusHeight.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isLongClickPlusHeight = false
                stopIncrementHeight()
            }
            false
        }


        //Weight
        //Long Click Plus Weight
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

        radioCm.setOnCheckedChangeListener { _, isChecked ->
            isCm = isChecked
            updateHeightDisplay()
        }
        radioKg.setOnCheckedChangeListener { _, isChecked ->
            isKg = isChecked
            updateWeightDisplay()
        }

        btnNext.setOnClickListener {
            findNavController().navigate(R.id.action_to_accurateMeasurementFragment)
        }

        changeHeight(0.0)
        changeWeight(0.0)

        btnNext.setOnClickListener {
            userViewModel.updateHeight(heightCm.format(2))
            userViewModel.updateWeight(weightKg.format(2))
            Log.d("User View Model update", "Height: $heightCm cm, $heightFeet feet")
            Log.d("User View Model update", "Weight: $weightKg kg, $weightLbs lbs")
            userViewModel.saveToPreferences(requireContext())
            (activity as? DemographicActivity)?.addFragment("BodyMetricsFragment")
            (activity as? DemographicActivity)?.nextPage()
            //findNavController().navigate(R.id.action_to_bodyMetricsFragment)
        }

        return view
    }

    //convert to two decimal places
    private fun Double.format(digits: Int) = "%.${digits}f".format(this).toDouble()

    private fun changeHeight(delta: Double) {
        if (isCm) {
            heightCm += delta
            heightFeet = cmtofeet(heightCm)
        } else {
            heightFeet += delta
            heightCm = feettocm(heightFeet)
        }
        updateHeightDisplay()
    }

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
    private fun cmtofeet(cm: Double): Double {
        return cm / 30.48
    }
    private fun feettocm(feet: Double): Double {
        return feet * 30.48
    }

    //Change display value in Height
    private fun updateHeightDisplay() {
        val displayHeight = if (isCm) heightCm else heightFeet
        val unit = if (isCm) "cm" else "ft"
        tvHeight.text = String.format(Locale.getDefault(), "%.1f %s", displayHeight, unit)
    }

    private fun updateWeightDisplay() {
        val displayWeight = if (isKg) weightKg else weightLbs
        val unit = if (isKg) "kg" else "lbs"
        tvWeight.text = String.format(Locale.getDefault(), "%.1f %s", displayWeight, unit)
    }


    private fun startIncrementHeight() {
        if (heightIncrementRunnable == null) {
            Log.d("HeightControl", "start Increment Height")
            heightIncrementRunnable = Runnable {
                changeHeight(0.1)
                handler.postDelayed(heightIncrementRunnable!!, 70) // Adjust delay as needed
            }
            handler.post(heightIncrementRunnable!!)
        }
    }
    private fun stopIncrementHeight() {
        Log.d("HeightControl", "Stop Incrementing height")
        heightIncrementRunnable?.let { handler.removeCallbacks(it) }
        heightIncrementRunnable = null
    }

    private fun startDecrementHeight() {
        if (heightDecrementRunnable == null) {
            Log.d("HeightControl", "start Decrement Height")
            heightDecrementRunnable = Runnable {
                changeHeight(-0.1)
                handler.postDelayed(heightDecrementRunnable!!, 70)
            }
            handler.post(heightDecrementRunnable!!)
        }
    }
    private fun stopDecrementHeight() {
        Log.d("HeightControl", "Stop Decrementing height")
        heightDecrementRunnable?.let { handler.removeCallbacks(it) }
        heightDecrementRunnable = null
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