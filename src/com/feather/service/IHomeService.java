package com.feather.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.PublicKey;

import javax.security.auth.PrivateCredentialPermission;

import com.example.ihome_client.R.bool;
import com.feather.activity.ClientActivity;
import com.feather.activity.ClientMainActivity;
import com.feather.activity.Instruction;

import android.R.integer;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

/**
 * @CopyRight: ������ 2015~2025
 * @qq:975559549
 * @Author Feather Hunter(����)
 * @Version:2.10
 * @Date:2016/1/10
 * @Description: ��̨service����,�����������֮���ͨ�š�֧��wifiģʽ��ethernet
 *              �����յ�����Ϣ��������󣬹㲥������activity��
 * @Function list:
 *     1. int onStartCommand(Intent intent, int flags, int startId)//��������activity���͹���������
 *     2. authRunnable           //��֤�̣߳����������Ϣ��������
 *     3. serverConnectRunnable  //���ӷ��������߳�
 *     4. allInfoFlushRunnable   //���������Եõ��¶ȵ����в���������
 *     5. revMsgRunnable         //����ѭ�����շ�������������Ϣ
 *     6. void onDestroy()       //�ر�socket
 * @History:
 *     v1.0  @date 2015/12/25 Service�������Զ����ӷ����������ӳɹ��󣬸������������ͨ�š����Զ���������
 *     v2.10 @date 2016/1/10  ����wifi�����ӿ������ĵĹ��ܣ�ʧ�ܺ������ӷ�������
 **/

public class IHomeService extends Service{

	Socket serverSocket;
	OutputStream outputStream; //ouput to server
	InputStream inputStream;   //input from server
	
	/*��Ƶר������*/
	Socket videoSocket;
	OutputStream videoOutputStream; //ouput to target
	InputStream videoInputStream;   //input from target
	
	String account, password;
	private int sleeptime = 100;
	private boolean accountReady = false;  //�Ƿ�������ȷ���û��ʻ���Ϣ������
	
	public boolean isConnected = false;  //ͨ������ �Ƿ����ӳɹ�
	public boolean videoIsConnected = false;
	private boolean isAuthed   = false;  //�Ƿ���֤�ɹ�
	private boolean isTestWifi = false;  //���ڲ���wifi���ܷ������Ͽ������ģ���ʱֹͣTCP������Ϣ�߳�
	private boolean iswified   = false;  //�Ƿ�wifi�����ӿ������ĳɹ�
	
	private boolean stopallthread = false; //ֹͣ�����߳�
	byte buffer[] = new byte[2048]; //2048�ֽڵ�ָ�����
	byte videoBuffer[] = new byte[4096]; //4096����Ƶ�ļ�������
	
	private ServiceReceiver serviceReceiver;
	private String SERVICE_ACTION = "android.intent.action.MAIN";
	
	private String serverString = "139.129.19.115";
	private String contrlCenterString = "192.168.16.106";
	private int generalPort = 8080;
	private int videoPort   = 8081;
	private String cameraIDString = "20000";
	
	private FileOutputStream jpegOutputStream = null;
	
	/*wifiģʽ���*/
	private WifiManager wifiManager;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @Function: public void onCreate();
	 * @Description:
	 *      1. ���������̣߳�����wifi��ֱ�����ӿ������Ļ���ͨ��ethernet���ӷ�����
	 *      2. ����������Ϣ�̣߳� ����Ŀ�����Ϣ
	 *      3. ���������¶�ʪ�ȵ����ݵ��̣߳�ͬʱ��Ϊ����������
	 *      4. ��̬ע��receiver-������������ģ�����Ϣ��������ת������������
	 *      5. ��ȡwifimanager��������
	 **/
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
		//thread = new Thread(allInfoFlushRunnable);
		//thread.start();
		
		/*�����������ݺ�����*/
		thread = new Thread(videoConnectRunnable);
		thread.start();
		
		/*��̬ע��receiver*/
		serviceReceiver = new ServiceReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(SERVICE_ACTION);
		registerReceiver(serviceReceiver, filter);//ע��
		
