package com.example.fyp_fitledger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class MethodSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_method_selection, container, false)

        val btnTapeMeasure = view.findViewById<Button>(R.id.btnTapeMeasure)
        val btnFatCaliper = view.findViewById<Button>(R.id.btnFatCaliper)

        btnTapeMeasure.setOnClickListener {
            //(activity as? DemographicActivity)?.addTapeMeasureFragment()
            (activity as? DemographicActivity)?.addFragment("TapeMeasureFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        btnFatCaliper.setOnClickListener {
            //(activity as? DemographicActivity)?.addFatCaliperFragment()
            (activity as? DemographicActivity)?.addFragment("FatCaliperFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }
}
