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
			"create table receive_data(android_name text, contents text, ts text, latitude double, longtitude double)";
	
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

    	String read_str ="";
    	String received_msg = "";
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
        
        /*=====================================
         * 	取得したデータを分割
         *=====================================*/
        //ラインごとに分割
        String[] line_str = read_str.split(",,");
        for(int i=0;i<line_str.length;i++){
        	Log.d("read_line", line_str[i]);
            //データを分割
            String[] strAry = line_str[i].split(",");        //分割したデータ保存用の変数
        	//メッセージ
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
    	    	 * 	sqlを実行し、データベースに追加
    	    	 *=====================================*/
    	    	//追加用のsql文を作成
    		    String sql = "insert into receive_data values('"+android_name+"','"+msg+"','"+ts+"','"+latitude+"','"+longtitude+"')";
    		    //sql文の実行
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
         * 	取得したデータを設定
         *=====================================*/
      	
        //メッセージ
        String android_name = strAry[0];
        String msg = strAry[1];
        String ts = strAry[2];
        String latitude = strAry[3];
        String longtitude = strAry[4];
        /*=====================================
   	   	 * 	sqlを実行し、データベースに追加
   	   	 *=====================================*/
   	   	//追加用のsql文を作成
   	    String sql = "insert into receive_data values('"+android_name+"','"+msg+"','"+ts+"','"+latitude+"','"+longtitude+"')";
   	    //sql文の実行
   	    db.execSQL(sql);
      	return msg;
    }
    
    /*=============================================
     * receive_dataのデータをすべて取得する
     * 
     *============================================*/
    public ArrayList<String> getReceiveData(SQLiteDatabase db){
    	
    	ArrayList<String> ar = new ArrayList<String>();
        
        /*=====================================
    	 * 	sqlを実行し、データベースから取得
    	 *=====================================*/
        String sql = "SELECT * FROM receive_data";
        //sql文の結果の受け取り
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
