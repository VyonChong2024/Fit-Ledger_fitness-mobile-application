package com.example.fyp_fitledger

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView

class DietPlanTypeFragment : Fragment() {
    private val cardCount = 10
    private val cardContainers = mutableListOf<FrameLayout>()
    private val frontCards = mutableListOf<CardView>()
    private val backCards = mutableListOf<CardView>()
    private val frontTexts = mutableListOf<TextView>()
    private val backTexts = mutableListOf<TextView>()
    private val isFrontVisible = MutableList(cardCount) { true } // Track visibility of each card

    private var selectedCards: String? = null // Store front text of selected cards

    private lateinit var btnNext: Button
    private lateinit var viewModel: WorkoutPlanViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_diet_plan_type, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(WorkoutPlanViewModel::class.java)

        btnNext = view.findViewById(R.id.btnNext)

        for (i in 1..cardCount) {
            val containerId = resources.getIdentifier("cardContainer$i","id","com.example.fyp_fitledger")
            val frontCardId = resources.getIdentifier("frontCard$i", "id", "com.example.fyp_fitledger")
            val backCardId = resources.getIdentifier("backCard$i", "id", "com.example.fyp_fitledger")
            val frontTextId = resources.getIdentifier("frontText$i", "id", "com.example.fyp_fitledger")
            val backTextId = resources.getIdentifier("backText$i", "id", "com.example.fyp_fitledger")

            cardContainers.add(view.findViewById(containerId))
            frontCards.add(view.findViewById(frontCardId))
            backCards.add(view.findViewById(backCardId))
            frontTexts.add(view.findViewById(frontTextId))
            backTexts.add(view.findViewById(backTextId))

            val index = i - 1 // Adjust index for list access
            cardContainers[index].setOnClickListener {
                flipCard(index)
            }
        }

        btnNext.setOnClickListener{
            viewModel.updateDietPlan(selectedCards!!)
            (activity as? DemographicActivity)?.addFragment("DietNutrientPlanFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }

    private fun flipCard(cardIndex: Int) {
        // Flip all other cards back to the front
        for (i in 0 until cardCount) {
            if (i != cardIndex && !isFrontVisible[i]) {
                flipToFront(i)
            }
        }

        val fromView = if (isFrontVisible[cardIndex]) frontCards[cardIndex] else backCards[cardIndex]
        val toView = if (isFrontVisible[cardIndex]) backCards[cardIndex] else frontCards[cardIndex]
        val currentFrontText = frontTexts[cardIndex].text.toString()

        val rotateOut = ObjectAnimator.ofFloat(fromView, "rotationY", 0f, 20f).apply {
            duration = 200
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fromView.visibility = View.GONE
                    toView.visibility = View.VISIBLE
                    val rotateIn = ObjectAnimator.ofFloat(toView, "rotationY", -20f, 0f).apply {
                        duration = 200
                    }
                    rotateIn.start()
                    isFrontVisible[cardIndex] = !isFrontVisible[cardIndex]

                    // Store/remove selected state
                    if (!isFrontVisible[cardIndex]) { // Flipped to back
                        selectedCards = currentFrontText
                        println("Selected Cards: $selectedCards") // For debugging
                    } else { // Flipped back to front
                        selectedCards = null
                        println("Selected Cards: $selectedCards") // For debugging
                    }
                    updateButton()
                }
            })
        }
        rotateOut.start()
    }

    private fun flipToFront(cardIndex: Int) {
        if (!isFrontVisible[cardIndex]) {
            val fromView = backCards[cardIndex]
            val toView = frontCards[cardIndex]

            val rotateOut = ObjectAnimator.ofFloat(fromView, "rotationY", 0f, 20f).apply {
                duration = 200
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        fromView.visibility = View.GONE
                        toView.visibility = View.VISIBLE
                        val rotateIn = ObjectAnimator.ofFloat(toView, "rotationY", -20f, 0f).apply {
                            duration = 200
                        }
                        rotateIn.start()
                        isFrontVisible[cardIndex] = true
                        selectedCards = null // Remove if flipped back
                        println("Selected Cards: $selectedCards") // For debugging
                    }
                })
            }
            rotateOut.start()
            updateButton()
        }
    }

    private fun updateButton() {
        if (selectedCards == null)
            btnNext.isEnabled = false
        else
            btnNext.isEnabled = true
    }
}