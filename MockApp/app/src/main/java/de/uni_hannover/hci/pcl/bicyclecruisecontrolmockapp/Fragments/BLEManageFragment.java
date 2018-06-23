/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.fragments;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.bluetoothconnection.UartActivity;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.bluetoothconnection.UartInterfaceActivity;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.InfoActivity;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.R;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.blemanagement.BeaconActivity;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.blemanagement.BleDevicesScanner;
import android.bluetooth.BluetoothDevice;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.blemanagement.BleManager;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.blemanagement.BleUtils;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.models.BicycleDriverGroup;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.ui.utils.DialogUtils;
import de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.ui.utils.ExpandableHeightExpandableListView;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.MODE_PRIVATE;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BLEManageFragment extends Fragment implements BleManager.BleManagerListener, BleUtils.ResetBluetoothAdapterListener {

    private static final String TAG = "BLEManageFragment";

    private final static long kMinDelayToUpdateUI = 200;    // in milliseconds
    private static final String kGenericAttributeService = "00001801-0000-1000-8000-00805F9B34FB";
    private static final String kServiceChangedCharacteristic = "00002A05-0000-1000-8000-00805F9B34FB";

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private final static String kPreferences = "MainActivity_prefs";
    private final static String kPreferences_filtersPanelOpen = "filtersPanelOpen";

    // Components
    private final static int kComponentsNameIds[] = {
            //we only need UART
            //R.string.scan_connectservice_info,
            R.string.scan_connectservice_uart
            //R.string.scan_connectservice_pinio,
            //R.string.scan_connectservice_controller,
            //R.string.scan_connectservice_beacon,
            //R.string.scan_connectservice_neopixel,
    };

    // Activity request codes (used for onActivityResult)
    private static final int kActivityRequestCode_EnableBluetooth = 1;
    private static final int kActivityRequestCode_Settings = 2;
    private static final int kActivityRequestCode_ConnectedActivity = 3;

    // UI
    private ExpandableHeightExpandableListView mScannedDevicesListView;
    private ExpandableListAdapter mScannedDevicesAdapter;
    private Button mScanButton;
    private long mLastUpdateMillis;
    private TextView mNoDevicesTextView;
    private ScrollView mDevicesScrollView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private AlertDialog mConnectingDialog;
    private View mFiltersPanelView;
    private ImageView mFiltersExpandImageView;
    private ImageButton mFiltersClearButton;
    private TextView mFiltersTitleTextView;
    private EditText mFiltersNameEditText;
    private SeekBar mFiltersRssiSeekBar;
    private TextView mFiltersRssiValueTextView;
    private CheckBox mFiltersUnnamedCheckBox;
    private CheckBox mFiltersUartCheckBox;

    // Data
    private BleManager mBleManager;
    private boolean mIsScanPaused = true;
    private BleDevicesScanner mScanner;
    private PeripheralList mPeripheralList;

    private ArrayList<BluetoothDeviceData> mScannedDevices;
    private BluetoothDeviceData mSelectedDeviceData;
    private Class<?> mComponentToStartWhenConnected;
    private boolean mShouldEnableWifiOnQuit = false;
    private String mLatestCheckedDeviceAddress;

    private DataFragment mRetainedDataFragment;

    private OnBLEManageFragmentInteractionListener mListener;

    // TODO: Rename and change types of parameters
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private BicycleDriverGroup bicycleDriverGroup;
    private String mParam1;
    private String mParam2;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private TextView mtextHeader;

    public BicycleDriverGroup getBicycleDriverGroup() {
        return bicycleDriverGroup;
    }

    public void setBicycleDriverGroup(BicycleDriverGroup bicycleDriverGroup) {
        this.bicycleDriverGroup = bicycleDriverGroup;
    }

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the 'conversation' thread (two devices sending and receiving data)
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;



    /**
     * Name of the device the app is running on
     */
    private String deviceName;

    /**
     * This fragtment is the controller for the (GUI) BLE device manager.
     * From here you can connect all devices, see the available and send
     * "pings"
     */
    public BLEManageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StatisticsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BLEManageFragment newInstance(String param1, String param2) {
        BLEManageFragment fragment = new BLEManageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        /*mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }*/
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        //get the device name
        //deviceName = android.os.Build.MODEL;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View bleManagerView = inflater.inflate(R.layout.fragment_ble_manager, container, false);

        Button buttonTest = (Button) bleManagerView.findViewById(R.id.buttonUARTTest);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(getActivity(), UartActivity.class);
                startActivity(myIntent);
            }
        });

        // Init variables
        mBleManager = BleManager.getInstance(getActivity());
        restoreRetainedDataFragment();
        mPeripheralList = new PeripheralList();

        // UI
        mScannedDevicesListView = (ExpandableHeightExpandableListView) bleManagerView.findViewById(R.id.scannedDevicesListView);
        mScannedDevicesAdapter = new ExpandableListAdapter();
        mScannedDevicesListView.setAdapter(mScannedDevicesAdapter);
        mScannedDevicesListView.setExpanded(true);

        LinearLayout linearLayoutExpandView = (LinearLayout) bleManagerView.findViewById(R.id.linearLayoutExpandFilters);
        linearLayoutExpandView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickExpandFilters(v);
            }
        });

        mScannedDevicesListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
            }
        });

        mScanButton = (Button) bleManagerView.findViewById(R.id.scanButton);

        mNoDevicesTextView = (TextView) bleManagerView.findViewById(R.id.nodevicesTextView);
        mDevicesScrollView = (ScrollView) bleManagerView.findViewById(R.id.devicesScrollView);
        mDevicesScrollView.setVisibility(View.GONE);

        mSwipeRefreshLayout = (SwipeRefreshLayout) bleManagerView.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mScannedDevices.clear();
                startScan(null);

                mSwipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 500);
            }
        });


        mFiltersPanelView = bleManagerView.findViewById(R.id.filtersExpansionView);
        mFiltersExpandImageView = (ImageView) bleManagerView.findViewById(R.id.filtersExpandImageView);
        mFiltersClearButton = (ImageButton) bleManagerView.findViewById(R.id.filtersClearButton);
        mFiltersTitleTextView = (TextView) bleManagerView.findViewById(R.id.filtersTitleTextView);
        mFiltersNameEditText = (EditText) bleManagerView.findViewById(R.id.filtersNameEditText);
        mFiltersNameEditText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                String text = s.toString();
                mPeripheralList.setFilterName(text);
                updateFilters();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        mFiltersRssiSeekBar = (SeekBar) bleManagerView.findViewById(R.id.filtersRssiSeekBar);
        mFiltersRssiSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int rssiValue = -seekBar.getProgress();
                mPeripheralList.setFilterRssiValue(rssiValue);
                updateRssiValue();
                updateFilters();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mFiltersRssiValueTextView = (TextView) bleManagerView.findViewById(R.id.filtersRssiValueTextView);
        mFiltersUnnamedCheckBox = (CheckBox) bleManagerView.findViewById(R.id.filtersUnnamedCheckBox);
        mFiltersUnnamedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPeripheralList.setFilterUnnamedEnabled(isChecked);
                updateFilters();
            }
        });
        mFiltersUartCheckBox = (CheckBox) bleManagerView.findViewById(R.id.filtersUartCheckBox);
        mFiltersUartCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPeripheralList.setFilterOnlyUartEnabled(isChecked);
                updateFilters();
            }
        });

        // Filters
        SharedPreferences preferences = getActivity().getSharedPreferences(kPreferences, MODE_PRIVATE);
        boolean filtersIsPanelOpen = preferences.getBoolean(kPreferences_filtersPanelOpen, false);
        openFiltersPanel(filtersIsPanelOpen, false);
        updateFiltersTitle();
        mFiltersNameEditText.setText(mPeripheralList.getFilterName());
        setRssiSliderValue(mPeripheralList.getFilterRssiValue());
        mFiltersUnnamedCheckBox.setChecked(mPeripheralList.isFilterUnnamedEnabled());
        mFiltersUartCheckBox.setChecked(mPeripheralList.isFilterOnlyUartEnabled());

        // Setup when activity is created for the first time
        if (savedInstanceState == null) {
            // Read preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean autoResetBluetoothOnStart = sharedPreferences.getBoolean("pref_resetble", false);
            boolean disableWifi = sharedPreferences.getBoolean("pref_disableWifi", false);
            boolean updatesEnabled = sharedPreferences.getBoolean("pref_updatesenabled", true);

            // Turn off wifi
            if (disableWifi) {
                final boolean isWifiEnabled = BleUtils.isWifiEnabled(getActivity());
                if (isWifiEnabled) {
                    BleUtils.enableWifi(false, getActivity());
                    mShouldEnableWifiOnQuit = true;
                }
            }

            // Check if bluetooth adapter is available
            final boolean wasBluetoothEnabled = manageBluetoothAvailability();
            final boolean areLocationServicesReadyForScanning = manageLocationServiceAvailabilityForScanning();

            // Reset bluetooth
            if (autoResetBluetoothOnStart && wasBluetoothEnabled && areLocationServicesReadyForScanning) {
                BleUtils.resetBluetoothAdapter(getActivity(), this);
            }
        }

        // Request Bluetooth scanning permissions
        requestLocationPermissionIfNeeded();

        return bleManagerView;
    }




    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == kActivityRequestCode_ConnectedActivity) {
            if (resultCode < 0) {
                Toast.makeText(getActivity(), R.string.scan_unexpecteddisconnect, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == kActivityRequestCode_EnableBluetooth) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth was enabled, resume scanning
                resumeScanning();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                AlertDialog dialog = builder.setMessage(R.string.dialog_error_no_bluetooth)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                DialogUtils.keepDialogOnOrientationChanges(dialog);

            }
        } else if (requestCode == kActivityRequestCode_Settings) {
            // Return from activity settings. Update app behaviour if needed
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean updatesEnabled = sharedPreferences.getBoolean("pref_updatesenabled", true);
            if (updatesEnabled) {
                mLatestCheckedDeviceAddress = null;
                //mFirmwareUpdater.refreshSoftwareUpdatesDatabase();
            } else {
                //mFirmwareUpdater = null;
            }
        }
    }

    private void autostartScan() {
        if (BleUtils.getBleStatus(getActivity()) == BleUtils.STATUS_BLE_ENABLED) {
            // If was connected, disconnect
            mBleManager.disconnect();

            // Force restart scanning
            if (mScannedDevices != null) {      // Fixed a weird bug when resuming the app (this was null on very rare occasions even if it should not be)
                mScannedDevices.clear();
            }
            startScan(null);
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnBLEManageFragmentInteractionListener) {
            mListener = (OnBLEManageFragmentInteractionListener) context;
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

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set listener
        mBleManager.setBleListener(this);

        // Autostart scan
        autostartScan();

        // Update UI
        updateUI();
    }

    @Override
    public synchronized void onPause() {
        // Stop scanning
        if (mScanner != null && mScanner.isScanning()) {
            mIsScanPaused = true;
            stopScanning();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mConnectingDialog != null) {
            mConnectingDialog.cancel();
            mConnectingDialog = null;
        }
        super.onStop();
    }

    public void onBackPressed() {
        if (mShouldEnableWifiOnQuit) {
            mShouldEnableWifiOnQuit = false;
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.settingsaction_confirmenablewifi_title))
                    .setMessage(getString(R.string.settingsaction_confirmenablewifi_message))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "enableNotification wifi");
                            BleUtils.enableWifi(true, getActivity());
                            getActivity().onBackPressed();
                        }

                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().onBackPressed();
                        }

                    })
                    .show();
        } else {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        // Stop ble adapter reset if in progress
        BleUtils.cancelBluetoothAdapterReset(getActivity());

        // Retain data
        saveRetainedDataFragment();

        // Clean
        if (mConnectingDialog != null) {
            mConnectingDialog.cancel();
        }
        super.onDestroy();
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
    public interface OnBLEManageFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    // region Permissions
    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can scan for Bluetooth peripherals");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location permission granted");
                    // Autostart scan
                    autostartScan();
                    // Update UI
                    updateUI();
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Bluetooth Scanning not available");
                    builder.setMessage("Since location access has not been granted, the app will not be able to scan for Bluetooth peripherals");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                break;
            }
            default:
                break;
        }
    }

    // endregion


    // region Filters
    private void openFiltersPanel(final boolean isOpen, boolean animated) {
        SharedPreferences.Editor preferencesEditor = getActivity().getSharedPreferences(kPreferences, MODE_PRIVATE).edit();
        preferencesEditor.putBoolean(kPreferences_filtersPanelOpen, isOpen);
        preferencesEditor.apply();

        mFiltersExpandImageView.setImageResource(isOpen ? R.drawable.ic_expand_less_black_24dp : R.drawable.ic_expand_more_black_24dp);

        /*
        float paddingTop = MetricsUtils.convertDpToPixel(this, (float) (isOpen ? 200 : 44));
        mScannedDevicesListView.setPadding(0, (int) paddingTop, 0, 0);

        mFiltersPanelView.setVisibility(View.VISIBLE);
        HeightAnimation heightAnim = new HeightAnimation(mFiltersPanelView, isOpen?0:200, isOpen?200:0);
        heightAnim.setDuration(300);
        mFiltersPanelView.startAnimation(heightAnim);
*/

        mFiltersPanelView.setVisibility(isOpen ? View.VISIBLE : View.GONE);

        mFiltersPanelView.animate()
                .alpha(isOpen ? 1.0f : 0)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mFiltersPanelView.setVisibility(isOpen ? View.VISIBLE : View.GONE);
                    }
                });

    }
