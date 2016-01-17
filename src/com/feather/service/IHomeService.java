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
 * @CopyRight: 王辰浩 2015~2025
 * @qq:975559549
 * @Author Feather Hunter(猎羽)
 * @Version:2.10
 * @Date:2016/1/10
 * @Description: 后台service服务,用于与服务器之间的通信。支持wifi模式和ethernet
 *              并将收到的信息经过处理后，广播给各个activity。
 * @Function list:
 *     1. int onStartCommand(Intent intent, int flags, int startId)//处理其他activity发送过来的数据
 *     2. authRunnable           //认证线程，发送身份信息给服务器
 *     3. serverConnectRunnable  //连接服务器的线程
 *     4. allInfoFlushRunnable   //发送请求以得到温度等所有参数的数据
 *     5. revMsgRunnable         //无限循环接收服务器发来的信息
 *     6. void onDestroy()       //关闭socket
 * @History:
 *     v1.0  @date 2015/12/25 Service启动后，自动连接服务器，连接成功后，负责与服务器间通信。会自动短线重连
 *     v2.10 @date 2016/1/10  增加wifi内连接控制中心的功能，失败后尝试连接服务器。
 **/

public class IHomeService extends Service{

	Socket serverSocket;
	OutputStream outputStream; //ouput to server
	InputStream inputStream;   //input from server
	
	/*视频专用连接*/
	Socket videoSocket;
	OutputStream videoOutputStream; //ouput to target
	InputStream videoInputStream;   //input from target
	
	String account, password;
	private int sleeptime = 100;
	private boolean accountReady = false;  //是否获得了明确的用户帐户信息和密码
	
	public boolean isConnected = false;  //通用连接 是否连接成功
	public boolean videoIsConnected = false;
	private boolean isAuthed   = false;  //是否验证成功
	private boolean isTestWifi = false;  //正在测试wifi内能否连接上控制中心，此时停止TCP接受信息线程
	private boolean iswified   = false;  //是否wifi内连接控制中心成功
	
	private boolean stopallthread = false; //停止所有线程
	byte buffer[] = new byte[2048]; //2048字节的指令缓冲区
	byte videoBuffer[] = new byte[4096]; //4096的视频文件缓冲区
	
	private ServiceReceiver serviceReceiver;
	private String SERVICE_ACTION = "android.intent.action.MAIN";
	
	private String serverString = "139.129.19.115";
	private String contrlCenterString = "192.168.16.106";
	private int generalPort = 8080;
	private int videoPort   = 8081;
	private String cameraIDString = "20000";
	
	private FileOutputStream jpegOutputStream = null;
	
	/*wifi模式相关*/
	private WifiManager wifiManager;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @Function: public void onCreate();
	 * @Description:
	 *      1. 创建连接线程：用于wifi内直接连接控制中心或者通过ethernet连接服务器
	 *      2. 创建接受信息线程： 接受目标的信息
	 *      3. 创建更新温度湿度等数据的线程，同时作为心跳函数。
	 *      4. 动态注册receiver-接受来自其他模块的信息，并将其转发给服务器。
	 *      5. 获取wifimanager管理器。
	 **/
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
		/*开启更新数据和心跳*/
		//thread = new Thread(allInfoFlushRunnable);
		//thread.start();
		
		/*开启更新数据和心跳*/
		thread = new Thread(videoConnectRunnable);
		thread.start();
		
		/*动态注册receiver*/
		serviceReceiver = new ServiceReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(SERVICE_ACTION);
		registerReceiver(serviceReceiver, filter);//注册
		
