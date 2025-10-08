package com.example.fyp_fitledger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fyp_fitledger.R

class CustomizeWorkoutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customize_workout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnYes: Button = view.findViewById(R.id.btnYes)
        val btnNo: Button = view.findViewById(R.id.btnNo)
        val btnMaybeNextTime: Button = view.findViewById(R.id.btnMaybeNextTime)

        btnYes.setOnClickListener {
            (activity as? DemographicActivity)?.addFragment("FitnessGoalFragment")
            (activity as? DemographicActivity)?.nextPage()
            //findNavController().navigate(R.id.action_customizeWorkoutFragment_to_fitnessGoalFragment)
        }

        btnNo.setOnClickListener {
            (activity as? DemographicActivity)?.addFragment("DietPlanFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        btnMaybeNextTime.setOnClickListener {
            (activity as? DemographicActivity)?.addFragment("DietPlanFragment")
            (activity as? DemographicActivity)?.nextPage()
        }
    }
}
