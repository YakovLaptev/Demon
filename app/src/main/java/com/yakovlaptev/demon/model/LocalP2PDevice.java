package com.yakovlaptev.demon.model;

import android.net.wifi.p2p.WifiP2pDevice;

import com.yakovlaptev.demon.data.Profile;

import lombok.Getter;
import lombok.Setter;

public class LocalP2PDevice {

    @Getter @Setter private WifiP2pDevice localDevice;
    @Getter @Setter private Profile profile;

    private static final LocalP2PDevice instance = new LocalP2PDevice();

    public static LocalP2PDevice getInstance() {
        return instance;
    }

    private LocalP2PDevice(){
        profile = new Profile();
        localDevice = new WifiP2pDevice();
    }

}
