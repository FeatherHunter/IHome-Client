package com.example.ihome_client;

import java.io.UnsupportedEncodingException;

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
 * @CopyRight: 王辰浩 2015~2025
 * @Author Feather Hunter(猎羽)
 * @qq:975559549
 * @Version:1.0 
 * @Date:2015/12/25
 * @Description: IHome的Fragment界面
 * @Function List:
 *   1. void onAttach(Activity activity); 用于绑定ClientMainActivity和handler
 *   2. Handler communicationHandler; //用于处理和ClientMainActivity的通信
 **/

public class FragmentIHome extends BaseFragment{

	ClientMainActivity mainActivity;
	TextView temp_value, humi_value;
	Button led1_button, led2_button, led3_button;
	Button temp_button, humi_button;
	
	byte COMMAND_MANAGE = 1;
	byte COMMAND_CONTRL = 2;
	byte COMMAND_RESULT = 3;
	byte MAN_LOGIN      = 11;
	byte CTL_LAMP       = 21;
	byte CTL_GET        = 22;
	byte RES_LOGIN      = 31;
	byte RES_LAMP       = 32;
	byte RES_TEMP       = 33;
	byte RES_HUMI       = 34;
	byte LOGIN_SUCCESS  = 1;
	byte LOGIN_FAILED   = 2;
	byte LAMP_ON        = 1;
	byte LAMP_OFF       = 2;
	byte COMMAND_SEPERATOR = 31;//31单元分隔符
	byte COMMAND_END    = 30; //30,一个指令结束
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.ihome_fragment1, container, false);
	}
	
	

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		/*要在onCreateView之后得到空间才是有效的*/
		temp_value = (TextView) getActivity().findViewById(R.id.temp_value);
		humi_value = (TextView) getActivity().findViewById(R.id.humi_value);
		led1_button = (Button) getActivity().findViewById(R.id.led1_button);
		led2_button = (Button) getActivity().findViewById(R.id.led2_button);
		led3_button = (Button) getActivity().findViewById(R.id.led3_button);
		/*设置监听器*/
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
	 *  处理Activity传递来的信息
	 */
	public Handler communicationHandler = new Handler()
	{
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
		    Bundle bundle = msg.getData();
		    String typeString = bundle.getString("type");
		    if(typeString.equals("temp"))/*设置温度*/
			{
		    	temp_value.setText(bundle.getString("temp"));
			}
		    else if(typeString.equals("humi"))/*设置湿度*/
			{
		    	humi_value.setText(bundle.getString("humi"));
			}
		    else if(typeString.equals("ledon"))/*设置灯*/
			{
		    	if( bundle.getString("ledon").equals("0"))
		    	{
		    		led1_button.setText("关台灯");
		    		led1_button.setTextColor(Color.RED);
		    	}
		    	else if( bundle.getString("ledon").equals("1"))
		    	{
		    		led2_button.setText("关壁灯");
		    		led2_button.setTextColor(Color.RED);
		    	}
		    	else if( bundle.getString("ledon").equals("2"))
		    	{
		    		led3_button.setText("关吊灯");
		    		led3_button.setTextColor(Color.RED);
		    	}
			}
		    else if(typeString.equals("ledoff"))
		    {
		    	if( bundle.getString("ledoff").equals("0"))
		    	{
		    		led1_button.setText("开台灯");
		    		led1_button.setTextColor(Color.GREEN);
		    	}
		    	else if( bundle.getString("ledoff").equals("1"))
		    	{
		    		led2_button.setText("开壁灯");
		    		led2_button.setTextColor(Color.GREEN);
		    	}
		    	else if( bundle.getString("ledoff").equals("2"))
		    	{
		    		led3_button.setText("开吊灯");
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
			byte type = COMMAND_CONTRL;
			byte subtype = CTL_LAMP;
			byte operator = 0;
			String IDString = new String("");;
			switch(lampid)			{
				case R.id.led1_button:
					{
						IDString = new String("0");
						if(led1_button.getText().equals("开台灯"))
						{
							operator = LAMP_ON;
						}
						else
						{
							operator = LAMP_OFF;
						}
					
					}
					break;
				case R.id.led2_button:
					{
						
						IDString = new String("1");
						if(led2_button.getText().equals("开壁灯"))
						{
							operator = LAMP_ON;
						}
						else
						{
							operator = LAMP_OFF;
						}
				
					}
				    break;
				case R.id.led3_button:
					{
						
						IDString = new String("2");
						if(led3_button.getText().equals("开吊灯"))
						{
							operator = LAMP_ON;
						}
						else
						{
							operator = LAMP_OFF;
						}
			
					}
					break;
			}//end of switch
			
			try {
				/*需要发送的指令,byte数组*/
				byte typeBytes[] = {type,COMMAND_SEPERATOR};
				byte subtypeBytes[] = {COMMAND_SEPERATOR,subtype, COMMAND_SEPERATOR};
				byte operatorBytes[] = {operator, COMMAND_SEPERATOR};
				byte IDBytes[] = IDString.getBytes("UTF-8");
				byte endBytes[] = {COMMAND_SEPERATOR, COMMAND_END};						
				byte buffer[] = new byte[subtypeBytes.length+operatorBytes.length
				                       +IDBytes.length+endBytes.length];
				
				/*转换account后面所有指令*/
				int start = 0;
				System.arraycopy(subtypeBytes ,0,buffer,start, subtypeBytes.length);
				start+=subtypeBytes.length;
				System.arraycopy(operatorBytes ,0,buffer,start, operatorBytes.length);
				start+=operatorBytes.length;
				System.arraycopy(IDBytes,0,buffer,start, IDBytes.length);
				start+=IDBytes.length;
				System.arraycopy(endBytes   ,0,buffer,start, endBytes.length);
				
				/*发送广播给Service，让其发送信息给服务器*/
				Intent intent = new Intent();
				intent.putExtra("type", "send");
				intent.putExtra("onefield", typeBytes);
				intent.putExtra("twofield", buffer);
				intent.setAction(intent.ACTION_MAIN);
				getActivity().sendBroadcast(intent);
				
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
}