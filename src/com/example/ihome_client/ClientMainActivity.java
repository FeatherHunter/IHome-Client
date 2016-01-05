package com.example.ihome_client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.feather.socketservice.IHomeService;

import ihome_client.bottombar.BaseFragment;
import ihome_client.bottombar.BottomBarPanel;
import ihome_client.bottombar.BottomBarPanel.BottomPanelCallback;
import ihome_client.bottombar.Constant;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.OnFinished;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import ihome_client.*;

/** 
 * @CopyRight: ������ 2015~2025
 * @Author Feather Hunter(����)
 * @qq:975559549
 * @Version:1.0 
 * @Date:2015/12/25
 * @Description: ��½֮��Ŀ��������档���ڼ���ҳ����Ƭ(fragment)���л���
 *              �������̨Service���н���������ɸ��ֿ��ƹ��ܡ�
 * @Function List:
 *      1. void onCreate 		//�жϵ�ǰ�Ĺ���ģʽ,��̬ע��㲥������
 *      2. class ContrlReceiver //������,�����¶ȵ�������Ϣ,��ʾ���Ӻ���֤��Ϣ
 *      3. void initUI() 		//��ʼ������
 *      4. void setTabSelection //����һ��Fragment����,���л�Fragment
 *      5. void switchFragment  //�л�Fragment
 *      6. void attachFragment  //
 *      7. void commitTransactions //
 *      8. void setDefaultFirstFragment
 *      9. FragmentTransaction ensureTransaction
 *      10.Fragment getFragment
 *      11.void detachFragment(Fragment f)
 *      12.void setHandler(Handler handler); //���ں�FragmentIHomeͨ��
 **/

public class ClientMainActivity extends Activity implements BottomPanelCallback {
	BottomBarPanel bottomPanel = null;
	//HeadControlPanel headPanel = null;
	
	private FragmentManager fragmentManager = null;
	private FragmentTransaction fragmentTransaction = null;
	FragmentIHome fragmentIHome;

	private String account;

	private OutputStream outputStream;
	private InputStream inputStream;
	private boolean isConnected = false;
	char seperator = (char) 31;//31��Ԫ�ָ���
	private ContrlReceiver contrlReceiver;
	private String CONTRL_ACTION = "android.intent.action.EDIT";
	
	public Handler communicationHandler;
/*	private MessageFragment messageFragment;
	private ContactsFragment contactsFragment;
	private NewsFragment newsFragment;
	private SettingFragment settingFragment;*/
	
