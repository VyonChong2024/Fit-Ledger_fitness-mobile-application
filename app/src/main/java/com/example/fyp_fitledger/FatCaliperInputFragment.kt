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

class FatCaliperInputFragment: Fragment() {

    private lateinit var userViewModel: UserViewModel
    private var ismm = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fat_caliper_input, container, false)

        val etChest = view.findViewById<EditText>(R.id.etChest)
        val etAbdomen = view.findViewById<EditText>(R.id.etAbdomen)
        val etTricep = view.findViewById<EditText>(R.id.etTricep)
        val etSuprailiac = view.findViewById<EditText>(R.id.etSuprailiac)
        val etThigh = view.findViewById<EditText>(R.id.etThigh)

        val containerChest = view.findViewById<View>(R.id.containerChest)
        val containerAbdomen = view.findViewById<View>(R.id.containerAbdomen)
        val containerTricep = view.findViewById<View>(R.id.containerTricep)
        val containerSuprailiac = view.findViewById<View>(R.id.containerSuprailiac)
        val containerThigh = view.findViewById<View>(R.id.containerThigh)

        val btnCalculate = view.findViewById<Button>(R.id.btnCalculate)

        val unitSwitch = view.findViewById<SwitchCompat>(R.id.unitSwitch)
        val mmTextView = view.findViewById<TextView>(R.id.mmTextView)
        val inchTextView = view.findViewById<TextView>(R.id.inchTextView)

        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)
        val gender = userViewModel.gender
        val age = userViewModel.age

        if (gender == "Male") {
            containerChest.visibility = View.VISIBLE
            containerAbdomen.visibility = View.VISIBLE

            containerTricep.visibility = View.GONE
            containerSuprailiac.visibility = View.GONE
        } else if (gender == "Female") {
            containerTricep.visibility = View.VISIBLE
            containerSuprailiac.visibility = View.VISIBLE

            containerChest.visibility = View.GONE
            containerAbdomen.visibility = View.GONE
        } else {
            Log.e("FatCaliperInputFragment", "Invalid gender: $gender")
        }

        updateUnitTextStyle(mmTextView, inchTextView)

        unitSwitch.setOnCheckedChangeListener { _, isChecked ->
            ismm = !isChecked
            updateUnitTextStyle(mmTextView, inchTextView)
            convertInputs(etChest, etAbdomen, etTricep, etSuprailiac, etThigh)
        }

        btnCalculate.setOnClickListener {
            // Convert EditText values to Double safely
            var chest = etChest.text.toString().toDoubleOrNull()
            var abdomen = etAbdomen.text.toString().toDoubleOrNull()
            var tricep = etTricep.text.toString().toDoubleOrNull()
            var suprailiac = etSuprailiac.text.toString().toDoubleOrNull()
            var thigh = etThigh.text.toString().toDoubleOrNull()

            if (!ismm) {
                chest = convertInchTomm(chest!!)
                abdomen = convertInchTomm(abdomen!!)
                tricep = convertInchTomm(tricep!!)
                suprailiac = convertInchTomm(suprailiac!!)
                thigh = convertInchTomm(thigh!!)
            }

            val isValid = validateInput(
                gender = gender?: "",
                chest = chest?.toInt(),
                abdomen = abdomen?.toInt(),
                tricep = tricep?.toInt(),
                suprailiac = suprailiac?.toInt(),
                thigh = thigh?.toInt(),
                context = requireContext()
            )
            if (!isValid) return@setOnClickListener

            // Calculate Body Fat Percentage
            val bodyDensity = if (gender == "Male") {
                1.10938 - (0.0008267 * (chest!! + abdomen!! + thigh!!)) +
                        (0.0000016 * Math.pow((chest + abdomen + thigh), 2.0)) -
                        (0.0002574 * age!!)
            } else {
                1.0994921 - (0.0009929 * (tricep!! + suprailiac!! + thigh!!)) +
                        (0.0000023 * Math.pow((tricep + suprailiac + thigh), 2.0)) -
                        (0.0001392 * age!!)
            }
            // Convert Body Density to Body Fat Percentage
            val bodyFatPercentage = (495 / bodyDensity) - 450

            userViewModel.updateBodyDensity(bodyDensity)
            userViewModel.updateBodyFatPercent(bodyFatPercentage)
            Log.d("BodyFatCalc - FatCaliper", "Body Fat: $bodyFatPercentage; Body Density: $bodyDensity")

            (activity as? DemographicActivity)?.addFragment("BodyFatResultFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }

    private fun convertmmToInch(mm: Double): Double {
        return mm / 25.4
    }

    private fun convertInchTomm(inch: Double): Double {
        return inch * 25.4
    }

    private fun updateUnitTextStyle(mmTextView: TextView, inchTextView: TextView) {
        if (ismm) {
            mmTextView.setTypeface(null, Typeface.BOLD)
            mmTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_brand_green))
            inchTextView.setTypeface(null, Typeface.NORMAL)
            inchTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
        } else {
            inchTextView.setTypeface(null, Typeface.BOLD)
            inchTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_brand_green))
            mmTextView.setTypeface(null, Typeface.NORMAL)
            mmTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }
    }

    private fun convertInputs(etChest: EditText, etAbdomen: EditText, etTricep: EditText, etSuprailiac: EditText, etThigh: EditText) {
        val chest = etChest.text.toString().toDoubleOrNull() ?: 0.0
        val abdomen = etAbdomen.text.toString().toDoubleOrNull() ?: 0.0
        val tricep = etTricep.text.toString().toDoubleOrNull() ?: 0.0
        val suprailiac = etSuprailiac.text.toString().toDoubleOrNull() ?: 0.0
        val thigh = etThigh.text.toString().toDoubleOrNull() ?: 0.0

        if (ismm) {
            etThigh.setText(String.format(Locale.getDefault(),"%.1f", convertInchTomm(thigh)))
            if (etTricep.visibility == View.VISIBLE) {
                etTricep.setText(String.format(Locale.getDefault(),"%.1f", convertInchTomm(tricep)))
            }
            if (etSuprailiac.visibility == View.VISIBLE) {
                etSuprailiac.setText(String.format(Locale.getDefault(),"%.1f", convertInchTomm(suprailiac)))
            }
            if (etChest.visibility == View.VISIBLE) {
                etChest.setText(String.format(Locale.getDefault(),"%.1f", convertInchTomm(chest)))
            }
            if (etAbdomen.visibility == View.VISIBLE) {
                etAbdomen.setText(String.format(Locale.getDefault(),"%.1f", convertInchTomm(abdomen)))
            }
        } else {
            etThigh.setText(String.format(Locale.getDefault(),"%.1f", convertmmToInch(thigh)))
            if (etTricep.visibility == View.VISIBLE) {
                etTricep.setText(String.format(Locale.getDefault(),"%.1f", convertmmToInch(tricep)))
            }
            if (etSuprailiac.visibility == View.VISIBLE) {
                etSuprailiac.setText(String.format(Locale.getDefault(),"%.1f", convertmmToInch(suprailiac)))
            }
            if (etChest.visibility == View.VISIBLE) {
                etChest.setText(String.format(Locale.getDefault(),"%.1f", convertmmToInch(chest)))
            }
            if (etAbdomen.visibility == View.VISIBLE) {
                etAbdomen.setText(String.format(Locale.getDefault(),"%.1f", convertmmToInch(abdomen)))
            }
        }
    }

    private fun validateInput(gender: String, chest: Int?, abdomen: Int?, tricep: Int?, suprailiac: Int?, thigh: Int?, context: Context): Boolean {
        val requiredParams = when (gender) {
            "Male" -> listOf(chest, abdomen, thigh)
            "Female" -> listOf(tricep, suprailiac, thigh)
            else -> {
                Log.e("FatCaliperInputFragment", "Invalid gender: $gender")
                Toast.makeText(context, "Invalid gender selected.", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        // Check missing input
        if (requiredParams.any { it == null }) {
            Toast.makeText(context, "Please enter all required skinfold values.", Toast.LENGTH_SHORT).show()
            return false
        }
        // Check range
        if (requiredParams.any { it!! < 3 || it > 50 }) {
            Toast.makeText(context, "Skinfold values must be between 3mm and 50mm.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}