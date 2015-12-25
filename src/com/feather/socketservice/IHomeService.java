package com.feather.socketservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;

import com.example.ihome_client.ClientActivity;
import com.example.ihome_client.ClientMainActivity;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

/**
 * @CopyRight: ������ 2015~2025
 * @qq:975559549
 * @Author Feather Hunter(����)
 * @Version:1.0
 * @Date:2015/12/25
 * @Description: ��̨service����,�����������֮���ͨ�š�
 *              �����յ�����Ϣ��������󣬹㲥������activity��
 * @Function list:
 *     1. int onStartCommand(Intent intent, int flags, int startId)//��������activity���͹���������
 *     2. authRunnable //��֤�̣߳����������Ϣ��������
 *     3. serverConnectRunnable //���ӷ��������߳�
 *     4. allInfoFlushRunnable //���������Եõ��¶ȵ����в���������
 *     5. revMsgRunnable       //����ѭ�����շ�������������Ϣ
 *     6. void onDestroy() //�ر�socket
 **/

public class IHomeService extends Service{

	Socket serverSocket;
	OutputStream outputStream; //ouput to server
	InputStream inputStream;   //input from server
	char seperator = (char) 31;//31��Ԫ�ָ���
	String account, password;
	public boolean isConnected = false;
//	private ServiceBinder serviceBinder = new ServiceBinder();
	private int sleeptime = 100;
	private boolean accountReady = false;  //�Ƿ�������ȷ���û��ʻ���Ϣ������
	private boolean isAuthed = false;
	private boolean stopallthread = false;
	byte buffer[] = new byte[2048]; //2048�ֽڵ�����
	
	private ServiceReceiver serviceReceiver;
	private String SERVICE_ACTION = "android.intent.action.MAIN";
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		stopallthread = false; //���������߳�
		Thread thread = new Thread(serverConnectRunnable);//���ӷ�����
		thread.start();	
		/*����������Ϣ���߳�*/
		thread = new Thread(revMsgRunnable);
		thread.start();	
		/*�����������ݺ�����*/
		thread = new Thread(allInfoFlushRunnable);
		thread.start();
		
