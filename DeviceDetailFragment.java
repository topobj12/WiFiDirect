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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * ����̃s�A���Ǘ����A�l�b�g���[�N�ڑ���ݒ肵�A�f�[�^��]������t���O�����g
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

	//�萔
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    
    //�ϐ�
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    //Activity
    Activity act ;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.act = getActivity();
    }

    //view���쐬���A�쐬����view��Ԃ�
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    	//device_detail����layout�ǂݍ���
        mContentView = inflater.inflate(R.layout.device_detail, null);
        
        //connect�{�^�����^�b�`�����Ƃ��̏���
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            	//�ڑ��ݒ�
                WifiP2pConfig config = new WifiP2pConfig();	//WifiP2pConfig�N���X�̃C���X�^���X����	 
                config.deviceAddress = device.deviceAddress;	//�ڑ��f�o�C�X��IP�A�h���X�̐ݒ�
                config.wps.setup = WpsInfo.PBC;	//WpsInfo �� Wi-Fi Protected Setup  / PBC �� Push button configuration �����^�b�`�Őڑ��ݒ�ł���悤�Ɏw��
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true

                        );
                //MainActivity��connect���\�b�h���Ăяo���Đڑ�
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        
        //disconnect�{�^�����^�b�`�����Ƃ��̏���
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    	//MainActivity��disconnect���\�b�h���Ăяo���Đؒf
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        //Send�{�^�����^�b�`�����Ƃ��̏���
        
        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                    	/*======================================
                    	 * �t�@�C�����瑗�M�p�f�[�^���擾
                    	 *=====================================*/
                    	String msg = readFile(1);
                    	
                    	Log.d("send_data msg", msg);
                    	/*======================================
                    	 *	�f�[�^�̑��M 
                    	 *=======================================*/
                    	writeFile(msg, 0);
                    	
                    	
                    	Intent serviceIntent = new Intent(getActivity(), TransferService.class);
                        serviceIntent.setAction(TransferService.ACTION_SEND_FILE);
                        serviceIntent.putExtra(TransferService.EXTRAS_SEND_DATA, msg);
                        serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                info.groupOwnerAddress.getHostAddress());
                        serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
                        getActivity().startService(serviceIntent);
                    	
                    }
                });

        return mContentView;
    }

    //intent�Ŏ󂯎�����f�[�^�𑗐M
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    	Log.d("onActivityResult", "onActivityResult run !!");
//        // User has picked an image. Transfer it to group owner i.e peer using
//        // TransferService.
//        Uri uri = data.getData();
//        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
//        statusText.setText("Sending: " + uri);
//        Log.d(MainActivity.TAG, "Intent----------- " + uri);
//        Intent serviceIntent = new Intent(getActivity(), TransferService.class);
//        serviceIntent.setAction(TransferService.ACTION_SEND_FILE);
//        serviceIntent.putExtra(TransferService.EXTRAS_FILE_PATH, uri.toString());
//        serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_ADDRESS,
//                info.groupOwnerAddress.getHostAddress());
//        serviceIntent.putExtra(TransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
//        getActivity().startService(serviceIntent);
    }

    //WifiP2pInfo�N���X����IP�A�h���X���擾�A�\��
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
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());	//�z�X�g�A�h���X��TextView�ɕ\��

        if (info.groupFormed && info.isGroupOwner) {
            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text), getActivity())
                    .execute();
        } else if (info.groupFormed) {
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));
        }
        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }


    //�ڍׂ�\��
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);//�����A�h���X���Z�b�g
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());//�[�������Z�b�g

    }

    //view�̏�����
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


    /*=======================================================================
     * 	�񓯊�����
     * 	��M�����f�[�^���o�͂��鏈���Ȃ�
     * 
     * 	onPreExcute ���@doInBackground �� onPostExecute 
     * 	�̏��ԂŌĂ΂��
     * ======================================================================*/
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;
        
        private Activity activity;
        
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
         * 	onPreExecute�̌�ɌĂяo�����
         *=============================================================*/
        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(MainActivity.TAG, "Server: Socket opened");
                //�f�[�^�̎󂯎��
                Socket client = serverSocket.accept();
                Log.d(MainActivity.TAG, "Server: connection done");

                InputStream inputstream = client.getInputStream();
                /*===============================================
                 * 	�󂯎�����f�[�^���f�[�^�x�[�X�Ɋi�[����
                 *===============================================*/
                /*===============================================*/
                
                //�f�[�^�x�[�X�p�̃C���X�^���X�𐶐�
                MySqLiteOpenHelper helper = new MySqLiteOpenHelper(activity);
                
                //�ǂݏ����ł���悤�ɊJ��
                SQLiteDatabase db = helper.getWritableDatabase();
                
                //�󂯎�����f�[�^���f�[�^�x�[�X�ɒǉ�
                String msg = helper.writeDB(db, inputstream);
                db.close();
                helper.close();
                serverSocket.close();
                
                //�󂯎�����f�[�^���t�@�C���ɏ����o��
                writeFile(msg, 1);
                
                return msg;
                /*===============================================*/

            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*=========================================================
         * doInBackground�̖߂�l���p�����[�^�Ƃ��ČĂяo��
         * ========================================================*/
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
            	String text = "send comment - " + result + "\n";
            	
                statusText.setText(text);
            }

        }

        /*==============================================
         * UI�X���b�h�ōŏ��ɌĂяo��
         * UI�Ɋւ�鏈���͂����ōs��
         *=============================================*/
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
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
            	file_path = directory.getAbsolutePath()+"/recieved_data.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
            	try{
                	out_stream = new BufferedOutputStream(new FileOutputStream(file_path));
                	out_stream.write(out_text.getBytes());		
                	out_stream.close();
                }catch(Exception e){
                	Log.e("write_received", "file error 1");
                }
            }
            
        }
        
    }

    //�t�@�C���̃R�s�[�@
    /*====================================================
     * 	�����ŁA����Ƃ����f�[�^�̏������ݏ���?
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
            	Log.e("write_file", "file error 0");
            }
        }
        return "nothing";
    }
    
    /*=================================================
     * 		�t�@�C���o�͗p
     * ================================================*/
    public void writeFile(String out_text,int num){
    	File directory = Environment.getExternalStorageDirectory();
    	String file_path = directory.getAbsolutePath()+"/log_send.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
        BufferedOutputStream out_stream ;
        
        if(num == 0){
        	directory = Environment.getExternalStorageDirectory();
        	file_path = directory.getAbsolutePath()+"/log_send.txt"; // �����X�g���[�W�̃��[�g�f�B���N�g�����w��
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
}

