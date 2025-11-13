package com.example.fyp_fitledger.ui.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fyp_fitledger.ui.activity.DemographicActivity
import com.example.fyp_fitledger.R

class FatCaliperFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fat_caliper, container, false)

        val ivFatCaliper = view.findViewById<ImageView>(R.id.ivFatCaliper)
        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val step3Text = view.findViewById<TextView>(R.id.step3Text)
        val step6Text = view.findViewById<TextView>(R.id.step6Text)

        // Load stored gender from SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val gender = sharedPreferences.getString("gender", "Male") // Default is Male

        // Set gender-based instructions
        if (gender == "Male") {
            step3Text.text = getString(R.string.frag_fat_cali_step_3)
            step6Text.text = getString(R.string.frag_fat_cali_step_6)
        } else {
            step3Text.text = getString(R.string.frag_fat_cali_step_3_female)
            step6Text.text = getString(R.string.frag_fat_cali_step_6_female)
        }



        // Navigate to the next step
        btnNext.setOnClickListener {
            (activity as? DemographicActivity)?.addFragment("FatCaliperInputFragment")
            (activity as? DemographicActivity)?.nextPage()
            //findNavController().navigate(R.id.action_fatCaliperFragment_to_nextFragment)
        }

        return view
    }
}
