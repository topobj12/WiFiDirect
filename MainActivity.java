package com.example.directnetwork;

import java.util.ArrayList;
import java.util.List;

import com.example.directnetwork.DeviceList.DeviceActionListener;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


//メインActivity
public class MainActivity extends Activity implements ChannelListener, DeviceActionListener {

	//定数
    public static final String TAG = "wifidirectdemo";
    
    //変数
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    
    /*============================================
     * 追加した変数
     * ===========================================
     * */
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>(); //取得したピアのリスト

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    //Activity起動時に呼び出し
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // intentFilterを作成し、Actionを監視する。　Actionが発生したらintentを通知
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);	//WiFi Directの有効・無効状態
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);	//デバイス情報の変更通知　（通信可能なデバイスの発見・ロストなど）
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);	//IPアドレスなどのコネクション情報。通信状態の変更通知
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);	//自分自信のデバイス状態の変更通知

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);	// p2p機能を利用するのに必要なインスタンス
    }

    //Activity実行時、Activity再開時に呼び出し
    @Override
    public void onResume() {
        super.onResume();
        receiver = new DirectReceiver(manager, channel, this); //DirectReceiverを作成して、イベントを受け取っています
        registerReceiver(receiver, intentFilter);	//ブロードキャストレシーバを登録
    }

    //別のActivityが実行されたときに呼び出し
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);	//ブロードキャストレシーバを解除
    }

    //すべてのピアを削除し、すべてのフィールドをクリアする。BroadcastReceiverが状態変更イベントを受信すると呼ばれる
    public void resetData() {
        DeviceList fragmentList = (DeviceList) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    //オプションメニューの作成
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    //オプションアイテムが選択されたときに呼び出し
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:	//wifi directの設定が選択された場合
                if (manager != null && channel != null) {

                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

                
            /*============================================
             * 端末検索 
             * ===========================================*/
            case R.id.atn_direct_discover:	//端末検索が選択された場合
                if (!isWifiP2pEnabled) {
                    Toast.makeText(MainActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceList fragment = (DeviceList) getFragmentManager()
                        .findFragmentById(R.id.frag_list);	//fragmentを用意する
                fragment.onInitiateDiscovery();		//
                
                //自信の端末の接続状況の確認 
                TextView view = (TextView)fragment.mContentView.findViewById(R.id.my_status);
                String st = view.getText().toString();
                //Connected状態の場合
                if(st == "Connected"){
                	return true;
                }
                
                //ピアの検索を開始する
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(MainActivity.this, "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //
    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }

    
    //接続開始
    @Override
    public void connect(WifiP2pConfig config) {
    	//WifiP2pManagerクラスのconnectメソッドで接続を開始する    	
        manager.connect(channel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }
            @Override
            public void onFailure(int reason) {
                Toast.makeText(MainActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    //コネクションの切断 (接続処理完了後)
    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        
        //WifiP2pManagerクラスのremoveGroupで切断をする
        manager.removeGroup(channel, new ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }
            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }

        });
    }

    
    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    //コネクションの切断（接続処理中）
    @Override
    public void cancelDisconnect() {

        if (manager != null) {
            final DeviceList fragment = (DeviceList) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            //接続済みであれば、disconnectメソッドを呼ぶ　→　MainActivity内のdisconnectメソッド
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } 
            //接続済み　or 要求済み　であれば、WifiP2pManagerクラスのcancelConnectメソッドを呼ぶ
            else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, "Aborting connection",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(MainActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }
}
