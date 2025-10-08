package com.example.fyp_fitledger

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

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
            step3Text.text = "Pinch the Chest skin"
            step6Text.text = "Repeat for Abdomen & Thigh"
        } else {
            step3Text.text = "Pinch the Tricep skin"
            step6Text.text = "Repeat for Suprailiac & Thigh"
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