		/*wifi相关*/
		wifiManager = (WifiManager) getSystemService(Service.WIFI_SERVICE);//获得wifi
	}

	/**
	 * @Function: int onStartCommand();
	 * @Description:
	 *      1. 用于接受登陆界面发送来的账号和密码信息用于登录。
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
	
	/**
	 * @Function: void onDestroy();
	 * @Description:
	 *      用于销毁Service时候的收尾工作。
	 *      1. 关闭socket
	 *      2. 解除动态注册的Receiver 
	 **/
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
	 * @Function: serverConnectRunnable;
	 * @Description:
	 *      用于连接服务器或者控制中心，并且断线的时候重新连接。
	 *      1. 关闭socket
	 *      2. 解除动态注册的Receiver 
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
				if(accountReady && (isConnected == false))//身份确定并且没有连接
				{
					System.out.println("accountReady is true");
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
					isTestWifi = true;
					iswified = false;
					/*检测是否在wifi内能连接到用户*/
					if(wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED)//wifi已经打开
					{
						SocketChannel socketChannel = null;
						try {
							socketChannel = SocketChannel.open();
							socketChannel.configureBlocking(false);
							socketChannel.connect(new InetSocketAddress(contrlCenterString, 8080));
							
							Thread.sleep(1000);  //睡眠500ms
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
								if(socketChannel != null)//关闭
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
						/*断开连接后,重新连接和身份认证*/
						try {
							serverSocket = new Socket(contrlCenterString, 8080);
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
					}//end of connecting ContrlCenter
					else {
						/*断开连接后,重新连接和身份认证*/
						try {
							serverSocket = new Socket(serverString, 8080);
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
						
					}//end of connecting server
				}
				/*在连接成功和用户身份确定的时候，进行身份认证*/
				if(isConnected && accountReady && (isAuthed == false))
				{
					/*通知activity正在验证信息*/
					Intent intent = new Intent();
					intent.setAction(intent.ACTION_EDIT);
					intent.putExtra("type", "disconnect");
					intent.putExtra("disconnect", "authing");
					sendBroadcast(intent);
					/*用户身份验证请求*/
					try {
						/*需要发送的指令,byte数组*/
						byte typeBytes[] = {Instruction.COMMAND_MANAGE,Instruction.COMMAND_SEPERATOR};
						byte accountBytes[] = account.getBytes("UTF-8");//得到标准的UTF-8编码
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
	 * @Function: videoConnectRunnable;
	 * @Description:
	 *      视频专用连接，用于接受视频信息
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
				/*指令专用链接认证不成功，则视频的也不会成功，等待*/
				while(!isAuthed) 
				{
					try {
						Thread.sleep(1000);  //睡眠
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if((isAuthed == true)&&(videoIsConnected == false))
				{
					/*正在重新连接*/
					Intent intent = new Intent();
					intent.setAction(intent.ACTION_EDIT);
					intent.putExtra("type", "disconnect");
					intent.putExtra("disconnect", "视频连接中...");
					sendBroadcast(intent);
					
					/*之前有过socket连接,先关闭，再开启新的*/
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
						/*断开连接后,重新连接*/
						try {
							videoSocket = new Socket(contrlCenterString, videoPort);
							/*得到输入流、输出流*/
							videoOutputStream = videoSocket.getOutputStream();
							videoInputStream = videoSocket.getInputStream();
							videoIsConnected = true; //视频連接成功
							
							/*告诉activity重新连接成功*/
							intent.setAction(intent.ACTION_EDIT);
							intent.putExtra("type", "disconnect");
							intent.putExtra("disconnect", "视频连接成功");
							sendBroadcast(intent);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							videoIsConnected = false; //連接失敗
							try {
								Thread.sleep(2500);  //失败后等待3s连接
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							videoIsConnected = false; //连接失败
							try {
								Thread.sleep(2500);  //失败后等待3s连接
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}	 
					}//end of connecting ContrlCenter
					else {
						/*断开连接后,重新连接*/
						try {
							videoSocket = new Socket(serverString, videoPort);
							/*得到输入流、输出流*/
							videoOutputStream = videoSocket.getOutputStream();
							videoInputStream = videoSocket.getInputStream();
							videoIsConnected = true; //视频連接成功
							
							/*告诉activity重新连接成功*/
							intent.setAction(intent.ACTION_EDIT);
							intent.putExtra("type", "disconnect");
							intent.putExtra("disconnect", "视频连接成功");
							sendBroadcast(intent);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							videoIsConnected = false; //連接失敗
							try {
								Thread.sleep(2500);  //失败后等待3s连接
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							videoIsConnected = false; //连接失败
							try {
								Thread.sleep(2500);  //失败后等待3s连接
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
					try {
						/*需要发送的指令,byte数组*/
						byte typeBytes[] = {Instruction.COMMAND_CONTRL,Instruction.COMMAND_SEPERATOR};
						byte accountBytes[] = account.getBytes("UTF-8");//得到标准的UTF-8编码
						byte twoBytes[] = {Instruction.COMMAND_SEPERATOR,Instruction.CTL_GET,Instruction.COMMAND_SEPERATOR, 
								Instruction.RES_TEMP, Instruction.COMMAND_SEPERATOR};
						String IDString = new String("10000");
						byte TempIDBytes[] = IDString.getBytes("UTF-8");
						//byte TempIDBytes[] = {'1','0'};
						byte threeBytes[] = {Instruction.COMMAND_SEPERATOR, Instruction.COMMAND_END};						
						byte temp_buffer[] = new byte[typeBytes.length + accountBytes.length+twoBytes.length
						                       +TempIDBytes.length+threeBytes.length];
						/*合并到一个byte数组中*/
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
					try {
						/*需要发送的指令(获取湿度),byte数组*/
						byte typeBytes[] = {Instruction.COMMAND_CONTRL,Instruction.COMMAND_SEPERATOR};
						byte accountBytes[] = account.getBytes("UTF-8");//得到标准的UTF-8编码
						byte twoBytes[] = {Instruction.COMMAND_SEPERATOR,Instruction.CTL_GET, Instruction.COMMAND_SEPERATOR, 
								Instruction.RES_HUMI, Instruction.COMMAND_SEPERATOR};
						String IDString = new String("10000");
						byte HumiIDBytes[] = IDString.getBytes("UTF-8");						
						byte threeBytes[] = {Instruction.COMMAND_SEPERATOR, Instruction.COMMAND_END};						
						byte humi_buffer[] = new byte[typeBytes.length + accountBytes.length+twoBytes.length
						                       +HumiIDBytes.length+threeBytes.length];
						/*合并到一个byte数组中*/
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
	* @Function: revMsgRunnable;
	* @Description:
	*      用于接受并且处理服务器发送来的信息
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
				if(isTestWifi == true)//正在测试wifi能否连接到stm32
				{
					try {
						Thread.sleep(1000);//先休眠一秒
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
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
//					Intent tempintent = new Intent();
//					tempintent.setAction(tempintent.ACTION_EDIT);
//					/*返回ihome模式开启情况*/
//					tempintent.putExtra("type", "video");
//					tempintent.putExtra("video", "read:"+temp);
//					sendBroadcast(tempintent);
					String revString = new String(buffer, 0, temp);	
					i = 0;
					System.out.println("get msg from server read:"+temp+" string:"+revString.length());
					while(i<revString.length())//可能有多组信息
					{
						/*获得指令主type*/
						if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
						{
							type = revString.charAt(i);
							i+=2;
						}
						else {
							/*当前指令错误则跳转到下一个指令*/
							while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*获得账户*/
						for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
						{
							;
						}
						i++;
						accountString = new String(revString.substring(start, end));
						/*确定来自于自己的控制中心或者SERVER*/
						if(!accountString.equals(account+'h')&&!accountString.equals("SERVER"))
						{
							while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*获得指令subtype*/
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
						/*预处理后处理指令*/
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
									/*返回ihome模式开启情况*/
									intent.putExtra("type", "ihome");
									intent.putExtra("ihome", "start");
									sendBroadcast(intent);
								}
								else if(subtype == Instruction.IHome_STOP){
									/*返回ihome模式开启情况*/
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
									if(subtype == Instruction.LOGIN_SUCCESS)//登陆成功
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
										isAuthed = false;    //认证失败
										accountReady = false;  //信息错误
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
							/*灯的状态*/
							else if(subtype == Instruction.RES_LAMP)
							{
								System.out.println("res_lamp");
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*获得灯的状态*/
								if((i + 1 <revString.length())&&(revString.charAt(i+1)==Instruction.COMMAND_SEPERATOR))
								{
									res = revString.charAt(i);
									i+=2;
								}
								else {
									/*不符合指令格式*/
									while((i<revString.length())&&(revString.charAt(i))!=Instruction.COMMAND_END)
									{
										i++;
									}
									i++;
									continue;
								}
								if(res == Instruction.LAMP_ON)
								{
									/*获得灯ID*/
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
									/*获得灯ID*/
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
							else if(subtype == Instruction.RES_TEMP)/*获取温度*/
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*获得设备ID*/
								for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
								{
									;
								}
								i++;
								String IDString = new String(revString.substring(start, end));
								/*获得value*/
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
								intent.putExtra("temp", IDString);//发送设备ID
								intent.putExtra(IDString, res+"");
								sendBroadcast(intent);
							}
							/*湿度*/
							else if(subtype == Instruction.RES_HUMI)
							{
								Intent intent = new Intent();
								intent.setAction(intent.ACTION_EDIT);
								/*获得设备ID*/
								for(start = i, end = start; (end<revString.length())&&((revString.charAt(end)!=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
								{
									;
								}
								i++;
								String IDString = new String(revString.substring(start, end));
								/*获得value*/
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
								intent.putExtra("humi", IDString);//发送设备ID
								intent.putExtra(IDString, res+"");
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
	* @Function: revVideoRunnable;
	* @Description:
	*      用于接受视频文件并且保存到手机中
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
				while(videoIsConnected == false)//连接已经断开，先等待重新链接
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
					int temp = videoInputStream.read(videoBuffer);
					if(temp < 0)
					{
						throw new Exception("断开连接");
					}
//					Intent tempintent = new Intent();
//					tempintent.setAction(tempintent.ACTION_EDIT);
//					/*返回ihome模式开启情况*/
//					tempintent.putExtra("type", "video");
//					tempintent.putExtra("video", "read:"+temp);
//					sendBroadcast(tempintent);
					//先复制到处理的缓冲区中
					System.arraycopy(videoBuffer    ,0,videoHandleBuffer, videoEnd, temp);
					videoEnd += temp;
					i = 0;
					System.out.println("get msg from server read:"+temp);
					while(i < videoHandleBuffer.length)//可能有多组信息
					{
						msgStart = i; //记录本次处理的信息头
						/*获得指令主type*/
						if((i + 1 <videoHandleBuffer.length)&&(videoHandleBuffer[i+1] ==Instruction.COMMAND_SEPERATOR))
						{
							type = videoHandleBuffer[i];
							i+=2;
						}
						else {
							/*当前指令错误则跳转到下一个指令*/
							while((i<videoHandleBuffer.length)&&(videoHandleBuffer[i])!=Instruction.COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
						/*获得账户*/
						for(start = i, end = start; (end<videoHandleBuffer.length)&&((videoHandleBuffer[end] !=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
						{
							;
						}
						i++;
						accountString = new String(videoHandleBuffer, start, end);
						/*确定来自于自己的控制中心或者SERVER*/
						if(!accountString.equals(account+'h')&&!accountString.equals("SERVER"))
						{
							/*当前指令错误则跳转到下一个指令*/
							while((i<videoHandleBuffer.length)&&(videoHandleBuffer[i])!=Instruction.COMMAND_END)
							{
								i++;
							}
							i++;
							continue;
						}
					   /*-------------------先处理视频指令------------------------*/
						if(type == Instruction.COMMAND_VIDEO)
						{	
							/*获得摄像头ID*/
							for(start = i, end = start; (end<videoHandleBuffer.length)&&((videoHandleBuffer[end] !=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
							{
								;
							}
							i++;
							cameraIDString = new String(videoHandleBuffer, start, end);
							if(cameraIDString.equals(cameraIDString))//确定为需要的视频ID：20000
							{
								subtype = videoHandleBuffer[i];
								if(subtype == Instruction.VIDEO_START)//视频流开始
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
									/*返回ihome模式开启情况*/
									intent.putExtra("type", "video");
									intent.putExtra("video", "video start");
									sendBroadcast(intent);
									
								}//end of video_start
								else if(subtype == Instruction.VIDEO_STOP)//数据流结束
								{
									i += 2;
									
									Intent intent = new Intent();
									intent.setAction(intent.ACTION_EDIT);
									/*返回ihome模式开启情况*/
									intent.putExtra("type", "video");
									intent.putExtra("video", "video stop");
									sendBroadcast(intent);
									
									if(jpegOutputStream != null)
									{
										try {
											jpegOutputStream.close();//关闭输出流
										} catch (Exception e) {
											// TODO: handle exception
											e.printStackTrace();
										}
									}
								}//end of video stop
								else {
									/*获得数据长度*/
									for(start = i, end = start; (end<videoHandleBuffer.length)&&((videoHandleBuffer[end] !=Instruction.COMMAND_SEPERATOR)) ; i++,end++)
									{
										;
									}
									i++;
									dataLengthString = new String(videoHandleBuffer, start, end);
									/*数据长度*/
									dataLength = Integer.valueOf(dataLengthString);
									//说明为数据
									
									if(i + dataLength <= videoHandleBuffer.length)//收到所有的数据
									{
										try {
											jpegOutputStream.write(videoHandleBuffer, i, i + dataLength);
											Intent intent = new Intent();
											intent.setAction(intent.ACTION_EDIT);
											/*返回ihome模式开启情况*/
											intent.putExtra("type", "video");
											intent.putExtra("video", dataLength+" ");
											sendBroadcast(intent);
										} catch (Exception e) {
											// TODO: handle exception
											e.printStackTrace();
										}
									}
									else {//没有受到所有数据
										System.arraycopy(videoHandleBuffer    , msgStart ,videoHandleBuffer, 0, videoHandleBuffer.length - msgStart);
										videoEnd = 10;
									}
									i += dataLength;
								}//end of 是数据
								continue;

							}//end of camera id
							
						}//end of 视频指令
					
					}//end of while(i < videoHandleBuffer.length) 处理多组信息
						
					
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					videoIsConnected = false; //断开连接
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					videoIsConnected = false; //断开连接
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();			
					videoIsConnected = false; //断开连接
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
				try {
					/*接收Fragment传递来的部分指令,并添加account*/
					byte onefield[] = intent.getByteArrayExtra("onefield");
					byte accountfield[] = account.getBytes("UTF-8");
					byte twofield[] = intent.getByteArrayExtra("twofield");
					byte buffer[] = new byte[onefield.length + accountfield.length+twofield.length];
					/*合并到一个byte数组中*/
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
					isConnected = false; //断开连接
					isAuthed = false;    //认证失效
				}//发送指令
			}
			else if(typeString.equals("ClientMainBack"))
			{
				/*主控界面按下了返回键，需要重新登录*/
				isConnected = false;
				isAuthed = false;
				accountReady = false;
				if(serverSocket != null)//如果有socket连接则解除连接
				{
					try {
						serverSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				/*通知登录activity要进行重新登录*/
				Intent intent1 = new Intent();
				intent1.setAction(intent1.ACTION_ANSWER);
	    		intent1.putExtra("result", "relogin");
	            sendBroadcast(intent1);
			}
		}
		
	}
}
