package com.feather.socketservice;

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

import com.example.ihome_client.ClientActivity;
import com.example.ihome_client.ClientMainActivity;
import com.example.ihome_client.Instruction;
import com.example.ihome_client.R.bool;

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
	
	String account, password;
	public boolean isConnected = false;
	private int sleeptime = 100;
	private boolean accountReady = false;  //�Ƿ�������ȷ���û��ʻ���Ϣ������
	private boolean isAuthed = false;
	private boolean stopallthread = false;
	private boolean isTestWifi = false;
	byte buffer[] = new byte[2048]; //2048�ֽڵ�����
	
	private ServiceReceiver serviceReceiver;
	private String SERVICE_ACTION = "android.intent.action.MAIN";
	
	private String serverString = "139.129.19.115";
	private String contrlCenterString = "192.168.16.106";
	
	/*wifiģʽ���*/
	private WifiManager wifiManager;
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
		
		/*wifi���*/
		wifiManager = (WifiManager) getSystemService(Service.WIFI_SERVICE);//���wifi
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
		
		boolean iswified = false;
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
					isTestWifi = true;
					iswified = false;
					/*����Ƿ���wifi�������ӵ��û�*/
					while(true)
					{
						/*����Ƿ���wifi�������ӵ��û�*/
						SocketChannel socketChannel = null;
						try {
							socketChannel = SocketChannel.open();
							socketChannel.configureBlocking(false);
							socketChannel.connect(new InetSocketAddress(contrlCenterString, 8080));
							
							Thread.sleep(500);  //˯��500ms
							if(!socketChannel.finishConnect())
							{
								iswified = false;
								break;
							}
							else {
								iswified = true;
								break;
							}
							
//							/*��Ҫ���͵�ָ��,byte����*/
//							byte typeBytes[] = {Instruction.COMMAND_MANAGE,Instruction.COMMAND_SEPERATOR};
//							byte accountBytes[] = account.getBytes("UTF-8");//�õ���׼��UTF-8����
//							byte twoBytes[] = {Instruction.COMMAND_SEPERATOR,Instruction.MAN_LOGIN, Instruction.COMMAND_SEPERATOR};
//							byte passwordBytes[] = password.getBytes("UTF-8");
//							byte threeBytes[] = {Instruction.COMMAND_SEPERATOR, Instruction.COMMAND_END};						
//							byte buffer[] = new byte[typeBytes.length + accountBytes.length+twoBytes.length
//							                       +passwordBytes.length+threeBytes.length];
//							
//							int start = 0;
//							System.arraycopy(typeBytes    ,0,buffer,start, typeBytes.length);
//							start+=typeBytes.length;
//							System.arraycopy(accountBytes ,0,buffer,start, accountBytes.length);
//							start+=accountBytes.length;
//							System.arraycopy(twoBytes     ,0,buffer,start, twoBytes.length);
//							start+=twoBytes.length;
//							System.arraycopy(passwordBytes,0,buffer,start, passwordBytes.length);
//							start+=passwordBytes.length;
//							System.arraycopy(threeBytes   ,0,buffer,start, threeBytes.length);
//							
//							ByteBuffer buf = ByteBuffer.allocate(48);//48�Ļ�����
//							
//							buf.clear();
//							buf.put(buffer);
//							buf.flip();
//							
//							/*������֤��Ϣ��������*/
//							while(buf.hasRemaining()) {   //д������
//								socketChannel.write(buf);
//							}
//							int numBytesRead = 0;
//							buf.clear(); 
//							
//							int ri = 0;
//							while ( ((numBytesRead = socketChannel.read(buf)) != -1) || true) {
//							        if (numBytesRead == 0) {
//							          // ���û�����ݣ�����΢�ȴ�һ��
//							          try {
//							        	ri++;
//							        	if(ri > 100)
//							        		break;
//							            Thread.sleep(1);
//							          } catch (InterruptedException e) {
//							            e.printStackTrace();
//							          }
//							          continue;
//							        }
//							        else {
//							        	// ת���ʼ
//								        buf.flip();
//								        buf.get(buffer, 0, numBytesRead);
//								        buf.clear();
//								        //break;
//									}
//							}
//							String revString = new String(buffer, 0, numBytesRead);	
//							int i = 0;
//							int type;
//							while(i<revString.length())//�����ж�����Ϣ
//							{
//								//if(revString.charAt(i) == Instruction.COMMAND_RESULT)
//								/*���ָ����type*/
//								if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
//								{
//									type = revString.charAt(i);
//									i+=2;
//								}
//								else {
//									/*��ǰָ���������ת����һ��ָ��*/
//									while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
//									{
//										i++;
//									}
//									i++;
//									continue;
//								}
//								if(type == Instruction.COMMAND_RESULT)
//								{
//									iswified = true;
//									break;
//								}
//							}
							
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}finally
						{
							try {
								if(socketChannel != null)//�ر�
									socketChannel.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} //�ر�
							break;
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
					String revString = new String(buffer, 0, temp);	
					i = 0;
					System.out.println("get msg from server");
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
						/*ȷ���������Լ��Ŀ������Ļ����Լ�*/
						if(!accountString.equals(account+"h")&&!accountString.equals(account))
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
				
				/*֪ͨ��¼activityҪ�������µ�¼*/
				Intent intent1 = new Intent();
				intent1.setAction(intent1.ACTION_ANSWER);
	    		intent1.putExtra("result", "relogin");
	            sendBroadcast(intent1);
			}
		}
		
	}
}
