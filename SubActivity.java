package com.example.directnetwork;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SubActivity extends Activity{
	
	//変数
	
	Activity act = this;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.output);
		
		Button btn1 = (Button)findViewById(R.id.get_comments);
		Button btn2 = (Button)findViewById(R.id.get_db);
		Button btn3 = (Button)findViewById(R.id.back_bt);
		
		/*「コメント取得」ボタンをタッチしたときの動作*/
		btn1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// intentのインスタンス生成
				Intent intent = new Intent(SubActivity.this, CommentActivity.class);
				//次のactivityの起動
				startActivity(intent);
			}
		});
		
		/*「ＤＢ取得」ボタンをタッチしたときの動作*/
		btn2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
		
		/*「戻る」ボタンをタッチしたときの動作*/
		btn3.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Activityの終了 →　元の画面に戻る
 				finish();
			}
		});
	}
}
