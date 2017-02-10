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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

/**
 * Fragment showing data for a connected device.
 */
public class DeviceFragment extends Fragment {

    private static final String ARG_ADDRESS = "address";
    private String mAddress;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private BluetoothGattService mMovService;
    private BluetoothGattCharacteristic mRead;

    private TextView mHex;

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
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
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
            mMovService = mGatt.getService(UUID.fromString("F000AA80-0451-4000-B000-000000000000"));
            BluetoothGattCharacteristic enable = mMovService.getCharacteristic(UUID.fromString("F000AA82-0451-4000-B000-000000000000"));
            if (enable == null) {
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
            enable.setValue(0b1000111000, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            mGatt.writeCharacteristic(enable);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            mRead = mMovService.getCharacteristic(UUID.fromString("F000AA81-0451-4000-B000-000000000000"));
            if (mRead == null) {
                Toast.makeText(getActivity(), R.string.characteristic_not_found, Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
            mGatt.readCharacteristic(mRead);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            double[] result = Util.convertAccel(characteristic.getValue());
            final String text = "AccelX: " + result[0] + " AccelY: " + result[1] + " AccelZ: " + result[2];
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mHex.setText(text);
                }
            });
            mGatt.readCharacteristic(mRead);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_device, container, false);
        mHex = (TextView) layout.findViewById(R.id.tvHex);
        return layout;
    }

}
