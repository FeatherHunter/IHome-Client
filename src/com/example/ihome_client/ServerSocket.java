package com.example.ihome_client;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;

import android.R.integer;

public class ServerSocket extends Socket implements Serializable{

	public ServerSocket(String dstName, int dstPort) throws UnknownHostException, IOException
	{
		super(dstName, dstPort);
	}
}
