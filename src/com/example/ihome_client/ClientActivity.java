package com.example.ihome_client;

import ihome_client.bottombar.BottomBarPanel;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.feather.socketservice.IHomeService;

/** 
 * @CopyRight: ������ 2015~2025
 * @Author Feather Hunter(����)
 * @qq:975559549
 * @Version: 1.0 
 * @Date:2015/12/25
 * @Description: ��½����,���������˺������������̨Service���������֤��¼��
 *        ��ϸ��Ŀ���ܣ�
 *        1.onCreate���úø������
 *        2.��������½������������̨Service��Receiver���ڵ�½�ɹ�����ת��ClientMainActivity�����ؽ���
 *       
 * @Function List:
 *      1. void onCreate 					//��ʼ�����������׼��dialog
 *      2. LoginButtonListener  			//������¼������̬ע��Receiver������������̨Service����
 *      3. Runnable loginOvertimeRunnable 	//�������ӳ�ʱʱ��ر�dialog
 *      4. AuthReceiver         			//���չ㲥�������½�ɹ��͵�¼ʧ�ܵ������
 *      5. void onDestroy()     			//���Receiver�����ҹرպ�̨����
 * @history:
 *    v1.0 ��ɻ�����¼�Ĺ���
 **/

public class ClientActivity extends Activity {

	private ImageView logoImageView;
	private EditText client_account, client_password;
	private Button client_login, client_bluetooth;
	private BottomBarPanel bottomBarPanel;
	
	private String accountString;     //�˻�
	private String passwordString;    //����
	private Boolean isAuthed = false;
	private String AUTH_ACTION = "android.intent.action.ANSWER";
	
	private Handler handler = new Handler();
	private AuthReceiver authReceiver;//�㲥������
	private ProgressDialog dialog; //��¼�Ľ�����
	private boolean firstSwitch = true;//��һ��ת����ClientMainActivity������Ҫˢ�½���,
									//����ͬʱ�յ������½�ɹ��㲥�����¶���л�
	
	private Intent serviceIntent; //����Intent
	private WifiManager wifiManager; //���ȿ���wifiģʽ
	private Toast toast;             //�Զ���Toast
	//IHomeService.ServiceBinder serviceBinder;//IHomeService�е�binder
	
	/**
	 *  @Function:void onCreate
	 *  @author:Feather Hunter
	 *  @Description:
	 *  	��ø������ID�����ҳ�ʼ��dialog�����ڵ�¼��
	 *  @calls:
	 *     1. new LoginButtonListener(); //��¼������
	 *  @Input:
	 *  @Return:
	 */
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

