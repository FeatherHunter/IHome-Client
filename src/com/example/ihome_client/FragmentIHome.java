package com.example.ihome_client;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import ihome_client.bottombar.*;

/** 
 * @CopyRight: ������ 2015~2025
 * @Author Feather Hunter(����)
 * @qq:975559549
 * @Version:1.0 
 * @Date:2015/12/25
 * @Description: IHome��Fragment����
 * @Function List:
 *   1. void onAttach(Activity activity); ���ڰ�ClientMainActivity��handler
 *   2. Handler communicationHandler; //���ڴ����ClientMainActivity��ͨ��
 **/

public class FragmentIHome extends BaseFragment{

	ClientMainActivity mainActivity;
	TextView temp_value, humi_value;
	Button led1_button, led2_button, led3_button;
	Button temp_button, humi_button;
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.ihome_fragment1, container, false);
	}
	
	

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		/*Ҫ��onCreateView֮��õ��ռ������Ч��*/
		temp_value = (TextView) getActivity().findViewById(R.id.temp_value);
		humi_value = (TextView) getActivity().findViewById(R.id.humi_value);
		led1_button = (Button) getActivity().findViewById(R.id.led1_button);
		led2_button = (Button) getActivity().findViewById(R.id.led2_button);
		led3_button = (Button) getActivity().findViewById(R.id.led3_button);
		/*���ü�����*/
  		led1_button.setOnClickListener(new LampButtonListener());
		led2_button.setOnClickListener(new LampButtonListener());
		led3_button.setOnClickListener(new LampButtonListener());
	}



	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		mainActivity = (ClientMainActivity) activity;
		mainActivity.setHandler(communicationHandler);
	}
	/**
	 *  ����Activity����������Ϣ
	 */
	public Handler communicationHandler = new Handler()
	{
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
		    Bundle bundle = msg.getData();
		    String typeString = bundle.getString("type");
		    if(typeString.equals("temp"))/*�����¶�*/
			{
		    	temp_value.setText(bundle.getString("temp"));
			}
		    else if(typeString.equals("humi"))/*����ʪ��*/
			{
		    	humi_value.setText(bundle.getString("humi"));
			}
		    else if(typeString.equals("ledon"))/*���õ�*/
			{
		    	if( bundle.getString("ledon").equals("LED1"))
		    	{
		    		led1_button.setText("��̨��");
		    		led1_button.setTextColor(Color.RED);
		    	}
		    	else if( bundle.getString("ledon").equals("LED2"))
		    	{
		    		led2_button.setText("�رڵ�");
		    		led2_button.setTextColor(Color.RED);
		    	}
		    	else if( bundle.getString("ledon").equals("LED3"))
		    	{
		    		led3_button.setText("�ص���");
		    		led3_button.setTextColor(Color.RED);
		    	}
			}
		    else if(typeString.equals("ledoff"))
		    {
		    	if( bundle.getString("ledoff").equals("LED1"))
		    	{
		    		led1_button.setText("��̨��");
		    		led1_button.setTextColor(Color.GREEN);
		    	}
		    	else if( bundle.getString("ledoff").equals("LED2"))
		    	{
		    		led2_button.setText("���ڵ�");
		    		led2_button.setTextColor(Color.GREEN);
		    	}
		    	else if( bundle.getString("ledoff").equals("LED3"))
		    	{
		    		led3_button.setText("������");
		    		led3_button.setTextColor(Color.GREEN);
		    	}
		    }
		    
		}
	};
	
	class LampButtonListener implements OnClickListener{

		@Override
		public void onClick(View view) {
			// TODO Auto-generated method stub
			int lampid = view.getId();
			String typeString = new String("CONTRL"); 
			String operationString = new String("");
			String numString = new String("");;
			switch(lampid)
			{
				case R.id.led1_button:
					{
						numString = new String("LED1");
						if(led1_button.getText().equals("��̨��"))
						{
							operationString = new String("LEDON");
						}
						else
						{
							operationString = new String("LEDFF");
						}
					
					}
					break;
				case R.id.led2_button:
					{
						
						numString = new String("LED2");
						if(led2_button.getText().equals("���ڵ�"))
						{
							operationString = new String("LEDON");
						}
						else
						{
							operationString = new String("LEDOFF");
						}
				
					}
				    break;
				case R.id.led3_button:
					{
						
						numString = new String("LED3");
						if(led3_button.getText().equals("������"))
						{
							operationString = new String("LEDON");
						}
						else
						{
							operationString = new String("LEDOFF");
						}
			
					}
					break;
			}//end of switch
			
			/*���͹㲥��Service�����䷢����Ϣ��������*/
			Intent intent = new Intent();
			intent.putExtra("type", "send");
			intent.putExtra("typestring", typeString);
			intent.putExtra("operationstring", operationString);
			intent.putExtra("numberstring", numString);
			
			intent.setAction(intent.ACTION_MAIN);
			getActivity().sendBroadcast(intent);
		}
		
	}
	
}