package com.example.fyp_fitledger

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexboxLayout

class TargetMuscleFragment : Fragment(){

    private lateinit var viewModel: WorkoutPlanViewModel
    private lateinit var btnNext: Button
    private lateinit var btnTurn: ImageButton
    //private lateinit var muscleContainer: FlexboxLayout

    private lateinit var muscleImageView: ImageView
    private lateinit var maskBitmap: Bitmap
    private lateinit var originalBitmap: Bitmap
    private lateinit var overlayBitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint
    private val selectedMuscles = mutableSetOf<Int>()

    private var isFrontDiplay: Boolean = true

    private val muscleMap = mapOf(
        Color.RED to "Arms",
        Color.BLUE to "Chest",
        Color.GREEN to "Abs",
        Color.YELLOW to "Legs",
        Color.MAGENTA to "Back",
        Color.CYAN to "Shoulders",
        Color.BLACK to "Glutes"
    )

    private val workoutPlanViewModel: WorkoutPlanViewModel by activityViewModels()

    private var progressDialog: AlertDialog? = null // Loading Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_target_muscle, container, false)
        viewModel = ViewModelProvider(requireActivity())[WorkoutPlanViewModel::class.java]

        btnNext = view.findViewById(R.id.btnNext)
        btnTurn = view.findViewById(R.id.btnTurn)
        muscleImageView = view.findViewById(R.id.muscleImageView)

        //muscleContainer = view.findViewById(R.id.selectedMuscleContainer)

