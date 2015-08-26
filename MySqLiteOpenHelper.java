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

	//アクセスするデータベース名
	private static final String DB_NAME = "connection.db";
	
	//DBのバージョン
	private static final int DB_VERSION = 1;
	
	//create table 文
	//connection_data
	private static final String createTableString1 = 
			"create table connection_data(_id integer primary key autoincrement, android_name text, mac_address text)";
	//receive_data
	private static final String createTableString2 = 
			"create table receive_data(android_name text, contents text, ts text)";
	
	public MySqLiteOpenHelper(Context context){
		super(context, DB_NAME,null,DB_VERSION);
	}
	
	//データベースの作成
	@Override
	public void onCreate(SQLiteDatabase db){
		db.execSQL(createTableString1);
		db.execSQL(createTableString2);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
	
	//比較用
	public Boolean compare_data(SQLiteDatabase db, String data){
		
		//比較用の　SQL文　を作成　
		String sql = 
				"SELECT * FROM connection_data WHERE mac_address LIKE "+"\""+data+"\"";
		//sql文の結果の受け取り
		Cursor cur = db.rawQuery(sql, null);
				
		//要素が見つかれば、true /　要素が無ければ　falseを返す
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
    * 	受け取ったデータの分割およびＤＢへの書き込み 及び　
    * 	追加したメッセージを返す
    *====================================================*/
    public String writeDB(SQLiteDatabase db,InputStream inputStream) {
    	char buf[] = new char[1024];
        int numRead;
    	String read_str ="";
    	
        String line ;
        /*=====================================
         *	Inputstreamからの読み込み 
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
         * 	取得したデータを分割
         *=====================================*/
        //分割したデータ保存用の変数
        String[] strAry = read_str.split(",");
    	//メッセージ
        String android_name = strAry[0];
    	String msg = strAry[1];
    	String ts = strAry[2];
    	
    	/*=====================================
    	 * 	sqlを実行し、データベースに追加
    	 *=====================================*/
    	//追加用のsql文を作成
	    String sql = "insert into receive_data values('"+android_name+"','"+msg+"','"+ts+"')";
	    //sql文の実行
	    db.execSQL(sql);
	  	
	  	return msg;
    }
}
