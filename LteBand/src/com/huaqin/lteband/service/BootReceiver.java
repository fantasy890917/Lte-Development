package com.huaqin.lteband.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.huaqin.lteband.util.*;

import android.util.Log;
public class BootReceiver extends BroadcastReceiver{
        public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
        public static final String ACTION_SUBINFO_RECORD_UPDATED = "android.intent.action.ACTION_SUBINFO_RECORD_UPDATED";
        
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		Log.d(Utils.TAG,"action = "+action);
		if(action.equals(ACTION_BOOT_COMPLETED)){
		        Intent serviceIntent = new Intent(context,com.huaqin.lteband.service.PhoneStateService.class);
		        context.startService(serviceIntent);
		}else if(action.equals(ACTION_SUBINFO_RECORD_UPDATED)){
		        Log.d(Utils.TAG,"ACTION_SUBINFO_RECORD_UPDATED");
		}
		
	}

}
