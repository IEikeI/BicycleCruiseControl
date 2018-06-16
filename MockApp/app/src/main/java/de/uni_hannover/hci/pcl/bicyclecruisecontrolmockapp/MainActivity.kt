package de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp

import android.content.Intent
import android.net.Uri
import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.fragments.BLEManageFragment
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.fragments.BicycleEmulatedFragment
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.fragments.SensorInputFragment
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.models.BicycleDriverGroup
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.settings.SettingsActivity

class MainActivity : AppCompatActivity(), BLEManageFragment.OnBLEManageFragmentInteractionListener, SensorInputFragment.OnSensorInputFragmentInteractionListener, BicycleEmulatedFragment.OnBicycleEmulatedFragmentInteractionListener {

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    private val kActivityRequestCode_EnableBluetooth = 1
    private val kActivityRequestCode_Settings = 2
    private val kActivityRequestCode_ConnectedActivity = 3

    /**
     * The [ViewPager] that will host the section contents.
     */
    private var mViewPager: ViewPager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById<View>(R.id.container) as ViewPager
        mViewPager!!.adapter = mSectionsPagerAdapter

        val tabLayout = findViewById<View>(R.id.tabs) as TabLayout

        mViewPager!!.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(mViewPager))

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            return true
        } else super.onOptionsItemSelected(item)

        //noinspection SimplifiableIfStatement
       if (id == R.id.action_help) {
            startHelp()
            return true
        } else if (id == R.id.action_settings) {
            val intent = Intent()
            intent.setClass(this@MainActivity, SettingsActivity::class.java!!)
            startActivityForResult(intent, kActivityRequestCode_Settings)
            return true
        } else if (id == R.id.action_licenses) {
            val intent = Intent(this, CommonHelpActivity::class.java)
            intent.putExtra("title", getString(R.string.licenses_title))
            intent.putExtra("help", "licenses.html")
            startActivity(intent)
           return true
        }
        return true
    }


    private fun startHelp() {
        // Launch app help activity
        val intent = Intent(this, MainHelpActivity::class.java)
        startActivity(intent)
    }

    override fun onFragmentInteraction(uri: Uri) {
        //TODO: Implement auto generated code stub
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            return inflater!!.inflate(R.layout.fragment_main, container, false)
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            when (position) {
                0 -> return BLEManageFragment.newInstance("", "")
                1 -> return BicycleEmulatedFragment.newInstance("", "")
                2 -> return SensorInputFragment.newInstance("", "")
            }
            return PlaceholderFragment.newInstance(position + 1)
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "BLE Manager"
                1 -> return "Bicycle Monitor"
                2 -> return "TODO"
            }
            return null
        }
    }
}
