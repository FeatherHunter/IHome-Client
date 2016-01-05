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
 * @CopyRight: 王辰浩 2015~2025
 * @Author Feather Hunter(猎羽)
 * @qq:975559549
 * @Version:1.0 
 * @Date:2015/12/25
 * @Description: 登陆之后的控制主界面。用于几个页面碎片(fragment)的切换。
 *              并且与后台Service进行交互，来完成各种控制功能。
 * @Function List:
 *      1. void onCreate 		//判断当前的工作模式,动态注册广播接收器
 *      2. class ContrlReceiver //接收器,更新温度等数据信息,显示连接和认证信息
 *      3. void initUI() 		//初始化界面
 *      4. void setTabSelection //开启一个Fragment事务,并切换Fragment
 *      5. void switchFragment  //切换Fragment
 *      6. void attachFragment  //
 *      7. void commitTransactions //
 *      8. void setDefaultFirstFragment
 *      9. FragmentTransaction ensureTransaction
 *      10.Fragment getFragment
 *      11.void detachFragment(Fragment f)
 *      12.void setHandler(Handler handler); //用于和FragmentIHome通信
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
	char seperator = (char) 31;//31单元分隔符
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
		/*初始化界面*/
		initUI();
		fragmentManager = getFragmentManager();
		setDefaultFirstFragment(Constant.FRAGMENT_FLAG_IHOME);
		
		Intent intent = getIntent();
		int mode = intent.getIntExtra("mode", 2); //得到模式信息，默认为蓝牙模式2
		if(mode == 1)//ethnet模式
		{
			Toast.makeText(this, "当前处于网络模式", Toast.LENGTH_SHORT).show();
			isConnected = true; //连接成功

		}
		else if(mode == 2)//当前处于蓝牙模式
		{
			Toast.makeText(this, "当前处于蓝牙模式", Toast.LENGTH_SHORT).show();
		}
		/*动态注册receiver*/
		contrlReceiver = new ContrlReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(CONTRL_ACTION);
		registerReceiver(contrlReceiver, filter);//注册
	}
	/*设置*/
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
			if(typeString.equals("temp"))/*发送给第一个ihome fragment*/
			{
				bundle.putString("type", "temp");
				String IDString = intent.getStringExtra("temp");
				bundle.putString("temp", IDString);
				bundle.putString(IDString, intent.getStringExtra(IDString));
				msgMessage.setData(bundle);
				communicationHandler.sendMessage(msgMessage);
				
			}
			/*更新温度信息*/
			else if(typeString.equals("humi"))
			{
				bundle.putString("type", "humi");
				String IDString = intent.getStringExtra("humi");
				bundle.putString("humi", IDString);
				bundle.putString(IDString, intent.getStringExtra(IDString));
				msgMessage.setData(bundle);
				communicationHandler.sendMessage(msgMessage);
			}
			/*灯的状态*/
			else if(typeString.equals("ledon"))
			{
				bundle.putString("type", "ledon");
				bundle.putString("ledon", intent.getStringExtra("ledon"));
				msgMessage.setData(bundle);
				communicationHandler.sendMessage(msgMessage);
			}
			/*灯的状态*/
			else if(typeString.equals("ledoff"))
			{
				bundle.putString("type", "ledoff");
				String ledString = intent.getStringExtra("ledoff");
				bundle.putString("ledoff", ledString);
				msgMessage.setData(bundle);
				communicationHandler.sendMessage(msgMessage);
			}
			/*显示连接和认证身份情况*/
			else if(typeString.equals("disconnect"))
			{
				String stateString = intent.getStringExtra("disconnect");
				if(stateString.equals("authing"))
				{
					Toast.makeText(ClientMainActivity.this, "正在验证信息...", Toast.LENGTH_SHORT).show();
				}
				else if(stateString.equals("connecting"))
				{
					Toast.makeText(ClientMainActivity.this, "正在连接服务器...", Toast.LENGTH_SHORT).show();
				}
				else if(stateString.equals("connected"))
				{
					Toast.makeText(ClientMainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
				}
				else if(stateString.equals("authed"))
				{
					Toast.makeText(ClientMainActivity.this, "认证成功", Toast.LENGTH_SHORT).show();
				}
			}
			/*发送IHome mode开启状况*/
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

	/* 处理BottomControlPanel的回调
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
		setTabSelection(tag); //切换Fragment
		//headPanel.setMiddleTitle(tag);//切换标题 
	}
	
	/**设置选中的Tag
	 * @param tag
	 */
	public  void setTabSelection(String tag) {
		// 开启一个Fragment事务
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
	
	/**切换fragment 
	 * @param tag
	 */
	private  void switchFragment(String tag){
		if(TextUtils.equals(tag, currFragTag)){
			return;
		}
		//把上一个fragment detach掉 
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
		
		/*发送广播给Service，告诉他链接已经断开*/
		Intent intent = new Intent();
		intent.putExtra("type", "ClientMainBack");
		intent.setAction(intent.ACTION_MAIN);
		this.sendBroadcast(intent);
		
		unregisterReceiver(contrlReceiver);

//		/*停止后台服务*/
//		Intent serviceIntent = new Intent();
//		serviceIntent.setClass(ClientMainActivity.this, IHomeService.class);
//		stopService(serviceIntent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
	}

}