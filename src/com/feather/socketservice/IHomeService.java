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
 * @CopyRight: 王辰浩 2015~2025
 * @qq:975559549
 * @Author Feather Hunter(猎羽)
 * @Version:1.0
 * @Date:2015/12/25
 * @Description: 后台service服务,用于与服务器之间的通信。
 *              并将收到的信息经过处理后，广播给各个activity。
 * @Function list:
 *     1. int onStartCommand(Intent intent, int flags, int startId)//处理其他activity发送过来的数据
 *     2. authRunnable //认证线程，发送身份信息给服务器
 *     3. serverConnectRunnable //连接服务器的线程
 *     4. allInfoFlushRunnable //发送请求以得到温度等所有参数的数据
 *     5. revMsgRunnable       //无限循环接收服务器发来的信息
 *     6. void onDestroy() //关闭socket
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
	byte COMMAND_SEPERATOR = 31;//31单元分隔符
	byte COMMAND_END    = 30; //30,一个指令结束
	
	String account, password;
	public boolean isConnected = false;
//	private ServiceBinder serviceBinder = new ServiceBinder();
	private int sleeptime = 100;
	private boolean accountReady = false;  //是否获得了明确的用户帐户信息和密码
	private boolean isAuthed = false;
	private boolean stopallthread = false;
	byte buffer[] = new byte[2048]; //2048字节的数组
	
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
		stopallthread = false; //允许所有线程
		Thread thread = new Thread(serverConnectRunnable);//连接服务器
		thread.start();	
		/*开启接受信息的线程*/
		thread = new Thread(revMsgRunnable);
		thread.start();	
