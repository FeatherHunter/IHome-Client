package com.example.ihome_client;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ListViewActivity extends ListActivity{

	ListView listView;
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.listview);
		
		//listView = (ListView)findViewById(R.id.mylistview);
		
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
		HashMap<String, String> map1 = new HashMap<String, String>();
		HashMap<String, String> map2 = new HashMap<String, String>();
		HashMap<String, String> map3 = new HashMap<String, String>();
		map1.put("user name", "wang");
		map1.put("user ip", "192.168.1.1");
		map2.put("user name", "wu");
		map2.put("user ip", "192.168.1.2");
		map3.put("user name", "li");
		map3.put("user ip", "192.168.1.3");
		list.add(map1);
		list.add(map2);
		list.add(map3);
		
		SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.user, new String[]{"user name", "user ip"}, new int[]{R.id.user_name, R.id.user_ip});
		
		setListAdapter(adapter);
	}

	@Override
	public void setListAdapter(ListAdapter adapter) {
		// TODO Auto-generated method stub
		super.setListAdapter(adapter);
	}

}
