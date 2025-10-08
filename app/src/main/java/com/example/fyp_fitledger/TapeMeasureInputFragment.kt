package com.example.fyp_fitledger

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.util.Locale
import kotlin.math.log10

class TapeMeasureInputFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel
    private var isCm = true // Default to cm

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tape_measure_input, container, false)

        val etNeck = view.findViewById<EditText>(R.id.etNeck)
        val etWaist = view.findViewById<EditText>(R.id.etWaist)
        val etHips = view.findViewById<EditText>(R.id.etHips)

        val containerHips = view.findViewById<View>(R.id.containerHips)

        val btnCalculate = view.findViewById<Button>(R.id.btnCalculate)

        val unitSwitch = view.findViewById<SwitchCompat>(R.id.unitSwitch)
        val cmTextView = view.findViewById<TextView>(R.id.cmTextView)
        val inchTextView = view.findViewById<TextView>(R.id.inchTextView)

        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        val gender = userViewModel.gender

        // Hide hip input for males
        if (gender == "Male") {
            containerHips.visibility = View.GONE
        } else if (gender == "Female") {
            containerHips.visibility = View.VISIBLE
        } else {
            Log.e("TapeMeasureInputFragment", "Invalid gender: $gender")
        }

        // Set default unit text style
        updateUnitTextStyle(cmTextView, inchTextView)

        unitSwitch.setOnCheckedChangeListener { _, isChecked ->
            isCm = !isChecked
            updateUnitTextStyle(cmTextView, inchTextView)
            convertInputs(etNeck, etWaist, etHips)
        }

        btnCalculate.setOnClickListener {
            val neck = etNeck.text.toString().toDoubleOrNull() ?: 0.0
            val waist = etWaist.text.toString().toDoubleOrNull() ?: 0.0
            val hips = etHips.text.toString().toDoubleOrNull() ?: 0.0

            val neckInCm = if (isCm) neck else convertInchToCm(neck)
            val waistInCm = if (isCm) waist else convertInchToCm(waist)
            val hipsInCm = if (isCm && etHips.visibility == View.VISIBLE) hips else if (!isCm && etHips.visibility == View.VISIBLE) convertInchToCm(hips) else 0.0

            if (waistInCm <= neckInCm || (gender == "Female" && (waistInCm + hipsInCm) <= neckInCm)) {
                Toast.makeText(context, "Invalid measurements!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bodyFat = if (gender == "Male") {
                495 / (1.0324 - 0.19077 * log10(waistInCm - neckInCm) + 0.15456 * log10(170.0)) - 450
            } else {
                495 / (1.29579 - 0.35004 * log10(waistInCm + hipsInCm - neckInCm) + 0.22100 * log10(170.0)) - 450
            }

            userViewModel.updateBodyFatPercent(bodyFat)
            userViewModel.updateBodyDensity(0.0)
            Log.d("BodyFatCalc", "Body Fat: $bodyFat, SaveBodyFat: ${userViewModel.bodyFatPercent}")

            (activity as? DemographicActivity)?.addFragment("BodyFatResultFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }

    private fun convertCmToInch(cm: Double): Double {
        return cm / 2.54
    }

    private fun convertInchToCm(inch: Double): Double {
        return inch * 2.54
    }

    private fun updateUnitTextStyle(cmTextView: TextView, inchTextView: TextView) {
        if (isCm) {
            cmTextView.setTypeface(null, Typeface.BOLD)
            cmTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.teal_700))
            inchTextView.setTypeface(null, Typeface.NORMAL)
            inchTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        } else {
            inchTextView.setTypeface(null, Typeface.BOLD)
            inchTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.teal_700))
            cmTextView.setTypeface(null, Typeface.NORMAL)
            cmTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    private fun convertInputs(etNeck: EditText, etWaist: EditText, etHips: EditText) {
        val neck = etNeck.text.toString().toDoubleOrNull() ?: 0.0
        val waist = etWaist.text.toString().toDoubleOrNull() ?: 0.0
        val hips = etHips.text.toString().toDoubleOrNull() ?: 0.0

        if (isCm) {
            etNeck.setText(String.format(Locale.getDefault(),"%.1f", convertInchToCm(neck)))
            etWaist.setText(String.format(Locale.getDefault(),"%.1f", convertInchToCm(waist)))
            if (etHips.visibility == View.VISIBLE) {
                etHips.setText(String.format(Locale.getDefault(),"%.1f", convertInchToCm(hips)))
            }
        } else {
            etNeck.setText(String.format(Locale.getDefault(),"%.1f", convertCmToInch(neck)))
            etWaist.setText(String.format(Locale.getDefault(),"%.1f", convertCmToInch(waist)))
            if (etHips.visibility == View.VISIBLE) {
                etHips.setText(String.format(Locale.getDefault(),"%.1f", convertCmToInch(hips)))
            }
        }
    }
}