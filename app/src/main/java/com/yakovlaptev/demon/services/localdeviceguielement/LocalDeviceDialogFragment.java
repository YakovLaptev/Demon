package com.yakovlaptev.demon.services.localdeviceguielement;
/*
 * Copyright (C) 2015-2016 Stefano Cappa
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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.yakovlaptev.R;
import com.yakovlaptev.demon.data.Profile;
import com.yakovlaptev.demon.model.LocalP2PDevice;

/**
 * Class that represents the DialogFragment to change the local device name, not only in the GUI
 * but also to be discoverable with this new name, by other devices.
 * <p></p>
 * Created by Stefano Cappa on 16/02/15.
 */
public class LocalDeviceDialogFragment extends DialogFragment {

    private Button confirmButton;
    private EditText deviceNameEditText;
    private EditText deviceEmailEditText;
    private Profile profile;


    /**
     * {@link com.yakovlaptev.demon.services.WiFiP2pServicesFragment} implements this interface.
     * But the method to change the device name in
     */
    public interface DialogConfirmListener {
        public void changeLocalDeviceName(Profile profile);
    }

    /**
     * Method to obtain a new Fragment's instance.
     * @return This Fragment instance.
     */
    public static LocalDeviceDialogFragment newInstance() {
        return new LocalDeviceDialogFragment();
    }

    /**
     * Default Fragment constructor.
     */
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

        //getDialog().setTitle(getResources().getString(R.string.choose_device_name));
        deviceNameEditText = (EditText) v.findViewById(R.id.deviceNameEditText);
        deviceEmailEditText = (EditText) v.findViewById(R.id.deviceEmailEditText);
        confirmButton = (Button) v.findViewById(R.id.confirmButton);

        profile = LocalP2PDevice.getInstance().getProfile();
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