/*
    public class HeightAnimation extends Animation {
        protected final int originalHeight;
        protected final View view;
        protected float perValue;

        public HeightAnimation(View view, int fromHeight, int toHeight) {
            this.view = view;
            this.originalHeight = fromHeight;
            this.perValue = (toHeight - fromHeight);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            view.getLayoutParams().height = (int) (originalHeight + perValue * interpolatedTime);
            view.requestLayout();
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }*/

    public void onClickExpandFilters(View view) {
        SharedPreferences preferences = getActivity().getSharedPreferences(kPreferences, MODE_PRIVATE);
        boolean filtersIsPanelOpen = preferences.getBoolean(kPreferences_filtersPanelOpen, false);

        openFiltersPanel(!filtersIsPanelOpen, true);
    }

    public void onClickRemoveFilters(View view) {
        mPeripheralList.setDefaultFilters();
        mFiltersNameEditText.setText(mPeripheralList.getFilterName());
        setRssiSliderValue(mPeripheralList.getFilterRssiValue());
        mFiltersUnnamedCheckBox.setChecked(mPeripheralList.isFilterUnnamedEnabled());
        mFiltersUartCheckBox.setChecked(mPeripheralList.isFilterOnlyUartEnabled());
        updateFilters();
    }

    public void onClickFilterNameSettings(View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                boolean processed = true;
                switch (item.getItemId()) {
                    case R.id.scanfilter_name_contains:
                        mPeripheralList.setFilterNameExact(false);
                        break;
                    case R.id.scanfilter_name_exact:
                        mPeripheralList.setFilterNameExact(true);
                        break;
                    case R.id.scanfilter_name_sensitive:
                        mPeripheralList.setFilterNameCaseInsensitive(false);
                        break;
                    case R.id.scanfilter_name_insensitive:
                        mPeripheralList.setFilterNameCaseInsensitive(true);
                        break;
                    default:
                        processed = false;
                        break;
                }
                updateFilters();
                return processed;
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        Menu menu = popup.getMenu();
        inflater.inflate(R.menu.menu_scan_filters_name, menu);
        final boolean isFilterNameExact = mPeripheralList.isFilterNameExact();
        menu.findItem(isFilterNameExact ? R.id.scanfilter_name_exact : R.id.scanfilter_name_contains).setChecked(true);
        final boolean isFilterNameCaseInsensitive = mPeripheralList.isFilterNameCaseInsensitive();
        menu.findItem(isFilterNameCaseInsensitive ? R.id.scanfilter_name_insensitive : R.id.scanfilter_name_sensitive).setChecked(true);
        popup.show();
    }


    private void updateFiltersTitle() {
        final String filtersTitle = mPeripheralList.filtersDescription();
        mFiltersTitleTextView.setText(filtersTitle != null ? String.format(Locale.ENGLISH, getString(R.string.scan_filters_title_filter_format), filtersTitle) : getString(R.string.scan_filters_title_nofilter));
        mFiltersClearButton.setVisibility(mPeripheralList.isAnyFilterEnabled() ? View.VISIBLE : View.GONE);
    }

    private void updateFilters() {
        updateFiltersTitle();
        mScannedDevicesAdapter.notifyDataSetChanged();
    }

    private void setRssiSliderValue(int value) {
        mFiltersRssiSeekBar.setProgress(-value);
        updateRssiValue();
    }

    private void updateRssiValue() {
        final int value = -mFiltersRssiSeekBar.getProgress();
        mFiltersRssiValueTextView.setText(String.format(Locale.ENGLISH, getString(R.string.scan_filters_rssi_value_format), value));
    }

    // endregion

    private void resumeScanning() {
        if (mIsScanPaused) {
            startScan(null);
            mIsScanPaused = mScanner == null;
        }
    }

    private void showChooseDeviceServiceDialog(final BluetoothDeviceData deviceData) {
        // Prepare dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String title = String.format(getString(R.string.scan_connectto_dialog_title_format), deviceData.getNiceName());
        String[] items = new String[kComponentsNameIds.length];
        for (int i = 0; i < kComponentsNameIds.length; i++)
            items[i] = getString(kComponentsNameIds[i]);

        builder.setTitle(title)
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (kComponentsNameIds[which]) {
                            case R.string.scan_connectservice_uart: {          // Uart
                                Log.e(TAG, "Zuordnen der UART-Activity");
                                mComponentToStartWhenConnected = UartActivity.class;
                                break;
                            }
                            case R.string.scan_connectservice_info: {           // Info
                                //mComponentToStartWhenConnected = InfoActivity.class;
                                break;
                            }
                            case R.string.scan_connectservice_pinio: {        // PinIO
                                //mComponentToStartWhenConnected = PinIOActivity.class;
                                break;
                            }
                            case R.string.scan_connectservice_controller: {    // Controller
                                //mComponentToStartWhenConnected = ControllerActivity.class;
                                break;
                            }
                            case R.string.scan_connectservice_beacon: {        // Beacon
                                //mComponentToStartWhenConnected = BeaconActivity.class;
                                break;
                            }
                            case R.string.scan_connectservice_neopixel: {       // Neopixel
                                //mComponentToStartWhenConnected = NeopixelActivity.class;
                                break;
                            }
                        }

                        if (mComponentToStartWhenConnected != null) {
                            connect(deviceData.device);            // First connect to the device, and when connected go to selected activity
                        }
                    }
                });

        // Show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean manageBluetoothAvailability() {
        boolean isEnabled = true;

        // Check Bluetooth HW status
        int errorMessageId = 0;
        final int bleStatus = BleUtils.getBleStatus(getActivity().getBaseContext());
        switch (bleStatus) {
            case BleUtils.STATUS_BLE_NOT_AVAILABLE:
                errorMessageId = R.string.dialog_error_no_ble;
                isEnabled = false;
                break;
            case BleUtils.STATUS_BLUETOOTH_NOT_AVAILABLE: {
                errorMessageId = R.string.dialog_error_no_bluetooth;
                isEnabled = false;      // it was already off
                break;
            }
            case BleUtils.STATUS_BLUETOOTH_DISABLED: {
                isEnabled = false;      // it was already off
                // if no enabled, launch settings dialog to enable it (user should always be prompted before automatically enabling bluetooth)
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, kActivityRequestCode_EnableBluetooth);
                // execution will continue at onActivityResult()
                break;
            }
        }
        if (errorMessageId != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            AlertDialog dialog = builder.setMessage(errorMessageId)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            DialogUtils.keepDialogOnOrientationChanges(dialog);
        }

        return isEnabled;
    }

    private boolean manageLocationServiceAvailabilityForScanning() {

        boolean areLocationServiceReady = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {        // Location services are only needed to be enabled from Android 6.0
            int locationMode = Settings.Secure.LOCATION_MODE_OFF;
            try {
                locationMode = Settings.Secure.getInt(getActivity().getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            areLocationServiceReady = locationMode != Settings.Secure.LOCATION_MODE_OFF;

            if (!areLocationServiceReady) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                AlertDialog dialog = builder.setMessage(R.string.dialog_error_nolocationservices_requiredforscan_marshmallow)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                DialogUtils.keepDialogOnOrientationChanges(dialog);
            }
        }

        return areLocationServiceReady;
    }

    private void connect(BluetoothDevice device) {
        boolean isConnecting = mBleManager.connect(getActivity(), device.getAddress());
        if (isConnecting) {
            showConnectionStatus(true);
        }
    }

    private void showConnectionStatus(boolean enable) {
        showStatusDialog(enable, R.string.scan_connecting);
    }

    private void showGettingUpdateInfoState() {
        showConnectionStatus(false);
        showStatusDialog(true, R.string.scan_gettingupdateinfo);
    }

    private void showStatusDialog(boolean show, int stringId) {
        if (show) {

            // Remove if a previous dialog was open (maybe because was clicked 2 times really quick)
            if (mConnectingDialog != null) {
                mConnectingDialog.cancel();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(stringId);

            // Show dialog
            mConnectingDialog = builder.create();
            mConnectingDialog.setCanceledOnTouchOutside(false);

            mConnectingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        mBleManager.disconnect();
                        mConnectingDialog.cancel();
                    }
                    return true;
                }
            });
            mConnectingDialog.show();
        } else {
            if (mConnectingDialog != null) {
                mConnectingDialog.cancel();
            }
        }
    }

    // region Actions
    public void onClickScannedDevice(final View view) {
        final int groupPosition = (Integer) view.getTag();

        if (mScannedDevicesListView.isGroupExpanded(groupPosition)) {
            mScannedDevicesListView.collapseGroup(groupPosition);
        } else {
            mScannedDevicesListView.expandGroup(groupPosition, true);

            // Force scrolling to view the children
            mDevicesScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScannedDevicesListView.scrollToGroup(groupPosition, view, mDevicesScrollView);
                }
            });
        }
    }

    public void onClickDeviceConnect(int scannedDeviceIndex) {
        stopScanning();

        ArrayList<BluetoothDeviceData> filteredPeripherals = mPeripheralList.filteredPeripherals(false);
        if (scannedDeviceIndex < filteredPeripherals.size()) {
            mSelectedDeviceData = filteredPeripherals.get(scannedDeviceIndex);
            BluetoothDevice device = mSelectedDeviceData.device;

            mBleManager.setBleListener(this);           // Force set listener (could be still checking for updates...)

            if (mSelectedDeviceData.type == BluetoothDeviceData.kType_Uart) {      // if is uart, show all the available activities
                showChooseDeviceServiceDialog(mSelectedDeviceData);
            } else {                          // if no uart, then go directly to info
                Log.d(TAG, "No UART service found. Go to InfoActivity");
                mComponentToStartWhenConnected = InfoActivity.class;
                connect(device);
            }
        } else {
            Log.w(TAG, "onClickDeviceConnect index does not exist: " + scannedDeviceIndex);
        }
    }

    public void onClickScan(View view) {
        boolean isScanning = mScanner != null && mScanner.isScanning();
        if (isScanning) {
            stopScanning();
        } else {
            startScan(null);
        }
    }
    // endregion

    // region Scan
    private void startScan(final UUID[] servicesToScan) {
        Log.d(TAG, "startScan");

        // Stop current scanning (if needed)
        stopScanning();

        // Configure scanning
        BluetoothAdapter bluetoothAdapter = BleUtils.getBluetoothAdapter(getActivity().getApplicationContext());
        if (BleUtils.getBleStatus(getActivity()) != BleUtils.STATUS_BLE_ENABLED) {
            Log.w(TAG, "startScan: BluetoothAdapter not initialized or unspecified address.");
        } else {
            mScanner = new BleDevicesScanner(bluetoothAdapter, servicesToScan, new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    //final String deviceName = device.getName();
                    //Log.d(TAG, "Discovered device: " + (deviceName != null ? deviceName : "<unknown>"));

                    BluetoothDeviceData previouslyScannedDeviceData = null;
                    if (mScannedDevices == null) {
                        mScannedDevices = new ArrayList<>();       // Safeguard
                    }

                    // Check that the device was not previously found
                    for (BluetoothDeviceData deviceData : mScannedDevices) {
                        if (deviceData.device.getAddress().equals(device.getAddress())) {
                            previouslyScannedDeviceData = deviceData;
                            break;
                        }
                    }

                    BluetoothDeviceData deviceData;
                    if (previouslyScannedDeviceData == null) {
                        // Add it to the mScannedDevice list
                        deviceData = new BluetoothDeviceData();
                        mScannedDevices.add(deviceData);
                    } else {
                        deviceData = previouslyScannedDeviceData;
                    }

                    deviceData.device = device;
                    deviceData.rssi = rssi;
                    deviceData.scanRecord = scanRecord;
                    decodeScanRecords(deviceData);

                    // Update device data
                    long currentMillis = SystemClock.uptimeMillis();
                    if (previouslyScannedDeviceData == null || currentMillis - mLastUpdateMillis > kMinDelayToUpdateUI) {
                        // Avoid updating when not a new device has been found and the time from the last update is really short to
                        // avoid updating UI so fast that it will become unresponsive
                        mLastUpdateMillis = currentMillis;

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                            }
                        });
                    }

                }
            });

            // Start scanning
            mScanner.start();
        }

        // Update UI
        updateUI();
    }

    private void stopScanning() {
        // Stop scanning
        if (mScanner != null) {
            mScanner.stop();
            mScanner = null;
        }

        updateUI();
    }
    // endregion

    private void decodeScanRecords(BluetoothDeviceData deviceData) {
        // based on http://stackoverflow.com/questions/24003777/read-advertisement-packet-in-android
        final byte[] scanRecord = deviceData.scanRecord;

        ArrayList<UUID> uuids = new ArrayList<>();
        byte[] advertisedData = Arrays.copyOf(scanRecord, scanRecord.length);
        int offset = 0;
        deviceData.type = BluetoothDeviceData.kType_Unknown;

        // Check if is an iBeacon ( 0x02, 0x0x1, a flag byte, 0x1A, 0xFF, manufacturer (2bytes), 0x02, 0x15)
        final boolean isBeacon = advertisedData[0] == 0x02 && advertisedData[1] == 0x01 && advertisedData[3] == 0x1A && advertisedData[4] ==
                (byte) 0xFF && advertisedData[7] == 0x02 && advertisedData[8] == 0x15;

        // Check if is an URIBeacon
        final byte[] kUriBeaconPrefix = {0x03, 0x03, (byte) 0xD8, (byte) 0xFE};
        final boolean isUriBeacon = Arrays.equals(Arrays.copyOf(scanRecord, kUriBeaconPrefix.length), kUriBeaconPrefix) && advertisedData[5] ==
                0x16 && advertisedData[6] == kUriBeaconPrefix[2] && advertisedData[7] == kUriBeaconPrefix[3];

        if (isBeacon) {
            deviceData.type = BluetoothDeviceData.kType_Beacon;

            // Read uuid
            offset = 9;
            UUID uuid = BleUtils.getUuidFromByteArrayBigEndian(Arrays.copyOfRange(scanRecord, offset, offset + 16));
            uuids.add(uuid);
            offset += 16;

            // Skip major minor
            offset += 2 * 2;   // major, minor

            // Read txpower
            final int txPower = advertisedData[offset++];
            deviceData.txPower = txPower;
        } else if (isUriBeacon) {
            deviceData.type = BluetoothDeviceData.kType_UriBeacon;

            // Read txpower
            final int txPower = advertisedData[9];
            deviceData.txPower = txPower;
        } else {
            // Read standard advertising packet
            while (offset < advertisedData.length - 2) {
                // Length
                int len = advertisedData[offset++];
                if (len == 0) break;

                // Type
                int type = advertisedData[offset++];
                if (type == 0) break;

                // Data
//            Log.d(TAG, "record -> lenght: " + length + " type:" + type + " data" + data);

                switch (type) {
                    case 0x02:          // Partial list of 16-bit UUIDs
                    case 0x03: {        // Complete list of 16-bit UUIDs
                        while (len > 1) {
                            int uuid16 = advertisedData[offset++] & 0xFF;
                            uuid16 |= (advertisedData[offset++] << 8);
                            len -= 2;
                            uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                        }
                        break;
                    }

                    case 0x06:          // Partial list of 128-bit UUIDs
                    case 0x07: {        // Complete list of 128-bit UUIDs
                        while (len >= 16) {
                            try {
                                // Wrap the advertised bits and order them.
                                UUID uuid = BleUtils.getUuidFromByteArraLittleEndian(Arrays.copyOfRange(advertisedData, offset, offset + 16));
                                uuids.add(uuid);

                            } catch (IndexOutOfBoundsException e) {
                                Log.e(TAG, "BlueToothDeviceFilter.parseUUID: " + e.toString());
                            } finally {
                                // Move the offset to read the next uuid.
                                offset += 16;
                                len -= 16;
                            }
                        }
                        break;
                    }

                    case 0x09: {
                        byte[] nameBytes = new byte[len - 1];
                        for (int i = 0; i < len - 1; i++) {
                            nameBytes[i] = advertisedData[offset++];
                        }

                        String name = null;
                        try {
                            name = new String(nameBytes, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        deviceData.advertisedName = name;
                        break;
                    }

                    case 0x0A: {        // TX Power
                        final int txPower = advertisedData[offset++];
                        deviceData.txPower = txPower;
                        break;
                    }

                    default: {
                        offset += (len - 1);
                        break;
                    }
                }
            }

            // Check if Uart is contained in the uuids
            boolean isUart = false;
            for (UUID uuid : uuids) {
                if (uuid.toString().equalsIgnoreCase(UartInterfaceActivity.UUID_SERVICE)) {
                    isUart = true;
                    break;
                }
            }
            if (isUart) {
                deviceData.type = BluetoothDeviceData.kType_Uart;
            }
        }

        deviceData.uuids = uuids;
    }


    private void updateUI() {
        // Scan button
        boolean isScanning = mScanner != null && mScanner.isScanning();
        mScanButton.setText(getString(isScanning ? R.string.scan_scanbutton_scanning : R.string.scan_scanbutton_scan));

        // Show list and hide "no devices" label
        final boolean isListEmpty = mScannedDevices == null || mScannedDevices.size() == 0;
        mNoDevicesTextView.setVisibility(isListEmpty ? View.VISIBLE : View.GONE);
        mDevicesScrollView.setVisibility(isListEmpty ? View.GONE : View.VISIBLE);

        // devices list
        mScannedDevicesAdapter.notifyDataSetChanged();
    }

    // region ResetBluetoothAdapterListener
    @Override
    public void resetBluetoothCompleted() {
        Log.d(TAG, "Reset completed -> Resume scanning");
        resumeScanning();
    }
    // endregion

    private void launchComponentActivity() {
        // Enable generic attribute service
        final BluetoothGattService genericAttributeService = mBleManager.getGattService(kGenericAttributeService);
        if (genericAttributeService != null) {
            Log.d(TAG, "kGenericAttributeService found. Check if kServiceChangedCharacteristic exists");

            final UUID characteristicUuid = UUID.fromString(kServiceChangedCharacteristic);
            final BluetoothGattCharacteristic dataCharacteristic = genericAttributeService.getCharacteristic(characteristicUuid);
            if (dataCharacteristic != null) {
                Log.d(TAG, "kServiceChangedCharacteristic exists. Enable indication");
                mBleManager.enableIndication(genericAttributeService, kServiceChangedCharacteristic, true);
            } else {
                Log.d(TAG, "Skip enable indications for kServiceChangedCharacteristic. Characteristic not found");
            }
        } else {
            Log.d(TAG, "Skip enable indications for kServiceChangedCharacteristic. kGenericAttributeService not found");
        }

        // Launch activity
        showConnectionStatus(false);
        if (mComponentToStartWhenConnected != null) {
            Log.d(TAG, "Start component:" + mComponentToStartWhenConnected);
            Intent intent = new Intent(getActivity(), mComponentToStartWhenConnected);
            Bundle bundle = new Bundle();
            if(bicycleDriverGroup != null){
                bundle.putSerializable("bDG", bicycleDriverGroup);
                intent.putExtras(bundle);
            }
            if (mComponentToStartWhenConnected == BeaconActivity.class && mSelectedDeviceData != null) {
                intent.putExtra("rssi", mSelectedDeviceData.rssi);
            }
            startActivityForResult(intent, kActivityRequestCode_ConnectedActivity);
        }
    }

    // region BleManagerListener
    @Override
    public void onConnected() {
        Log.d(TAG, "BLEManageFragnent Connected");
    }

    @Override
    public void onConnecting() {
        Log.d(TAG, "BLEManageFragnent Connecting ...");
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "BLEManageFragnent onDisconnected");
        showConnectionStatus(false);
    }

    @Override
    public void onServicesDiscovered() {
        Log.d(TAG, "services discovered");

        // Check if there is a failed installation that was stored to retry
        boolean isFailedInstallationDetected = false;//FirmwareUpdater.isFailedInstallationRecoveryAvailable(this, mBleManager.getConnectedDeviceAddress());
        if (isFailedInstallationDetected) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Failed installation detected");
                    // Ask user if should update
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.scan_failedupdatedetected_title)
                            .setMessage(R.string.scan_failedupdatedetected_message)
                            .setPositiveButton(R.string.scan_failedupdatedetected_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    showConnectionStatus(false);        // hide current dialogs because software update will display a dialog
                                    stopScanning();

                                    //mFirmwareUpdater.startFailedInstallationRecovery(MainActivity.this);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //FirmwareUpdater.clearFailedInstallationRecoveryParams(MainActivity.this);
                                    launchComponentActivity();
                                }
                            })
                            .setCancelable(false)
                            .show();
                }
            });
        } else {
                    // Check if a firmware update is available
                    boolean isCheckingFirmware = true; //this is fucked up now
                    if (isCheckingFirmware) {//mFirmwareUpdater != null
                        // Don't bother the user waiting for checks if the latest connected device was this one too
                        String deviceAddress = mBleManager.getConnectedDeviceAddress();
                        if (!deviceAddress.equals(mLatestCheckedDeviceAddress)) {
                            mLatestCheckedDeviceAddress = deviceAddress;

                    // Check if should update device software
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showGettingUpdateInfoState();
                        }
                    });/*
                    mFirmwareUpdater.checkFirmwareUpdatesForTheCurrentConnectedDevice();        // continues asynchronously in onFirmwareUpdatesChecked
                    isCheckingFirmware = true;*/
                            launchComponentActivity();
                        } else {
                            Log.d(TAG, "Updates: Device already checked previously. Skipping...");
                        }
                    }
        }
    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic) {
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor) {
    }

    @Override
    public void onReadRemoteRssi(int rssi) {

    }
    // endregion

    // region Helpers
    private class BluetoothDeviceData {
        BluetoothDevice device;
        public int rssi;
        byte[] scanRecord;
        private String advertisedName;           // Advertised name
        private String cachedNiceName;
        private String cachedName;

        // Decoded scan record (update R.array.scan_devicetypes if this list is modified)
        static final int kType_Unknown = 0;
        static final int kType_Uart = 1;
        static final int kType_Beacon = 2;
        static final int kType_UriBeacon = 3;

        public int type;
        int txPower;
        ArrayList<UUID> uuids;

        String getName() {
            if (cachedName == null) {
                cachedName = device.getName();
                if (cachedName == null) {
                    cachedName = advertisedName;      // Try to get a name (but it seems that if device.getName() is null, this is also null)
                }
            }

            return cachedName;
        }

        String getNiceName() {
            if (cachedNiceName == null) {
                cachedNiceName = getName();
                if (cachedNiceName == null) {
                    cachedNiceName = device.getAddress();
                }
            }

            return cachedNiceName;
        }
    }
    //endregion

    // region Peripheral List
    private class PeripheralList {
        // Constants
        private final static int kMaxRssiValue = -100;

        private final static String kPreferences = "PeripheralList_prefs";
        private final static String kPreferences_filtersName = "filtersName";
        private final static String kPreferences_filtersIsNameExact = "filtersIsNameExact";
        private final static String kPreferences_filtersIsNameCaseInsensitive = "filtersIsNameCaseInsensitive";
        private final static String kPreferences_filtersRssi = "filtersRssi";
        private final static String kPreferences_filtersUnnamedEnabled = "filtersUnnamedEnabled";
        private final static String kPreferences_filtersUartEnabled = "filtersUartEnabled";

        // Data
        private String mFilterName;
        private boolean mIsFilterNameExact;
        private boolean mIsFilterNameCaseInsensitive;
        private int mRssiFilterValue;
        private boolean mIsUnnamedEnabled;
        private boolean mIsOnlyUartEnabled;
        private ArrayList<BluetoothDeviceData> mCachedFilteredPeripheralList;
        private boolean mIsFilterDirty;

        private SharedPreferences.Editor preferencesEditor = getActivity().getSharedPreferences(kPreferences, MODE_PRIVATE).edit();

        PeripheralList() {
            mIsFilterDirty = true;
            mCachedFilteredPeripheralList = new ArrayList<>();

            SharedPreferences preferences = getActivity().getSharedPreferences(kPreferences, MODE_PRIVATE);
            mFilterName = preferences.getString(kPreferences_filtersName, null);
            mIsFilterNameExact = preferences.getBoolean(kPreferences_filtersIsNameExact, false);
            mIsFilterNameCaseInsensitive = preferences.getBoolean(kPreferences_filtersIsNameCaseInsensitive, true);
            mRssiFilterValue = preferences.getInt(kPreferences_filtersRssi, kMaxRssiValue);
            mIsUnnamedEnabled = preferences.getBoolean(kPreferences_filtersUnnamedEnabled, true);
            mIsOnlyUartEnabled = preferences.getBoolean(kPreferences_filtersUartEnabled, false);
        }

        String getFilterName() {
            return mFilterName;
        }

        void setFilterName(String name) {
            mFilterName = name;
            mIsFilterDirty = true;

            preferencesEditor.putString(kPreferences_filtersName, name);
            preferencesEditor.apply();
        }

        boolean isFilterNameExact() {
            return mIsFilterNameExact;
        }

        void setFilterNameExact(boolean exact) {
            mIsFilterNameExact = exact;
            mIsFilterDirty = true;

            preferencesEditor.putBoolean(kPreferences_filtersIsNameExact, exact);
            preferencesEditor.apply();
        }

        boolean isFilterNameCaseInsensitive() {
            return mIsFilterNameCaseInsensitive;
        }

        void setFilterNameCaseInsensitive(boolean caseInsensitive) {
            mIsFilterNameCaseInsensitive = caseInsensitive;
            mIsFilterDirty = true;

            preferencesEditor.putBoolean(kPreferences_filtersIsNameCaseInsensitive, caseInsensitive);
            preferencesEditor.apply();
        }

        int getFilterRssiValue() {
            return mRssiFilterValue;
        }

        void setFilterRssiValue(int value) {
            mRssiFilterValue = value;
            mIsFilterDirty = true;

            preferencesEditor.putInt(kPreferences_filtersRssi, value);
            preferencesEditor.apply();
        }

        boolean isFilterUnnamedEnabled() {
            return mIsUnnamedEnabled;
        }

        void setFilterUnnamedEnabled(boolean enabled) {
            mIsUnnamedEnabled = enabled;
            mIsFilterDirty = true;

            preferencesEditor.putBoolean(kPreferences_filtersUnnamedEnabled, enabled);
            preferencesEditor.apply();
        }


        boolean isFilterOnlyUartEnabled() {
            return mIsOnlyUartEnabled;
        }

        void setFilterOnlyUartEnabled(boolean enabled) {
            mIsOnlyUartEnabled = enabled;
            mIsFilterDirty = true;

            preferencesEditor.putBoolean(kPreferences_filtersUartEnabled, enabled);
            preferencesEditor.apply();
        }


        void setDefaultFilters() {
            mFilterName = null;
            mIsFilterNameExact = false;
            mIsFilterNameCaseInsensitive = true;
            mRssiFilterValue = kMaxRssiValue;
            mIsUnnamedEnabled = true;
            mIsOnlyUartEnabled = false;
        }

        boolean isAnyFilterEnabled() {
            return (mFilterName != null && !mFilterName.isEmpty()) || mRssiFilterValue > kMaxRssiValue || mIsOnlyUartEnabled || !mIsUnnamedEnabled;
        }

        ArrayList<BluetoothDeviceData> filteredPeripherals(boolean forceUpdate) {
            if (mIsFilterDirty || forceUpdate) {
                mCachedFilteredPeripheralList = calculateFilteredPeripherals();
                mIsFilterDirty = false;
            }

            return mCachedFilteredPeripheralList;
        }

        private ArrayList<BluetoothDeviceData> calculateFilteredPeripherals() {

            if (mScannedDevices == null) {
                mScannedDevices = new ArrayList<>();       // Safegua
            }
            ArrayList<BluetoothDeviceData> peripherals = (ArrayList<BluetoothDeviceData>) mScannedDevices.clone();

            // Sort devices alphabetically
            Collections.sort(peripherals, new Comparator<BluetoothDeviceData>() {
                @Override
                public int compare(BluetoothDeviceData o1, BluetoothDeviceData o2) {
                    return o1.getNiceName().compareToIgnoreCase(o2.getNiceName());
                }
            });

            // Apply filters
            if (mIsOnlyUartEnabled) {
                for (Iterator<BluetoothDeviceData> it = peripherals.iterator(); it.hasNext(); ) {
                    if (it.next().type != BluetoothDeviceData.kType_Uart) {
                        it.remove();
                    }
                }
            }

            if (!mIsUnnamedEnabled) {
                for (Iterator<BluetoothDeviceData> it = peripherals.iterator(); it.hasNext(); ) {
                    if (it.next().getName() == null) {
                        it.remove();
                    }
                }
            }

            if (mFilterName != null && !mFilterName.isEmpty()) {
                for (Iterator<BluetoothDeviceData> it = peripherals.iterator(); it.hasNext(); ) {
                    String name = it.next().getName();
                    boolean testPassed = false;
                    if (name != null) {
                        if (mIsFilterNameExact) {
                            if (mIsFilterNameCaseInsensitive) {
                                testPassed = name.compareToIgnoreCase(mFilterName) == 0;
                            } else {
                                testPassed = name.compareTo(mFilterName) == 0;
                            }
                        } else {
                            if (mIsFilterNameCaseInsensitive) {
                                testPassed = name.toLowerCase().contains(mFilterName.toLowerCase());
                            } else {
                                testPassed = name.contains(mFilterName);
                            }
                        }
                    }
                    if (!testPassed) {
                        it.remove();
                    }
                }
            }

            for (Iterator<BluetoothDeviceData> it = peripherals.iterator(); it.hasNext(); ) {
                if (it.next().rssi < mRssiFilterValue) {
                    it.remove();
                }
            }

            return peripherals;
        }

        String filtersDescription() {
            String filtersTitle = null;

            if (mFilterName != null && !mFilterName.isEmpty()) {
                filtersTitle = mFilterName;
            }

            if (mRssiFilterValue > kMaxRssiValue) {
                String rssiString = String.format(Locale.ENGLISH, getString(R.string.scan_filters_name_rssi_format), mRssiFilterValue);
                if (filtersTitle != null && !filtersTitle.isEmpty()) {
                    filtersTitle = filtersTitle + ", " + rssiString;
                } else {
                    filtersTitle = rssiString;
                }
            }

            if (!mIsUnnamedEnabled) {
                String namedString = getString(R.string.scan_filters_name_named);
                if (filtersTitle != null && !filtersTitle.isEmpty()) {
                    filtersTitle = filtersTitle + ", " + namedString;
                } else {
                    filtersTitle = namedString;
                }
            }

            if (mIsOnlyUartEnabled) {
                String uartString = getString(R.string.scan_filters_name_uart);
                if (filtersTitle != null && !filtersTitle.isEmpty()) {
                    filtersTitle = filtersTitle + ", " + uartString;
                } else {
                    filtersTitle = uartString;
                }
            }

            return filtersTitle;
        }
    }
    // endregion

    // region adapters
    private class ExpandableListAdapter extends BaseExpandableListAdapter {
        // Data
        private ArrayList<BluetoothDeviceData> mFilteredPeripherals;

        private class GroupViewHolder {
            TextView nameTextView;
            TextView descriptionTextView;
            ImageView rssiImageView;
            TextView rssiTextView;
            Button connectButton;
        }

        @Override
        public int getGroupCount() {
            mFilteredPeripherals = mPeripheralList.filteredPeripherals(true);
            return mFilteredPeripherals.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return mFilteredPeripherals.get(groupPosition);
        }

        @Override
        public Spanned getChild(int groupPosition, int childPosition) {
            BluetoothDeviceData deviceData = mFilteredPeripherals.get(groupPosition);

            String text;
            switch (deviceData.type) {
                case BluetoothDeviceData.kType_Beacon:
                    text = getChildBeacon(deviceData);
                    break;

                case BluetoothDeviceData.kType_UriBeacon:
                    text = getChildUriBeacon(deviceData);
                    break;

                default:
                    text = getChildCommon(deviceData);
                    break;
            }

            Spanned result;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(text);
            }
            return result;
        }


        private String getChildUriBeacon(BluetoothDeviceData deviceData) {
            StringBuilder result = new StringBuilder();

            String name = deviceData.getName();
            if (name != null) {
                result.append(getString(R.string.scan_device_localname)).append(": <b>").append(name).append("</b><br>");
            }

            String address = deviceData.device.getAddress();
            result.append(getString(R.string.scan_device_address) + ": <b>" + (address == null ? "" : address) + "</b><br>");

            //String uri = UriBeaconUtils.getUriFromAdvertisingPacket(deviceData.scanRecord) + "</b><br>";
            //result.append(getString(R.string.scan_device_uribeacon_uri)).append(": <b>").append(uri);

            result.append(getString(R.string.scan_device_txpower)).append(": <b>").append(deviceData.txPower).append("</b>");

            return result.toString();
        }


        private String getChildCommon(BluetoothDeviceData deviceData) {
            StringBuilder result = new StringBuilder();

            String name = deviceData.getName();
            if (name != null) {
                result.append(getString(R.string.scan_device_localname)).append(": <b>").append(name).append("</b><br>");
            }
            String address = deviceData.device.getAddress();
            result.append(getString(R.string.scan_device_address)).append(": <b>").append(address == null ? "" : address).append("</b><br>");

            StringBuilder serviceText = new StringBuilder();
            if (deviceData.uuids != null) {
                int i = 0;
                for (UUID uuid : deviceData.uuids) {
                    if (i > 0) serviceText.append(", ");
                    serviceText.append(uuid.toString().toUpperCase());
                    i++;
                }
            }
            if (!serviceText.toString().isEmpty()) {
                result.append(getString(R.string.scan_device_services)).append(": <b>").append(serviceText).append("</b><br>");
            }
            result.append(getString(R.string.scan_device_txpower)).append(": <b>").append(deviceData.txPower).append("</b>");

            return result.toString();
        }

        private String getChildBeacon(BluetoothDeviceData deviceData) {
            StringBuilder result = new StringBuilder();

            String name = deviceData.getName();
            if (name != null) {
                result.append(getString(R.string.scan_device_localname)).append(": <b>").append(name).append("</b><br>");
            }
            String address = deviceData.device.getAddress();
            result.append(getString(R.string.scan_device_address)).append(": <b>").append(address == null ? "" : address).append("</b><br>");

            final byte[] manufacturerBytes = {deviceData.scanRecord[6], deviceData.scanRecord[5]};      // Little endian
            String manufacturer = BleUtils.bytesToHex(manufacturerBytes);

            // Check if the manufacturer is known, and replace the id for a name
            String kKnownManufacturers[] = getResources().getStringArray(R.array.beacon_manufacturers_ids);
            int knownIndex = Arrays.asList(kKnownManufacturers).indexOf(manufacturer);
            if (knownIndex >= 0) {
                String kManufacturerNames[] = getResources().getStringArray(R.array.beacon_manufacturers_names);
                manufacturer = kManufacturerNames[knownIndex];
            }

            result.append(getString(R.string.scan_device_beacon_manufacturer)).append(": <b>").append(manufacturer == null ? "" : manufacturer).append("</b><br>");

            StringBuilder text = new StringBuilder();
            if (deviceData.uuids != null && deviceData.uuids.size() == 1) {
                UUID uuid = deviceData.uuids.get(0);
                text.append(uuid.toString().toUpperCase());
            }
            result.append(getString(R.string.scan_device_uuid)).append(": <b>").append(text).append("</b><br>");

            final byte[] majorBytes = {deviceData.scanRecord[25], deviceData.scanRecord[26]};           // Big endian
            String major = BleUtils.bytesToHex(majorBytes);
            result.append(getString(R.string.scan_device_beacon_major)).append(": <b>").append(major).append("</b><br>");

            final byte[] minorBytes = {deviceData.scanRecord[27], deviceData.scanRecord[28]};           // Big endian
            String minor = BleUtils.bytesToHex(minorBytes);
            result.append(getString(R.string.scan_device_beacon_minor)).append(": <b>").append(minor).append("</b><br>");

            result.append(getString(R.string.scan_device_txpower)).append(": <b>").append(deviceData.txPower).append("</b>");

            return result.toString();
        }


        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            GroupViewHolder holder;

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_scan_item_title, parent, false);

                holder = new GroupViewHolder();

                holder.nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
                holder.descriptionTextView = (TextView) convertView.findViewById(R.id.descriptionTextView);
                holder.rssiImageView = (ImageView) convertView.findViewById(R.id.rssiImageView);
                holder.rssiTextView = (TextView) convertView.findViewById(R.id.rssiTextView);
                holder.connectButton = (Button) convertView.findViewById(R.id.connectButton);

                convertView.setTag(R.string.scan_tag_id, holder);

            } else {
                holder = (GroupViewHolder) convertView.getTag(R.string.scan_tag_id);
            }

            convertView.setTag(groupPosition);
            holder.connectButton.setTag(groupPosition);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickScannedDevice(v);
                }
            });

            /*
            holder.connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickDeviceConnect(groupPosition);
                }
            });

            convertView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                        onClickScannedDevice(v);
                        return true;
                    }
                    return false;
                }
            });
            */

            holder.connectButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        onClickDeviceConnect(groupPosition);
                        return true;
                    }
                    return false;
                }
            });


            BluetoothDeviceData deviceData = mFilteredPeripherals.get(groupPosition);
            holder.nameTextView.setText(deviceData.getNiceName());

            holder.descriptionTextView.setVisibility(deviceData.type != BluetoothDeviceData.kType_Unknown ? View.VISIBLE : View.INVISIBLE);
            holder.descriptionTextView.setText(getResources().getStringArray(R.array.scan_devicetypes)[deviceData.type]);
            holder.rssiTextView.setText(deviceData.rssi == 127 ? getString(R.string.scan_device_rssi_notavailable) : String.valueOf(deviceData.rssi));

            int rrsiDrawableResource = getDrawableIdForRssi(deviceData.rssi);
            holder.rssiImageView.setImageResource(rrsiDrawableResource);

            return convertView;
        }

        private int getDrawableIdForRssi(int rssi) {
            int index;
            if (rssi == 127 || rssi <= -84) {       // 127 reserved for RSSI not available
                index = 0;
            } else if (rssi <= -72) {
                index = 1;
            } else if (rssi <= -60) {
                index = 2;
            } else if (rssi <= -48) {
                index = 3;
            } else {
                index = 4;
            }

            final int kSignalDrawables[] = {
                    R.drawable.signalstrength0,
                    R.drawable.signalstrength1,
                    R.drawable.signalstrength2,
                    R.drawable.signalstrength3,
                    R.drawable.signalstrength4};
            return kSignalDrawables[index];
        }

        @Override
        public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_scan_item_child, parent, false);
            }

            // We don't expect many items so for clarity just find the views each time instead of using a ViewHolder
            TextView textView = (TextView) convertView.findViewById(R.id.dataTextView);
            Spanned text = getChild(groupPosition, childPosition);
            textView.setText(text);

            Button rawDataButton = (Button) convertView.findViewById(R.id.rawDataButton);
            rawDataButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<BluetoothDeviceData> filteredPeripherals = mPeripheralList.filteredPeripherals(false);
                    if (groupPosition < filteredPeripherals.size()) {
                        final BluetoothDeviceData deviceData = filteredPeripherals.get(groupPosition);
                        final byte[] scanRecord = deviceData.scanRecord;
                        final String packetText = BleUtils.bytesToHexWithSpaces(scanRecord);
                        final String clipboardLabel = getString(R.string.scan_device_advertising_title);

                        new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.scan_device_advertising_title)
                                .setMessage(packetText)
                                .setPositiveButton(android.R.string.ok, null)
                                .setNeutralButton(android.R.string.copy, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText(clipboardLabel, packetText);
                                        clipboard.setPrimaryClip(clip);
                                    }
                                })
                                .show();
                    }

                }
            });

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }

    // region DataFragment
    public static class DataFragment extends android.app.Fragment {
        private ArrayList<BluetoothDeviceData> mScannedDevices;
        private Class<?> mComponentToStartWhenConnected;
        private boolean mShouldEnableWifiOnQuit;
        private String mLatestCheckedDeviceAddress;
        private BluetoothDeviceData mSelectedDeviceData;
        private PeripheralList mPeripheralList;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

    }

    private void restoreRetainedDataFragment() {
        // find the retained fragment
        FragmentManager fm = getActivity().getFragmentManager();
        mRetainedDataFragment = (DataFragment) fm.findFragmentByTag(TAG);

        if (mRetainedDataFragment == null) {
            // Create
            mRetainedDataFragment = new DataFragment();
            fm.beginTransaction().add(mRetainedDataFragment, TAG).commitAllowingStateLoss();
            // http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-h

            mScannedDevices =  new ArrayList<BluetoothDeviceData>();
            mPeripheralList = new PeripheralList();

        } else {
            // Restore status
            mScannedDevices = mRetainedDataFragment.mScannedDevices;
            mComponentToStartWhenConnected = mRetainedDataFragment.mComponentToStartWhenConnected;
            mShouldEnableWifiOnQuit = mRetainedDataFragment.mShouldEnableWifiOnQuit;
            mLatestCheckedDeviceAddress = mRetainedDataFragment.mLatestCheckedDeviceAddress;
            mSelectedDeviceData = mRetainedDataFragment.mSelectedDeviceData;
            mPeripheralList = mRetainedDataFragment.mPeripheralList;

        }
    }

    private void saveRetainedDataFragment() {
        mRetainedDataFragment.mScannedDevices = mScannedDevices;
        mRetainedDataFragment.mComponentToStartWhenConnected = mComponentToStartWhenConnected;
        mRetainedDataFragment.mShouldEnableWifiOnQuit = mShouldEnableWifiOnQuit;
        mRetainedDataFragment.mLatestCheckedDeviceAddress = mLatestCheckedDeviceAddress;
        mRetainedDataFragment.mSelectedDeviceData = mSelectedDeviceData;
        mRetainedDataFragment.mPeripheralList = mPeripheralList;
    }

}
