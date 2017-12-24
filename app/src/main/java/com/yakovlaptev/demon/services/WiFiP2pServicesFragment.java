
package com.yakovlaptev.demon.services;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.yakovlaptev.R;
import com.yakovlaptev.demon.MainActivity;
import com.yakovlaptev.demon.SplashScreen;
import com.yakovlaptev.demon.data.ImageConverter;
import com.yakovlaptev.demon.data.Profile;
import com.yakovlaptev.demon.model.LocalP2PDevice;
import com.yakovlaptev.demon.services.localdeviceguielement.LocalDeviceDialogFragment;

import java.util.Arrays;

import lombok.Getter;

public class WiFiP2pServicesFragment extends Fragment implements
        //ItemClickListener is the interface in the adapter to intercept item's click events.
        //I use this to call itemClicked(v) in this class from WiFiServicesAdapter.
        WiFiServicesAdapter.ItemClickListener,
        //DialogConfirmListener is the interface in LocalDeviceDialogFragment. I use this to call
        //public void changeLocalDeviceName(String deviceName) in this class from the DialogFragment without to pass attributes or other stuff
        LocalDeviceDialogFragment.DialogConfirmListener {

    private static final String TAG = "WiFiP2pServicesFragment";

    private RecyclerView mRecyclerView;
    @Getter private WiFiServicesAdapter mAdapter;

    private TextView localDeviceNameText;
    private TextView localDeviceEmailText;

    public interface DeviceClickListener {
        public void tryToConnectToAService(int position);
    }

    public static WiFiP2pServicesFragment newInstance() {
        return new WiFiP2pServicesFragment();
    }

    public WiFiP2pServicesFragment() {}

    @Override
    public void changeLocalDeviceName(Profile profile) {
        if(profile.getName()==null || profile.getEmail()==null) {
            return;
        }
        LocalP2PDevice.getInstance().setProfile(profile);
        localDeviceNameText.setText(profile.getName());
        localDeviceEmailText.setText(profile.getEmail());

        ((MainActivity)getActivity()).changeProfile(profile);
    }

    @Override
    public void itemClicked(View view) {
        int clickedPosition = mRecyclerView.getChildPosition(view);

        if(clickedPosition>=0) {
            ((DeviceClickListener) getActivity()).tryToConnectToAService(clickedPosition);
        }
    }

    public void showLocalDeviceGoIcon(){
        if(getView() !=null && getView().findViewById(R.id.go_logo)!=null && getView().findViewById(R.id.i_am_a_go_textview)!=null) {
            ImageView goLogoImageView = (ImageView) getView().findViewById(R.id.go_logo);
            TextView i_am_a_go_textView = (TextView) getView().findViewById(R.id.i_am_a_go_textview);

            //goLogoImageView.setImageDrawable(getResources().getDrawable(R.drawable.go_logo));
            //goLogoImageView.setVisibility(View.VISIBLE);
            //i_am_a_go_textView.setVisibility(View.VISIBLE);
        }
    }

    public void resetLocalDeviceIpAddress() {
        if(getView()!=null && getView().findViewById(R.id.localDeviceIpAddress)!=null) {
            TextView ipAddress = (TextView) getView().findViewById(R.id.localDeviceIpAddress);
            ipAddress.setText(getResources().getString(R.string.ip_not_available));
        }
    }

    public void setLocalDeviceIpAddress(String ipAddress) {
        if(getView()!=null && getView().findViewById(R.id.localDeviceIpAddress)!=null) {
            TextView ipAddressTextView = (TextView) getView().findViewById(R.id.localDeviceIpAddress);
            ipAddressTextView.setText(ipAddress);
        }
    }

    public void hideLocalDeviceGoIcon() {
        if(getView()!=null && getView().findViewById(R.id.go_logo)!=null && getView().findViewById(R.id.i_am_a_go_textview)!=null) {
            ImageView goLogoImageView = (ImageView) getView().findViewById(R.id.go_logo);
            TextView i_am_a_go_textView = (TextView) getView().findViewById(R.id.i_am_a_go_textview);

            goLogoImageView.setVisibility(View.INVISIBLE);
            i_am_a_go_textView.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.services_list, container, false);
        rootView.setTag(TAG);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // allows for optimizations if all item views are of the same size:
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new WiFiServicesAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        localDeviceNameText = (TextView) rootView.findViewById(R.id.localDeviceName);
        localDeviceEmailText = (TextView) rootView.findViewById(R.id.localDeviceEmail);
        ImageView avatar = (ImageView) rootView.findViewById(R.id.imageView);

        SplashScreen.DBHelper dbHelper = SplashScreen.dbHelper;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query("profile", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            int nameColIndex = cursor.getColumnIndex("name");
            int emailColIndex = cursor.getColumnIndex("email");
            int avatarColIndex = cursor.getColumnIndex("avatar");
            Profile profile = new Profile();
            profile.setName(cursor.getString(nameColIndex));
            profile.setEmail(cursor.getString(emailColIndex));
            if(cursor.getBlob(avatarColIndex).length == 0) {
                profile.setAvatar(new byte[]{});
            } else {
                profile.setAvatar(cursor.getBlob(avatarColIndex));
            }
            LocalP2PDevice.getInstance().setProfile(profile);
            localDeviceNameText.setText(profile.getName());
            localDeviceEmailText.setText(profile.getEmail());
            Log.d("-----", "avatar = " + Arrays.toString(profile.getAvatar()));
            if(profile.getAvatar().length == 0) {
                avatar.setImageDrawable(getResources().getDrawable(R.drawable.android_logo_device));
            } else {
                avatar.setImageBitmap(ImageConverter.convertToBitmap(profile.getAvatar()));
            }
        } else {
            localDeviceNameText.setText(LocalP2PDevice.getInstance().getLocalDevice().deviceName);
        }
        dbHelper.close();
        cursor.close();



        TextView localDeviceAddressText = (TextView) rootView.findViewById(R.id.localDeviceAddress);
        localDeviceAddressText.setText(LocalP2PDevice.getInstance().getLocalDevice().deviceAddress);

        CardView cardViewLocalDevice = (CardView) rootView.findViewById(R.id.cardviewLocalDevice);
        cardViewLocalDevice.setOnClickListener(new OnClickListenerLocalDevice(this));

        return rootView;
    }

    class OnClickListenerLocalDevice implements View.OnClickListener {

        private final Fragment fragment;

        public OnClickListenerLocalDevice(Fragment fragment1) {
            fragment = fragment1;
        }

        @Override
        public void onClick(View v) {
            LocalDeviceDialogFragment localDeviceDialogFragment = (LocalDeviceDialogFragment) getFragmentManager()
                    .findFragmentByTag("localDeviceDialogFragment");

            if (localDeviceDialogFragment == null) {
                localDeviceDialogFragment = LocalDeviceDialogFragment.newInstance();
                localDeviceDialogFragment.setTargetFragment(fragment, 0);

                localDeviceDialogFragment.show(getFragmentManager(), "localDeviceDialogFragment");
                getFragmentManager().executePendingTransactions();
            }
        }
    }
}


