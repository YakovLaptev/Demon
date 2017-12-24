package com.yakovlaptev.demon.services.localdeviceguielement;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.yakovlaptev.R;
import com.yakovlaptev.demon.data.ImageConverter;
import com.yakovlaptev.demon.data.Profile;
import com.yakovlaptev.demon.model.LocalP2PDevice;

public class LocalDeviceDialogFragment extends DialogFragment {

    private Button confirmButton;
    private EditText deviceNameEditText;
    private EditText deviceEmailEditText;
    private Profile profile;

    public interface DialogConfirmListener {
        public void changeLocalDeviceName(Profile profile);
    }

    public static LocalDeviceDialogFragment newInstance() {
        return new LocalDeviceDialogFragment();
    }

    public LocalDeviceDialogFragment() {}


    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setOnDismissListener(null);
        }
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog, container, false);

        getDialog().setTitle(getResources().getString(R.string.choose_device_name));
        deviceNameEditText = (EditText) v.findViewById(R.id.deviceNameEditText);
        deviceEmailEditText = (EditText) v.findViewById(R.id.deviceEmailEditText);
        ImageView avatar = (ImageView) v.findViewById(R.id.imageViewAvatar);
        confirmButton = (Button) v.findViewById(R.id.confirmButton);

        profile = LocalP2PDevice.getInstance().getProfile();

        avatar.setImageDrawable(new BitmapDrawable(getResources(), ImageConverter.convertToBitmap(profile.getAvatar())));
        deviceNameEditText.setText(profile.getName());
        deviceEmailEditText.setText(profile.getEmail());

        //set listener to call changeLocalDeviceName in WiFiP2pServicesFragment, after a click on confirmButton
        this.setListener();

        return v;
    }

    private void setListener() {
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                profile.setName(deviceNameEditText.getText().toString());
                profile.setEmail(deviceEmailEditText.getText().toString());
                ((DialogConfirmListener)getTargetFragment()).changeLocalDeviceName(profile);
                dismiss();
            }
        });
    }
}
