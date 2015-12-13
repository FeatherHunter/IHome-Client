package com.example.ihome_client;

import com.example.ihome_client.ClientActivity.ButtonListener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Button;
import android.widget.TextView;

public class ClientMainActivity extends ActionBarActivity{

	TextView client_account, client_password;
	Button client_login, client_bluetooth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_main);

        client_login = (Button) findViewById(R.id.test);
        Intent intent = this.getIntent();
        String string = intent.getStringExtra("extras");
        client_login.setText(string);
        
    }

}
