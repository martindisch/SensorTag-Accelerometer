package com.martindisch.accelerometer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Adapter for items containing the MAC address of a BLE device.
 */
public class DeviceRecyclerViewAdapter extends RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder> {

    private ArrayList<String> mAddresses;
    private final ScanFragment.OnListFragmentInteractionListener mListener;

    public DeviceRecyclerViewAdapter(ScanFragment.OnListFragmentInteractionListener listener) {
        mAddresses = new ArrayList<>();
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mAddress.setText(mAddresses.get(position));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onListFragmentInteraction(mAddresses.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAddresses.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mAddress;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mAddress = (TextView) view.findViewById(R.id.tvAddress);
        }
    }

    /**
     * Inserts the address into the list if it doesn't yet exist.
     *
     * @param address the MAC address of the new device
     */
    public void addDevice(String address) {
        // after the first device has been discovered, disable the spinning
        // status indicator that is partly hiding the element
        if (mAddresses.size() == 0) mListener.onHideProgress();

        // add the device to list if it doesn't exist
        if (!mAddresses.contains(address)) {
            mAddresses.add(address);
            notifyItemInserted(mAddresses.indexOf(address));
        }
    }
}
