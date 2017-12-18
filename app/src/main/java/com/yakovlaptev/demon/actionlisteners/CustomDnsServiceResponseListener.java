package com.yakovlaptev.demon.actionlisteners;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.yakovlaptev.demon.Configuration;
import com.yakovlaptev.demon.TabFragment;
import com.yakovlaptev.demon.services.ServiceList;
import com.yakovlaptev.demon.services.WiFiP2pService;
import com.yakovlaptev.demon.services.WiFiP2pServicesFragment;
import com.yakovlaptev.demon.services.WiFiServicesAdapter;

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
