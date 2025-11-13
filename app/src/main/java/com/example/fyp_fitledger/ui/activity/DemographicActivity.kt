package com.example.fyp_fitledger.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.fyp_fitledger.R
import com.example.fyp_fitledger.data.viewmodel.UserViewModel
import com.example.fyp_fitledger.ui.adapter.DemographicPagerAdapter
import com.example.fyp_fitledger.ui.fragment.AccurateMeasurementFragment
import com.example.fyp_fitledger.ui.fragment.ActivityLevelFrequencyFragment
import com.example.fyp_fitledger.ui.fragment.AgeFragment
import com.example.fyp_fitledger.ui.fragment.BodyFatResultFragment
import com.example.fyp_fitledger.ui.fragment.BodyMetricsFragment
import com.example.fyp_fitledger.ui.fragment.CustomizeWorkoutFragment
import com.example.fyp_fitledger.ui.fragment.DietNutrientPlanFragment
import com.example.fyp_fitledger.ui.fragment.DietPlanFragment
import com.example.fyp_fitledger.ui.fragment.DietPlanTypeFragment
import com.example.fyp_fitledger.ui.fragment.FatCaliperFragment
import com.example.fyp_fitledger.ui.fragment.FatCaliperInputFragment
import com.example.fyp_fitledger.ui.fragment.FinishSetupFragment
import com.example.fyp_fitledger.ui.fragment.FitnessGoalFragment
import com.example.fyp_fitledger.ui.fragment.GenderFragment
import com.example.fyp_fitledger.ui.fragment.HeightWeightFragment
import com.example.fyp_fitledger.ui.fragment.MethodSelectionFragment
import com.example.fyp_fitledger.ui.fragment.TapeMeasureFragment
import com.example.fyp_fitledger.ui.fragment.TapeMeasureInputFragment
import com.example.fyp_fitledger.ui.fragment.TargetBodyFatFragment
import com.example.fyp_fitledger.ui.fragment.TargetMuscleFragment
import com.example.fyp_fitledger.ui.fragment.TargetWeightFragment
import com.example.fyp_fitledger.ui.fragment.WorkoutDayFragment
import com.example.fyp_fitledger.ui.fragment.WorkoutPlanFragment
import com.example.fyp_fitledger.ui.fragment.WorkoutTimeFragment
import java.lang.reflect.Field

class DemographicActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: DemographicPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FYP_FitLedger)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demographic)

        viewPager = findViewById(R.id.viewPager)
        viewPagerAdapter = DemographicPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        //save the user id
        val userId = intent.getStringExtra("USER_ID")
        val userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        userViewModel.updateUserID(userId ?: "")

        disableViewPager2Swipe(viewPager)
        //createFragments()     //trying for debug
        saveAllFragmentsAvailable()
        createFirstFragment()
    }

    fun removeFragmentandBehind(n: Int) {
        viewPagerAdapter.removeFragmentAndBehind(n)
    }

    fun removeFragment(n: Int) {
        viewPagerAdapter.removeFragmentAndBehind(n)
    }

    fun checkFragment(name: String): Boolean {
        return viewPagerAdapter.findFragment(name)
    }

    fun checkFragment(n: Int): Boolean {
        return viewPagerAdapter.findFragment(n)
    }

    fun addFragment(name: String) {
        viewPagerAdapter.addFragment(name)
    }

    fun removeCurrentFragment() {
        viewPagerAdapter.removeLastFragment()
    }


    fun createFirstFragment(){
        viewPagerAdapter.fragments.clear()
        viewPagerAdapter.addFragment(GenderFragment())
    }


    fun saveAllFragmentsAvailable() {
        viewPagerAdapter.fragmentAvailable.clear()
        viewPagerAdapter.addAvailableFragment(GenderFragment())
        viewPagerAdapter.addAvailableFragment(AgeFragment())
        viewPagerAdapter.addAvailableFragment(HeightWeightFragment())
        viewPagerAdapter.addAvailableFragment(BodyMetricsFragment())
        viewPagerAdapter.addAvailableFragment(AccurateMeasurementFragment())
        viewPagerAdapter.addAvailableFragment(MethodSelectionFragment())
        viewPagerAdapter.addAvailableFragment(TapeMeasureFragment())
        viewPagerAdapter.addAvailableFragment(FatCaliperFragment())
        viewPagerAdapter.addAvailableFragment(TapeMeasureInputFragment())
        viewPagerAdapter.addAvailableFragment(FatCaliperInputFragment())
        viewPagerAdapter.addAvailableFragment(BodyFatResultFragment())
        viewPagerAdapter.addAvailableFragment(CustomizeWorkoutFragment())
        viewPagerAdapter.addAvailableFragment(FitnessGoalFragment())
        viewPagerAdapter.addAvailableFragment(TargetWeightFragment())
        viewPagerAdapter.addAvailableFragment(TargetBodyFatFragment())
        viewPagerAdapter.addAvailableFragment(ActivityLevelFrequencyFragment())
        viewPagerAdapter.addAvailableFragment(TargetMuscleFragment())
        viewPagerAdapter.addAvailableFragment(WorkoutDayFragment())
        viewPagerAdapter.addAvailableFragment(WorkoutTimeFragment())
        viewPagerAdapter.addAvailableFragment(WorkoutPlanFragment())
        viewPagerAdapter.addAvailableFragment(DietPlanFragment())
        viewPagerAdapter.addAvailableFragment(DietPlanTypeFragment())
        viewPagerAdapter.addAvailableFragment(DietNutrientPlanFragment())
        viewPagerAdapter.addAvailableFragment(FinishSetupFragment())
    }


    fun nextPage() {
        if (viewPager.currentItem < viewPagerAdapter.itemCount - 1) {
            viewPager.currentItem += 1
        }
    }

    //Display page swipe left and right
    fun disableViewPager2Swipe(viewPager: ViewPager2) {
        try {
            val recyclerViewField: Field = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(viewPager) as RecyclerView
            viewPager.isUserInputEnabled = false
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) { // onSaveInstanceState inside the Activity
        super.onSaveInstanceState(outState)
        outState.putInt("currentPage", viewPager.currentItem)
    }

    override fun onBackPressed() {
        if (viewPager.currentItem > 0) {
            removeCurrentFragment()
            viewPager.currentItem -= 1
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close DemographicActivity
            super.onBackPressed()
        }
    }
}
