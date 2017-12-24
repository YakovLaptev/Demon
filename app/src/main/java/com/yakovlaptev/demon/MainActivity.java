package com.yakovlaptev.demon;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.yakovlaptev.R;
import com.yakovlaptev.demon.actionlisteners.CustomDnsSdTxtRecordListener;
import com.yakovlaptev.demon.actionlisteners.CustomDnsServiceResponseListener;
import com.yakovlaptev.demon.actionlisteners.CustomizableActionListener;
import com.yakovlaptev.demon.chatmessages.WiFiChatFragment;
import com.yakovlaptev.demon.chatmessages.waitingtosend.WaitingToSendQueue;
import com.yakovlaptev.demon.data.DBManager;
import com.yakovlaptev.demon.data.ImageConverter;
import com.yakovlaptev.demon.data.Profile;
import com.yakovlaptev.demon.model.LocalP2PDevice;
import com.yakovlaptev.demon.model.P2pDestinationDevice;
import com.yakovlaptev.demon.services.ServiceList;
import com.yakovlaptev.demon.services.WiFiP2pService;
import com.yakovlaptev.demon.services.WiFiP2pServicesFragment;
import com.yakovlaptev.demon.services.WiFiServicesAdapter;
import com.yakovlaptev.demon.socketmanagers.ChatManager;
import com.yakovlaptev.demon.socketmanagers.ClientSocketHandler;
import com.yakovlaptev.demon.socketmanagers.GroupOwnerSocketHandler;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import static com.yakovlaptev.demon.SplashScreen.dbHelper;

