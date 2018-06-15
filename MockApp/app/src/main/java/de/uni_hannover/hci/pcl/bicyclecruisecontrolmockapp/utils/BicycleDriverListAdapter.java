package de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.utils;

import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.MainActivity;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.R;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.models.BicycleDriver;

/**
 * Costume adapter for emergency contacts
 */
public class BicycleDriverListAdapter extends RecyclerView.Adapter<BicycleDriverListAdapter.ViewHolder> {
    private ArrayList<BicycleDriver> mDataset;
    private MainActivity mainActivity;

    private AlertDialog alertDialog;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextViewName;
        public TextView mTextViewID;
        public TextView mTextViewSpeed;
        public TextView mTextViewHeartRate;
        public ViewHolder(View v) {
            super(v);
            mTextViewName = v.findViewById(R.id.textViewName);
            mTextViewID = v.findViewById(R.id.textViewId);
            mTextViewSpeed = v.findViewById(R.id.textViewSpeed);
            mTextViewHeartRate = v.findViewById(R.id.textViewIdHeartRate);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public BicycleDriverListAdapter(ArrayList<BicycleDriver> dataset, MainActivity mainActivity) {
        mDataset = dataset;
        this.mainActivity = mainActivity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BicycleDriverListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View listItem = (View) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bicycledriver_list_item, parent, false);;
        ViewHolder vh = new ViewHolder(listItem);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        //PlaceHolder
        //holder.mTextView.setText("My Name is Jeff");
        holder.mTextViewName.setText(mDataset.get(position).getName());
        holder.mTextViewID.setText(String.format("%d", mDataset.get(position).getGroupId()));
        holder.mTextViewSpeed.setText(String.format("%d Kmh", mDataset.get(position).getSpeed()));
        holder.mTextViewHeartRate.setText(String.format("%d bpm", mDataset.get(position).getHeartRate()));
        boolean heartRateTrigger = false; //TODO
        if (heartRateTrigger){
            holder.mTextViewName.setTextColor(Color.RED);
        }
    }


    private void onInformationUpdate(long speed, long heartRate){
        //TODO notwendig?
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }


}
