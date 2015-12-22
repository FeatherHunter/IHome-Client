package com.example.ihome_client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import ihome_client.bottombar.BottomBarPanel;
import ihome_client.bottombar.BottomBarPanel;
import ihome_client.bottombar.Constant;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.location.GpsStatus.Listener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
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
import android.widget.TextView;
import android.widget.Toast;

import ihome_client.*;

public class ClientActivity extends Activity {

	EditText client_account, client_password;
	Button client_login, client_bluetooth;
	BottomBarPanel bottomBarPanel;
	ServerSocket socket;             //server socket
	OutputStream outputStream; //ouput to server
	InputStream inputStream;   //input from server
	String accountString;     //账户
	String passwordString;    //密码
	char seperator = (char) 31;//31单元分隔符
	Boolean isConnected = false;
	Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);  	
		
        client_account = (EditText) findViewById(R.id.client_account);
        client_password = (EditText) findViewById(R.id.client_password);
        client_login = (Button) findViewById(R.id.client_login);
        client_bluetooth = (Button) findViewById(R.id.client_bluetooth);
        client_login.setOnClickListener(new ButtonListener());
        //client_bluetooth.setOnClickListener(new HandlerStopListener());
        
    }
    
    class ButtonListener implements OnClickListener
    {

    	public void onClick(View v) {
    		// TODO Auto-generated method stub
    		//Uri uri = Uri.parse("smsto:100000");
    		//Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
    		//intent.putExtra("sms_body", "nice!");
    		accountString = client_account.getText().toString();
    		passwordString = client_password.getText().toString();
    		
    		Thread thread = new Thread(connectingSocketRunnable);
    		thread.start();
    	}	
    }

    Runnable connectingSocketRunnable = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
				try {
					socket = new ServerSocket("139.129.19.115", 8080);
					
					/*得到输入流、输出流*/
					outputStream = socket.getOutputStream();
					inputStream = socket.getInputStream();
					/*发送登录请求*/
					String loginRequestString = new String("MANAGEMENT"+seperator+accountString+seperator
							+"LOGIN"+seperator+passwordString+seperator);
					byte buffer[] = loginRequestString.getBytes();
					outputStream.write(buffer, 0, buffer.length);
					outputStream.flush();
					/*得到服务器返回信息*/
					int temp = inputStream.read(buffer);
					String revString = new String(buffer, 0, temp);		
					String successString = new String("RESULT"+seperator+"LOGIN"+seperator+"SUCCESS"+seperator);
					if(revString.equals(successString))
					{
						isConnected = true; //登陆成功
						/*跳转到主界面(传递账号进去)*/
						Intent intent = new Intent();
						
						intent.putExtra("mode", 1);//选择模式：1为 以太网模式
			    		intent.putExtra("account", client_account.getText().toString());
			    		//intent.putExtra("password", client_password.getText().toString());
			    		Bundle bundle = new Bundle();
			    		bundle.putSerializable("socket", socket);
			    		intent.putExtras(bundle);
			    		//intent.putExtra("socket", socket);
			            intent.setClass(ClientActivity.this, ClientMainActivity.class);
			            ClientActivity.this.startActivity(intent);
					}
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
		}
	};

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
    
    
}