public class MainActivity extends ActionBarActivity implements
        WiFiP2pServicesFragment.DeviceClickListener,
        WiFiChatFragment.AutomaticReconnectionListener,
        Handler.Callback,
        ConnectionInfoListener {

    private static final String TAG = "MainActivity";
    private boolean retryChannel = false;
    @Setter
    private boolean connected = false;
    @Getter
    private int tabNum = 1;
    @Getter
    @Setter
    private boolean blockForcedDiscoveryInBroadcastReceiver = false;
    private boolean discoveryStatus = true;

    @Getter
    private TabFragment tabFragment;
    @Getter
    @Setter
    private Toolbar toolbar;

    private WifiP2pManager manager;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private Channel channel;

    private final IntentFilter intentFilter = new IntentFilter();
    private BroadcastReceiver receiver = null;

    private Thread socketHandler;
    private final Handler handler = new Handler(this);

    private ChatManager chatManager;

    private WifiP2pDevice p2pDevice;
    private P2pDestinationDevice device;

    Handler getHandler() {
        return handler;
    }

    @Override
    public void reconnectToService(WiFiP2pService service) {
        if (service != null) {
            Log.d(TAG, "reconnectToService called");

            //Finally, add device to the DeviceTabList, only if required.
            //Go to addDeviceIfRequired()'s javadoc for more informations.
            DestinationDeviceTabList.getInstance().addDeviceIfRequired(new P2pDestinationDevice(service.getDevice()));

            this.connectP2p(service);
        }
    }

    private void forcedCancelConnect() {
        manager.cancelConnect(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "forcedCancelConnect success");
                Toast.makeText(MainActivity.this, "Cancel connect success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "forcedCancelConnect failed, reason: " + reason);
                Toast.makeText(MainActivity.this, "Cancel connect failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void forceDiscoveryStop() {
        if (discoveryStatus) {
            discoveryStatus = false;

            ServiceList.getInstance().clear();
            toolbar.getMenu().findItem(R.id.discovery).setIcon(getResources().getDrawable(R.drawable.ic_action_search_stopped));

            this.internalStopDiscovery();
        }
    }

    private void internalStopDiscovery() {
        manager.stopPeerDiscovery(channel,
                new CustomizableActionListener(
                        MainActivity.this,
                        "internalStopDiscovery",
                        "Discovery stopped",
                        "Discovery stopped",
                        "Discovery stop failed",
                        "Discovery stop failed"));
        manager.clearServiceRequests(channel,
                new CustomizableActionListener(
                        MainActivity.this,
                        "internalStopDiscovery",
                        "ClearServiceRequests success",
                        null,
                        "Discovery stop failed",
                        null));
        manager.clearLocalServices(channel,
                new CustomizableActionListener(
                        MainActivity.this,
                        "internalStopDiscovery",
                        "ClearLocalServices success",
                        null,
                        "clearLocalServices failure",
                        null));
    }

    public void restartDiscovery() {
        discoveryStatus = true;

        //starts a new registration, restarts discovery and updates the gui
        this.startRegistration();
        this.discoverService();
        this.updateServiceAdapter();
    }

    private void discoverService() {

        ServiceList.getInstance().clear();

        toolbar.getMenu().findItem(R.id.discovery).setIcon(getResources().getDrawable(R.drawable.ic_action_search_searching));

        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
        manager.setDnsSdResponseListeners(channel,
                new CustomDnsServiceResponseListener(), new CustomDnsSdTxtRecordListener());

        // After attaching listeners, create a service request and initiate
        // discovery.
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();

        //inititiates discovery
        manager.addServiceRequest(channel, serviceRequest,
                new CustomizableActionListener(
                        MainActivity.this,
                        "discoverService",
                        "Added service discovery request",
                        null,
                        "Failed adding service discovery request",
                        "Failed adding service discovery request"));

        //starts services disovery
        manager.discoverServices(channel, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discovery initiated");
                Toast.makeText(MainActivity.this, "Service discovery initiated", Toast.LENGTH_SHORT).show();
                blockForcedDiscoveryInBroadcastReceiver = false;
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Service discovery failed");
                Toast.makeText(MainActivity.this, "Service discovery failed, " + reason, Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void updateServiceAdapter() {
        WiFiP2pServicesFragment fragment = TabFragment.getWiFiP2pServicesFragment();
        if (fragment != null) {
            WiFiServicesAdapter adapter = fragment.getMAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void disconnectBecauseOnStop() {

        this.closeAndKillSocketHandler();

        this.setDisableAllChatManagers();

        this.addColorActiveTabs(true);

        if (manager != null && channel != null) {

            manager.removeGroup(channel,
                    new CustomizableActionListener(
                            MainActivity.this,
                            "disconnectBecauseOnStop",
                            "Disconnected",
                            "Disconnected",
                            "Disconnect failed",
                            "Disconnect failed"));
        } else {
            Log.d("disconnectBecauseOnStop", "Impossible to disconnect");
        }
    }

    private void closeAndKillSocketHandler() {
        if (socketHandler instanceof GroupOwnerSocketHandler) {
            ((GroupOwnerSocketHandler) socketHandler).closeSocketAndKillThisThread();
        } else if (socketHandler instanceof ClientSocketHandler) {
            ((ClientSocketHandler) socketHandler).closeSocketAndKillThisThread();
        }
    }

    private void forceDisconnectAndStartDiscovery() {
        //When BroadcastReceiver gets the disconnect's notification, this method will be executed two times.
        //For this reason, i use a boolean called blockForcedDiscoveryInBroadcastReceiver to check if i
        //need to call this method from BroadcastReceiver or not.
        this.blockForcedDiscoveryInBroadcastReceiver = true;

        this.closeAndKillSocketHandler();

        this.setDisableAllChatManagers();

        if (manager != null && channel != null) {

            manager.removeGroup(channel, new ActionListener() {
                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                    Toast.makeText(MainActivity.this, "Disconnect failed", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "Disconnected");
                    Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();

                    Log.d(TAG, "Discovery status: " + discoveryStatus);

                    forceDiscoveryStop();
                    restartDiscovery();
                }

            });
        } else {
            Log.d(TAG, "Disconnect impossible");
        }
    }

    private void startRegistration() {
        Map<String, String> record = new HashMap<>();
        record.put(Configuration.TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                Configuration.SERVICE_INSTANCE, Configuration.SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service,
                new CustomizableActionListener(
                        MainActivity.this,
                        "startRegistration",
                        "Added Local Service",
                        null,
                        "Failed to add a service",
                        "Failed to add a service"));
    }

    private void connectP2p(WiFiP2pService service) {
        Log.d(TAG, "connectP2p, tabNum before = " + tabNum);

        if (DestinationDeviceTabList.getInstance().containsElement(new P2pDestinationDevice(service.getDevice()))) {
            this.tabNum = DestinationDeviceTabList.getInstance().indexOfElement(new P2pDestinationDevice(service.getDevice())) + 1;
        }

        if (this.tabNum == -1) {
            Log.d("ERROR", "ERROR TABNUM=-1"); //only for testing purposes.
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.getDevice().deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0; //because i want that this device is the client. Attention, sometimes can be a GO, also if i used 0 here.

        if (serviceRequest != null) {
            manager.removeServiceRequest(channel, serviceRequest,
                    new CustomizableActionListener(
                            MainActivity.this,
                            "connectP2p",
                            null,
                            "RemoveServiceRequest success",
                            null,
                            "removeServiceRequest failed"));
        }

        manager.connect(channel, config,
                new CustomizableActionListener(
                        MainActivity.this,
                        "connectP2p",
                        null,
                        "Connecting to service",
                        null,
                        "Failed connecting to service"));
    }

    public void tryToConnectToAService(int position) {
        WiFiP2pService service = ServiceList.getInstance().getElementByPosition(position);

        //if connected, force disconnect and restart discovery phase.
        if (connected) {
            this.forceDisconnectAndStartDiscovery();
        }

        //Finally, add device to the DeviceTabList, only if required.
        //Go to addDeviceIfRequired()'s javadoc for more informations.
        DestinationDeviceTabList.getInstance().addDeviceIfRequired(new P2pDestinationDevice(service.getDevice()));

        this.connectP2p(service);
    }

    private void sendAddress(String deviceMacAddress, Profile profile, ChatManager chatManager) {
        if (chatManager != null) {
            InetAddress ipAddress;
            if (socketHandler instanceof GroupOwnerSocketHandler) {
                ipAddress = ((GroupOwnerSocketHandler) socketHandler).getIpAddress();
                Log.d(TAG, "sending message with MAGICADDRESSKEYWORD, with ipaddress= " + ipAddress.getHostAddress());
                chatManager.write((Configuration.PLUSSYMBOLS + Configuration.MAGICADDRESSKEYWORD +
                        "___" + deviceMacAddress +
                        "___" + profile.getName() +
                        "___" + profile.getEmail() +
                        "___" + ipAddress.getHostAddress()).getBytes());
                chatManager.write((Configuration.PLUSSYMBOLS + Configuration.MAGICADDRESSKEYWORD +
                        "___" + profile.getAvatar()).getBytes());
            } else {
                Log.d(TAG, "sending message with MAGICADDRESSKEYWORD, without ipaddress");
                //i use "+" symbols as initial spacing to be sure that also if some initial character will be lost i'll have always
                //the Configuration.MAGICADDRESSKEYWORD and i can set the associated device to the correct WifiChatFragment.
                chatManager.write((Configuration.PLUSSYMBOLS + Configuration.MAGICADDRESSKEYWORD +
                        "___" + deviceMacAddress +
                        "___" + profile.getName() +
                        "___" + profile.getEmail()).getBytes());
                chatManager.write((Configuration.PLUSSYMBOLS + Configuration.MAGICADDRESSKEYWORD +
                        "___" + profile.getAvatar()).getBytes());
            }
        }
    }

    public void setDisableAllChatManagers() {
        for (WiFiChatFragment chatFragment : TabFragment.getWiFiChatFragmentList()) {
            if (chatFragment != null && chatFragment.getChatManager() != null) {
                chatFragment.getChatManager().setDisable(true);
            }
        }
    }

    public void setTabFragmentToPage(int numPage) {
        TabFragment tabfrag1 = ((TabFragment) getSupportFragmentManager().findFragmentByTag("tabfragment"));
        if (tabfrag1 != null && tabfrag1.getMViewPager() != null) {
            tabfrag1.getMViewPager().setCurrentItem(numPage);
        }
    }

    public void addColorActiveTabs(boolean grayScale) {
        Log.d(TAG, "addColorActiveTabs() called, tabNum= " + tabNum);

        if (tabFragment.isValidTabNum(tabNum) && tabFragment.getChatFragmentByTab(tabNum) != null) {
            tabFragment.getChatFragmentByTab(tabNum).setGrayScale(grayScale);
            tabFragment.getChatFragmentByTab(tabNum).updateChatMessageListAdapter();
        }
    }

    public void setDeviceNameWithReflection(String deviceName) {
        try {
            Method m = manager.getClass().getMethod(
                    "setDeviceName",
                    Channel.class, String.class,
                    ActionListener.class);

            m.invoke(manager, channel, deviceName,
                    new CustomizableActionListener(
                            MainActivity.this,
                            "setDeviceNameWithReflection",
                            "Device name changed",
                            "Device name changed",
                            "Error, device name not changed",
                            "Error, device name not changed"));
        } catch (Exception e) {
            Log.e(TAG, "Exception during setDeviceNameWithReflection", e);
            Toast.makeText(MainActivity.this, "Impossible to change the device name", Toast.LENGTH_SHORT).show();
        }
    }

    public void changeProfile(Profile profile) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String strFilter = "_id=" + 1;
        ContentValues cv = new ContentValues();
        cv.put("name", profile.getName());
        cv.put("email", profile.getEmail());
        String LOG_TAG = "Logs";
        Log.d(LOG_TAG, "name = " + profile.getName() + "  email = " + profile.getEmail());
        db.update("profile", cv, strFilter, null);
        dbHelper.close();
        setDeviceNameWithReflection(profile.getName());
    }

    public void changeAvatar(View view) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Bitmap bitmap = null;
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        //ImageView imageView2 = (ImageView) findViewById(R.id.imageViewAvatar);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    imageView.setImageBitmap(bitmap);
                    DBManager.dbInsertAvatar(ImageConverter.convertToBase64(bitmap));
                    // Log.d("-----", str);
                    //imageView2.setImageBitmap(bitmap);
                }
        }
    }

    private void setupToolBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(getResources().getString(R.string.app_name));
            toolbar.setTitleTextColor(Color.WHITE);
            toolbar.inflateMenu(R.menu.action_items);
            this.setSupportActionBar(toolbar);
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                Log.d(TAG, "socketHandler!=null? = " + (socketHandler != null));
                socketHandler = new GroupOwnerSocketHandler(this.getHandler());
                socketHandler.start();

                //set Group Owner ip address
                TabFragment.getWiFiP2pServicesFragment().setLocalDeviceIpAddress(p2pInfo.groupOwnerAddress.getHostAddress());

                //if this device is the Group Owner, i sets the GO's
                //ImageView of the cardview inside the WiFiP2pServicesFragment.
                //TabFragment.getWiFiP2pServicesFragment().showLocalDeviceGoIcon();

            } catch (IOException e) {
                Log.e(TAG, "Failed to create a server thread - " + e);
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            socketHandler = new ClientSocketHandler(this.getHandler(), p2pInfo.groupOwnerAddress);
            socketHandler.start();

            //if this device is the Group Owner, i set the GO's ImageView
            //of the cardview inside the WiFiP2pServicesFragment.
            TabFragment.getWiFiP2pServicesFragment().hideLocalDeviceGoIcon();
        }

        Log.d(TAG, "onConnectionInfoAvailable setTabFragmentToPage with tabNum == " + tabNum);

        this.setTabFragmentToPage(tabNum);
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.d(TAG, "handleMessage, tabNum in this activity is: " + tabNum);
        boolean flag = false;

        switch (msg.what) {
            //called by every device at the beginning of every connection (new or previously removed and now recreated)
            case Configuration.FIRSTMESSAGEXCHANGE:
                final Object obj = msg.obj;

                Log.d(TAG, "handleMessage, " + Configuration.FIRSTMESSAGEXCHANGE_MSG + " case");

                chatManager = (ChatManager) obj;

                sendAddress(LocalP2PDevice.getInstance().getLocalDevice().deviceAddress,
                        LocalP2PDevice.getInstance().getProfile(),
                        chatManager);

                break;
            case Configuration.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;

                Log.d(TAG, "handleMessage, " + Configuration.MESSAGE_READ_MSG + " case");

                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);

                Log.d(TAG, "Message: " + readMessage);

                //if the message received contains Configuration.MAGICADDRESSKEYWORD is because now someone want to connect to this device
                if (readMessage.contains(Configuration.MAGICADDRESSKEYWORD)) {

                    if (readMessage.split("___").length == 2) {
                        device.setAvatar((readMessage.split("___")[1]).getBytes());
                        Log.d(TAG, "handleMessage, p2pDevice get avatar: " + Arrays.toString(device.getAvatar()));
                        manageAddressMessageReception(device);
                    } else {
                        p2pDevice = new WifiP2pDevice();
                        p2pDevice.deviceAddress = readMessage.split("___")[1];
                        p2pDevice.deviceName = readMessage.split("___")[2];
                        device = new P2pDestinationDevice(p2pDevice);
                        device.setEmail(readMessage.split("___")[3]);
                        tabFragment.setUserName(p2pDevice.deviceName);

                        if (readMessage.split("___").length == 4) {
                            Log.d(TAG, "handleMessage, p2pDevice created with: " + p2pDevice.deviceName + ", " + p2pDevice.deviceAddress);
                            //manageAddressMessageReception(device);
                        } else if (readMessage.split("___").length == 5) {
                            device.setDestinationIpAddress(readMessage.split("")[4]);

                            TabFragment.getWiFiP2pServicesFragment().setLocalDeviceIpAddress(device.getDestinationIpAddress());

                            Log.d(TAG, "handleMessage, p2pDevice created with: " + p2pDevice.deviceName + ", "
                                    + p2pDevice.deviceAddress + ", " + device.getDestinationIpAddress());
                            //manageAddressMessageReception(device);
                        }
                        flag = true;
                    }
                    readMessage = readMessage.split("___")[1] + " in chat";
                }

                //i check if tabNum is valid only to be sure.
                //i using this if, because this peace of code is critical and "sometimes can throw exceptions".
                //tabNum = 1;
                if (tabFragment.isValidTabNum(tabNum)) {

                    if (Configuration.DEBUG_VERSION) {
                        //i use this to re-format the message (not really necessary because in the "commercial"
                        //version, if a message contains MAGICADDRESSKEYWORD, this message should be removed and used
                        // only by the logic without display anything.
                        if (readMessage.contains(Configuration.MAGICADDRESSKEYWORD)) {
                            readMessage = readMessage.replace("+", "");
                            readMessage = readMessage.replace(Configuration.MAGICADDRESSKEYWORD, "");
                        }
                        if(!readMessage.contains("@")) {
                            tabFragment.getChatFragmentByTab(tabNum).pushMessage(p2pDevice.deviceName + ": " + readMessage);
                        }

                        if (flag) {
                            tabFragment.getChatFragmentByTab(tabNum).setDeviceAdress(p2pDevice.deviceAddress);
                            tabFragment.getChatFragmentByTab(tabNum).setDeviceName(p2pDevice.deviceName);
                            tabFragment.getChatFragmentByTab(tabNum).setDeviceAvatar(device.getAvatar());
                            tabFragment.getChatFragmentByTab(tabNum).addHistoryToChat();
                            flag = false;
                        }
                        if(!(readMessage.contains("@") || readMessage.contains("___") || readMessage.contains("in chat"))) {
                            ContentValues cv = new ContentValues();
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            cv.put("adressFrom", p2pDevice.deviceAddress);
                            cv.put("adressTo", LocalP2PDevice.getInstance().getLocalDevice().deviceAddress);
                            cv.put("name",p2pDevice.deviceName);
                            cv.put("message", readMessage);
                            long rowID = db.insert("history", null, cv);
                            Log.d("chat", "history inserted, ID = " + rowID);
                            dbHelper.close();
                        }

                    } else {
                        if (!readMessage.contains(Configuration.MAGICADDRESSKEYWORD)) {
                            tabFragment.getChatFragmentByTab(tabNum).pushMessage(p2pDevice.deviceName + ": " + readMessage);
                        }
                    }

                    //if the WaitingToSendQueue is not empty, send all his messages to target device.
                    if (!WaitingToSendQueue.getInstance().getWaitingToSendItemsList(tabNum).isEmpty()) {
                        tabFragment.getChatFragmentByTab(tabNum).sendForcedWaitingToSendQueue();
                    }
                } else {
                    Log.e("handleMessage", "Error tabNum = " + tabNum + " because is <=0");
                }
                break;
        }
        return true;
    }

    private void manageAddressMessageReception(P2pDestinationDevice p2pDevice) {

        if (!DestinationDeviceTabList.getInstance().containsElement(p2pDevice)) {
            Log.d(TAG, "handleMessage, p2pDevice IS NOT in the DeviceTabList -> OK! ;)");

            if (DestinationDeviceTabList.getInstance().getDevice(tabNum - 1) == null) {

                DestinationDeviceTabList.getInstance().setDevice(tabNum - 1, p2pDevice);

                Log.d(TAG, "handleMessage, p2pDevice in DeviceTabList at position tabnum= " + (tabNum - 1) + " is null");
            } else {
                DestinationDeviceTabList.getInstance().addDeviceIfRequired(p2pDevice);

                Log.d(TAG, "handleMessage, p2pDevice in DeviceTabList at position tabnum= " + (tabNum - 1) + " isn't null");
            }
        } else {
            Log.d(TAG, "handleMessage, p2pDevice IS already in the DeviceTabList -> OK! ;)");
        }

        //ok, now in this method i want to be sure to send this message to the other device with LocalDevice macaddress.
        //Before, i need to select the correct tabNum index. It's possible that this tabNum index is not correct,
        // and i need to choose a correct index to prevent Exception

        //update tabNum to select the tab associated to p2pDevice
        tabNum = DestinationDeviceTabList.getInstance().indexOfElement(p2pDevice) + 1;

        Log.d(TAG, "handleMessage, updated tabNum = " + tabNum);

        Log.d(TAG, "handleMessage, chatManager!=null? " + (chatManager != null));

        //if chatManager != null i'm receiving the message with MAGICADDRESSKEYWORD from another device
        if (chatManager != null) {
            //add a new tab, only if necessary.
            //i mean that if there is a conversation created and stopped,
            // i must restart this one and i don't create another one.
            //TABNUM
            //tabNum = 1;
            if (tabNum > TabFragment.getWiFiChatFragmentList().size()) {
                WiFiChatFragment frag = WiFiChatFragment.newInstance();
                //adds a new fragment, sets the tabNumber with listsize+1, because i want to add an element to this list and get
                //this position, but at the moment the list is not updated. When i use listsize+1
                // i'm considering "+1" as the new element that i want to add.
                frag.setTabNumber(TabFragment.getWiFiChatFragmentList().size() + 1);
                //add new tab
                TabFragment.getWiFiChatFragmentList().add(frag);
                tabFragment.getMSectionsPagerAdapter().notifyDataSetChanged();
            }

            //update current displayed tab and the color.
            this.setTabFragmentToPage(tabNum);
            this.addColorActiveTabs(false);

            Log.d(TAG, "tabNum is : " + tabNum);

            //i set chatmanager, because if i am in Configuration.FIRSTMESSAGEXCHANGE's case is
            //when two devices starting to connect each other for the first time
            //or after a disconnect event and GroupInfo is available.
            tabFragment.getChatFragmentByTab(tabNum).setChatManager(chatManager);

            //because i don't want to re-execute the code inside this if, every received message.
            chatManager = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.setupToolBar();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        tabFragment = TabFragment.newInstance();

        this.getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_root, tabFragment, "tabfragment")
                .commit();

        this.getSupportFragmentManager().executePendingTransactions();
    }


    @Override
    protected void onRestart() {

        Fragment frag = getSupportFragmentManager().findFragmentByTag("services");
        if (frag != null) {
            getSupportFragmentManager().beginTransaction().remove(frag).commit();
        }

        TabFragment tabfrag = ((TabFragment) getSupportFragmentManager().findFragmentByTag("tabfragment"));
        if (tabfrag != null) {
            tabfrag.getMViewPager().setCurrentItem(0);
        }

        super.onRestart();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.discovery:
                ServiceList.getInstance().clear();

                if (discoveryStatus) {
                    discoveryStatus = false;
                    item.setIcon(R.drawable.ic_action_search_stopped);
                    internalStopDiscovery();
                } else {
                    discoveryStatus = true;
                    item.setIcon(R.drawable.ic_action_search_searching);
                    startRegistration();
                    discoverService();
                }

                updateServiceAdapter();
                this.setTabFragmentToPage(0);
                return true;
            case R.id.disconnect:
                this.setTabFragmentToPage(0);
                this.forceDisconnectAndStartDiscovery();
                return true;
            case R.id.cancelConnection:
                this.setTabFragmentToPage(0);
                this.forcedCancelConnect();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiP2pBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onStop() {
        this.disconnectBecauseOnStop();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_items, menu);
        return true;
    }

}