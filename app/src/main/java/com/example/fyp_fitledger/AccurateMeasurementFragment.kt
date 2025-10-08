package com.example.fyp_fitledger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController

class AccurateMeasurementFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        userViewModel = ViewModelProvider(requireActivity()).get(UserViewModel::class.java)

        val view = inflater.inflate(R.layout.fragment_accurate_measurement, container, false)

        val btnYes = view.findViewById<Button>(R.id.btnYes)
        val btnNo = view.findViewById<Button>(R.id.btnNo)
        val btnMaybe = view.findViewById<Button>(R.id.btnMaybe)

        btnYes.setOnClickListener {
            //(activity as? DemographicActivity)?.addMethodSelectionFragment()
            (activity as? DemographicActivity)?.addFragment("MethodSelectionFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        btnNo.setOnClickListener {
            //(activity as? DemographicActivity)?.addCustomizeWorkoutFragment_1()
            userViewModel.updateAccuFat(false)
            (activity as? DemographicActivity)?.addFragment("CustomizeWorkoutFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        btnMaybe.setOnClickListener {
            //(activity as? DemographicActivity)?.addCustomizeWorkoutFragment_1()
            userViewModel.updateAccuFat(false)
            (activity as? DemographicActivity)?.addFragment("CustomizeWorkoutFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }
}