		/*wifi���*/
		wifiManager = (WifiManager) getSystemService(Service.WIFI_SERVICE);//���wifi
	}

	/**
	 * @Function: int onStartCommand();
	 * @Description:
	 *      1. ���ڽ��ܵ�½���淢�������˺ź�������Ϣ���ڵ�¼��
	 **/
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
	
	/**
	 * @Function: void onDestroy();
	 * @Description:
	 *      ��������Serviceʱ�����β������
	 *      1. �ر�socket
	 *      2. �����̬ע���Receiver 
	 **/
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
	 * @Function: serverConnectRunnable;
	 * @Description:
	 *      �������ӷ��������߿������ģ����Ҷ��ߵ�ʱ���������ӡ�
	 *      1. �ر�socket
	 *      2. �����̬ע���Receiver 
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
				if(accountReady && (isConnected == false))//���ȷ������û������
				{
					System.out.println("accountReady is true");
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
					isTestWifi = true;
					iswified = false;
					/*����Ƿ���wifi�������ӵ��û�*/
					if(wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED)//wifi�Ѿ���
					{
						SocketChannel socketChannel = null;
						try {
							socketChannel = SocketChannel.open();
							socketChannel.configureBlocking(false);
							socketChannel.connect(new InetSocketAddress(contrlCenterString, 8080));
							
							Thread.sleep(1000);  //˯��500ms
							if(!socketChannel.finishConnect())
							{
								iswified = false;
							}
							else {
								iswified = true;
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}finally{
							try {
								if(socketChannel != null)//�ر�
									socketChannel.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}	
						}
					}//end of connecting stm32 by wifi
					isTestWifi = false;
					if(iswified == true)
					{
						/*�Ͽ����Ӻ�,�������Ӻ������֤*/
						try {
							serverSocket = new Socket(contrlCenterString, 8080);
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
					}//end of connecting ContrlCenter
					else {
						/*�Ͽ����Ӻ�,�������Ӻ������֤*/
						try {
							serverSocket = new Socket(serverString, 8080);
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
						
					}//end of connecting server
				}
				/*�����ӳɹ����û����ȷ����ʱ�򣬽��������֤*/
				if(isConnected && accountReady && (isAuthed == false))
				{
					/*֪ͨactivity������֤��Ϣ*/
					Intent intent = new Intent();
					intent.setAction(intent.ACTION_EDIT);
					intent.putExtra("type", "disconnect");
					intent.putExtra("disconnect", "authing");
					sendBroadcast(intent);
					/*�û������֤����*/
					try {
						/*��Ҫ���͵�ָ��,byte����*/
						byte typeBytes[] = {Instruction.COMMAND_MANAGE,Instruction.COMMAND_SEPERATOR};
						byte accountBytes[] = account.getBytes("UTF-8");//�õ���׼��UTF-8����
						byte twoBytes[] = {Instruction.COMMAND_SEPERATOR,Instruction.MAN_LOGIN, Instruction.COMMAND_SEPERATOR};
						byte passwordBytes[] = password.getBytes("UTF-8");
						byte threeBytes[] = {Instruction.COMMAND_SEPERATOR, Instruction.COMMAND_END};						
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
	 * @Function: videoConnectRunnable;
	 * @Description:
	 *      ��Ƶר�����ӣ����ڽ�����Ƶ��Ϣ
	 */
	Runnable videoConnectRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true)
			{
				if(stopallthread)
				{
					break;
				}
				/*ָ��ר��������֤���ɹ�������Ƶ��Ҳ����ɹ����ȴ�*/
				while(!isAuthed) 
				{
					try {
						Thread.sleep(1000);  //˯��
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if((isAuthed == true)&&(videoIsConnected == false))
				{
					/*������������*/
					Intent intent = new Intent();
					intent.setAction(intent.ACTION_EDIT);
					intent.putExtra("type", "disconnect");
					intent.putExtra("disconnect", "��Ƶ������...");
					sendBroadcast(intent);
					
					/*֮ǰ�й�socket����,�ȹرգ��ٿ����µ�*/
					if(videoSocket != null)
					{
						try {
							videoSocket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if(iswified == true)
					{
						/*�Ͽ����Ӻ�,��������*/
						try {
							videoSocket = new Socket(contrlCenterString, videoPort);
							/*�õ��������������*/
							videoOutputStream = videoSocket.getOutputStream();
							videoInputStream = videoSocket.getInputStream();
							videoIsConnected = true; //��Ƶ�B�ӳɹ�
							
							/*����activity�������ӳɹ�*/
							intent.setAction(intent.ACTION_EDIT);
							intent.putExtra("type", "disconnect");
							intent.putExtra("disconnect", "��Ƶ���ӳɹ�");
							sendBroadcast(intent);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							videoIsConnected = false; //�B��ʧ��
							try {
								Thread.sleep(2500);  //ʧ�ܺ�ȴ�3s����
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							videoIsConnected = false; //����ʧ��
							try {
								Thread.sleep(2500);  //ʧ�ܺ�ȴ�3s����
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}	 
					}//end of connecting ContrlCenter
					else {
						/*�Ͽ����Ӻ�,��������*/
						try {
							videoSocket = new Socket(serverString, videoPort);
							/*�õ��������������*/
							videoOutputStream = videoSocket.getOutputStream();
							videoInputStream = videoSocket.getInputStream();
							videoIsConnected = true; //��Ƶ�B�ӳɹ�
							
							/*����activity�������ӳɹ�*/
							intent.setAction(intent.ACTION_EDIT);
							intent.putExtra("type", "disconnect");
							intent.putExtra("disconnect", "��Ƶ���ӳɹ�");
							sendBroadcast(intent);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							videoIsConnected = false; //�B��ʧ��
							try {
								Thread.sleep(2500);  //ʧ�ܺ�ȴ�3s����
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							videoIsConnected = false; //����ʧ��
							try {
								Thread.sleep(2500);  //ʧ�ܺ�ȴ�3s����
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}	
						
					}//end of connecting video
				}//end of isAuthed true and videoIsConnected false

			}//end of while(1)
			
		}//end of run
	
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
					try {
						/*��Ҫ���͵�ָ��,byte����*/
						byte typeBytes[] = {Instruction.COMMAND_CONTRL,Instruction.COMMAND_SEPERATOR};
						byte accountBytes[] = account.getBytes("UTF-8");//�õ���׼��UTF-8����
						byte twoBytes[] = {Instruction.COMMAND_SEPERATOR,Instruction.CTL_GET,Instruction.COMMAND_SEPERATOR, 
								Instruction.RES_TEMP, Instruction.COMMAND_SEPERATOR};
						String IDString = new String("10000");
						byte TempIDBytes[] = IDString.getBytes("UTF-8");
						//byte TempIDBytes[] = {'1','0'};
						byte threeBytes[] = {Instruction.COMMAND_SEPERATOR, Instruction.COMMAND_END};						
						byte temp_buffer[] = new byte[typeBytes.length + accountBytes.length+twoBytes.length
						                       +TempIDBytes.length+threeBytes.length];
						/*�ϲ���һ��byte������*/
						int start = 0;
						System.arraycopy(typeBytes    ,0,temp_buffer,start, typeBytes.length);
						start+=typeBytes.length;
						System.arraycopy(accountBytes ,0,temp_buffer,start, accountBytes.length);
						start+=accountBytes.length;
						System.arraycopy(twoBytes     ,0,temp_buffer,start, twoBytes.length);
						start+=twoBytes.length;
						System.arraycopy(TempIDBytes,0,temp_buffer,start, TempIDBytes.length);
						start+=TempIDBytes.length;
						System.arraycopy(threeBytes   ,0,temp_buffer,start, threeBytes.length);
						
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
					try {
						/*��Ҫ���͵�ָ��(��ȡʪ��),byte����*/
						byte typeBytes[] = {Instruction.COMMAND_CONTRL,Instruction.COMMAND_SEPERATOR};
						byte accountBytes[] = account.getBytes("UTF-8");//�õ���׼��UTF-8����
						byte twoBytes[] = {Instruction.COMMAND_SEPERATOR,Instruction.CTL_GET, Instruction.COMMAND_SEPERATOR, 
								Instruction.RES_HUMI, Instruction.COMMAND_SEPERATOR};
						String IDString = new String("10000");
						byte HumiIDBytes[] = IDString.getBytes("UTF-8");						
						byte threeBytes[] = {Instruction.COMMAND_SEPERATOR, Instruction.COMMAND_END};						
						byte humi_buffer[] = new byte[typeBytes.length + accountBytes.length+twoBytes.length
						                       +HumiIDBytes.length+threeBytes.length];
						/*�ϲ���һ��byte������*/
						int start = 0;
						System.arraycopy(typeBytes    ,0,humi_buffer,start, typeBytes.length);
						start+=typeBytes.length;
						System.arraycopy(accountBytes ,0,humi_buffer,start, accountBytes.length);
						start+=accountBytes.length;
						System.arraycopy(twoBytes     ,0,humi_buffer,start, twoBytes.length);
						start+=twoBytes.length;
						System.arraycopy(HumiIDBytes,0,humi_buffer,start, HumiIDBytes.length);
						start+=HumiIDBytes.length;
						System.arraycopy(threeBytes   ,0,humi_buffer,start, threeBytes.length);
						
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
	* @Function: revMsgRunnable;
	* @Description:
	*      ���ڽ��ܲ��Ҵ������������������Ϣ
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
			int subtype = 0;
			int res;
			
			while(true)
			{
				if(stopallthread)
				{
					break;
				}
				if(isTestWifi == true)//���ڲ���wifi�ܷ����ӵ�stm32
				{
					try {
						Thread.sleep(1000);//������һ��
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
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
//					Intent tempintent = new Intent();
//					tempintent.setAction(tempintent.ACTION_EDIT);
//					/*����ihomeģʽ�������*/
//					tempintent.putExtra("type", "video");
//					tempintent.putExtra("video", "read:"+temp);
//					sendBroadcast(tempintent);
					String revString = new String(buffer, 0, temp);	
					i = 0;
					System.out.println("get msg from server read:"+temp+" string:"+revString.length());
					while(i<revString.length())//�����ж�����Ϣ
					{
						/*���ָ����type*/
						if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
						{
							type = revString.charAt(i);
							i+=2;
						}
						else {
							/*��ǰָ���������ת����һ��ָ��*/
							while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*����˻�*/
						for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
						{
							;
						}
						i++;
						accountString = new String(revString.substring(start, end));
						/*ȷ���������Լ��Ŀ������Ļ���SERVER*/
						if(!accountString.equals(account+'h')&&!accountString.equals("SERVER"))
						{
							while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*���ָ��subtype*/
						if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
						{
							subtype = revString.charAt(i);
							i+=2;
						}
						else {
							while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						System.out.println("type:"+type + "sub:"+subtype);
						/*Ԥ�������ָ��*/
						if(type == Instruction.COMMAND_RESULT)
						{
							System.out.println("COMMAND_RESULT");
							/*---------------rev res_ihome----------*/
							if(subtype == Instruction.RES_IHome)
							{
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
								{
									subtype = revString.charAt(i);
									i+=2;
								}
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								if(subtype == Instruction.IHome_START)
								{
									/*����ihomeģʽ�������*/
									intent.putExtra("type", "ihome");
									intent.putExtra("ihome", "start");
									sendBroadcast(intent);
								}
								else if(subtype == Instruction.IHome_STOP){
									/*����ihomeģʽ�������*/
									intent.putExtra("type", "ihome");
									intent.putExtra("ihome", "stop");
									sendBroadcast(intent);
								}
							}//end of res_ihome
							else if(subtype == Instruction.RES_LOGIN)
							{
								System.out.println("MAN_LOGIN");
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
								{
									subtype = revString.charAt(i);
									i+=2;
									if(subtype == Instruction.LOGIN_SUCCESS)//��½�ɹ�
									{
										Intent intent = new Intent();
										intent.setAction(intent.ACTION_ANSWER);
										if(accountString.equals("SERVER"))
										{
											intent.putExtra("result", "server");
											intent.putExtra("server", "success");
										}
										else {
											intent.putExtra("result", "center");
											intent.putExtra("center", "success");
										}
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
										if(accountString.equals("SERVER"))
										{
											intent.putExtra("result", "server");
											intent.putExtra("server", "failed");
										}
										else {
											intent.putExtra("result", "center");
											intent.putExtra("center", "failed");
										}
							            sendBroadcast(intent);
										isAuthed = false;    //��֤ʧ��
										accountReady = false;  //��Ϣ����
									}//end of login state
								}
								else 
								{
									while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
							}//end of man_login
							/*�Ƶ�״̬*/
							else if(subtype == Instruction.RES_LAMP)
							{
								System.out.println("res_lamp");
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*��õƵ�״̬*/
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
								{
									res = revString.charAt(i);
									i+=2;
								}
								else {
									/*������ָ���ʽ*/
									while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
								if(res == Instruction.LAMP_ON)
								{
									/*��õ�ID*/
									for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
									{
										;
									}
									i++;
									String IDString = new String(revString.substring(start, end));
									intent.putExtra("type", "ledon");
									intent.putExtra("ledon", IDString);
								}
								else if(res == Instruction.LAMP_OFF)
								{
									/*��õ�ID*/
									for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
									{
										;
									}
									i++;
									String IDString = new String(revString.substring(start, end));
									intent.putExtra("type", "ledoff");
									intent.putExtra("ledoff", IDString);
								}
								sendBroadcast(intent);
							}
							else if(subtype == Instruction.RES_TEMP)/*��ȡ�¶�*/
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*����豸ID*/
								for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
								{
									;
								}
								i++;
								String IDString = new String(revString.substring(start, end));
								/*���value*/
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
								{
									res = revString.charAt(i);
									i+=2;
								}
								else {
									while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
								intent.putExtra("type", "temp");
								intent.putExtra("temp", IDString);//�����豸ID
								intent.putExtra(IDString, res+"");
								sendBroadcast(intent);
							}
							/*ʪ��*/
							else if(subtype == Instruction.RES_HUMI)
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*����豸ID*/
								for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
								{
									;
								}
								i++;
								String IDString = new String(revString.substring(start, end));
								/*���value*/
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
								{
									res = revString.charAt(i);
									i+=2;
								}
								else {
									while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
								intent.putExtra("type", "humi");
								intent.putExtra("humi", IDString);//�����豸ID
								intent.putExtra(IDString, res+"");
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
	* @Function: revVideoRunnable;
	* @Description:
	*      ���ڽ�����Ƶ�ļ����ұ��浽�ֻ���
	**/
	Runnable revVideoRunnable = new Runnable() {

		private byte[] videoHandleBuffer = new byte[4096];
		private int videoEnd = 0;
		@Override
		public void run() {
			// TODO Auto-generated method stub
			int i;
			int start;
			int end;
			int type;
			String accountString;
			String cameraIDString;
			String dataLengthString;
			int dataLength;
			int subtype = 0;
			int res;
			int msgStart;
			
			while(true)
			{
				if(stopallthread)
				{
					break;
				}
				while(videoIsConnected == false)//�����Ѿ��Ͽ����ȵȴ���������
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
					int temp = videoInputStream.read(videoBuffer);
					if(temp < 0)
					{
						throw new Exception("�Ͽ�����");
					}
//					Intent tempintent = new Intent();
//					tempintent.setAction(tempintent.ACTION_EDIT);
//					/*����ihomeģʽ�������*/
//					tempintent.putExtra("type", "video");
//					tempintent.putExtra("video", "read:"+temp);
//					sendBroadcast(tempintent);
					//�ȸ��Ƶ�����Ļ�������
					System.arraycopy(videoBuffer    ,0,videoHandleBuffer, videoEnd, temp);
					videoEnd += temp;
					i = 0;
					System.out.println("get msg from server read:"+temp);
					while(i < videoHandleBuffer.length)//�����ж�����Ϣ
					{
						msgStart = i; //��¼���δ������Ϣͷ
						/*���ָ����type*/
						if((i + 1 <videoHandleBuffer.length)&&(videoHandleBuffer[i+1] ==Instruction.COMMAND_SEPERATOR))
						{
							type = videoHandleBuffer[i];
							i+=2;
						}
						else {
							/*��ǰָ���������ת����һ��ָ��*/
							while((i<videoHandleBuffer.length)&&(videoHandleBuffer[i])!=Instruction.COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*����˻�*/
						for(start = i, end = start; (end<videoHandleBuffer.length)&&((videoHandleBuffer[end] !=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
						{
							;
						}
						i++;
						accountString = new String(videoHandleBuffer, start, end);
						/*ȷ���������Լ��Ŀ������Ļ���SERVER*/
						if(!accountString.equals(account+'h')&&!accountString.equals("SERVER"))
						{
							/*��ǰָ���������ת����һ��ָ��*/
							while((i<videoHandleBuffer.length)&&(videoHandleBuffer[i])!=Instruction.COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
					   /*-------------------�ȴ�����Ƶָ��------------------------*/
						if(type == Instruction.COMMAND_VIDEO)
						{	
							/*�������ͷID*/
							for(start = i, end = start; (end<videoHandleBuffer.length)&&((videoHandleBuffer[end] !=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
							{
								;
							}
							i++;
							cameraIDString = new String(videoHandleBuffer, start, end);
							if(cameraIDString.equals(cameraIDString))//ȷ��Ϊ��Ҫ����ƵID��20000
							{
								subtype = videoHandleBuffer[i];
								if(subtype == Instruction.VIDEO_START)//��Ƶ����ʼ
								{
									i += 2;
									try {
										jpegOutputStream = new FileOutputStream("mnt/sdcard/camera.jpg");
									} catch (Exception e) {
										// TODO: handle exception
										e.printStackTrace();
									}
									Intent intent = new Intent();
									intent.setAction(intent.ACTION_EDIT);
									/*����ihomeģʽ�������*/
									intent.putExtra("type", "video");
									intent.putExtra("video", "video start");
									sendBroadcast(intent);
									
								}//end of video_start
								else if(subtype == Instruction.VIDEO_STOP)//����������
								{
									i += 2;
									
									Intent intent = new Intent();
									intent.setAction(intent.ACTION_EDIT);
									/*����ihomeģʽ�������*/
									intent.putExtra("type", "video");
									intent.putExtra("video", "video stop");
									sendBroadcast(intent);
									
									if(jpegOutputStream != null)
									{
										try {
											jpegOutputStream.close();//�ر������
										} catch (Exception e) {
											// TODO: handle exception
											e.printStackTrace();
										}
									}
								}//end of video stop
								else {
									/*������ݳ���*/
									for(start = i, end = start; (end<videoHandleBuffer.length)&&((videoHandleBuffer[end] !=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
									{
										;
									}
									i++;
									dataLengthString = new String(videoHandleBuffer, start, end);
									/*���ݳ���*/
									dataLength = Integer.valueOf(dataLengthString);
									//˵��Ϊ����
									
									if(i + dataLength <= videoHandleBuffer.length)//�յ����е�����
									{
										try {
											jpegOutputStream.write(videoHandleBuffer, i, i + dataLength);
											Intent intent = new Intent();
											intent.setAction(intent.ACTION_EDIT);
											/*����ihomeģʽ�������*/
											intent.putExtra("type", "video");
											intent.putExtra("video", dataLength+" ");
											sendBroadcast(intent);
										} catch (Exception e) {
											// TODO: handle exception
											e.printStackTrace();
										}
									}
									else {//û���ܵ���������
										System.arraycopy(videoHandleBuffer    , msgStart ,videoHandleBuffer, 0, videoHandleBuffer.length - msgStart);
										videoEnd = 10;
									}
									i += dataLength;
								}//end of ������
								continue;

							}//end of camera id
							
						}//end of ��Ƶָ��
					
					}//end of while(i < videoHandleBuffer.length) ���������Ϣ
						
					
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					videoIsConnected = false; //�Ͽ�����
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					videoIsConnected = false; //�Ͽ�����
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();			
					videoIsConnected = false; //�Ͽ�����
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
				try {
					/*����Fragment�������Ĳ���ָ��,�����account*/
					byte onefield[] = intent.getByteArrayExtra("onefield");
					byte accountfield[] = account.getBytes("UTF-8");
					byte twofield[] = intent.getByteArrayExtra("twofield");
					byte buffer[] = new byte[onefield.length + accountfield.length+twofield.length];
					/*�ϲ���һ��byte������*/
					int start = 0;
					System.arraycopy(onefield    ,0,buffer,start, onefield.length);
					start+=onefield.length;
					System.arraycopy(accountfield ,0,buffer,start, accountfield.length);
					start+=accountfield.length;
					System.arraycopy(twofield     ,0,buffer,start, twofield.length);
					
					outputStream.write(buffer, 0, buffer.length);
					outputStream.flush();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					isConnected = false; //�Ͽ�����
					isAuthed = false;    //��֤ʧЧ
				}//����ָ��
			}
			else if(typeString.equals("ClientMainBack"))
			{
				/*���ؽ��水���˷��ؼ�����Ҫ���µ�¼*/
				isConnected = false;
				isAuthed = false;
				accountReady = false;
				if(serverSocket != null)//�����socket������������
				{
					try {
						serverSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				/*֪ͨ��¼activityҪ�������µ�¼*/
				Intent intent1 = new Intent();
				intent1.setAction(intent1.ACTION_ANSWER);
	    		intent1.putExtra("result", "relogin");
	            sendBroadcast(intent1);
			}
		}
		
	}
}