//		/*开启更新数据和心跳*/
//		thread = new Thread(allInfoFlushRunnable);
//		thread.start();
		
		/*动态注册receiver*/
		serviceReceiver = new ServiceReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(SERVICE_ACTION);
		registerReceiver(serviceReceiver, filter);//注册
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
				accountReady = true; //用户信息准备好了
			}
			/*停止所有线程*/
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
				serverSocket.close(); //关闭socket
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		unregisterReceiver(serviceReceiver); //解除Receiver
	}

	/**
	 * @Description:连接到服务器兵器,开启接受信息的线程
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
				/*tcp连接成功,用户信息没有准备好,认证成功---不满足其中一项则进行处理*/
				while((isConnected == true)&&(!accountReady)&&(isAuthed == true)) 
				{
					try {
						Thread.sleep(1000);  //睡眠
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if(isConnected == false)
				{
					/*正在重新连接*/
					Intent intent = new Intent();
					intent.setAction(intent.ACTION_EDIT);
					intent.putExtra("type", "disconnect");
					intent.putExtra("disconnect", "connecting");
					sendBroadcast(intent);
					
					/*之前有过socket连接,先关闭，再开启新的*/
					if(serverSocket != null)
					{
						try {
							serverSocket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					/*断开连接后,重新连接和身份认证*/
					try {
						serverSocket = new Socket("139.129.19.115", 8080);
						/*得到输入流、输出流*/
						outputStream = serverSocket.getOutputStream();
						inputStream = serverSocket.getInputStream();
						isConnected = true; //連接成功
						
						/*告诉activity重新连接成功*/
						intent.setAction(intent.ACTION_EDIT);
						intent.putExtra("type", "disconnect");
						intent.putExtra("disconnect", "connected");
						sendBroadcast(intent);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isConnected = false; //連接失敗
						try {
							Thread.sleep(2500);  //失败后等待3s连接
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isConnected = false; //连接失败
						try {
							Thread.sleep(2500);  //失败后等待3s连接
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}	
				}
				if(isConnected && accountReady && (isAuthed == false))
				{
					/*通知activity正在验证信息*/
					Intent intent = new Intent();
					intent.setAction(intent.ACTION_EDIT);
					intent.putExtra("type", "disconnect");
					intent.putExtra("disconnect", "authing");
					sendBroadcast(intent);
					/*用户身份验证请求*/
					String loginRequestString = new String(
							COMMAND_MANAGE+COMMAND_SEPERATOR
							+account+COMMAND_SEPERATOR
							+MAN_LOGIN+COMMAND_SEPERATOR
							+password+COMMAND_SEPERATOR
							+COMMAND_END);
					try {
						byte typeBytes[] = {COMMAND_MANAGE,COMMAND_SEPERATOR};
						byte accountBytes[] = account.getBytes("UTF-8");//得到标准的UTF-8编码
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
							isConnected = false; //连接失败
							isAuthed = false;    //认证失效
							try {
								Thread.sleep(2500);  //身份验证失败后等待3s
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
						Thread.sleep(1000);  //身份验证失败后等待1s
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}//end of 身份认证

			}
			
		}
	};
	
	
	/**
	 * @Description:用于定时得到温度,湿度,灯初始信息---起到心跳的作用
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
				while(isAuthed == false)//等待重新链接和身份认证
				{
					try {
						Thread.sleep(1000);//先休眠一秒等待链接
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				if(selectflag)
				{
					selectflag = !selectflag;//每次交替检查一次温度和湿度
					/*请求温度*/
					RequestString = new String(COMMAND_CONTRL+COMMAND_SEPERATOR
							+account+COMMAND_SEPERATOR
								+CTL_GET+COMMAND_SEPERATOR
								+RES_TEMP+COMMAND_SEPERATOR
								+COMMAND_END);
					byte temp_buffer[]  = RequestString.getBytes();

					try {
						outputStream.write(temp_buffer, 0, temp_buffer.length);//发送指令
						outputStream.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isConnected = false; //断开连接
						isAuthed = false;    //认证失效
					}
					while(isAuthed == false)//等待重新链接和身份认证
					{
						try {
							Thread.sleep(1000);//先休眠一秒等待链接
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				else {
					selectflag = !selectflag;//每次交替检查一次温度和湿度
					/*请求湿度*/
					RequestString = new String(COMMAND_CONTRL+COMMAND_SEPERATOR
							+account+COMMAND_SEPERATOR
							+CTL_GET+COMMAND_SEPERATOR
							+RES_HUMI+COMMAND_SEPERATOR
							+COMMAND_END);
					byte humi_buffer[] = RequestString.getBytes();
					try {
						outputStream.write(humi_buffer, 0, humi_buffer.length);//发送指令
						outputStream.flush();
					} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							isConnected = false; //断开连接
							isAuthed = false;    //认证失效
					}
					
				}
				try {
					Thread.sleep(10000);      //10s获得一次
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			   				
			}
		}
			
		
	};
	
	
	/**
	 * @Description:等待接收服务器发来的信息
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
				while(isConnected == false)//断开连接，先等待重新链接
				{
					try {
						Thread.sleep(1000);//先休眠一秒等待链接
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {

					/*得到服务器返回信息*/
					int temp = inputStream.read(buffer);
					if(temp < 0)
					{
						throw new Exception("断开连接");
					}
					String revString = new String(buffer, 0, temp);	
					i = 0;
					System.out.println("get msg from server");
					while(i<revString.length())//可能有多组信息
					{
						/*获得指令主type*/
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
						/*获得账户*/
						for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=COMMAND_SEPERATOR)) ; i++,end++)
						{
							;
						}
						i++;
						accountString = new String(revString.substring(start, end));
						/*确定来自于自己的控制中心或者自己*/
						if(!accountString.equals(account+"h")&&!accountString.equals(account))
						{
							while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*获得指令subtype*/
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
						/*预处理后处理指令*/
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
									if(subtype == LOGIN_SUCCESS)//登陆成功
									{
										Intent intent = new Intent();
										intent.setAction(intent.ACTION_ANSWER);
							    		intent.putExtra("result", "success");
							            sendBroadcast(intent);
							            isAuthed = true; //身份认真成功
							            
							            /*身份认证成功*/
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
										isAuthed = false;    //认证失败
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
							/*灯的状态*/
							else if(subtype == RES_LAMP)
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*获得灯的状态*/
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==COMMAND_SEPERATOR))
								{
									res = revString.charAt(i);
									i+=2;
								}
								else {
									/*不符合指令格式*/
									while((i<revString.length())&&(revString.charAt(i))!=COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
								if(res == LAMP_ON)
								{
									/*获得灯ID*/
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
									/*获得灯ID*/
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
							else if(subtype == RES_TEMP)/*获取温度*/
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*获得value*/
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
							/*湿度*/
							else if(subtype == RES_HUMI)
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*获得value*/
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
					
					isConnected = false; //断开连接
					isAuthed = false;    //认证失效
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					isConnected = false; //断开连接
					isAuthed = false;    //认证失效
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();			
					isConnected = false; //断开连接
					isAuthed = false;    //认证失效
				}
			}
			
		}
		
	};
	
	/** 
	 * @Description:
	 * 	 监听发送给Service的广播 
	 *  用于转发信息给服务器
	 **/
	private class ServiceReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String typeString = intent.getStringExtra("type");
			if(typeString.equals("send"))
			{
//				/*需要发送的信息*/
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
				/*主控界面按下了返回键，需要重新登录*/
				isConnected = false;
				isAuthed = false;
				accountReady = false;
				
				/*通知登录activity要进行重新登录*/
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
	 * 	 用于发送信息给服务器
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
					outputStream.write(buffer, 0, buffer.length);//发送指令
					outputStream.flush();
				} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isConnected = false; //断开连接
						isAuthed = false;    //认证失效
				}
			}
			
		}
		
	}


}
