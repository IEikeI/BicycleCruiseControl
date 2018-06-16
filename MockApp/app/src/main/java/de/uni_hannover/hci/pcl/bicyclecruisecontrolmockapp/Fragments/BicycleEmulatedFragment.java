package de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;

import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.MainActivity;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.R;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.bluetoothconnection.UartActivity;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.models.BicycleDriver;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.models.BicycleDriverGroup;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.utils.BicycleDriverListAdapter;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnBicycleEmulatedFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BicycleEmulatedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BicycleEmulatedFragment extends Fragment {
    private static final String TAG = "BicycleEmulatedFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;


    private BicycleDriverGroup bicycleDriverGroup;

    private OnBicycleEmulatedFragmentInteractionListener mListener;

    /**
     * This fragment is the controller for the (GUI) for the simulated bicycle.
     * It shows all the information on the current speed, heart-rate and other
     * useful informations for the mockup.
     */
    public BicycleEmulatedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EmergencyContactsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BicycleEmulatedFragment newInstance(String param1, String param2) {
        BicycleEmulatedFragment fragment = new BicycleEmulatedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View emulatedView = inflater.inflate(R.layout.fragment_bicycle_emulated, container, false);

        mRecyclerView = (RecyclerView) emulatedView.findViewById(R.id.recyclerViewContacts);
        mRecyclerView.setHasFixedSize(true);

        //mLayoutManager = new LinearLayoutManager(this);
        //mRecyclerView.setLayoutManager(mLayoutManager);

        int groupId = 1;
        bicycleDriverGroup = new BicycleDriverGroup(groupId);
        bicycleDriverGroup.addToDriverGroup(new BicycleDriver(groupId,"Jeff", 1, 80L, 70L));
        bicycleDriverGroup.addToDriverGroup(new BicycleDriver(groupId,"Karl", 2, 81L, 90L));
        bicycleDriverGroup.addToDriverGroup(new BicycleDriver(groupId,"Greta", 3, 85L, 110L));
        bicycleDriverGroup.addToDriverGroup(new BicycleDriver(groupId,"Max", 4, 70L, 75L));
        ArrayList<BicycleDriver> dataset;

        if(bicycleDriverGroup != null && bicycleDriverGroup.getBicycleDrivers().size() < 1){
            dataset = new ArrayList<BicycleDriver>();
        } else {
            dataset = bicycleDriverGroup.getBicycleDrivers();
        }

        mAdapter = new BicycleDriverListAdapter(dataset, (MainActivity) getActivity());
        mRecyclerView.setAdapter(mAdapter);

        Button addDriverButton = (Button) emulatedView.findViewById(R.id.buttonAddNewDriver);
        addDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //NewDriverDialog nDD = NewDriverDialog.newInstance(bicycleDriverGroup, (MainActivity) getActivity());
                 //      nDD.show(getActivity().getFragmentManager(),"newDriverDialog");
                //Intent intent = new Intent(getActivity(), NewDriverDialog.class);
                //Bundle bundle = new Bundle();
                //bundle.putSerializable("bDG", bicycleDriverGroup);
                //intent.putExtras(bundle);

                NewDriverDialog nDD = NewDriverDialog.newInstance(bicycleDriverGroup, mAdapter, mRecyclerView);
                nDD.show(getActivity().getFragmentManager(),"newDriverDialog");
            }
        });

        Button startSimButton = (Button) emulatedView.findViewById(R.id.startSendingDriverData);
        startSimButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewPager vp =(ViewPager) getActivity().findViewById(R.id.container);
                BLEManageFragment bleFragment = (BLEManageFragment) getActivity().getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + 0);
                bleFragment.setBicycleDriverGroup(bicycleDriverGroup);
                Log.d(TAG, "Fragment should switch");
                //((MainActivity) getActivity()).getActionBar().setSelectedNavigationItem(0); //deprecated
                vp.setCurrentItem(0);
            }
        });

        return emulatedView;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnBicycleEmulatedFragmentInteractionListener) {
            mListener = (OnBicycleEmulatedFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnBicycleEmulatedFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
