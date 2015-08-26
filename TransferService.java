package com.example.directnetwork;


import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class TransferService extends IntentService {

	//定数
    private static final int SOCKET_TIMEOUT = 50000; //タイムアウト
    public static final String ACTION_SEND_FILE = "com.example.directnetwork.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
    public static final String EXTRAS_SEND_DATA = "send_data";
    
    public TransferService(String name) {
        super(name);
    }

    public TransferService() {
        super("FileTransferService");
    }

    /*
     * (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    
    //データ転送で利用する
    @Override
    protected void onHandleIntent(Intent intent) {

//        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
//            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            String send_data = intent.getExtras().getString(EXTRAS_SEND_DATA);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(MainActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(MainActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                byte[] w = send_data.getBytes();
                InputStream is = new ByteArrayInputStream(w);
                

                DeviceDetailFragment.copyFile(is, stream);
                Log.d(MainActivity.TAG, "Client: Data written");

            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
               
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                            Log.d(MainActivity.TAG,"Client close socket");
                            Log.d("SEND_PHASE","SEND_PHASE 2 END");

                            MainActivity.SEND_SUCCESS = true;
//                            MainActivity.SEND_PHASE = 3;
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }

        
    }
}