	public static String currFragTag = "";

	Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
		}
		
	};
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_main);
		/*��ʼ������*/
		initUI();
		fragmentManager = getFragmentManager();
		setDefaultFirstFragment(Constant.FRAGMENT_FLAG_IHOME);
		
		Intent intent = getIntent();
		int mode = intent.getIntExtra("mode", 2); //�õ�ģʽ��Ϣ��Ĭ��Ϊ����ģʽ2
		if(mode == 1)//ethnetģʽ
		{
			Toast.makeText(this, "��ǰ��������ģʽ", Toast.LENGTH_SHORT).show();
			isConnected = true; //���ӳɹ�

		}
		else if(mode == 2)//��ǰ��������ģʽ
		{
			Toast.makeText(this, "��ǰ��������ģʽ", Toast.LENGTH_SHORT).show();
		}
		/*��̬ע��receiver*/
		contrlReceiver = new ContrlReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(CONTRL_ACTION);
		registerReceiver(contrlReceiver, filter);//ע��
	}
	/*����*/
	public void setHandler(Handler handler)
	{
		communicationHandler = handler;
	}
	private class ContrlReceiver extends BroadcastReceiver{

		public ContrlReceiver() {
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String typeString = intent.getStringExtra("type");
			Message msgMessage = new Message();
			Bundle bundle = new Bundle();
			if(typeString.equals("temp"))/*���͸���һ��ihome fragment*/
			{
				bundle.putString("type", "temp");
				String IDString = intent.getStringExtra("temp");
				bundle.putString("temp", IDString);
				bundle.putString(IDString, intent.getStringExtra(IDString));
				msgMessage.setData(bundle);
				communicationHandler.sendMessage(msgMessage);
				
			}
			/*�����¶���Ϣ*/
			else if(typeString.equals("humi"))
			{
				bundle.putString("type", "humi");
				String IDString = intent.getStringExtra("humi");
				bundle.putString("humi", IDString);
				bundle.putString(IDString, intent.getStringExtra(IDString));
				msgMessage.setData(bundle);
				communicationHandler.sendMessage(msgMessage);
			}
			/*�Ƶ�״̬*/
			else if(typeString.equals("ledon"))
			{
				bundle.putString("type", "ledon");
				bundle.putString("ledon", intent.getStringExtra("ledon"));
				msgMessage.setData(bundle);
				communicationHandler.sendMessage(msgMessage);
			}
			/*�Ƶ�״̬*/
			else if(typeString.equals("ledoff"))
			{
				bundle.putString("type", "ledoff");
				String ledString = intent.getStringExtra("ledoff");
				bundle.putString("ledoff", ledString);
				msgMessage.setData(bundle);
				communicationHandler.sendMessage(msgMessage);
			}
			/*��ʾ���Ӻ���֤������*/
			else if(typeString.equals("disconnect"))
			{
				String stateString = intent.getStringExtra("disconnect");
				if(stateString.equals("authing"))
				{
					Toast.makeText(ClientMainActivity.this, "������֤��Ϣ...", Toast.LENGTH_SHORT).show();
				}
				else if(stateString.equals("connecting"))
				{
					Toast.makeText(ClientMainActivity.this, "�������ӷ�����...", Toast.LENGTH_SHORT).show();
				}
				else if(stateString.equals("connected"))
				{
					Toast.makeText(ClientMainActivity.this, "���ӳɹ�", Toast.LENGTH_SHORT).show();
				}
				else if(stateString.equals("authed"))
				{
					Toast.makeText(ClientMainActivity.this, "��֤�ɹ�", Toast.LENGTH_SHORT).show();
				}
			}
			/*����IHome mode����״��*/
			else if(typeString.equals("ihome"))
			{
				bundle.putString("type", "ihome");
				String modeString = intent.getStringExtra("ihome");
				bundle.putString("ihome", modeString);
				msgMessage.setData(bundle);
				communicationHandler.sendMessage(msgMessage);
				Toast.makeText(ClientMainActivity.this, modeString, Toast.LENGTH_SHORT).show();
			}

		}
		
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.client, menu);
		return true;
	}
	private void initUI(){
		bottomPanel = (BottomBarPanel)findViewById(R.id.bottom_layout);
		if(bottomPanel != null){
			bottomPanel.initBottomPanel();
			bottomPanel.setBottomCallback(this);
		}
		/*
		headPanel = (HeadControlPanel)findViewById(R.id.head_layout);
		if(headPanel != null){
			headPanel.initHeadPanel();
		}
		*/
	}

	/* ����BottomControlPanel�Ļص�
	 * @see org.yanzi.ui.BottomControlPanel.BottomPanelCallback#onBottomPanelClick(int)
	 */
	@Override
	public void onBottomPanelClick(int itemId) {
		// TODO Auto-generated method stub
		String tag = "";
		if((itemId & Constant.BTN_FLAG_IHOME) != 0){
			tag = Constant.FRAGMENT_FLAG_IHOME;
		}else if((itemId & Constant.BTN_FLAG_CONTRL) != 0){
			tag = Constant.FRAGMENT_FLAG_CONTRL;
		}else if((itemId & Constant.BTN_FLAG_VIDEO) != 0){
			tag = Constant.FRAGMENT_FLAG_VIDEO;
		}else if((itemId & Constant.BTN_FLAG_CSERVICE) != 0){
			tag = Constant.FRAGMENT_FLAG_CSERVICE;
		}
		setTabSelection(tag); //�л�Fragment
		//headPanel.setMiddleTitle(tag);//�л����� 
	}
	
	/**����ѡ�е�Tag
	 * @param tag
	 */
	public  void setTabSelection(String tag) {
		// ����һ��Fragment����
		fragmentTransaction = fragmentManager.beginTransaction();
		 if(TextUtils.equals(tag, Constant.FRAGMENT_FLAG_IHOME)){
		   if (fragmentIHome == null) {
			   fragmentIHome = new FragmentIHome();
			} 
		 }
/*			
		}else if(TextUtils.equals(tag, Constant.FRAGMENT_FLAG_CONTACTS)){
			if (contactsFragment == null) {
				contactsFragment = new ContactsFragment();
			} 
			
		}else if(TextUtils.equals(tag, Constant.FRAGMENT_FLAG_NEWS)){
			if (newsFragment == null) {
				newsFragment = new NewsFragment();
			}
			
		}else if(TextUtils.equals(tag,Constant.FRAGMENT_FLAG_SETTING)){
			if (settingFragment == null) {
				settingFragment = new SettingFragment();
			}
		}else if(TextUtils.equals(tag, Constant.FRAGMENT_FLAG_SIMPLE)){
			if (simpleFragment == null) {
				simpleFragment = new SimpleFragment();
			} 
			
		}*/
		 switchFragment(tag);
		 
	}
	
	/**�л�fragment 
	 * @param tag
	 */
	private  void switchFragment(String tag){
		if(TextUtils.equals(tag, currFragTag)){
			return;
		}
		//����һ��fragment detach�� 
		if(currFragTag != null && !currFragTag.equals("")){
			detachFragment(getFragment(currFragTag));
		}
		attachFragment(R.id.main_window, getFragment(tag), tag);
		commitTransactions( tag);
	} 
	
	private void attachFragment(int layout, Fragment f, String tag){
		if(f != null){
			if(f.isDetached()){
				ensureTransaction();
				fragmentTransaction.attach(f);
				
			}else if(!f.isAdded()){
				ensureTransaction();
				fragmentTransaction.add(layout, f, tag);
			}
		}
	}
	
	private void commitTransactions(String tag){
		if (fragmentTransaction != null && !fragmentTransaction.isEmpty()) {
			fragmentTransaction.commit();
			currFragTag = tag;
			fragmentTransaction = null;
		}
	}
	
	private void setDefaultFirstFragment(String tag){
		Log.i("yan", "setDefaultFirstFragment enter... currFragTag = " + currFragTag);
		setTabSelection(tag);
		bottomPanel.defaultBtnChecked();
		Log.i("yan", "setDefaultFirstFragment exit...");
	}
	
	
	private FragmentTransaction ensureTransaction( ){
		if(fragmentTransaction == null){
			fragmentTransaction = fragmentManager.beginTransaction();
			fragmentTransaction
			.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			
		}
		return fragmentTransaction;
		
	}
	
	private Fragment getFragment(String tag){
		
		Fragment f = fragmentManager.findFragmentByTag(tag);
		
		if(f == null){
			//Toast.makeText(getApplicationContext(), "fragment = null tag = " + tag, Toast.LENGTH_SHORT).show();
			f = BaseFragment.newInstance(getApplicationContext(), tag);
		}
		return f;
		
	}
	private void detachFragment(Fragment f){
		
		if(f != null && !f.isDetached()){
			ensureTransaction();
			fragmentTransaction.detach(f);
		}
	}
	

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		currFragTag = "";
		
		/*���͹㲥��Service�������������Ѿ��Ͽ�*/
		Intent intent = new Intent();
		intent.putExtra("type", "ClientMainBack");
		intent.setAction(intent.ACTION_MAIN);
		this.sendBroadcast(intent);
		
		unregisterReceiver(contrlReceiver);

//		/*ֹͣ��̨����*/
//		Intent serviceIntent = new Intent();
//		serviceIntent.setClass(ClientMainActivity.this, IHomeService.class);
//		stopService(serviceIntent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
	}

}