package com.feather.socketservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;

import com.example.ihome_client.ClientActivity;
import com.example.ihome_client.ClientMainActivity;
import com.example.ihome_client.R.bool;

import android.R.integer;
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
	
	//char COMMAND_PULSE  '0'
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
	byte COMMAND_SEPERATOR = 31;//31��Ԫ�ָ���
	byte COMMAND_END    = 30; //30,һ��ָ�����
	
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
//		/*�����������ݺ�����*/
//		thread = new Thread(allInfoFlushRunnable);
//		thread.start();
		
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
					String loginRequestString = new String(
							COMMAND_MANAGE+COMMAND_SEPERATOR
							+account+COMMAND_SEPERATOR
							+MAN_LOGIN+COMMAND_SEPERATOR
							+password+COMMAND_SEPERATOR
							+COMMAND_END);
					try {
						byte typeBytes[] = {COMMAND_MANAGE,COMMAND_SEPERATOR};
						byte accountBytes[] = account.getBytes("UTF-8");//�õ���׼��UTF-8����
						byte twoBytes[] = {COMMAND_SEPERATOR,MAN_LOGIN, COMMAND_SEPERATOR};
						byte passwordBytes[] = password.getBytes("UTF-8");
						byte threeBytes[] = {COMMAND_SEPERATOR, COMMAND_END};
						
						byte buffer[] = new byte[typeBytes.length + accountBytes.length+twoBytes.length
						                         +passwordBytes.length+threeBytes.length];
						int start = 0;
						System.arraycopy(typeBytes    ,0,buffer,start, typeBytes.length);
						start+=typeBytes.length;
						System.arraycopy(accountBytes ,0,buffer,start, accountBytes.length);
						start+=accountBytes.length;
						System.arraycopy(twoBytes     ,0,buffer,start, twoBytes.length);
						start+=twoBytes.length;
						System.arraycopy(passwordBytes,0,buffer,start, passwordBytes.length);
						start+=passwordBytes.length;
						System.arraycopy(threeBytes   ,0,buffer,start, threeBytes.length);
						
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
					} catch (UnsupportedEncodingException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
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
		boolean selectflag = true;
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

				if(selectflag)
				{
					selectflag = !selectflag;//ÿ�ν�����һ���¶Ⱥ�ʪ��
					/*�����¶�*/
					RequestString = new String(COMMAND_CONTRL+COMMAND_SEPERATOR
							+account+COMMAND_SEPERATOR
								+CTL_GET+COMMAND_SEPERATOR
								+RES_TEMP+COMMAND_SEPERATOR
								+COMMAND_END);
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
				}
				else {
					selectflag = !selectflag;//ÿ�ν�����һ���¶Ⱥ�ʪ��
					/*����ʪ��*/
					RequestString = new String(COMMAND_CONTRL+COMMAND_SEPERATOR
							+account+COMMAND_SEPERATOR
							+CTL_GET+COMMAND_SEPERATOR
							+RES_HUMI+COMMAND_SEPERATOR
							+COMMAND_END);
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
					
				}
				try {
					Thread.sleep(10000);      //10s���һ��
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
			int i;
			int start;
			int end;
			int type;
			String accountString;
			int subtype;
			int res;
			
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
					i = 0;
					System.out.println("get msg from server");
					while(i<revString.length())//�����ж�����Ϣ
					{
						/*���ָ����type*/
						if((i + 1 <revString.length())&&(revString.charAt(i+1)==COMMAND_SEPERATOR))
						{
							type = revString.charAt(i);
							i+=2;
						}
						else {
							while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*����˻�*/
						for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=COMMAND_SEPERATOR)) ; i++,end++)
						{
							;
						}
						i++;
						accountString = new String(revString.substring(start, end));
						/*ȷ���������Լ��Ŀ������Ļ����Լ�*/
						if(!accountString.equals(account+"h")&&!accountString.equals(account))
						{
							while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*���ָ��subtype*/
						if((i + 1 <revString.length())&&(revString.charAt(i+1)==COMMAND_SEPERATOR))
						{
							subtype = revString.charAt(i);
							i+=2;
						}
						else {
							while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*Ԥ�������ָ��*/
						if(type == COMMAND_RESULT)
						{
							System.out.println("COMMAND_RESULT");
							if(subtype == RES_LOGIN)
							{
								System.out.println("MAN_LOGIN");
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==COMMAND_SEPERATOR))
								{
									subtype = revString.charAt(i);
									i+=2;
									if(subtype == LOGIN_SUCCESS)//��½�ɹ�
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
									}//end of login state
								}
								else 
								{
									while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
							}//end of man_login
							/*�Ƶ�״̬*/
							else if(subtype == RES_LAMP)
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*��õƵ�״̬*/
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==COMMAND_SEPERATOR))
								{
									res = revString.charAt(i);
									i+=2;
								}
								else {
									/*������ָ���ʽ*/
									while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
								if(res == LAMP_ON)
								{
									/*��õ�ID*/
									if((i + 1 <revString.length())&&(revString.charAt(i+1)==COMMAND_SEPERATOR))
									{
										res = revString.charAt(i);
										i+=2;
									}
									else {
										while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
										{
											i++;
										}
										i++;
										continue;
									}
									intent.putExtra("type", "ledon");
									intent.putExtra("ledon", res+"");
								}
								else if(res == LAMP_OFF)
								{
									/*��õ�ID*/
									if((i + 1 <revString.length())&&(revString.charAt(i+1)==COMMAND_SEPERATOR))
									{
										res = revString.charAt(i);
										i+=2;
									}
									else {
										while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
										{
											i++;
										}
										i++;
										continue;
									}
									intent.putExtra("type", "ledoff");
									intent.putExtra("ledoff", res+"");
								}
								sendBroadcast(intent);
							}
							else if(subtype == RES_TEMP)/*��ȡ�¶�*/
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*���value*/
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==COMMAND_SEPERATOR))
								{
									res = revString.charAt(i);
									i+=2;
								}
								else {
									while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
								intent.putExtra("type", "temp");
								intent.putExtra("temp", ""+res);
								sendBroadcast(intent);
							}
							/*ʪ��*/
							else if(subtype == RES_HUMI)
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*���value*/
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==COMMAND_SEPERATOR))
								{
									res = revString.charAt(i);
									i+=2;
								}
								else {
									while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
								intent.putExtra("type", "humi");
								intent.putExtra("humi", ""+res);
								sendBroadcast(intent);
								
							}
						}
					
					}
						
					
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
//				/*��Ҫ���͵���Ϣ*/
//				String msgString = new String(intent.getStringExtra("typestring")+seperator
//						+account+seperator
//						+intent.getStringExtra("operationstring")+seperator
//						+intent.getStringExtra("numberstring")+seperator);
//				sendRunnable.sendMsg(msgString);
//				Thread thread = new Thread(sendRunnable);
//				thread.start();
			}
			else if(typeString.equals("ClientMainBack"))
			{
				/*���ؽ��水���˷��ؼ�����Ҫ���µ�¼*/
				isConnected = false;
				isAuthed = false;
				accountReady = false;
				
				/*֪ͨ��¼activityҪ�������µ�¼*/
				Intent intent1 = new Intent();
				intent1.setAction(intent1.ACTION_ANSWER);
	    		intent1.putExtra("result", "relogin");
	            sendBroadcast(intent1);
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
