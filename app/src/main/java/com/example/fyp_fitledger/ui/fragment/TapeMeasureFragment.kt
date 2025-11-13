package com.example.fyp_fitledger.ui.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fyp_fitledger.ui.activity.DemographicActivity
import com.example.fyp_fitledger.R

class TapeMeasureFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tape_measure, container, false)

        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val step5Text = view.findViewById<TextView>(R.id.step5Text)

        // Load stored gender from SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val gender = sharedPreferences.getString("gender", "Male") // Default is Male

        // Set gender-based instructions
        if (gender == "Male") {
            step5Text.text = getString(R.string.frag_tape_meas_step_5)
        } else {
            step5Text.text = getString(R.string.frag_tape_meas_step_5_female)
        }

        // Navigate to the next step
        btnNext.setOnClickListener {
            (activity as? DemographicActivity)?.addFragment("TapeMeasureInputFragment")
            (activity as? DemographicActivity)?.nextPage()
            //findNavController().navigate(R.id.action_tapeMeasureFragment_to_nextFragment)
        }

        return view
    }
}
