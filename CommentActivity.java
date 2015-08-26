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

/*�R�����g���擾�A�\������Activity*/
public class CommentActivity extends Activity {
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.comment_view);
		
		Button btn1 = (Button)findViewById(R.id.back_view);
		ListView list = (ListView)findViewById(R.id.listView1);
		
		/*�u�R�����g�擾�v�{�^�����^�b�`�����Ƃ��̓���*/
		btn1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Activity�̏I��
				finish();
				
			}
		});
		
		
		/*===================================
		 * �R�����g�f�[�^�̎擾
		 * 	���\��
		 * 	���X�g���^�b�`����
		 *===================================*/
		//�f�[�^�x�[�X�p�̃C���X�^���X�̐���
		MySqLiteOpenHelper helper = new MySqLiteOpenHelper(this);
		
		//�f�[�^�x�[�X���J��
		SQLiteDatabase db = helper.getReadableDatabase();
		
		//�f�[�^���擾
		ArrayList<String> ar = helper.getReceiveData(db);
		ArrayAdapter< String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
		
		ArrayList<String> longtitude = new ArrayList<String>();
		ArrayList<String> lattitude = new ArrayList<String>();
		
		//�f�[�^�𕪊�����
		String name ="";
		String comment ="";
		String days = "";
		for(int i=0;i<ar.size();i++){
			String str = ar.get(i);
			//�擾�����f�[�^�̕\��
			Log.d("db_read",str);
	        //���������f�[�^�ۑ��p�̕ϐ�
	        String[] strAry = str.split(",");
	    	//���b�Z�[�W
	        name = strAry[0];
	    	comment = strAry[1];
	    	days = strAry[2];
	    	
	    	//���W����
	    	lattitude.add(i,strAry[3]);
	    	longtitude.add(i,strAry[4]);
	    	
			//���X�g�ɒǉ�����
			adapter.add(name + " : " + comment +" : "+days);
			list.setAdapter(adapter);
		}
	}
	
	
}
