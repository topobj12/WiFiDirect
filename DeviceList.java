package com.example.directnetwork;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiConfiguration.Status;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Environment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView.FindListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceList extends ListFragment implements PeerListListener {

	public static WifiP2pDeviceList mPeerList ;
	public static String SEND_DATA = "";
	
	//変数
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private WifiP2pDevice device;

    private SQLiteDatabase db;
    
    private boolean send = false;
    
    //send
    String send_data = "";
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(getActivity(), R.layout.row_devices, peers));


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.device_list, null);
        
        mContentView.findViewById(R.id.Send).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/*======================
				 * sendフラグ
				 *=======================*/
//				send = true;
				MainActivity.SEND_PHASE = 1;
				/*=======================
				 * 端末検索
				 *======================*/
				WifiP2pManager manager = (WifiP2pManager) getActivity().getSystemService(Context.WIFI_P2P_SERVICE);
		        Channel channel = manager.initialize(getActivity(), getActivity().getMainLooper(), null);	// p2p機能を利用するのに必要なインスタンス

		        
                final DeviceList fragment = (DeviceList) getFragmentManager()
                        .findFragmentById(R.id.frag_list);	//fragmentを用意する
                fragment.onInitiateDiscovery();		//
                
                //自信の端末の接続状況の確認 
                TextView view = (TextView)fragment.mContentView.findViewById(R.id.my_status);
                String st = view.getText().toString();
                //Connected状態の場合
                if(st == "Connected"){
                	return;
                }

                //ピアの検索を開始する
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(getActivity(), "Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(getActivity(), "Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                
                
			}
		});
        
        return mContentView;
    }

    /**
     * @return this device
     */
    public WifiP2pDevice getDevice() {
        return device;
    }
    
    //deviceのstatusを判断するためのメソッド
    private static String getDeviceStatus(int deviceStatus) {
        Log.d(MainActivity.TAG, "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }

    /**
     * Initiate a connection with the peer.
     */
    //peerを選択したときの処理
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        
        
        //選択したデバイスの詳細を表示
        ((DeviceActionListener) getActivity()).showDetails(device);
        
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;

        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Context context, int textViewResourceId,
                List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;

        }

        
        //発見したデバイス一覧のViewを作成
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null) {
                    top.setText(device.deviceName);
                }
                if (bottom != null) {
                    bottom.setText(getDeviceStatus(device.status));
                }
            }

            return v;

        }
    }

    /**
     * Update UI for this device.
     * 
     * @param device WifiP2pDevice object
     */
    //自分自身のデバイス情報の更新
    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        
        //statusをTextViewに反映
        this.send_data = device.deviceName;
        writeFile(this.send_data, 1);
        view.setText(getDeviceStatus(device.status));
  
        MainActivity.MY_ADDRESS = device.deviceAddress;
    }

    //有効なデバイスリストの取得 /　受け取ったWifiP2pDeviceListを peersに保存
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        mPeerList = peerList;
        
        //ListAdapterに対してデータ更新を通知することでリストを更新
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            Log.d(MainActivity.TAG, "No devices found");
            return;
        }

        
        if(MainActivity.SEND_PHASE == 1){
	         /*==========================================================
	          * peer選択　→　接続　→　詳細 
	          *========================================================== */
	         /*===========================================================*/
	         //変数
	         int item_num =0;
	         //接続設定
	         WifiP2pDevice device ; 
	         
	         
	         /*========================================================
	          * 	データベース上に接続先が存在するかどうかをチェックし、
	          * 	1．存在しない場合、接続先を追加 
	          * 	2．他に存在する場合、次のItemを選択する
	          *  3.すべての接続先が存在する場合は、一番上の接続先を利用する
	          * =======================================================*/
	         
	         //データベース用のインスタンスを生成
	         MySqLiteOpenHelper helper = new MySqLiteOpenHelper(getActivity());
	         
	         //読み書きできるように開く
	         db = helper.getWritableDatabase();
	         
	         //ピアリストのitem数
	         int count_peers = getListAdapter().getCount();
	         
	         //データベース上に存在したかどうかの判断
	         Boolean db_exist = false;
	         
	         /*========================================================*/
	         for(int i=0; i<count_peers; i++){
	         	//device情報の取得
	         	device = (WifiP2pDevice) getListAdapter().getItem(i);
	         	
	         	//データベース上に存在しない場合
	             if(!helper.compare_data(db, device.deviceAddress)){
	             	//logに書き出し
	             	String str = "DB : "+device.deviceAddress+" not exist \n";
	             	writeFile(str,0);
	             	//レコードを設定
	     	        ContentValues values = new ContentValues();
	     	        values.put("android_name", device.deviceName);
	     	        values.put("mac_address",device.deviceAddress);
	     	        //レコードを追加
	     	        db.insert("connection_data", null, values);    	        
	     	        //クローズ処理
	     	        db.close();
	     	        values.clear();
	     	        helper.close();
	     	        item_num = i;
	     	        db_exist = true;
	     	        break;
	             }        
	             //データベース上に存在する場合
	             else{
	             	//logに書き出し
	             	String str = "DB : "+device.deviceAddress+" exist \n";
	             	writeFile(str,0);
	             }   
	         }
	        
	         
	         //データベース上にすべて存在した場合の処理
	         if(db_exist == false){
	         	item_num = 0;
	         }
	         
	         /*===========================================================
	          * 	接続処理
	          * =========================================================*/
	         
	         /*==========================================================*/
	         //接続設定
	         device = (WifiP2pDevice) getListAdapter().getItem(item_num); 
	         //接続済みかどうかの判断
	         if(device.status != WifiP2pDevice.CONNECTED){
	         	WifiP2pConfig config = new WifiP2pConfig();	//WifiP2pConfigクラスのインスタンス生成	 
	 	        config.deviceAddress = device.deviceAddress;	//接続デバイスのMACアドレスの設定
	 	        config.wps.setup = WpsInfo.PBC;	//WpsInfo → Wi-Fi Protected Setup  / PBC → Push button configuration ワンタッチで接続設定できるように指定
	 	        if (progressDialog != null && progressDialog.isShowing()) {
	 	            progressDialog.dismiss();
	 	        }
	 	        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
	 	                "Connecting to :" + device.deviceAddress, true, true
	 	
	 	                );
	 	        //MainActivityのconnectメソッドを呼び出して接続
	 	        ((DeviceActionListener) getActivity()).connect(config);
	 	
	 	        /*========================================================
	 	         * 	接続ログをファイルに追加　
	 	         * =======================================================*/
	 	        //書き込み
	 	        String out_text = "connection : "+device.deviceAddress+" : "+device.deviceName+" : "+getCalendar()+" / " +device.status+" \n";
	 	        writeFile(out_text,0);     
	 		 	 //送信データの作成
	 	       EditText text = (EditText) mContentView.findViewById(R.id.SendText);
	 	       TextView name = (TextView) mContentView.findViewById(R.id.my_name); 
	 	       SEND_DATA = name.getText()+","+text.getText().toString()+","+getCalendar()+",34.147662,131.467635,";
	 	        
	 	        //選択したデバイスの詳細を表示
	 	        ((DeviceActionListener) getActivity()).showDetails(device);	
	 	        Log.d("check_device", "device showDetails");

	             /*=========================================================
	              * SEND_PHASEを変更
	              *========================================================*/
//	             MainActivity.SEND_PHASE = 3;
		 	     Log.d("send_flag","PHASE_1 END");
		 	     
		 	     
	         }
	         /*==========================================================*/

        }
        

    }

    public void clearPeers() {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

   
    //端末の検索を開始時に呼び出し
    public void onInitiateDiscovery() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        
                    }
                });
    }
    
    /*=================================================
     * 		ファイル出力用
     * ================================================*/
    public void writeFile(String out_text,int num){
    	File directory = Environment.getExternalStorageDirectory();
    	String file_path = directory.getAbsolutePath()+"/log_device.txt"; // 内部ストレージのルートディレクトリを指定
        BufferedOutputStream out_stream ;
        
        if(num == 0){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/log_device.txt"; // 内部ストレージのルートディレクトリを指定
        	try{
            	out_stream = new BufferedOutputStream(new FileOutputStream(file_path,true));
            	out_stream.write(out_text.getBytes());		
            	out_stream.close();
            }catch(Exception e){
            	Log.e("write_file", "file error 0");
            }
        }
        else if(num == 1){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/send_data.txt"; // 内部ストレージのルートディレクトリを指定
        	try{
            	out_stream = new BufferedOutputStream(new FileOutputStream(file_path));
            	out_stream.write(out_text.getBytes());		
            	out_stream.close();
            }catch(Exception e){
            	Log.e("write_file", "file error 1");
            }
        }
        
    }
    
    /*====================================================
     * 	現在の時間の取得
     * ===================================================*/
    public String getCalendar(){
    	String str ="";
    	
    	Calendar cal = Calendar.getInstance();
    	str =""+ cal.get(Calendar.YEAR) +"/"+(cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH)+"_"
    									+cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND);
    	return  str;
    }
    
    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config);

        void disconnect();


    }
    
    /*=================================================
     * 		ファイル入力用
     * ================================================*/
    public String readFile(int num){
    	File directory = Environment.getExternalStorageDirectory();
    	String file_path = directory.getAbsolutePath()+"/log_device.txt"; // 内部ストレージのルートディレクトリを指定
        BufferedInputStream in_stream ;
        
        byte[] fbytes = new byte[1024];
        String msg = "";
        
        if(num == 0){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/log_device.txt"; // 内部ストレージのルートディレクトリを指定
        	try{
            	in_stream = new BufferedInputStream(new FileInputStream(file_path));
            	while(in_stream.read(fbytes) >= 0){
            		String str = new String(fbytes);
            		msg += str;
            	}	
            	in_stream.close();
            	return msg;
            }catch(Exception e){
            	Log.e("write_file", "file error 0");
            }
        }
        else if(num == 1){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/send_data.txt"; // 内部ストレージのルートディレクトリを指定
        	try{
            	in_stream = new BufferedInputStream(new FileInputStream(file_path));
            	while(in_stream.read(fbytes) >= 0){
            		String str = new String(fbytes);
            		msg += str;
            	}	
            	in_stream.close();
            	return msg;
            }catch(Exception e){
            	Log.e("write_file", "file error 1");
            }
        }
        else if(num == 2){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/ipaddress.txt"; // 内部ストレージのルートディレクトリを指定
        	try{
            	in_stream = new BufferedInputStream(new FileInputStream(file_path));
            	while(in_stream.read(fbytes) >= 0){
            		String str = new String(fbytes);
            		msg += str;
            	}	
            	in_stream.close();
            	String[] msg_ary = msg.split(",");
            	Log.d("get_ipfile", msg_ary[0]);
            	return msg_ary[0];
            }catch(Exception e){
            	Log.e("write_file", "file error 2");
            }
        }
        return "nothing";
    }
    

}
