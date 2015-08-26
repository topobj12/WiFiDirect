package com.example.directnetwork;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SubActivity extends Activity{
	
	//�ϐ�
	
	Activity act = this;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.output);
		
		Button btn1 = (Button)findViewById(R.id.get_comments);
		Button btn2 = (Button)findViewById(R.id.get_db);
		Button btn3 = (Button)findViewById(R.id.back_bt);
		
		/*�u�R�����g�擾�v�{�^�����^�b�`�����Ƃ��̓���*/
		btn1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// intent�̃C���X�^���X����
				Intent intent = new Intent(SubActivity.this, CommentActivity.class);
				//����activity�̋N��
				startActivity(intent);
			}
		});
		
		/*�u�c�a�擾�v�{�^�����^�b�`�����Ƃ��̓���*/
		btn2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
		
		/*�u�߂�v�{�^�����^�b�`�����Ƃ��̓���*/
		btn3.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Activity�̏I�� ���@���̉�ʂɖ߂�
 				finish();
			}
		});
	}
}