        // Load main image and mask image
        originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.muscular_image_front)
        maskBitmap = BitmapFactory.decodeResource(resources, R.drawable.muscular_image_front_mask)

        // Create an overlay bitmap for drawing selections
        overlayBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(overlayBitmap)

        paint = Paint().apply {
            color = Color.GREEN // Color for selected muscles
            alpha = 150 // Semi-transparent
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }

        // Set touch listener
        muscleImageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt()
                val y = event.y.toInt()
                handleTouch(x, y)
            }
            true
        }

        btnTurn.setOnClickListener {
            if(isFrontDiplay) {
                muscleImageView.setImageResource(R.drawable.muscular_image_back)
                originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.muscular_image_back)
                maskBitmap = BitmapFactory.decodeResource(resources, R.drawable.muscular_image_back_mask)
                isFrontDiplay = false

                showLoadingDialog(requireContext())
                highlightMuscles()
            }
            else {
                muscleImageView.setImageResource(R.drawable.muscular_image_front)
                originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.muscular_image_front)
                maskBitmap = BitmapFactory.decodeResource(resources, R.drawable.muscular_image_front_mask)
                isFrontDiplay = true

                showLoadingDialog(requireContext())
                highlightMuscles()
            }

        }

        btnNext.setOnClickListener {
            val selectedMuscleNames = selectedMuscles.mapNotNull { muscleMap[it] }
            workoutPlanViewModel.updateTargetMuscles(selectedMuscleNames)

            (activity as? DemographicActivity)?.addFragment("WorkoutDayFragment")
            (activity as? DemographicActivity)?.nextPage()
        }

        return view
    }

    private fun handleTouch(x: Int, y: Int) {
        // Get ImageView's actual displayed width and height
        val imageViewWidth = muscleImageView.width.toDouble()
        val imageViewHeight = muscleImageView.height.toDouble()
        Log.d("TargetMuscleFragment", "ImageView, Width: $imageViewWidth    Height: $imageViewHeight")
        // Get the original bitmap width and height
        val bitmapWidth = maskBitmap.width.toDouble()
        val bitmapHeight = maskBitmap.height.toDouble()
        Log.d("TargetMuscleFragment", "Bitmap, Width: $bitmapWidth    Height: $bitmapHeight")
        Log.d("TargetMuscleFragment", "x: $x    y: $y")

        // Convert touch coordinates to match the bitmap's coordinates
        val x = (x.toDouble() / imageViewWidth * bitmapWidth).toInt()
        val y = (y.toDouble() / imageViewHeight * bitmapHeight).toInt()

        Log.d("TargetMuscleFragment", "x: $x    y: $y")
        val searchRadius = 5 // Expand search area slightly
        var detectedColor: Int? = null
        var closestDistance = Int.MAX_VALUE

        if (x in 0 until maskBitmap.width && y in 0 until maskBitmap.height) {
            // Search in a circular pattern (center first, then expand)
            for (radius in 0..searchRadius) {
                for (dx in -radius..radius) {
                    for (dy in -radius..radius) {
                        val newX = x + dx
                        val newY = y + dy

                        if (newX in 0 until maskBitmap.width && newY in 0 until maskBitmap.height) {
                            val pixelColor = maskBitmap.getPixel(newX, newY)

                            if (muscleMap.containsKey(pixelColor)) {
                                // Calculate distance from original click
                                val distance = dx * dx + dy * dy
                                if (distance < closestDistance) {
                                    detectedColor = pixelColor
                                    closestDistance = distance
                                }
                            }
                        }
                    }
                }
                if (detectedColor != null) break // Stop as soon as we find the closest valid muscle
            }

            if (selectedMuscles.isNotEmpty()) {
                btnNext.isEnabled = true
            } else {
                btnNext.isEnabled = false
            }

            detectedColor?.let { muscleColor ->
                val muscleName = muscleMap[muscleColor] ?: return
                Log.d("TargetMuscleFragment", "Selected Muscle: $muscleName")

                if (selectedMuscles.contains(muscleColor)) {
                    selectedMuscles.remove(muscleColor)
                } else {
                    selectedMuscles.add(muscleColor)
                }
                showLoadingDialog(requireContext())

                highlightMuscles()
                updateSelectedMuscleDisplay()
            }
        }
    }


    private fun highlightMuscles() {
        // Create a new overlay for each update
        val combinedBitmap = Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, Bitmap.Config.ARGB_8888)
        val combinedCanvas = Canvas(combinedBitmap)

        // Draw the original muscle image first
        combinedCanvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // Paint setup for semi-transparent overlay
        val paint = Paint().apply {
            color = Color.GREEN // Base highlight color
            alpha = 120 // Adjust transparency (0 = fully transparent, 255 = fully opaque)
            style = Paint.Style.FILL
        }

        // Loop through selected muscles and highlight them
        for (muscleColor in selectedMuscles) {
            for (row in 0 until maskBitmap.width) {
                for (col in 0 until maskBitmap.height) {
                    if (maskBitmap.getPixel(row, col) == muscleColor) {
                        combinedCanvas.drawPoint(row.toFloat(), col.toFloat(), paint)
                    }
                }
            }
        }
        // Update the ImageView
        muscleImageView.setImageBitmap(combinedBitmap)
        dismissLoadingDialog()
    }

    private fun updateSelectedMuscleDisplay() {
        val muscleContainer: FlexboxLayout =
            requireView().findViewById(R.id.selectedMuscleContainer)
        muscleContainer.removeAllViews() // Clear previous tags

        for (muscleName in selectedMuscles.mapNotNull { muscleMap[it] }) {
            val textView = TextView(requireContext()).apply {
                text = muscleName
                setPadding(16, 8, 16, 8)
                setBackgroundResource(R.drawable.muscle_tag_background) // Your custom background
                setTextColor(Color.WHITE)
                textSize = 14f
            }

            // Add some margin dynamically
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 8, 8, 8)  // Add margin between items

            muscleContainer.addView(textView, params)
        }
    }

    private fun showLoadingDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_loading, null)

        Log.d("TargetMuscleFragment", "Loading Dialog")

        builder.setView(dialogView)
        builder.setCancelable(false) // Prevent manual dismissal

        progressDialog = builder.create()

        // Show the dialog first to get the window reference
        progressDialog?.show()

        // Force the dialog size to match content
        progressDialog?.window?.setLayout(200, 200) // Adjust size as needed
        progressDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Removes default margins
    }

    private fun dismissLoadingDialog() {
        Log.d("TargetMuscleFragment", "Finish Loading Dialog")
        progressDialog?.dismiss()
    }
}
