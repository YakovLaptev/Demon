
package com.yakovlaptev.demon.services;

import android.net.wifi.p2p.WifiP2pDevice;

import com.yakovlaptev.demon.data.Profile;

import lombok.Getter;
import lombok.Setter;

public class WiFiP2pService {
    @Getter @Setter private WifiP2pDevice device;
    @Getter @Setter private Profile profile;
    @Getter @Setter private String instanceName = null;
    @Getter @Setter private String serviceRegistrationType = null;
}
