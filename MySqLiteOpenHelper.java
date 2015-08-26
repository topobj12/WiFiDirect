package com.example.directnetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
			"create table receive_data(android_name text, contents text, ts text)";
	
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
    	char buf[] = new char[1024];
        int numRead;
    	String read_str ="";
    	
        String line ;
        /*=====================================
         *	Inputstream����̓ǂݍ��� 
         *====================================*/
        try{
        	InputStreamReader reader = new InputStreamReader(inputStream);
        	BufferedReader br = new BufferedReader(reader);
        	StringBuilder builder = new StringBuilder();
//        	while(0 <= (numRead = reader.read(buf))){
//        		builder.append(buf, 0, numRead);
//        	}
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
        //���������f�[�^�ۑ��p�̕ϐ�
        String[] strAry = read_str.split(",");
    	//���b�Z�[�W
        String android_name = strAry[0];
    	String msg = strAry[1];
    	String ts = strAry[2];
    	
    	/*=====================================
    	 * 	sql�����s���A�f�[�^�x�[�X�ɒǉ�
    	 *=====================================*/
    	//�ǉ��p��sql�����쐬
	    String sql = "insert into receive_data values('"+android_name+"','"+msg+"','"+ts+"')";
	    //sql���̎��s
	    db.execSQL(sql);
	  	
	  	return msg;
    }
}
