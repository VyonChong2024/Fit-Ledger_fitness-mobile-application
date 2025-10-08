package com.example.fyp_fitledger

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
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

    /*
    override fun onBackPressed() {
        if (viewPager.currentItem > 0) {
            // Get the current fragment
            val currentFragment: Fragment? = supportFragmentManager.findFragmentByTag("f" + viewPager.currentItem)

            if (currentFragment != null) {
                // Remove the current fragment
                supportFragmentManager.beginTransaction()
                    .remove(currentFragment)
                    .commit()
            }

            // Navigate to the previous fragment in the ViewPager
            viewPager.currentItem -= 1
        } else {
            // Go to landing page (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close DemographicActivity
            super.onBackPressed()
        }
    }
     */
}
