package com.example.ihome_client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;

import com.feather.socketservice.IHomeService;

import ihome_client.bottombar.BottomBarPanel;
import ihome_client.bottombar.BottomBarPanel;
import ihome_client.bottombar.Constant;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.R.integer;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.GpsStatus.Listener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import ihome_client.*;

public class ClientActivity extends Activity {

	private ImageView logoImageView;
	private EditText client_account, client_password;
	private Button client_login, client_bluetooth;
	private BottomBarPanel bottomBarPanel;
	
	private String accountString;     //账户
	private String passwordString;    //密码
	private Boolean isAuthed = false;
	private String AUTH_ACTION = "android.intent.action.ANSWER";
	
	private Handler handler = new Handler();
	private AuthReceiver authReceiver;//广播接收器
	private ProgressDialog dialog; //登录的进度条
	private boolean firstSwitch = true;//第一次转换到ClientMainActivity，才需要刷新界面,
									//可能同时收到多个登陆成功广播，导致多次切换
	
	private Intent serviceIntent; //服务Intent
	
	//IHomeService.ServiceBinder serviceBinder;//IHomeService中的binder
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);  	
		
        logoImageView = (ImageView) findViewById(R.id.ihome_logo);
        client_account = (EditText) findViewById(R.id.client_account);
        client_password = (EditText) findViewById(R.id.client_password);
        client_login = (Button) findViewById(R.id.client_login);
        client_bluetooth = (Button) findViewById(R.id.client_bluetooth);
        client_login.setOnClickListener(new LoginButtonListener());
        client_bluetooth.setOnClickListener(new BluetoothButtonListener());
        
        /* connect server */
		dialog = new ProgressDialog(this);
		dialog.setTitle("提示");
		dialog.setMessage("正在登录中...");
		dialog.setCancelable(false);
        
    }
    class LoginButtonListener implements OnClickListener
    {

    	public void onClick(View v) {
    		// TODO Auto-generated method stub
    		accountString = client_account.getText().toString();
    		passwordString = client_password.getText().toString();
    		
    		/*动态注册receiver*/
    		authReceiver = new AuthReceiver();
    		IntentFilter filter = new IntentFilter();
    		filter.addAction(AUTH_ACTION);
    		registerReceiver(authReceiver, filter);//注册
    		
    		/*绑定service, 利用connection建立与service的联系*/
    		serviceIntent = new Intent();
    		serviceIntent.putExtra("command", "auth");
    		serviceIntent.putExtra("account", accountString);
    		serviceIntent.putExtra("password", passwordString);
    		serviceIntent.setClass(ClientActivity.this, IHomeService.class);
    		//bindService(serviceIntent, connection, BIND_AUTO_CREATE); //绑定service,并且自动创建service
    		startService(serviceIntent); //开启服务
    		dialog.show(); //显示登陆进度条
    		/*超时处理*/
    		Thread thread = new Thread(loginOvertimeRunnable);
    		thread.start();
    	}	
    }
    
    class BluetoothButtonListener implements OnClickListener
    {

    	public void onClick(View v) {
    		Intent intent = new Intent();
			
			intent.putExtra("mode", 2);//选择模式：2为蓝牙模式
    		intent.putExtra("account", client_account.getText().toString());
            intent.setClass(ClientActivity.this, ClientMainActivity.class);
            ClientActivity.this.startActivity(intent);
    	}	
    }
    /*超过10S还没有连接成功，则一定失败了*/
    Runnable loginOvertimeRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(isAuthed == false)
			{
				dialog.dismiss();
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Toast.makeText(ClientActivity.this, "登陆超时", Toast.LENGTH_SHORT).show();
						
			    		serviceIntent = new Intent();
			    		serviceIntent.putExtra("command", "stop");
			    		serviceIntent.setClass(ClientActivity.this, IHomeService.class);
			    		startService(serviceIntent); //发送停止指令
			    		stopService(serviceIntent);  //关闭后台连接服务
					}
				});
			}
			
		}
	};
//    /*用于service和activity之间通信，直接调用IHomeService之中ServiceBinder的方法来与service通信*/
//    private ServiceConnection connection = new ServiceConnection() {
//		
//		@Override
//		public void onServiceDisconnected(ComponentName name) {
//			// TODO Auto-generated method stub
//			
//		}
//		
//		@Override
//		public void onServiceConnected(ComponentName name, IBinder service) {
//			// TODO Auto-generated method stub
//			serviceBinder = (ServiceBinder) service;
//			//serviceBinder.auth(accountString, passwordString);
//			
//		}
//		
//	};
	
	private class AuthReceiver extends BroadcastReceiver{

		public AuthReceiver() {
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
				String resultString = intent.getStringExtra("result");
				if(resultString.equals("success"))
				{
					if(firstSwitch == false) return;
					firstSwitch = false;
					/*切换到主控界面*/
		    		Intent intentMain = new Intent();
					
		    		intentMain.putExtra("mode", 1);//选择模式：1为ethnet模式
		    		intentMain.putExtra("account", client_account.getText().toString());
		    		intentMain.setClass(ClientActivity.this, ClientMainActivity.class);
		            ClientActivity.this.startActivity(intentMain);
		            
					isAuthed = true;
					dialog.dismiss(); //登陆成功，解除进度条
				}
				else if (resultString.equals("falied")) {
					if(firstSwitch == false) return;
					firstSwitch = false;
					
					Toast.makeText(ClientActivity.this, "登陆失败！", Toast.LENGTH_SHORT).show();
					/*清空输入*/
					client_account.setText("");
					client_password.setText("");
					isAuthed = false;
					dialog.dismiss(); //登陆失败，解除进度条
				}
				else if(resultString.equals("relogin"))
				{
					/*要开始重新登录*/
					System.out.println("relogin");
					firstSwitch = true;
				}
		}
		
		
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, 0, 0, "退出");
    	menu.add(1, 1, 1, "设置");
    	menu.add(2, 2, 2, "帮助");
        //getMenuInflater().inflate(R.menu.client, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(authReceiver); //解除receiver的注册
		/*停止后台服务*/
		Intent serviceIntent = new Intent();
		serviceIntent.setClass(ClientActivity.this, IHomeService.class);
		stopService(serviceIntent);
		//unbindService(connection);//解除绑定
	}
    
    
    
}


