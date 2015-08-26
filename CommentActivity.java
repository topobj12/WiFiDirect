package com.example.directnetwork;

import java.util.ArrayList;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

/*コメントを取得、表示するActivity*/
public class CommentActivity extends Activity {
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.comment_view);
		
		Button btn1 = (Button)findViewById(R.id.back_view);
		ListView list = (ListView)findViewById(R.id.listView1);
		
		/*「コメント取得」ボタンをタッチしたときの動作*/
		btn1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Activityの終了
				finish();
				
			}
		});
		
		
		/*===================================
		 * コメントデータの取得
		 * 	→表示
		 * 	リスト→タッチ動作
		 *===================================*/
		//データベース用のインスタンスの生成
		MySqLiteOpenHelper helper = new MySqLiteOpenHelper(this);
		
		//データベースを開く
		SQLiteDatabase db = helper.getReadableDatabase();
		
		//データを取得
		ArrayList<String> ar = helper.getReceiveData(db);
		ArrayAdapter< String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
		
		ArrayList<String> longtitude = new ArrayList<String>();
		ArrayList<String> lattitude = new ArrayList<String>();
		
		//データを分割する
		String name ="";
		String comment ="";
		String days = "";
		for(int i=0;i<ar.size();i++){
			String str = ar.get(i);
			//取得したデータの表示
			Log.d("db_read",str);
	        //分割したデータ保存用の変数
	        String[] strAry = str.split(",");
	    	//メッセージ
	        name = strAry[0];
	    	comment = strAry[1];
	    	days = strAry[2];
	    	
	    	//座標入力
	    	lattitude.add(i,strAry[3]);
	    	longtitude.add(i,strAry[4]);
	    	
			//リストに追加する
			adapter.add(name + " : " + comment +" : "+days);
			list.setAdapter(adapter);
		}
	}
	
	
}
