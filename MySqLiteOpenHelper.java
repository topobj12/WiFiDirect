package com.example.directnetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySqLiteOpenHelper extends SQLiteOpenHelper{

	//�A�N�Z�X����f�[�^�x�[�X��
	private static final String DB_NAME = "connection.db";
	
	//DB�̃o�[�W����
	private static final int DB_VERSION = 1;
	
	//create table ��
	//connection_data
	private static final String createTableString1 = 
			"create table connection_data(_id integer primary key autoincrement, android_name text, mac_address text)";
	//receive_data
	private static final String createTableString2 = 
			"create table receive_data(android_name text, contents text, ts text, latitude double, longtitude double)";
	
	public MySqLiteOpenHelper(Context context){
		super(context, DB_NAME,null,DB_VERSION);
	}
	
	//�f�[�^�x�[�X�̍쐬
	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL(createTableString1);
		db.execSQL(createTableString2);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
	
	//��r�p
	public Boolean compare_data(SQLiteDatabase db, String data){
		
		//��r�p�́@SQL���@���쐬�@
		String sql = 
				"SELECT * FROM connection_data WHERE mac_address LIKE "+"\""+data+"\"";
		//sql���̌��ʂ̎󂯎��
		Cursor cur = db.rawQuery(sql, null);
				
		//�v�f��������΁Atrue /�@�v�f��������΁@false��Ԃ�
		if(cur.getCount()!=0){
			cur.close();
			return true;
		}
		else{
			cur.close();
			return false;
		}
	}
	
	/*====================================================
    * 	�󂯎�����f�[�^�̕�������тc�a�ւ̏������� �y�с@
    * 	�ǉ��������b�Z�[�W��Ԃ�
    *====================================================*/
    public String writeDB(SQLiteDatabase db,InputStream inputStream) {

    	String read_str ="";
    	String received_msg = "";
        String line ;
        /*=====================================
         *	Inputstream����̓ǂݍ��� 
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
        
        /*=====================================
         * 	�擾�����f�[�^�𕪊�
         *=====================================*/
        //���C�����Ƃɕ���
        String[] line_str = read_str.split(",,");
        for(int i=0;i<line_str.length;i++){
        	Log.d("read_line", line_str[i]);
            //�f�[�^�𕪊�
            String[] strAry = line_str[i].split(",");        //���������f�[�^�ۑ��p�̕ϐ�
        	//���b�Z�[�W
            String android_name = strAry[0];
        	String msg = strAry[1];
        	String ts = strAry[2];
        	String latitude = strAry[3];
        	String longtitude = strAry[4];
        	if(i==0){
        		received_msg = msg;
        	}
//        	if(msg != "FINISH"){
    	    	/*=====================================
    	    	 * 	sql�����s���A�f�[�^�x�[�X�ɒǉ�
    	    	 *=====================================*/
    	    	//�ǉ��p��sql�����쐬
    		    String sql = "insert into receive_data values('"+android_name+"','"+msg+"','"+ts+"','"+latitude+"','"+longtitude+"')";
    		    //sql���̎��s
    		    db.execSQL(sql);
//        	}        	
        }


	  	
	  	return received_msg;
    }
    /*==============================================
     *  writeDB String 
     * 
     *==============================================*/
    public String writeDB(SQLiteDatabase db,String[] strAry) {
        
        /*=====================================
         * 	�擾�����f�[�^��ݒ�
         *=====================================*/
      	
        //���b�Z�[�W
        String android_name = strAry[0];
        String msg = strAry[1];
        String ts = strAry[2];
        String latitude = strAry[3];
        String longtitude = strAry[4];
        /*=====================================
   	   	 * 	sql�����s���A�f�[�^�x�[�X�ɒǉ�
   	   	 *=====================================*/
   	   	//�ǉ��p��sql�����쐬
   	    String sql = "insert into receive_data values('"+android_name+"','"+msg+"','"+ts+"','"+latitude+"','"+longtitude+"')";
   	    //sql���̎��s
   	    db.execSQL(sql);
      	return msg;
    }
    
    /*=============================================
     * receive_data�̃f�[�^�����ׂĎ擾����
     * 
     *============================================*/
    public ArrayList<String> getReceiveData(SQLiteDatabase db){
    	
    	ArrayList<String> ar = new ArrayList<String>();
        
        /*=====================================
    	 * 	sql�����s���A�f�[�^�x�[�X����擾
    	 *=====================================*/
        String sql = "SELECT * FROM receive_data";
        //sql���̌��ʂ̎󂯎��
      	Cursor cur = db.rawQuery(sql, null);
      	cur.moveToFirst();
      	if(cur.getCount() == 0){
      		ar.add("no_data");
      		Log.d("received_data", ar.get(0));
      		cur.close();
      		return ar;
      	}
      	
        for(int i=0;i<cur.getCount();i++){
        	String str = cur.getString(cur.getColumnIndex("android_name"))+","+cur.getString(cur.getColumnIndex("contents"))+","
        			+cur.getString(cur.getColumnIndex("ts"))+","+cur.getString(cur.getColumnIndex("latitude"))
        			+","+cur.getString(cur.getColumnIndex("longtitude"))+",";
        	Log.d("received_data", str);
        	ar.add(str);
        	cur.moveToNext();
        }
        cur.close();
	  	return ar;
    }
}