		/*��̬ע��receiver*/
		serviceReceiver = new ServiceReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(SERVICE_ACTION);
		registerReceiver(serviceReceiver, filter);//ע��
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		if(intent != null)
		{
			String commandString = intent.getStringExtra("command");
			if(commandString.equals("auth"))
			{
				account = intent.getStringExtra("account");
				password = intent.getStringExtra("password");
				accountReady = true; //�û���Ϣ׼������
			}
			/*ֹͣ�����߳�*/
			else if(commandString.equals("stop"))
			{
				stopallthread = true;
			}
			
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(serverSocket != null)
		{
			try {
				serverSocket.close(); //�ر�socket
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		unregisterReceiver(serviceReceiver); //���Receiver
	}

	/**
	 * @Description:���ӵ�����������,����������Ϣ���߳�
	 **/ 
	Runnable serverConnectRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true)
			{
				if(stopallthread)
				{
					break;
				}
				/*tcp���ӳɹ�,�û���Ϣû��׼����,��֤�ɹ�---����������һ������д���*/
				while((isConnected == true)&&(!accountReady)&&(isAuthed == true)) 
				{
					try {
						Thread.sleep(1000);  //˯��
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if(isConnected == false)
				{
					/*������������*/
					Intent intent = new Intent();
					intent.setAction(intent.ACTION_EDIT);
					intent.putExtra("type", "disconnect");
					intent.putExtra("disconnect", "connecting");
					sendBroadcast(intent);
					
					/*֮ǰ�й�socket����,�ȹرգ��ٿ����µ�*/
					if(serverSocket != null)
					{
						try {
							serverSocket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					/*�Ͽ����Ӻ�,�������Ӻ������֤*/
					try {
						serverSocket = new Socket("139.129.19.115", 8080);
						/*�õ��������������*/
						outputStream = serverSocket.getOutputStream();
						inputStream = serverSocket.getInputStream();
						isConnected = true; //�B�ӳɹ�
						
						/*����activity�������ӳɹ�*/
						intent.setAction(intent.ACTION_EDIT);
						intent.putExtra("type", "disconnect");
						intent.putExtra("disconnect", "connected");
						sendBroadcast(intent);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isConnected = false; //�B��ʧ��
						try {
							Thread.sleep(2500);  //ʧ�ܺ�ȴ�3s����
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isConnected = false; //����ʧ��
						try {
							Thread.sleep(2500);  //ʧ�ܺ�ȴ�3s����
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}	
				}
				if(isConnected && accountReady && (isAuthed == false))
				{
					/*֪ͨactivity������֤��Ϣ*/
					Intent intent = new Intent();
					intent.setAction(intent.ACTION_EDIT);
					intent.putExtra("type", "disconnect");
					intent.putExtra("disconnect", "authing");
					sendBroadcast(intent);
					/*�û������֤����*/
					String loginRequestString = new String("MANAGE"+seperator+account+seperator
							+"LOGIN"+seperator+password+seperator);
					byte buffer[] = loginRequestString.getBytes();
					try {
						outputStream.write(buffer, 0, buffer.length);
						outputStream.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isConnected = false; //����ʧ��
						isAuthed = false;    //��֤ʧЧ
						try {
							Thread.sleep(2500);  //�����֤ʧ�ܺ�ȴ�3s
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					try {
						Thread.sleep(1000);  //�����֤ʧ�ܺ�ȴ�1s
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}//end of �����֤

			}
			
		}
	};
	
	
	/**
	 * @Description:���ڶ�ʱ�õ��¶�,ʪ��,�Ƴ�ʼ��Ϣ---������������
	 **/
	Runnable allInfoFlushRunnable = new Runnable() {

		String RequestString;
		public void run() {
			// TODO Auto-generated method stub
			
			while(true)
			{
				if(stopallthread)
				{
					break;
				}
				while(isAuthed == false)//�ȴ��������Ӻ������֤
				{
					try {
						Thread.sleep(1000);//������һ��ȴ�����
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			    /*�����¶�*/
				RequestString = new String("CONTRL"+seperator+account+seperator
							+"GET"+seperator+"TEMP"+seperator);
//				String RequestTempString = new Strng("MANAGE"+seperator+account+seperator
//						+"LOGIN"+seperator+password+seperator);
				byte temp_buffer[]  = RequestString.getBytes();

				try {
					outputStream.write(temp_buffer, 0, temp_buffer.length);//����ָ��
					outputStream.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					isConnected = false; //�Ͽ�����
					isAuthed = false;    //��֤ʧЧ
				}
				while(isAuthed == false)//�ȴ��������Ӻ������֤
				{
					try {
						Thread.sleep(1000);//������һ��ȴ�����
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				/*����ʪ��*/
				RequestString = new String("CONTRL"+seperator+account+seperator
							+"GET"+seperator+"HUMI"+seperator);
				byte humi_buffer[] = RequestString.getBytes();
				try {
					outputStream.write(humi_buffer, 0, humi_buffer.length);//����ָ��
					outputStream.flush();
				} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isConnected = false; //�Ͽ�����
						isAuthed = false;    //��֤ʧЧ
				}
				try {
					Thread.sleep(2000);      //2s���һ��
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
		}
			
		
	};
	
	
	/**
	 * @Description:�ȴ����շ�������������Ϣ
	 **/
	Runnable revMsgRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			int start;
			int end;
			String oneString;
			String twoString;
			String threeString;
			String fourString;
			while(true)
			{
				if(stopallthread)
				{
					break;
				}
				while(isConnected == false)//�Ͽ����ӣ��ȵȴ���������
				{
					try {
						Thread.sleep(1000);//������һ��ȴ�����
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {

					/*�õ�������������Ϣ*/
					int temp = inputStream.read(buffer);
					if(temp < 0)
					{
						throw new Exception("�Ͽ�����");
					}
					String revString = new String(buffer, 0, temp);	
					/*�õ���һ��ָ����*/
					/*===ע��Ҫ���ж�end��û��Խ�磬��ȻrevString.charAt(end)���ܻ����*/
					for(start = 0, end = 0; (end<revString.length())&&((revString.charAt(end)!=seperator)) ; end++)
					{
						;
					}
					oneString = new String(revString.substring(start, end));
					/*�õ��ڶ���ָ����*/
					for(start = end + 1, end = end + 1; (end<revString.length())&&((revString.charAt(end)!=seperator)) ; end++)
					{
						;
					}
					twoString = new String(revString.substring(start, end));
					/*�õ�������ָ����*/
					for(start = end + 1, end = end + 1; (end<revString.length())&&((revString.charAt(end)!=seperator)) ; end++)
					{
						;
					}
					threeString = new String(revString.substring(start, end));
					/*�õ����ĸ�ָ����*/
					for(start = end + 1, end = end + 1; (end<revString.length())&&((revString.charAt(end)!=seperator)); end++)
					{
						;
					}
					fourString = new String(revString.substring(start, end));
//					System.out.println(oneString + " " + twoString + " " + threeString + " " + fourString + " ");
					
					if(oneString.equals("RESULT"))
					{
						/*���ص�¼��Ϣ�Ĵ���*/
						if(twoString.equals("LOGIN"))
						{
							if(threeString.equals("SUCCESS"))
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_ANSWER);
					    		intent.putExtra("result", "success");
					            sendBroadcast(intent);
					            isAuthed = true; //�������ɹ�
					            
					            /*�����֤�ɹ�*/
								intent.setAction(intent.ACTION_EDIT);
								intent.putExtra("type", "disconnect");
								intent.putExtra("disconnect", "authed");
								sendBroadcast(intent);
							}
							else
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_ANSWER);
					    		intent.putExtra("result", "failed");
					            sendBroadcast(intent);
								isAuthed = false;    //��֤ʧ��
							}
						}
						else if(twoString.equals(new String(account+"h")))
						{
							Intent intent = new Intent();
							intent.setAction(intent.ACTION_EDIT);
							if(threeString.equals("TEMP"))
							{
								intent.putExtra("type", "temp");
								intent.putExtra("temp", fourString);
							}
							else if(threeString.equals("HUMI"))
							{
								intent.putExtra("type", "humi");
								intent.putExtra("humi", fourString);
							}
							else if(threeString.equals("LEDON"))
							{
								intent.putExtra("type", "ledon");
								intent.putExtra("ledon", fourString);
							}
							else if(threeString.equals("LEDOFF"))
							{
								intent.putExtra("type", "ledoff");
								intent.putExtra("ledoff", fourString);
							}
							sendBroadcast(intent);
						}
					}//end of result
						
					
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					isConnected = false; //�Ͽ�����
					isAuthed = false;    //��֤ʧЧ
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					isConnected = false; //�Ͽ�����
					isAuthed = false;    //��֤ʧЧ
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();			
					isConnected = false; //�Ͽ�����
					isAuthed = false;    //��֤ʧЧ
				}
			}
			
		}
		
	};
	
	/** 
	 * @Description:
	 * 	 �������͸�Service�Ĺ㲥 
	 *  ����ת����Ϣ��������
	 **/
	private class ServiceReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String typeString = intent.getStringExtra("type");
			if(typeString.equals("send"))
			{
				/*��Ҫ���͵���Ϣ*/
				String msgString = new String(intent.getStringExtra("typestring")+seperator
						+account+seperator
						+intent.getStringExtra("operationstring")+seperator
						+intent.getStringExtra("numberstring")+seperator);
				sendRunnable.sendMsg(msgString);
				Thread thread = new Thread(sendRunnable);
				thread.start();
			}
		}
		
	}
	SendRunnable sendRunnable = new SendRunnable();
	/** 
	 * @Description:
	 * 	 ���ڷ�����Ϣ��������
	 **/
	private class SendRunnable implements Runnable
	{
		String msgString;
		public void sendMsg(String msg)
		{
			msgString = new String(msg);
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			byte buffer[] = msgString.getBytes();
			if(isAuthed == true)
			{
				try {
					outputStream.write(buffer, 0, buffer.length);//����ָ��
					outputStream.flush();
				} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isConnected = false; //�Ͽ�����
						isAuthed = false;    //��֤ʧЧ
				}
			}
			
		}
		
	}


}
