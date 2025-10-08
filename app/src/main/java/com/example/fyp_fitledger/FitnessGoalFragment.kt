package com.example.fyp_fitledger

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController

class FitnessGoalFragment : Fragment(R.layout.fragment_fitness_goal) {
    private val workoutPlanViewModel: WorkoutPlanViewModel by activityViewModels()
    private val selectedGoals = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnNext: Button = view.findViewById(R.id.btnNext)
        val buttons = listOf(
            view.findViewById<Button>(R.id.btnBecomeHealthier),
            view.findViewById<Button>(R.id.btnLoseWeight),
            view.findViewById<Button>(R.id.btnLoseFat),
            view.findViewById<Button>(R.id.btnGainWeight),
            view.findViewById<Button>(R.id.btnGainMuscle)
        )

        buttons.forEach { button ->
            button.setOnClickListener {
                toggleSelection(button)

                btnNext.isEnabled = selectedGoals.isNotEmpty()
                btnNext.setBackgroundColor(ContextCompat.getColor(requireContext(), if (btnNext.isEnabled) R.color.purple_500 else R.color.gray))
            }
        }

        btnNext.setOnClickListener {
            workoutPlanViewModel.updateGoal(selectedGoals)     //tried 'joinToString' feature on view model
            workoutPlanViewModel.loadUserData(requireContext())

            (activity as? DemographicActivity)?.addFragment("TargetWeightFragment")
            (activity as? DemographicActivity)?.nextPage()
        }
    }

    private fun toggleSelection(button: Button) {
        val goal = button.text.toString()
        if (selectedGoals.contains(goal)) {
            selectedGoals.remove(goal)
            button.isSelected = false
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.teal_700))
        } else {
            selectedGoals.add(goal)
            button.isSelected = true
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.teal_700))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
    }
}
