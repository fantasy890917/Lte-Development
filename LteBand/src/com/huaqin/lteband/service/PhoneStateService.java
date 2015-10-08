package com.huaqin.lteband.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.ServiceState;

import com.huaqin.lteband.provider.QueryListener;
import com.huaqin.lteband.provider.SimStateInfo;
import com.huaqin.lteband.provider.TodoAsyncQuery;
import com.huaqin.lteband.util.*;

import android.content.Context;
import android.database.Cursor;
import android.telephony.gsm.GsmCellLocation;

public class PhoneStateService extends Service implements QueryListener{
        public static final String TAG = Utils.TAG;
        
        private PhoneStateListener[] mPhoneServiceStateListener;
        private int mSlotCount = -1;
        private TelephonyManager mTelephonyManager;
        private String mCellId = "";
        private String mLacId = "";
        
        private String mCurrentSim1State = null;
        private String mCurrentSim2State = null;
        
        private TodoAsyncQuery mAsyncQuery = null;
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.d(TAG, "service onCreate()");
		// set listen for SIM INFO
		mAsyncQuery = TodoAsyncQuery.getInstatnce(getApplicationContext());
		mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                mSlotCount = getSlotCount();
                mPhoneServiceStateListener = new PhoneStateListener[mSlotCount];
                registerPhoneStateListener();
	}
	
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
                
                return Service.START_STICKY;
        }   

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static int getSlotCount() {
                //FIXME: the slot count may not always be equal to phone count
                return TelephonyManager.getDefault().getPhoneCount();
        }
        
        private void registerPhoneStateListener() {
                for (int i = 0 ; i < mSlotCount ; i++) {
                    final int subId = getFirstSubInSlot(i);
                    Log.d(TAG, "subId: " + subId);
                    if (subId >= 0) {
                        mPhoneServiceStateListener[i] = getPhoneStateListener(subId, i);
                        mTelephonyManager.listen(mPhoneServiceStateListener[i], PhoneStateListener.LISTEN_SERVICE_STATE);
                    } else {
                        mPhoneServiceStateListener[i] = null;
                    }
                }
        }
        
        public static int getFirstSubInSlot(int slotId) {
                int[] subIds = SubscriptionManager.getSubId(slotId);
                if (subIds != null && subIds.length > 0) {
                    return subIds[0];
                }
                Log.d(TAG, "Cannot get first sub in slot: " + slotId);
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        private String getTargetString(String target) {
		if (target == null || target.equals("")) {
			return "null";
		}
		return target;

	}
        private PhoneStateListener getPhoneStateListener(final int subId, final int slotId) {
                return new PhoneStateListener(subId) {
                    @Override
                    public void onDataConnectionStateChanged(int state) {
                        //updateNetworkType();
                        Log.d(TAG,"onDataConnectionStateChanged");
                    }
                    
                    @Override
                    public void onServiceStateChanged(ServiceState state) {
                        String networkType = getNetworkType(subId);
                        android.util.Log.d(TAG, "PhoneStateListener:onServiceStateChanged, slot " + slotId + " subId "+subId);
                        SimStateInfo simInfo = new SimStateInfo();
                        simInfo.setSlotId("SlotId: "+String.valueOf(slotId+1));
                        simInfo.setCreateTime(String.valueOf(System.currentTimeMillis()));
                        if (slotId == 0){
                        	Log.d(TAG,"1 mCurrentSim1State = "+mCurrentSim1State+" networkType ="+networkType);
                           if(!networkType.equals(mCurrentSim1State)){
                        	   mCurrentSim1State = networkType;
                        	   simInfo.setServiceState(networkType);
                        	   startInsert(simInfo);
                           }
                            if (state.getState() == ServiceState.STATE_IN_SERVICE){
                                Log.d(TAG,"onServiceStateChanged");
                                GsmCellLocation cellLocation = (GsmCellLocation) mTelephonyManager.getCellLocationBySubId(subId);
		                if (cellLocation != null) {
			                mCellId = Integer.toString(cellLocation.getCid());
			                mLacId = Integer.toString(cellLocation.getLac());
			                Log.d(TAG,"sim1 mLacId = "+mLacId+" mCellId = "+mCellId);
		                }
                            }else{
                                Log.d(TAG,"onServiceStateChanged 0");
                            }
                        }
                        if (slotId == 1){
                        	Log.d(TAG,"2 mCurrentSim1State = "+mCurrentSim1State+" networkType ="+networkType);
                        	if(!networkType.equals(mCurrentSim2State)){
                        		mCurrentSim2State = networkType;
                         	   simInfo.setServiceState(networkType);
                         	   startInsert(simInfo);
                            }
                            if (state.getState() == ServiceState.STATE_IN_SERVICE){
                                Log.d(TAG,"onServiceStateChanged 1");
                                GsmCellLocation cellLocation = (GsmCellLocation) mTelephonyManager.getCellLocationBySubId(subId);
		                if (cellLocation != null) {
			                mCellId = Integer.toString(cellLocation.getCid());
			                mLacId = Integer.toString(cellLocation.getLac());
			                Log.d(TAG,"sim2 mLacId = "+mLacId+" mCellId = "+mCellId);
		                }
                            }else{
                                Log.d(TAG,"onServiceStateChanged 11");
                            }
                        }
                    }
                };
        }
        
        private String getNetworkType(int subId) {
            // Whether EDGE, UMTS, etc...
            String networktype = "UNKNOWN";
            final int actualDataNetworkType = mTelephonyManager.getDataNetworkType(subId);
            final int actualVoiceNetworkType = mTelephonyManager.getVoiceNetworkType(subId);
            Log.d(TAG,"actualDataNetworkType = " + actualDataNetworkType +
                      "actualVoiceNetworkType = " + actualVoiceNetworkType);
            if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualDataNetworkType) {
                networktype = mTelephonyManager.getNetworkTypeName(actualDataNetworkType);
            } else if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualVoiceNetworkType) {
                networktype = mTelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
            }
            return networktype;
        }
        
		@Override
		public void startQuery(String selection) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onQueryComplete(int token, Cursor cur) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startDelete(SimStateInfo info) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDeleteComplete(int token, int result) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startUpdate(SimStateInfo info) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onUpdateComplete(int token, int result) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void startInsert(SimStateInfo info) {
			// TODO Auto-generated method stub
			LogUtils.d(TAG, "startInsert().");
	        DBUtils.writeAdapterDataToDB(this, info, mAsyncQuery, DBUtils.OPERATOR_INSERT);
		}

		@Override
		public void onInsertComplete(int token, Uri uri) {
			// TODO Auto-generated method stub
			
		}
}
