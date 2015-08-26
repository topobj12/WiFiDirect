package com.example.directnetwork;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;


public class DirectReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private Channel channel;
    private MainActivity activity;

    public DirectReceiver(WifiP2pManager manager, Channel channel,
            MainActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        //Wi-Fi　Directの有効・無効
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // UI update to indicate wifi p2p status.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            //Wi-Fi Directの有効・無効情報をActivityに通知
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
                activity.resetData();

            }
            Log.d(MainActivity.TAG, "P2P state changed - " + state);
        }
        //通信できるデバイスに変更があったときに通知
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (manager != null) {
                manager.requestPeers(channel, (PeerListListener) activity.getFragmentManager()
                        .findFragmentById(R.id.frag_list));
            }
            Log.d(MainActivity.TAG, "P2P peers changed");
        }
        //Wi-Fi Direct通信状態の変更通知
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }
            //ネットワーク情報を取り出す
            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            //isConnectedメソッドで接続状態の確認
            if (networkInfo.isConnected()) {

                DeviceDetailFragment fragment = (DeviceDetailFragment) activity
                        .getFragmentManager().findFragmentById(R.id.frag_detail);
                //IPアドレスを表示するために、フラグメントへ情報通知
                manager.requestConnectionInfo(channel, fragment);
            } else {
                // It's a disconnect
                activity.resetData();
            }
        }
        //自分自身のデバイス状態の変更通知 / ユーザ操作、設定アプリにより状態が変わった際に通知される
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            DeviceList fragment = (DeviceList) activity.getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            //デバイス情報を取り出す
            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

        }
    }
}

