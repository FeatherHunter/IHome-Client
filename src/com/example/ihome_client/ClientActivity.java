package com.example.ihome_client;

import ihome_client.bottombar.BottomBarPanel;
import ihome_client.bottombar.BottomBarPanel;
import ihome_client.bottombar.Constant;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

import ihome_client.*;

public class ClientActivity extends Activity {

	EditText client_account, client_password;
	Button client_login, client_bluetooth;
	BottomBarPanel bottomBarPanel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        //Fragment1 fragment1 = new Fragment1();
        //getFragmentManager().beginTransaction().replace(R.id.linear1, fragment1).commit();

        //Fragment2 fragment2 = new Fragment2();
        //getFragmentManager().beginTransaction().replace(R.id.linear2, fragment2).commit();
        	
		
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
    		Intent intent = new Intent();
    		intent.putExtra("account", client_account.getText().toString());
    		intent.putExtra("password", client_password.getText().toString());
            //intent.setClass(ClientActivity.this, ClientMainActivity.class);
            intent.setClass(ClientActivity.this, ClientMainActivity.class);
            ClientActivity.this.startActivity(intent);
    	}	
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, 0, 0, "退出");
    	menu.add(0, 1, 1, "设置");
    	menu.add(1, 0, 0, "关于");
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


