package com.example.fyp_fitledger

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.util.Locale

class AdvancedFilterDialog(
    context: Context,
    private val allExercises: List<ExerciseSummary>,
    private val onFilterApplied: (List<ExerciseSummary>, Set<String>, Set<String>, Boolean) -> Unit, // ← updated
    private val previouslySelectedMuscles: Set<String> = emptySet(),
    private val previouslySelectedEquipments: Set<String> = emptySet(),
    private val previouslySelectedCardio: Boolean = false
) : Dialog(context) {

    private val selectedMuscles = previouslySelectedMuscles.toMutableSet()
    private val selectedEquipments = previouslySelectedEquipments.toMutableSet()
    private var isCardioSelected = previouslySelectedCardio

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_advanced_filter, null)
        setContentView(view)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        setupMuscleFilters(view)
        setupEquipmentFilters(view)
        setupOtherFilters(view)

        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        val applyButton = view.findViewById<Button>(R.id.applyButton)

        cancelButton.setOnClickListener {
            dismiss()
        }

        applyButton.setOnClickListener {
            applyFilters()
            dismiss()
        }
    }

    private fun setupMuscleFilters(view: android.view.View) {
        val muscleFilters = listOf(
            view.findViewById<TextView>(R.id.tvMuscleFilter1),
            view.findViewById<TextView>(R.id.tvMuscleFilter2),
            view.findViewById<TextView>(R.id.tvMuscleFilter3),
            view.findViewById<TextView>(R.id.tvMuscleFilter4),
            view.findViewById<TextView>(R.id.tvMuscleFilter5),
            view.findViewById<TextView>(R.id.tvMuscleFilter6),
            view.findViewById<TextView>(R.id.tvMuscleFilter7)
        )

        for (chip in muscleFilters) {
            val text = chip.text.toString()
            if (selectedMuscles.contains(text)) {
                chip.backgroundTintList = context.getColorStateList(R.color.des_cyan)
            }

            chip.setOnClickListener {
                if (selectedMuscles.contains(text)) {
                    selectedMuscles.remove(text)
                    chip.backgroundTintList = context.getColorStateList(R.color.light_grey)
                } else {
                    selectedMuscles.add(text)
                    chip.backgroundTintList = context.getColorStateList(R.color.des_cyan)
                }
            }
        }
    }


    private fun setupEquipmentFilters(view: android.view.View) {
        val equipmentFilters = listOf(
            view.findViewById<TextView>(R.id.tvEquipFilter1),
            view.findViewById<TextView>(R.id.tvEquipFilter2),
            view.findViewById<TextView>(R.id.tvEquipFilter3),
            view.findViewById<TextView>(R.id.tvEquipFilter4),
            view.findViewById<TextView>(R.id.tvEquipFilter5),
            view.findViewById<TextView>(R.id.tvEquipFilter6),
            view.findViewById<TextView>(R.id.tvEquipFilter7),
            view.findViewById<TextView>(R.id.tvEquipFilter8)
        )

        for (chip in equipmentFilters) {
            val text = chip.text.toString()

            // Pre-select and color if previously selected
            if (selectedEquipments.contains(text)) {
                chip.backgroundTintList = context.getColorStateList(R.color.des_cyan)
            }

            chip.setOnClickListener {
                if (selectedEquipments.contains(text)) {
                    selectedEquipments.remove(text)
                    chip.backgroundTintList = context.getColorStateList(R.color.light_grey)
                } else {
                    selectedEquipments.add(text)
                    chip.backgroundTintList = context.getColorStateList(R.color.des_cyan)
                }
            }
        }
    }


    private fun setupOtherFilters(view: android.view.View) {
        val cardioFilter = view.findViewById<TextView>(R.id.tvCardioFilter)

        // Pre-select and color if previously selected
        if (isCardioSelected) {
            cardioFilter.backgroundTintList = context.getColorStateList(R.color.des_cyan)
        }

        cardioFilter.setOnClickListener {
            isCardioSelected = !isCardioSelected
            if (isCardioSelected) {
                cardioFilter.backgroundTintList = context.getColorStateList(R.color.des_cyan)
            } else {
                cardioFilter.backgroundTintList = context.getColorStateList(R.color.light_grey)
            }
        }
    }


    private fun applyFilters() {
        val filtered = allExercises.filter { exercise ->
            val exerciseCategoryLower = exercise.category.lowercase(Locale.ROOT)
            val exerciseEquipmentLower = exercise.equipment?.lowercase(Locale.ROOT) ?: ""

            Log.d("AdvanceFilter", "exerciseCategoryLower: $exerciseCategoryLower  exerciseEquipmentLower: $exerciseEquipmentLower")

            val matchesMuscle = if (selectedMuscles.isEmpty()) {
                true
            } else {
                selectedMuscles.all { selectedMuscle ->
                    exerciseCategoryLower.contains(selectedMuscle.lowercase(Locale.ROOT))
                }
            }

            val matchesEquipment = if (selectedEquipments.isEmpty()) {
                true
            } else {
                selectedEquipments.all { selectedEquipment ->
                    exerciseEquipmentLower.contains(selectedEquipment.lowercase(Locale.ROOT))
                }
            }

            val matchesCardio = if (!isCardioSelected) {
                true
            } else {
                exerciseCategoryLower.contains("cardio")
            }


            // FINAL: must satisfy muscle AND equipment AND cardio conditions
            matchesMuscle && matchesEquipment && matchesCardio
        }

        Log.d("AdvanceFilter", "filtered: $filtered  selectedMuscles: $selectedMuscles  selectedEquipments: $selectedEquipments  isCardioSelected: $isCardioSelected")
        onFilterApplied(filtered, selectedMuscles, selectedEquipments, isCardioSelected)
    }

}