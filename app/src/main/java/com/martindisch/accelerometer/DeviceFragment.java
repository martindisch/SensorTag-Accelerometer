package com.martindisch.accelerometer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.UUID;

/**
 * Fragment showing data for a connected device.
 */
public class DeviceFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_ADDRESS = "address";
    private String mAddress;
    private boolean mIsRecording = false;
    private LinkedList<Measurement> mRecording;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private BluetoothGattService mMovService;
    private BluetoothGattCharacteristic mRead, mEnable, mPeriod;

    private TextView mXAxis, mYAxis, mZAxis;
    private Button mStart, mStop, mExport;

    /**
     * Mandatory empty constructor.
     */
    public DeviceFragment() {
    }

    /**
     * Returns a new instance of this Fragment.
     *
     * @param address the MAC address of the device to connect
     * @return A new instance of {@link DeviceFragment}
     */
    public static DeviceFragment newInstance(String address) {
        DeviceFragment fragment = new DeviceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ADDRESS, address);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAddress = getArguments().getString(ARG_ADDRESS);
        }

        // initialize bluetooth manager & adapter
        BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        connectDevice(mAddress);
    }

    @Override
    public void onPause() {
        stopRecording();
        mGatt.disconnect();
        super.onPause();
    }

    /**
     * Creates a GATT connection to the given device.
     *
     * @param address String containing the address of the device
     */
    private void connectDevice(String address) {
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getActivity(), R.string.state_off, Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mGatt = device.connectGatt(getActivity(), false, mCallback);
    }

    private BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        double result[];
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault());

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    // as soon as we're connected, discover services
                    mGatt.discoverServices();
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    // TODO: return to ScanFragment
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // as soon as services are discovered, acquire characteristic and try enabling
            mMovService = mGatt.getService(UUID.fromString("F000AA80-0451-4000-B000-000000000000"));
            mEnable = mMovService.getCharacteristic(UUID.fromString("F000AA82-0451-4000-B000-000000000000"));
            if (mEnable == null) {
                Toast.makeText(getActivity(), R.string.service_not_found, Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
            /**
             * Bits starting with the least significant bit (the rightmost one)
             * 0       Gyroscope z axis enable
             * 1       Gyroscope y axis enable
             * 2       Gyroscope x axis enable
             * 3       Accelerometer z axis enable
             * 4       Accelerometer y axis enable
             * 5       Accelerometer x axis enable
             * 6       Magnetometer enable (all axes)
             * 7       Wake-On-Motion Enable
             * 8:9	    Accelerometer range (0=2G, 1=4G, 2=8G, 3=16G)
             * 10:15   Not used
             */
            mEnable.setValue(0b1000111000, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            mGatt.writeCharacteristic(mEnable);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (characteristic == mEnable) {
                // if enable was successful, set the sensor period to the lowest value
                mPeriod = mMovService.getCharacteristic(UUID.fromString("F000AA83-0451-4000-B000-000000000000"));
                if (mPeriod == null) {
                    Toast.makeText(getActivity(), R.string.service_not_found, Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }
                mPeriod.setValue(0x0A, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                mGatt.writeCharacteristic(mPeriod);
            } else if (characteristic == mPeriod) {
                // if setting sensor period was successful, start polling for sensor values
                mRead = mMovService.getCharacteristic(UUID.fromString("F000AA81-0451-4000-B000-000000000000"));
                if (mRead == null) {
                    Toast.makeText(getActivity(), R.string.characteristic_not_found, Toast.LENGTH_LONG).show();
                    getActivity().finish();
                }
                mGatt.readCharacteristic(mRead);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            // convert raw byte array to G unit values for xyz axes
            result = Util.convertAccel(characteristic.getValue());
            if (mIsRecording) {
                Measurement measurement = new Measurement(result[0], result[1], result[2], formatter.format(Calendar.getInstance().getTime()));
                mRecording.add(measurement);
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()) {
                        // update current acceleration readings
                        mXAxis.setText(String.format(getString(R.string.xAxis), Math.abs(result[0])));
                        mYAxis.setText(String.format(getString(R.string.yAxis), Math.abs(result[1])));
                        mZAxis.setText(String.format(getString(R.string.zAxis), Math.abs(result[2])));
                        mXAxis.setTextColor(ContextCompat.getColor(getActivity(), result[0] < 0 ? R.color.red_light : R.color.green_light));
                        mYAxis.setTextColor(ContextCompat.getColor(getActivity(), result[1] < 0 ? R.color.red_light : R.color.green_light));
                        mZAxis.setTextColor(ContextCompat.getColor(getActivity(), result[2] < 0 ? R.color.red_light : R.color.green_light));
                    }
                }
            });
            // poll for next values
            mGatt.readCharacteristic(mRead);
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bStart:
                startRecording();
                break;
            case R.id.bStop:
                stopRecording();
                break;
            case R.id.bExport:
                // TODO: create CSV data and share intent
                break;
        }
    }

    private void startRecording() {
        // update UI
        mStart.setEnabled(false);
        mExport.setEnabled(false);
        mStop.setEnabled(true);
        mIsRecording = true;

        mRecording = new LinkedList<>();
    }

    private void stopRecording() {
        // update UI
        mStop.setEnabled(false);
        mStart.setEnabled(true);
        mExport.setEnabled(true);
        mIsRecording = false;

        // TODO: update graph
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_device, container, false);
        mXAxis = (TextView) layout.findViewById(R.id.tvXAxis);
        mYAxis = (TextView) layout.findViewById(R.id.tvYAxis);
        mZAxis = (TextView) layout.findViewById(R.id.tvZAxis);
        mStart = (Button) layout.findViewById(R.id.bStart);
        mStop = (Button) layout.findViewById(R.id.bStop);
        mExport = (Button) layout.findViewById(R.id.bExport);

        mStart.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mExport.setOnClickListener(this);
        return layout;
    }
}
