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
	
	//�ϐ�
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
				 * send�t���O
				 *=======================*/
//				send = true;
				MainActivity.SEND_PHASE = 1;
				/*=======================
				 * �[������
				 *======================*/
				WifiP2pManager manager = (WifiP2pManager) getActivity().getSystemService(Context.WIFI_P2P_SERVICE);
		        Channel channel = manager.initialize(getActivity(), getActivity().getMainLooper(), null);	// p2p�@�\�𗘗p����̂ɕK�v�ȃC���X�^���X

		        
                final DeviceList fragment = (DeviceList) getFragmentManager()
                        .findFragmentById(R.id.frag_list);	//fragment��p�ӂ���
                fragment.onInitiateDiscovery();		//
                
                //���M�̒[���̐ڑ��󋵂̊m�F 
                TextView view = (TextView)fragment.mContentView.findViewById(R.id.my_status);
                String st = view.getText().toString();
                //Connected��Ԃ̏ꍇ
                if(st == "Connected"){
                	return;
                }

                //�s�A�̌������J�n����
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
    
    //device��status�𔻒f���邽�߂̃��\�b�h
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
    //peer��I�������Ƃ��̏���
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
        
        
        //�I�������f�o�C�X�̏ڍׂ�\��
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

        
        //���������f�o�C�X�ꗗ��View���쐬
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
    //�������g�̃f�o�C�X���̍X�V
    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        TextView view = (TextView) mContentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);
        view = (TextView) mContentView.findViewById(R.id.my_status);
        
        //status��TextView�ɔ��f
        this.send_data = device.deviceName;
        writeFile(this.send_data, 1);
        view.setText(getDeviceStatus(device.status));
  
        MainActivity.MY_ADDRESS = device.deviceAddress;
    }

    //�L���ȃf�o�C�X���X�g�̎擾 /�@�󂯎����WifiP2pDeviceList�� peers�ɕۑ�
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        mPeerList = peerList;
        
        //ListAdapter�ɑ΂��ăf�[�^�X�V��ʒm���邱�ƂŃ��X�g���X�V
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            Log.d(MainActivity.TAG, "No devices found");
            return;
        }

        
        if(MainActivity.SEND_PHASE == 1){
	         /*==========================================================
	          * peer�I���@���@�ڑ��@���@�ڍ� 
	          *========================================================== */
	         /*===========================================================*/
	         //�ϐ�
	         int item_num =0;
	         //�ڑ��ݒ�
	         WifiP2pDevice device ; 
	         
	         
	         /*========================================================
	          * 	�f�[�^�x�[�X��ɐڑ��悪���݂��邩�ǂ������`�F�b�N���A
	          * 	1�D���݂��Ȃ��ꍇ�A�ڑ����ǉ� 
	          * 	2�D���ɑ��݂���ꍇ�A����Item��I������
	          *  3.���ׂĂ̐ڑ��悪���݂���ꍇ�́A��ԏ�̐ڑ���𗘗p����
	          * =======================================================*/
	         
	         //�f�[�^�x�[�X�p�̃C���X�^���X�𐶐�
	         MySqLiteOpenHelper helper = new MySqLiteOpenHelper(getActivity());
	         
	         //�ǂݏ����ł���悤�ɊJ��
	         db = helper.getWritableDatabase();
	         
	         //�s�A���X�g��item��
	         int count_peers = getListAdapter().getCount();
	         
	         //�f�[�^�x�[�X��ɑ��݂������ǂ����̔��f
	         Boolean db_exist = false;
	         
	         /*========================================================*/
	         for(int i=0; i<count_peers; i++){
	         	//device���̎擾
	         	device = (WifiP2pDevice) getListAdapter().getItem(i);
	         	
	         	//�f�[�^�x�[�X��ɑ��݂��Ȃ��ꍇ
	             if(!helper.compare_data(db, device.deviceAddress)){
	             	//log�ɏ����o��
	             	String str = "DB : "+device.deviceAddress+" not exist \n";
	             	writeFile(str,0);
	             	//���R�[�h��ݒ�
	     	        ContentValues values = new ContentValues();
	     	        values.put("android_name", device.deviceName);
	     	        values.put("mac_address",device.deviceAddress);
	     	        //���R�[�h��ǉ�
	     	        db.insert("connection_data", null, values);    	        
	     	        //�N���[�Y����
	     	        db.close();
	     	        values.clear();
	     	        helper.close();
	     	        item_num = i;
	     	        db_exist = true;
	     	        break;
	             }        
	             //�f�[�^�x�[�X��ɑ��݂���ꍇ
	             else{
	             	//log�ɏ����o��
	             	String str = "DB : "+device.deviceAddress+" exist \n";
	             	writeFile(str,0);
	             }   
	         }
	        
	         
	         //�f�[�^�x�[�X��ɂ��ׂđ��݂����ꍇ�̏���
	         if(db_exist == false){
	         	item_num = 0;
	         }
	         
	         /*===========================================================
	          * 	�ڑ�����
	          * =========================================================*/
	         
	         /*==========================================================*/
	         //�ڑ��ݒ�
	         device = (WifiP2pDevice) getListAdapter().getItem(item_num); 
	         //�ڑ��ς݂��ǂ����̔��f
	         if(device.status != WifiP2pDevice.CONNECTED){
	         	WifiP2pConfig config = new WifiP2pConfig();	//WifiP2pConfig�N���X�̃C���X�^���X����	 
	 	        config.deviceAddress = device.deviceAddress;	//�ڑ��f�o�C�X��MAC�A�h���X�̐ݒ�
	 	        config.wps.setup = WpsInfo.PBC;	//WpsInfo �� Wi-Fi Protected Setup  / PBC �� Push button configuration �����^�b�`�Őڑ��ݒ�ł���悤�Ɏw��
	 	        if (progressDialog != null && progressDialog.isShowing()) {
	 	            progressDialog.dismiss();
	 	        }
	 	        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
	 	                "Connecting to :" + device.deviceAddress, true, true
	 	
	 	                );
	 	        //MainActivity��connect���\�b�h���Ăяo���Đڑ�
	 	        ((DeviceActionListener) getActivity()).connect(config);
	 	
	 	        /*========================================================
	 	         * 	�ڑ����O���t�@�C���ɒǉ��@
	 	         * =======================================================*/
	 	        //��������
	 	        String out_text = "connection : "+device.deviceAddress+" : "+device.deviceName+" : "+getCalendar()+" / " +device.status+" \n";
	 	        writeFile(out_text,0);     
	 		 	 //���M�f�[�^�̍쐬
	 	       EditText text = (EditText) mContentView.findViewById(R.id.SendText);
	 	       TextView name = (TextView) mContentView.findViewById(R.id.my_name); 
	 	       SEND_DATA = name.getText()+","+text.getText().toString()+","+getCalendar()+",34.147662,131.467635,";
	 	        
	 	        //�I�������f�o�C�X�̏ڍׂ�\��
	 	        ((DeviceActionListener) getActivity()).showDetails(device);	
	 	        Log.d("check_device", "device showDetails");

	             /*=========================================================
	              * SEND_PHASE��ύX
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

   
    //�[���̌������J�n���ɌĂяo��
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
     * 		�t�@�C���o�͗p
     * ================================================*/
    public void writeFile(String out_text,int num){
    	File directory = Environment.getExternalStorageDirectory();
    	String file_path = directory.getAbsolutePath()+"/log_device.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
        BufferedOutputStream out_stream ;
        
        if(num == 0){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/log_device.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
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
        	file_path = directory.getAbsolutePath()+"/send_data.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
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
     * 	���݂̎��Ԃ̎擾
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
     * 		�t�@�C�����͗p
     * ================================================*/
    public String readFile(int num){
    	File directory = Environment.getExternalStorageDirectory();
    	String file_path = directory.getAbsolutePath()+"/log_device.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
        BufferedInputStream in_stream ;
        
        byte[] fbytes = new byte[1024];
        String msg = "";
        
        if(num == 0){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/log_device.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
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
        	file_path = directory.getAbsolutePath()+"/send_data.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
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
        	file_path = directory.getAbsolutePath()+"/ipaddress.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
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
