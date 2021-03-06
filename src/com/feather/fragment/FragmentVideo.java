package com.feather.fragment;

import java.io.File;
import java.io.UnsupportedEncodingException;

import com.example.ihome_client.R;
import com.feather.activity.ClientMainActivity;
import com.feather.activity.Instruction;
import com.feather.bottombar.BaseFragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentVideo extends BaseFragment{

	ClientMainActivity mainActivity;
	Button video_button;
	ImageView cameraiImageView;
	private boolean selection = true;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		return inflater.inflate(R.layout.video_fragment, container, false);
	}
	
	

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		/*要在onCreateView之后得到空间才是有效的*/
		video_button = (Button) getActivity().findViewById(R.id.video_button);
		cameraiImageView = (ImageView) getActivity().findViewById(R.id.camera_jpg);
		/*设置监听器*/
		video_button.setOnClickListener(new ButtonListener());
		//videoStart(); //start video
	}

	public void videoStart()
	{
		try {
			/*需要发送的指令,byte数组*/
			String IDString = "20000";
			byte typeBytes[] = {Instruction.COMMAND_CONTRL,Instruction.COMMAND_SEPERATOR};
			byte subtypeBytes[] = {Instruction.COMMAND_SEPERATOR,Instruction.CTL_VIDEO, Instruction.COMMAND_SEPERATOR};
			byte operatorBytes[] = {Instruction.VIDEO_START, Instruction.COMMAND_SEPERATOR};
			byte IDBytes[] = IDString.getBytes("UTF-8");
			byte endBytes[] = {Instruction.COMMAND_SEPERATOR, Instruction.COMMAND_END};
			byte buffer[] = new byte[subtypeBytes.length+operatorBytes.length
					+IDBytes.length+endBytes.length];

			/*转换account后面所有指令*/
			int start = 0;
			System.arraycopy(subtypeBytes ,0,buffer,start, subtypeBytes.length);
			start+=subtypeBytes.length;
			System.arraycopy(operatorBytes ,0,buffer,start, operatorBytes.length);
			start+=operatorBytes.length;
			System.arraycopy(IDBytes,0,buffer,start, IDBytes.length);
			start+=IDBytes.length;
			System.arraycopy(endBytes   ,0,buffer,start, endBytes.length);

				/*发送广播给Service，让其发送信息给服务器*/
			Intent intent = new Intent();
			intent.putExtra("type", "send");
			intent.putExtra("onefield", typeBytes);
			intent.putExtra("twofield", buffer);
			intent.setAction(intent.ACTION_MAIN);
			getActivity().sendBroadcast(intent);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void videoEnd()
	{
		try {
			/*需要发送的指令,byte数组*/
			String IDString = "20000";
			byte typeBytes[] = {Instruction.COMMAND_CONTRL,Instruction.COMMAND_SEPERATOR};
			byte subtypeBytes[] = {Instruction.COMMAND_SEPERATOR,Instruction.CTL_VIDEO, Instruction.COMMAND_SEPERATOR};
			byte operatorBytes[] = {Instruction.VIDEO_STOP, Instruction.COMMAND_SEPERATOR};
			byte IDBytes[] = IDString.getBytes("UTF-8");
			byte endBytes[] = {Instruction.COMMAND_SEPERATOR, Instruction.COMMAND_END};
			byte buffer[] = new byte[subtypeBytes.length+operatorBytes.length
					+IDBytes.length+endBytes.length];

			/*转换account后面所有指令*/
			int start = 0;
			System.arraycopy(subtypeBytes ,0,buffer,start, subtypeBytes.length);
			start+=subtypeBytes.length;
			System.arraycopy(operatorBytes ,0,buffer,start, operatorBytes.length);
			start+=operatorBytes.length;
			System.arraycopy(IDBytes,0,buffer,start, IDBytes.length);
			start+=IDBytes.length;
			System.arraycopy(endBytes   ,0,buffer,start, endBytes.length);

				/*发送广播给Service，让其发送信息给服务器*/
			Intent intent = new Intent();
			intent.putExtra("type", "send");
			intent.putExtra("onefield", typeBytes);
			intent.putExtra("twofield", buffer);
			intent.setAction(intent.ACTION_MAIN);
			getActivity().sendBroadcast(intent);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 *  @Description: 将FragmentVideo和ClientMainActivity的handler绑定起来，便于通信。
	 */

	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		mainActivity = (ClientMainActivity) activity;
		mainActivity.setVideoHandler(videoHandler);
	}
	/**
	 *  处理Activity传递来的信息
	 *  打开图片进行显示
	 */
	public Handler videoHandler = new Handler()
	{
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle bundle = msg.getData();
			String typeString = bundle.getString("type");
			if(typeString.equals("video"))
			{
				String mode = bundle.getString("video");
				if(mode.equals("finish"))
				{
					synchronized (Instruction.class){
						String path = "mnt/sdcard/camera.jpg";
						File file = new File(path);
						if(file.exists()) {
							BitmapFactory.Options options = new BitmapFactory.Options();
							options.inSampleSize = 2;
							Bitmap bm = BitmapFactory.decodeFile(path, options);
							cameraiImageView.setImageBitmap(bm);
							Toast.makeText(getActivity(), "file found", Toast.LENGTH_SHORT).show();
						}else {
							Toast.makeText(getActivity(), "file readme.txt not found", Toast.LENGTH_SHORT).show();
						}
					}
				}//end of finish

			}

		}
	};
	
	class ButtonListener implements OnClickListener{

		@Override
		public void onClick(View view) {
			if(selection)
			{
				videoStart();
				selection = !selection;
			}
			else
			{
				videoEnd();
				selection = !selection;
			}
		}
		
	}
	
	
}
