package com.yakovlaptev.demon.actionlisteners;
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
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.yakovlaptev.demon.Configuration;
import com.yakovlaptev.demon.TabFragment;
import com.yakovlaptev.demon.services.ServiceList;
import com.yakovlaptev.demon.services.WiFiP2pService;
import com.yakovlaptev.demon.services.WiFiP2pServicesFragment;
import com.yakovlaptev.demon.services.WiFiServicesAdapter;

/**
 * A custom Bonjour's DnsSdServiceResponseListener used to update the UI when a service is available.
 * <p></p>
 * This class use Bonjour Prot
 * <p></p>
 * Created by Stefano Cappa on 28/02/15.
 */
public class CustomDnsServiceResponseListener implements WifiP2pManager.DnsSdServiceResponseListener {

    private static final String TAG = "DnsRespListener";

    @Override
    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
        // A service has been discovered. Is this our app?
        if (instanceName.equalsIgnoreCase(Configuration.SERVICE_INSTANCE)) {

            // update the UI and add the item the discovered device.
            WiFiP2pServicesFragment fragment = TabFragment.getWiFiP2pServicesFragment();
            if (fragment != null) {
                WiFiServicesAdapter adapter = fragment.getMAdapter();
                WiFiP2pService service = new WiFiP2pService();
                service.setDevice(srcDevice);
                service.setInstanceName(instanceName);
                service.setServiceRegistrationType(registrationType);

                ServiceList.getInstance().addServiceIfNotPresent(service);

                if (adapter != null) {
                    adapter.notifyItemInserted(ServiceList.getInstance().getSize() - 1);
                }
                Log.d(TAG, "onDnsSdServiceAvailable " + instanceName);
            }
        }
    }
}