		wifiManager = (WifiManager) ClientActivity.this.getSystemService(Service.WIFI_SERVICE);
		if(wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED)//wifiû�д�
		{
			wifiManager.setWifiEnabled(true);
		}
		toast = Toast.makeText(getApplicationContext(), "tip:���ն���ͬһWIFI�ڲ�������������", Toast.LENGTH_LONG);
		toast.setGravity(Gravity.TOP, 0, 0);
		toast.show();
		//Toast.makeText(ClientActivity.this, "tip:���ն���ͬһWIFI�ڲ�������������", Toast.LENGTH_LONG).show();
		/* connect server */
		dialog = new ProgressDialog(this);
		dialog.setTitle("��ʾ");
		dialog.setMessage("���ڵ�¼��...");
		dialog.setCancelable(false);

	}
    /**
	 *  @Class:LoginButtonListener
	 *  @author:Feather Hunter
	 *  @Description:
	 *  	��¼������������̬ע��authReceiver���ڽ��ܺ�̨Service���͵Ĺ㲥
	 *  	������Service������ʾ��dialog�������˳��е���ʱ����������һ��ʱ��
	 *  	��δ��¼�ɹ�������ʾʧ�ܡ�
	 */
    class LoginButtonListener implements OnClickListener
    {

    	public void onClick(View v) {
    		// TODO Auto-generated method stub
    		accountString = client_account.getText().toString();
    		passwordString = client_password.getText().toString();
    		
    		/*��̬ע��receiver*/
    		authReceiver = new AuthReceiver();
    		IntentFilter filter = new IntentFilter();
    		filter.addAction(AUTH_ACTION);
    		registerReceiver(authReceiver, filter);//ע��
    		
    		/*��service, ����connection������service����ϵ*/
    		serviceIntent = new Intent();
    		serviceIntent.putExtra("command", "auth");
    		serviceIntent.putExtra("account", accountString);
    		serviceIntent.putExtra("password", passwordString);
    		serviceIntent.setClass(ClientActivity.this, IHomeService.class);
    		//bindService(serviceIntent, connection, BIND_AUTO_CREATE); //��service,�����Զ�����service
    		startService(serviceIntent); //��������
    		dialog.show(); //��ʾ��½������
    		/*��ʱ����*/
    		Thread thread = new Thread(loginOvertimeRunnable);
    		thread.start();
    	}	
    }
    
    class BluetoothButtonListener implements OnClickListener
    {

    	public void onClick(View v) {
    		Intent intent = new Intent();
			
			intent.putExtra("mode", 2);//ѡ��ģʽ��2Ϊ����ģʽ
    		intent.putExtra("account", client_account.getText().toString());
            intent.setClass(ClientActivity.this, ClientMainActivity.class);
            ClientActivity.this.startActivity(intent);
    	}	
    }
    
    /**
	 *  @Object: loginOvertimeRunnable
	 *  @Description:
	 *  	������½��ļ�ʱ���ܣ���һ��ʱ����û�����ӳɹ���һ��ʧ����
	 */
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
						Toast.makeText(ClientActivity.this, "��½��ʱ", Toast.LENGTH_SHORT).show();
						
			    		serviceIntent = new Intent();
			    		serviceIntent.putExtra("command", "stop");
			    		serviceIntent.setClass(ClientActivity.this, IHomeService.class);
			    		startService(serviceIntent); //����ָֹͣ��
			    		stopService(serviceIntent);  //�رպ�̨���ӷ���
					}
				});
			}
			
		}
	};
	
	/**
	 *  @Class: AuthReceiver
	 *  @Description:
	 *  	����½�ɹ�,���͵�¼ģʽ��ClientMainActivity,�����л���ClientMainActivity
	 */
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
					/*�л������ؽ���*/
		    		Intent intentMain = new Intent();
					
		    		intentMain.putExtra("mode", 1);//ѡ��ģʽ��1Ϊethnetģʽ
		    		intentMain.putExtra("account", client_account.getText().toString());
		    		intentMain.setClass(ClientActivity.this, ClientMainActivity.class);
		            ClientActivity.this.startActivity(intentMain);
		            
					isAuthed = true;
					dialog.dismiss(); //��½�ɹ������������
				}
				else if (resultString.equals("falied")) {
					if(firstSwitch == false) return;
					firstSwitch = false;
					
					Toast.makeText(ClientActivity.this, "��½ʧ�ܣ�", Toast.LENGTH_SHORT).show();
					/*�������*/
					client_account.setText("");
					client_password.setText("");
					isAuthed = false;
					dialog.dismiss(); //��½ʧ�ܣ����������
				}
				else if(resultString.equals("relogin"))
				{
					/*Ҫ��ʼ���µ�¼*/
					System.out.println("relogin");
					firstSwitch = true;
				}
		}
	}

	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, 0, 0, "�˳�");
    	menu.add(1, 1, 1, "����");
    	menu.add(2, 2, 2, "����");
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
		unregisterReceiver(authReceiver); //���receiver��ע��
		/*ֹͣ��̨����*/
		Intent serviceIntent = new Intent();
		serviceIntent.setClass(ClientActivity.this, IHomeService.class);
		stopService(serviceIntent);
		//unbindService(connection);//�����
	}
    
    
    
}

