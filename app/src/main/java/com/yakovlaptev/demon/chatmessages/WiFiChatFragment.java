package com.yakovlaptev.demon.chatmessages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.yakovlaptev.R;
import com.yakovlaptev.demon.DestinationDeviceTabList;
import com.yakovlaptev.demon.SplashScreen;
import com.yakovlaptev.demon.chatmessages.waitingtosend.WaitingToSendQueue;
import com.yakovlaptev.demon.model.LocalP2PDevice;
import com.yakovlaptev.demon.services.ServiceList;
import com.yakovlaptev.demon.services.WiFiP2pService;
import com.yakovlaptev.demon.socketmanagers.ChatManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

import static com.yakovlaptev.demon.SplashScreen.dbHelper;

public class WiFiChatFragment extends Fragment {

    private static final String TAG = "WiFiChatFragment";

    @Getter
    @Setter
    private Integer tabNumber;
    @Getter
    @Setter
    private static boolean firstStartSendAddress;
    @Getter
    @Setter
    private boolean grayScale = true;
    @Getter
    private final List<String> items = new ArrayList<>();

    private TextView chatLine;

    @Getter
    @Setter
    private ChatManager chatManager;
    private WiFiChatMessageListAdapter adapter = null;
    @Getter @Setter private String deviceAdress;
    @Getter @Setter private String deviceName;
    @Getter @Setter private byte[] deviceAvatar;

    public interface AutomaticReconnectionListener {
        public void reconnectToService(WiFiP2pService wifiP2pService);
    }

    public static WiFiChatFragment newInstance() {
        return new WiFiChatFragment();
    }

    public WiFiChatFragment() {
    }

    public void sendForcedWaitingToSendQueue() {

        Log.d(TAG, "sendForcedWaitingToSendQueue() called");

        String combineMessages = "";
        List<String> listCopy = WaitingToSendQueue.getInstance().getWaitingToSendItemsList(tabNumber);
        for (String message : listCopy) {
            if (!message.equals("") && !message.equals("\n")) {
                combineMessages = combineMessages + "\n" + message;
            }
        }
        combineMessages = combineMessages + "\n";

        Log.d(TAG, "Queued message to send: " + combineMessages);

        if (!chatManager.isDisable()) {
            chatManager.write((combineMessages).getBytes());
            WaitingToSendQueue.getInstance().getWaitingToSendItemsList(tabNumber).clear();
        } else {
            Log.d(TAG, "Chatmanager disabled, impossible to send the queued combined message");
        }

    }

    public void pushMessage(String readMessage) {
        items.add(readMessage);
        adapter.notifyDataSetChanged();
    }

    public void updateChatMessageListAdapter() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void addToWaitingToSendQueueAndTryReconnect() {
        //add message to the waiting to send queue
        WaitingToSendQueue.getInstance().getWaitingToSendItemsList(tabNumber).add(chatLine.getText().toString());

        //try to reconnect
        WifiP2pDevice device = DestinationDeviceTabList.getInstance().getDevice(tabNumber - 1);
        if (device != null) {
            WiFiP2pService service = ServiceList.getInstance().getServiceByDevice(device);
            Log.d(TAG, "device address: " + device.deviceAddress + ", service: " + service);

            //call reconnectToService in MainActivity
            ((AutomaticReconnectionListener) getActivity()).reconnectToService(service);

        } else {
            Log.d(TAG, "addToWaitingToSendQueueAndTryReconnect device == null, i can't do anything");
        }
    }

    public void addHistoryToChat() {
        pushMessage("<<History>>");
        SplashScreen.DBHelper dbHelper = SplashScreen.dbHelper;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(">>>>>>history", deviceAdress);
        Cursor cursor = db.rawQuery("SELECT name, message FROM history WHERE adressFrom = '"+deviceAdress+"' OR adressTo = '"+deviceAdress+"'" , null);
        if (cursor.moveToFirst()){
            do {
                String name = cursor.getString(0);
                String message = cursor.getString(1);
                if(!message.contains("c6:43:8f:b4:1b:8e")) {
                    if(Objects.equals(name, LocalP2PDevice.getInstance().getProfile().getName())) {
                        pushMessage(name + " " + message);
                        Log.d(">>>>>>history", name + " " + message);
                    } else {
                        pushMessage(name + ": " + message);
                        Log.d(">>>>>>history", name + ": " + message);
                    }
                }

            } while(cursor.moveToNext());
        }
        cursor.close();
        db.close();
        Log.d(TAG, "addHistoryToChat");
        pushMessage("<<History>>");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chatmessage_list, container, false);

        chatLine = (TextView) view.findViewById(R.id.txtChatLine);
        ListView listView = (ListView) view.findViewById(R.id.list);

        adapter = new WiFiChatMessageListAdapter(getActivity(), R.id.txtChatLine, this);
        listView.setAdapter(adapter);

        view.findViewById(R.id.sendMessage).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        if (!chatManager.isDisable()) {
                            Log.d(TAG, "chatmanager state: enable");

                            //send message to the ChatManager's outputStream.
                            chatManager.write(chatLine.getText().toString().getBytes());
                        } else {
                            Log.d(TAG, "chatmanager disabled, trying to send a message with tabNum= " + tabNumber);

                            addToWaitingToSendQueueAndTryReconnect();
                        }
                        pushMessage(chatLine.getText().toString());


                        WifiP2pDevice device = DestinationDeviceTabList.getInstance().getDevice(tabNumber - 1);
                        ContentValues cv = new ContentValues();
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        cv.put("adressFrom", LocalP2PDevice.getInstance().getLocalDevice().deviceAddress);
                        cv.put("adressTo", device.deviceAddress);
                        cv.put("name", LocalP2PDevice.getInstance().getProfile().getName());
                        cv.put("message", chatLine.getText().toString());
                        long rowID = db.insert("history", null, cv);
                        Log.d("chat", "history inserted, ID = " + rowID);
                        dbHelper.close();

                        //Log.d("TEST", chatLine.getText().toString());
                        chatLine.setText("");
                    }
                });
        return view;
    }


}
