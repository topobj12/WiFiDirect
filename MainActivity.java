package com.example.directnetwork;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.example.directnetwork.DeviceList.DeviceActionListener;


import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */

//���C��Activity
public class MainActivity extends Activity implements ChannelListener, DeviceActionListener {

	//�萔
    public static final String TAG = "wifidirectdemo";
    public static int SEND_PHASE = 0;
    public static boolean SEND_SUCCESS = false;    
    public static String MY_ADDRESS = "";
    public static Activity MAIN_ACT ;
    //�ϐ�
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    
    //IP
    String IPaddress = "";
    
    /*============================================
     * �ǉ������ϐ�
     * ===========================================
     * */
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>(); //�擾�����s�A�̃��X�g

    
    
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    //Activity�N�����ɌĂяo��
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // add necessary intent values to be matched.

        // intentFilter���쐬���AAction���Ď�����B�@Action������������intent��ʒm
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);	//WiFi Direct�̗L���E�������
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);	//�f�o�C�X���̕ύX�ʒm�@�i�ʐM�\�ȃf�o�C�X�̔����E���X�g�Ȃǁj
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);	//IP�A�h���X�Ȃǂ̃R�l�N�V�������B�ʐM��Ԃ̕ύX�ʒm
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);	//�������M�̃f�o�C�X��Ԃ̕ύX�ʒm

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);	// p2p�@�\�𗘗p����̂ɕK�v�ȃC���X�^���X
        MAIN_ACT = this;
    }

    /** register the BroadcastReceiver with the intent values to be matched */
    //Activity���s���AActivity�ĊJ���ɌĂяo��
    @Override
    public void onResume() {
        super.onResume();
        receiver = new DirectReceiver(manager, channel, this); //DirectReceiver���쐬���āA�C�x���g���󂯎���Ă��܂�
        registerReceiver(receiver, intentFilter);	//�u���[�h�L���X�g���V�[�o��o�^
    }

    //�ʂ�Activity�����s���ꂽ�Ƃ��ɌĂяo��
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);	//�u���[�h�L���X�g���V�[�o������
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    //���ׂẴs�A���폜���A���ׂẴt�B�[���h���N���A����BBroadcastReceiver����ԕύX�C�x���g����M����ƌĂ΂��
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

    //�I�v�V�������j���[�̍쐬
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    //�I�v�V�����A�C�e�����I�����ꂽ�Ƃ��ɌĂяo��
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:	//wifi direct�̐ݒ肪�I�����ꂽ�ꍇ
                if (manager != null && channel != null) {

                	// intent�̃C���X�^���X����
    				Intent intent = new Intent(MainActivity.this, SubActivity.class);
    				//����activity�̋N��
    				startActivity(intent);

                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

                
            /*============================================
             * �[������ 
             * ===========================================*/
            case R.id.atn_direct_discover:	//�[���������I�����ꂽ�ꍇ
                if (!isWifiP2pEnabled) {
                    Toast.makeText(MainActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceList fragment = (DeviceList) getFragmentManager()
                        .findFragmentById(R.id.frag_list);	//fragment��p�ӂ���
                fragment.onInitiateDiscovery();		//
                
                //���M�̒[���̐ڑ��󋵂̊m�F 
                TextView view = (TextView)fragment.mContentView.findViewById(R.id.my_status);
                String st = view.getText().toString();
                //Connected��Ԃ̏ꍇ
                if(st == "Connected"){
                	return true;
                }
                
                //�s�A�̌������J�n����
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

    
    //�ڑ��J�n
    @Override
    public void connect(WifiP2pConfig config) {
    	//WifiP2pManager�N���X��connect���\�b�h�Őڑ����J�n����    	
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

    //�R�l�N�V�����̐ؒf (�ڑ�����������)
    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        
        //WifiP2pManager�N���X��removeGroup�Őؒf������
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

    //�R�l�N�V�����̐ؒf�i�ڑ��������j
    @Override
    public void cancelDisconnect() {

        if (manager != null) {
            final DeviceList fragment = (DeviceList) getFragmentManager()
                    .findFragmentById(R.id.frag_list);
            //�ڑ��ς݂ł���΁Adisconnect���\�b�h���Ăԁ@���@MainActivity����disconnect���\�b�h
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } 
            //�ڑ��ς݁@or �v���ς݁@�ł���΁AWifiP2pManager�N���X��cancelConnect���\�b�h���Ă�
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
