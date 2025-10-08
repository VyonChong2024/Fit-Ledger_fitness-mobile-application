package com.example.fyp_fitledger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class DietPlanFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_diet_plan, container, false)

        val btnYes = view.findViewById<Button>(R.id.btnYes)
        val btnNo = view.findViewById<Button>(R.id.btnNo)

        btnYes.setOnClickListener {
            //(activity as? DemographicActivity)?.addTapeMeasureFragment()
            (activity as? DemographicActivity)?.addFragment("DietPlanTypeFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        btnNo.setOnClickListener {
            //(activity as? DemographicActivity)?.addFatCaliperFragment()
            (activity as? DemographicActivity)?.addFragment("DietNutrientPlanFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }
}