package com.example.directnetwork;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.directnetwork.DeviceList.DeviceActionListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 * 特定のピアを管理し、ネットワーク接続を設定し、データを転送するフラグメント
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

	//定数
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    
    //変数
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    static ProgressDialog progressDialog = null;

    //Activity
    Activity act ;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.act = getActivity();
    }

    //viewを作成し、作成したviewを返す
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    	//device_detailからlayout読み込み
        mContentView = inflater.inflate(R.layout.device_detail, null);
        
        //connectボタンをタッチしたときの処理
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            	//接続設定
                WifiP2pConfig config = new WifiP2pConfig();	//WifiP2pConfigクラスのインスタンス生成	 
                config.deviceAddress = device.deviceAddress;	//接続デバイスのIPアドレスの設定
                config.wps.setup = WpsInfo.PBC;	//WpsInfo → Wi-Fi Protected Setup  / PBC → Push button configuration ワンタッチで接続設定できるように指定
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true

                        );
                //MainActivityのconnectメソッドを呼び出して接続
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        
        //disconnectボタンをタッチしたときの処理
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    	//MainActivityのdisconnectメソッドを呼び出して切断
                        ((DeviceActionListener) getActivity()).disconnect();

                    }
                });

        //Sendボタンをタッチしたときの処理
        
        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    	/*======================================
                    	 * ファイルから送信用データを取得
                    	 *=====================================*/
                    	String msg = readFile(1);
                    	
                    	Log.d("send_data msg", msg);
                    	/*======================================
                    	 *	データの送信 
                    	 *=======================================*/
                    	writeFile(msg, 0);
                    	
                    	
                    	Intent serviceIntent = new Intent(getActivity(), TransferService.class);
                        serviceIntent.setAction(TransferService.ACTION_SEND_FILE);
                        serviceIntent.putExtra(TransferService.EXTRAS_SEND_DATA, msg);
                        serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                info.groupOwnerAddress.getHostAddress());
                        serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
                        Log.d("owner_address",info.groupOwnerAddress.getHostAddress());
                        Log.d("send_text", msg);
                        getActivity().startService(serviceIntent);
                    	

                    }
                });

        return mContentView;
    }

    //intentで受け取ったデータを送信
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    	Log.d("onActivityResult", "onActivityResult run !!");

    	
    }

    //WifiP2pInfoクラスからIPアドレスを取得、表示
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                        : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());	//ホストアドレスをTextViewに表示
        
        //IP取得
	    Log.d("WIFI_ADDRESS", "serach");

	    Enumeration<NetworkInterface> netIFs = null;
	    try {
	    	try {
				netIFs = NetworkInterface.getNetworkInterfaces();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            while( netIFs.hasMoreElements() ) {
                NetworkInterface netIF = netIFs.nextElement();
                Enumeration<InetAddress> ipAddrs = netIF.getInetAddresses();
                while( ipAddrs.hasMoreElements() ) {
                    InetAddress ip = ipAddrs.nextElement();
                    if( ! ip.isLoopbackAddress() ) {
                    	
                    	MainActivity.MY_ADDRESS = ip.getHostAddress().toString();
                    }
                }
            }
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    Log.d("WIFI_ADDRESS", MainActivity.MY_ADDRESS);
        
    	Log.d("FileServerAsyncTask","FileServerAsyncTask : start");
        new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text), getActivity())
                .execute();
        if (info.groupFormed && info.isGroupOwner) {

        } else if (info.groupFormed) {

            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE); // SENDボタンの表示
            if(MainActivity.SEND_PHASE == 3){
                //FINAL_DATAを送信する
                Log.d("FINAL_SEND1", "SEND_DATA FINAL");
                Send_data("lastSend,FINISH,xxxx/aa/bb,"+"34.147662,131.467635,");
            }
            else{
                //SEND_DATAを送信する
                Log.d("SEND_BOTTUN", "SEND_DATA");
                Send_data(DeviceList.SEND_DATA);
            }

                       
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
        }
        // hide the connect button
        writeFile(info.groupOwnerAddress.getHostAddress(), 2);
        
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    //詳細を表示
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);//物理アドレスをセット
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());//端末情報をセット

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    //viewの初期化
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    /*=======================================================================
     * 	非同期処理
     * 	受信したデータを出力する処理など
     * 
     * 	onPreExcute →　doInBackground → onPostExecute 
     * 	の順番で呼ばれる
     * ======================================================================*/
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;
        
        private Activity activity;
        private String sendIP;
        
        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText, Activity act) {
            this.context = context;
            this.statusText = (TextView) statusText;
            this.activity = act;
        }

        /*=============================================================
         * 	onPreExecuteの後に呼び出される
         *=============================================================*/
        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(MainActivity.TAG, "Server: Socket opened");
                //データの受け取り
                Socket client = serverSocket.accept();
                Log.d(MainActivity.TAG, "Server: connection done");

                InputStream inputstream = client.getInputStream();
                /*===============================================
                 * 	受け取ったデータをデータベースに格納する
                 *===============================================*/
                /*===============================================*/
                
                String msg="";
                //データベース用のインスタンスを生成
                MySqLiteOpenHelper helper = new MySqLiteOpenHelper(activity);
                
                //読み書きできるように開く
                SQLiteDatabase db = helper.getWritableDatabase();
                
                //受け取ったデータをStringに変換
                String get_str = InputstreamToString(inputstream);
                //ラインごとに分割
                String[] line_str = get_str.split(",,");
                for(int i=0;i<line_str.length;i++){
                	Log.d("RECEIVE_DATA", line_str[i]);
                	//データを分割
	                String[] data_str = line_str[i].split(",");
	                if(data_str[1].equals("FINISH")){
	                	sendIP = data_str[0];
	                	return msg = "FINISH";
	                }
	                else{
	                	//データをデータベースに追加
	                	if(i==0){
	                		msg = data_str[1];
	                	}
		                String tmp = helper.writeDB(db,data_str);
		                //受け取ったデータをファイルに書き出し
		                writeFile(msg, 1);
	                }
	                
                }
                db.close();
                helper.close();
                serverSocket.close();
                
                return msg;
                /*===============================================*/

            } catch (IOException e) {
                
            	Log.e("receiver_error", e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        /*=========================================================
         * doInBackgroundの戻り値をパラメータとして呼び出し
         * ========================================================*/
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
            	String text = "send comment - " + result + "\n";
            	
                statusText.setText(text);
                
                /*切断処理*/
                if(result.equals("FINISH") && MainActivity.SEND_SUCCESS){
                	((DeviceActionListener) activity).disconnect();
                	MainActivity.SEND_SUCCESS = false;
                	Log.d("AsyncTask", "AsyncTask : disconnect run");
                }
                else if(result.equals("FINISH")){
                	Send_data();
//                	((DeviceActionListener) activity).disconnect();
//                	
//                	//接続設定
//                    WifiP2pConfig config = new WifiP2pConfig();	//WifiP2pConfigクラスのインスタンス生成	 
//                    config.deviceAddress = sendIP;	//接続デバイスのIPアドレスの設定
//                    config.wps.setup = WpsInfo.PBC;	//WpsInfo → Wi-Fi Protected Setup  / PBC → Push button configuration ワンタッチで接続設定できるように指定
//                    if (progressDialog != null && progressDialog.isShowing()) {
//                        progressDialog.dismiss();
//                    }
//                    progressDialog = ProgressDialog.show(activity, "Press back to cancel",
//                            "Connecting to :" + sendIP, true, true
//
//                            );
//                    //MainActivityのconnectメソッドを呼び出して接続
//                    ((DeviceActionListener) activity).connect(config);
//                    MainActivity.SEND_PHASE = 3;
                	Log.d("AsyncTask","AsyncTask : Send_FINISH");
                }
            }

        }

        /*==============================================
         * UIスレッドで最初に呼び出し
         * UIに関わる処理はここで行う
         *=============================================*/
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
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
            	file_path = directory.getAbsolutePath()+"/recieved_data.txt"; // 内部ストレージのルートディレクトリを指定
            	try{
                	out_stream = new BufferedOutputStream(new FileOutputStream(file_path));
                	out_stream.write(out_text.getBytes());		
                	out_stream.close();
                }catch(Exception e){
                	Log.e("write_received", "file error 1");
                }
            }
            else if(num == 2){
            	directory = Environment.getExternalStorageDirectory();
            	file_path = directory.getAbsolutePath()+"/ipaddress.txt"; // 内部ストレージのルートディレクトリを指定
            	try{
            		out_text += ",";
                	out_stream = new BufferedOutputStream(new FileOutputStream(file_path));
                	out_stream.write(out_text.getBytes());		
                	out_stream.close();
                }catch(Exception e){
                	Log.e("write_received", "file error 2");
                }
            }
        }
        /*=====================================
         * 	inputstream →　String 変換
         *=====================================*/
        public String InputstreamToString(InputStream inputStream){
        	String read_str ="";
        	String line ;
            /*=====================================
             *	Inputstreamからの読み込み 
             *====================================*/
            try{
            	InputStreamReader reader = new InputStreamReader(inputStream);
            	BufferedReader br = new BufferedReader(reader);
            	StringBuilder builder = new StringBuilder();

            	while((line = br.readLine())!=null){
            		builder.append(line);
            	}
            	read_str = builder.toString();
            	Log.d("get_string",read_str);
            }catch(IOException e){
            	Log.e("writeDB", "failed reader.read");
            }
            return read_str;
        }
        /*==========================================================
   	 	 * 終了データを送信する
   	 	 *==========================================================*/
        public void Send_data(){
	       	String msg = "lastSend,FINISH,xxxx/aa/bb,"+"34.147662,131.467635,";
	   	 	Log.d("send_text2", msg);
	   	 	Intent serviceIntent = new Intent(activity, TransferService.class);
	   	 	serviceIntent.setAction(TransferService.ACTION_SEND_FILE);
	   	 	serviceIntent.putExtra(TransferService.EXTRAS_SEND_DATA, msg);
	   	 	serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_ADDRESS,
	   	 			sendIP);
	   	 	serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
	   	 	Log.d("owner_address",sendIP);
	   	 	Log.d("send_text3", msg);
	   	 	activity.startService(serviceIntent);
       }
    }

    //ファイルのコピー　
    /*====================================================
     * 	ここで、受っとったデータの書き込み処理?
     *  
     * ===================================================*/
    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(MainActivity.TAG, e.toString());
            return false;
        }
        return true;
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

        	msg = "android_test,テストデータ,20xx/aa/bb,"+"34.147662,131.467635,";
        	return msg;
        }
        else if(num == 2){
        	
        	msg = MainActivity.MY_ADDRESS+",FINISH,xxxx/aa/bb,"+"34.147662,131.467635";
        	return msg;
        }
        return "nothing";
    }
    
    /*=================================================
     * 		ファイル出力用
     * ================================================*/
    public void writeFile(String out_text,int num){
    	File directory = Environment.getExternalStorageDirectory();
    	String file_path = directory.getAbsolutePath()+"/log_send.txt"; // 内部ストレージのルートディレクトリを指定
        BufferedOutputStream out_stream ;
        
        if(num == 0){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/log_send.txt"; // 内部ストレージのルートディレクトリを指定
        	out_text += "\n"; 
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
        else if(num == 2){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/ipaddress.txt"; // 内部ストレージのルートディレクトリを指定
        	try{
        		out_text += ",";
            	out_stream = new BufferedOutputStream(new FileOutputStream(file_path));
            	out_stream.write(out_text.getBytes());		
            	out_stream.close();
            }catch(Exception e){
            	Log.e("write_received", "file error 2");
            }
        }
    } 
    /*==================================
     *	送信用クラス 
     *================================*/
    public void Send_data(String msg){
       	 /*==========================================================
	 	 * データを送信する
	 	 *==========================================================*/
    	msg += ","+readFile(2)+",";
//	 	 this.send_data = name.getText()+","+text.getText().toString()+","+getCalendar()+",34.147662,131.467635,";
	 	 Log.d("send_text2", msg);

     	Intent serviceIntent = new Intent(getActivity(), TransferService.class);
        serviceIntent.setAction(TransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(TransferService.EXTRAS_SEND_DATA, msg);
        serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        Log.d("owner_address",info.groupOwnerAddress.getHostAddress());
        Log.d("send_text3", msg);
        getActivity().startService(serviceIntent);
        
    }
    
    
}

