package com.pritesh.ritintercom.activities;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pritesh.ritintercom.data.DeviceData;

import com.pritesh.ritintercom.R;

import java.util.List;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.ViewHolder> {

    private final List<DeviceData> mDevices;
    private final PeerListFragment.OnListFragmentInteractionListener mListener;

    public PeerListAdapter(List<DeviceData> devices, PeerListFragment.OnListFragmentInteractionListener
            listener) {
        mDevices = devices;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.deviceData = mDevices.get(position);
        holder.mContentView.setText(mDevices.get(position).getPlayerName() + "-" + mDevices.get
                (position).getDeviceName());

        holder.ip.setText(mDevices.get(position).getIp() + "-" + mDevices.get
                (position).getPort());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.deviceData);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mContentView;
        public final TextView ip;

        public DeviceData deviceData;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = (TextView) view.findViewById(R.id.device_layout).findViewById(R.id.device_name);
            ip = (TextView) view.findViewById(R.id.device_layout).findViewById(R.id.device_ip);

        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
