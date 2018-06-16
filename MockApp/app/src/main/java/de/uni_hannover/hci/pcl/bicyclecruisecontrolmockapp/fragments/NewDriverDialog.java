package de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.fragments;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

//import com.google.gson.Gson;

import java.util.ArrayList;

import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.MainActivity;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.R;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.models.BicycleDriver;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.models.BicycleDriverGroup;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.utils.BicycleDriverListAdapter;

public class NewDriverDialog extends DialogFragment {

    private BicycleDriverGroup bicycleDriverGroup;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView mRecyclerView;

    public NewDriverDialog(){
        // Required empty public constructor
    }

    public static NewDriverDialog newInstance(BicycleDriverGroup bicycleDriverGroup, RecyclerView.Adapter mAdapter,
                                              RecyclerView mRecyclerVie) {
        NewDriverDialog nDD = new NewDriverDialog();

        Bundle args = new Bundle();
        nDD.setArguments(args);
        nDD.setComplexVariable(bicycleDriverGroup, mAdapter, mRecyclerVie);

        //First Try - ood
        /*Intent activity = new Intent(mainActivity, NewDriverDialog.class);
        activity.putExtra("bicycleDriverGroup", new Gson().toJson(bicycleDriverGroup));
        startActivity(activity);

        Intent intent = new Intent(mainActivity, NewDriverDialog.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("bDG", bicycleDriverGroup);
        intent.putExtras(bundle);*/

        return nDD;
    }

    /**
     * Helper for setting complex Objects to this DialogFragment
     * @param bicycleDriverGroup
     * @param mAdapter
     * @param mRecyclerVie
     */
    public void setComplexVariable(BicycleDriverGroup bicycleDriverGroup, RecyclerView.Adapter mAdapter, RecyclerView mRecyclerVie) {
        this.bicycleDriverGroup = bicycleDriverGroup;
        this.mAdapter = mAdapter;
        this.mRecyclerView = mRecyclerVie;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        /*String jsonMyObject = "bicycleDriverGroup";
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            jsonMyObject = extras.getString("myObject");
        }
        bicycleDriverGroup = new Gson().fromJson(jsonMyObject, BicycleDriverGroup.class);*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View newDriverView = inflater.inflate(R.layout.add_driver_dialog, container, false);;

        EditText editTextName = (EditText) newDriverView.findViewById(R.id.editTextViewName);
        EditText editTextId = (EditText) newDriverView.findViewById(R.id.editTextViewId);
        EditText editTextSpeed = (EditText) newDriverView.findViewById(R.id.editTextViewSpeed);
        EditText editTextHeartRate = (EditText) newDriverView.findViewById(R.id.editTextViewIdHeartRate);

        final int id = bicycleDriverGroup.getBicycleDrivers().size() + 1;
        final int groupId = Integer.valueOf(editTextId.getText().toString());
        final String name = editTextName.getText().toString();
        final long speed = Long.valueOf(editTextSpeed.getText().toString());
        final long heartRate = Long.valueOf(editTextHeartRate.getText().toString());

        Button subButton = (Button) newDriverView.findViewById(R.id.driverSubmitButton);
        subButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubtmit(groupId, name, id, speed, heartRate);
            }
        });

        return newDriverView;
    }

    /**
     * Add just one new driver an close the dialog and updates the UI
     * @param groupId
     * @param name
     * @param id
     * @param speed
     * @param heartrate
     */
    private void onSubtmit(int groupId, String name, int id, long speed, long heartrate) {
        BicycleDriver bicycleDriver = new BicycleDriver(groupId, name, id,  speed, heartrate);
        bicycleDriverGroup.addToDriverGroup(bicycleDriver);

        //since ArrayList has no listener, force refresh stuff
        ArrayList<BicycleDriver> dataset;

        if(bicycleDriverGroup != null && bicycleDriverGroup.getBicycleDrivers().size() < 1){
            dataset = new ArrayList<BicycleDriver>();
        } else {
            dataset = bicycleDriverGroup.getBicycleDrivers();
        }

        mAdapter = new BicycleDriverListAdapter(dataset, (MainActivity) getActivity());
        mRecyclerView.setAdapter(mAdapter);
        dismiss();
    }


    /**
     * Clearing all allocated Objects here
     * @param dialog
     */
    @Override
    public void onDismiss (DialogInterface dialog){
        super.onDismiss(dialog);
        bicycleDriverGroup = null;
    }

    /**
     * Helper for getting a single child of ListView by Index
     * @param pos
     * @param listView
     * @return
     */
    private View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }
}

