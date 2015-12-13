package com.example.ihome_client;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;


public class ClientActivity extends ActionBarActivity {

	TextView client_account, client_password;
	Button client_login, client_bluetooth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        client_account = (TextView) findViewById(R.id.client_account);
        client_password = (TextView) findViewById(R.id.client_password);
        client_login = (Button) findViewById(R.id.client_login);
        client_bluetooth = (Button) findViewById(R.id.client_bluetooth);
        client_login.setOnClickListener(new ButtonListener());
        
    }
    class ButtonListener implements OnClickListener
    {

    	public void onClick(View v) {
    		// TODO Auto-generated method stub
    		//Uri uri = Uri.parse("smsto:100000");
    		//Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
    		//intent.putExtra("sms_body", "nice!");
    		Intent intent = new Intent();
    		intent.putExtra("extras", "haha");
            intent.setClass(ClientActivity.this, ClientMainActivity.class);
            ClientActivity.this.startActivity(intent);
    	}
    	
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.client, menu);
   
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
}


