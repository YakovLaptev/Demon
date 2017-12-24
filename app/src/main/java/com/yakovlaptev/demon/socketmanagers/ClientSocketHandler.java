
package com.yakovlaptev.demon.socketmanagers;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.yakovlaptev.demon.Configuration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketHandler extends Thread {

    private static final String TAG = "ClientSocketHandler";

    private final Handler handler;
    private final InetAddress mAddress;
    private Socket socket;

    public ClientSocketHandler(@NonNull Handler handler, @NonNull InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.mAddress = groupOwnerAddress;
    }

    @Override
    public void run() {
        ChatManager chat;
        socket = new Socket();
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                    Configuration.GROUPOWNER_PORT), Configuration.CLIENT_PORT);
            Log.d(TAG, "Launching the I/O handler");
            chat = new ChatManager(socket, handler);
            new Thread(chat).start();
        } catch (IOException e) {
            Log.e(TAG,"IOException throwed by socket", e);
            try {
                socket.close();
            } catch (IOException e1) {
                Log.e(TAG,"IOException during close Socket" , e1);
            }
        }
    }

    public void closeSocketAndKillThisThread() {
        if(socket!=null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG,"IOException during close Socket" , e);
            }
        }

        //to interrupt this thread, without the threadpoolexecutor
        if(!this.isInterrupted()) {
            Log.d(TAG,"Stopping ClientSocketHandler");
            this.interrupt();
        }
    }
}
