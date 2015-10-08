package com.huaqin.lteband.activity;

import com.huaqin.lteband.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.content.res.Resources;
import android.preference.Preference;
import android.util.Log;

import java.util.ArrayList;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.huaqin.lteband.util.BandModeContent;
import com.huaqin.lteband.util.FeatureSupport;
import com.huaqin.lteband.util.LogUtils;
import com.huaqin.lteband.util.UrcParser;
import com.huaqin.lteband.util.Utils;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;
public class Status extends PreferenceActivity{
	
	private static final String TAG ="Status";
    private static final String KEY_LTE_STATUS = "lte_status";
    private static final String KEY_SIM1_BAND ="band_support_SIM1";
    private static final String KEY_SIM2_BAND ="band_support_SIM2";
    private static final String KEY_CURRENT_BAND = "lte_band";
    private static final String KEY_SIM_STATUS = "sim_status";	
    
    private static final int MSG_NW_INFO = 1;
    private static final int MSG_NW_INFO_LTEDC = 2;
    private static final int MSG_NW_INFO_URC = 3;
    private static final int MSG_NW_INFO_OPEN = 4;
    private static final int MSG_NW_INFO_CLOSE = 5;
    private static final int MSG_UPDATE_UI = 5;
    private static final int TOTAL_TIMER = 5000;
    private static final int FLAG_OR_DATA = 0xFFFFFFF7;
    private static final int FLAG_OFFSET_BIT = 0x08;
    private static final int FLAG_DATA_BIT = 8;
    private Timer mTimer = new Timer();
    private int mFlag = 0;
    
    private static final int WCDMA = 0x04;
    private static final int TDSCDMA = 0x08;
    private static final String GSM_BASEBAND = "gsm.baseband.capability";
    private static final String PREF_FILE = "band_select_";
    private static final String PREF_KEYS[] = {"gsm", "umts", "lte_fdd", "lte_tdd", "cdma"};

    private static final int INDEX_GSM_BAND = 0;
    private static final int INDEX_UMTS_BAND = 1;
    private static final int INDEX_LTE_FDD_BAND = 2;
    private static final int INDEX_LTE_TDD_BAND = 3;
    private static final int INDEX_CDMA_BAND = 4;
    private static final int INDEX_BAND_MAX = 5;
    
    private static  String mCapabilitySimId;
    private String mCurrentMode;
    private boolean mIsGemini = true;
    private boolean mIsThisAlive = false;
    private Phone mPhone1 = null;
    private Phone mPhone2 = null;
    private Phone mPhone = null;
    private Preference  mCapabilitySim ; 
    private Preference  mSIM1BandSupport;
    private Preference  mSIM2BandSupport;
    private Preference  mCurrentBand;
    private String mSim1BandInfo = "";
    private String mSim2BandInfo = "";
    private Resources mRes;
    private final ArrayList<BandModeMap> mSim1ModeArray = new ArrayList<BandModeMap>();
    private final ArrayList<BandModeMap> mSim2ModeArray = new ArrayList<BandModeMap>();
    
    private HashMap<String, String> mNetworkInfo = new HashMap<String, String>();
    
    private static class BandModeMap {
        public String mModeName;
        public int mIndex;
        public int mBit;
        public String mModeType;
        /**
         * @param name
         *            the string from the values
         * @param index
         *            the integer of the modem value (index)
         * @param bit
         *            the integer of the modem value (bit offset)
         */
        BandModeMap(final String name, final String type,final int index, final int bit) {
            mModeName = name;
            mIndex = index;
            mBit = bit;
            mModeType = type;
        }
    }
    
    private Handler mATCmdHander = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case MSG_NW_INFO:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    String[] data = (String[]) ar.result;
                    Log.d(Utils.TAG, "data[0] is : " + data[0]);
                    Log.d(Utils.TAG, "flag is : " + data[0].substring(FLAG_DATA_BIT));
                    mFlag = Integer.valueOf(data[0].substring(FLAG_DATA_BIT));
                    mFlag = mFlag | FLAG_OFFSET_BIT;
                    Log.d(Utils.TAG, "flag change is : " + mFlag);
                    String[] atCommand = new String[2];
                    atCommand[0] = "AT+EINFO=" + mFlag + "," + "249" + ",0";
                    atCommand[1] = "+EINFO";
                    sendATCommand(atCommand, MSG_NW_INFO_OPEN);
                } else {
                    //showToast(getString(R.string.send_at_fail));
                }
                break;
            case MSG_NW_INFO_OPEN:
            case MSG_NW_INFO_CLOSE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    //showToast(getString(R.string.send_at_fail));
                }
                break;
            default:
                break;
            }
        }
    };
    
    private final Handler mUrcHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_NW_INFO_URC) {
                AsyncResult ar = (AsyncResult) msg.obj;
                String[] data = (String[]) ar.result;
                //Log.d(Utils.TAG, "Receive URC: " + data[0] + ", " + data[1]);

                int type = -1;
                try {
                    type = Integer.parseInt(data[0]);
                } catch (NumberFormatException e) {
                    //showToast("Return type error");
                    return;
                }
                if(data[1] !=null && 
                        (!data[1].equals(mCurrentMode))){
                        mNetworkInfo.put("type", data[0]);
                        mNetworkInfo.put("data", data[1]);
                        showNetworkInfo();
                        mCurrentMode = data[1];
                }
                
            }
        }
    };
    
    private final Handler mUiHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_UI) {
                showNetworkInfo();
            }
        }
    };
    
    private final Handler mResponseHander = new Handler() {

        public void handleMessage(final Message msg) {
            if (!mIsThisAlive) {
                return;
            }
            AsyncResult asyncResult;
            switch (msg.what) {
            case BandModeContent.EVENT_QUERY_SIM1_SUPPORTED:
                asyncResult = (AsyncResult) msg.obj;
                if (asyncResult.exception == null) {
                    setMode(asyncResult, BandModeContent.EVENT_QUERY_SIM1_SUPPORTED);
                } else {
                    setSupportedMode(new long[INDEX_BAND_MAX], mSim1ModeArray, BandModeContent.EVENT_QUERY_SIM1_SUPPORTED);
                }
                break;
            case BandModeContent.EVENT_QUERY_SIM2_SUPPORTED:
                asyncResult = (AsyncResult) msg.obj;
                if (asyncResult.exception == null) {
                    setMode(asyncResult, BandModeContent.EVENT_QUERY_SIM2_SUPPORTED);
                } else {
                    setSupportedMode(new long[INDEX_BAND_MAX], mSim2ModeArray, BandModeContent.EVENT_QUERY_SIM2_SUPPORTED);
                }
                break;
            case BandModeContent.EVENT_QUERY_SUPPORTED:
                asyncResult = (AsyncResult) msg.obj;
                if (asyncResult.exception == null) {
                    setMode(asyncResult, BandModeContent.EVENT_QUERY_SUPPORTED);
                } else {
                    setSupportedMode(new long[INDEX_BAND_MAX],mSim1ModeArray, BandModeContent.EVENT_QUERY_SUPPORTED);
                }
                break;
            
            default:
                break;
            }
        }
    };
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.device_info_status);
		mRes = getResources();
		mCapabilitySim = findPreference(KEY_LTE_STATUS);
		mSIM1BandSupport = findPreference(KEY_SIM1_BAND);
		mSIM2BandSupport = findPreference(KEY_SIM2_BAND);
		mCurrentBand = findPreference(KEY_CURRENT_BAND);
		mCapabilitySimId = Utils.getCapaBitilySim(mRes);
		mCapabilitySim.setSummary(mCapabilitySimId);
		initBandMode();
		configureActionBar();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (TelephonyManager.getDefault().getPhoneCount() > 1) {
                    Log.v(Utils.TAG, "Gemini");
                    mIsGemini = true;
                    mPhone1 = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
                    mPhone2 = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
                    if(mCapabilitySimId.equals(mRes.getString(R.string.radio_sim_1))){
                        mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_1);
                    }else {
                        mPhone = PhoneFactory.getPhone(PhoneConstants.SIM_ID_2);
                    }
                } else {
                    Log.v(Utils.TAG, "Single");
                    mIsGemini = false;
                    mPhone = PhoneFactory.getDefaultPhone();
                }
                mPhone.registerForNetworkInfo(mUrcHandler, MSG_NW_INFO_URC, null);
                registerNetwork();
                querySupportMode();
                //updateCurrentBandUI();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	@Override
        public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(Utils.TAG, "onCreateOptionsMenu()");
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.list_default, menu);
            return true;
        }
        
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            Log.d(Utils.TAG, "onOptionsItemSelected()");
                super.onOptionsItemSelected(item);
                Intent serviceIntent = new Intent(Status.this,com.huaqin.lteband.service.PhoneStateService.class);
                switch (item.getItemId()) {
                case R.id.btn_start_service:
                    Log.d(Utils.TAG, "btn_start_service");
                    
                    startService(serviceIntent);
                    break;
                case R.id.btn_stop_service:
                	stopService(serviceIntent);
                    break;
                default:
                    break;
                }
                return true;
        }
        
    private void configureActionBar(){
    	LogUtils.d(TAG, "configureActionBar()");
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
    }
    private static int getModemType() {
        String property = GSM_BASEBAND;
        //if (FeatureSupport.isSupported(FeatureSupport.FK_DT_SUPPORT)
        //        && simType == PhoneConstants.SIM_ID_2) {
        //   property = SystemProperties.get("gsm.baseband.capability.md2");
        //}
        String networkType = SystemProperties.get(property);
        int mode;
        if (networkType == null) {
            mode = 0;
        } else {
            try {
                final int mask = Integer.valueOf(networkType);
                if ((mask & WCDMA) != 0) {
                    mode = WCDMA;
                } else if ((mask & TDSCDMA) != 0) {
                    mode = TDSCDMA;
                } else {
                    mode = 0;
                }
            } catch (NumberFormatException e) {
                mode = 0;
            }
        }
        return mode;
    }	
    
    private void initBandMode(){
    	String gsm ="2G";
    	String hspa ="3G";
        int modemType = getModemType();
        if(mCapabilitySimId.equals(mRes.getString(R.string.radio_sim_1))){
                mSim1ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_EGSM900), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_EGSM900_BIT)); 
                mSim1ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_DCS1800), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_DCS1800_BIT));
                mSim1ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_PCS1900), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_PCS1900_BIT));
                mSim1ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_GSM850), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_GSM850_BIT));
            if(modemType ==TDSCDMA){
                mSim1ModeArray.add(new BandModeMap(
                             mRes.getString(R.string.TDD_UMTS_BAND_I), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_I_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_II), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_II_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_III), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_III_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_IV), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_IV_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_V), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_V_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_VI), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_VI_BIT));
                if (FeatureSupport.isSupported(FeatureSupport.FK_LTE_SUPPORT)) {
                    initLteArray(mSim1ModeArray);
                }
            }else if(modemType == WCDMA){
                   mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_I), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_I_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_II), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_II_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_III), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_III_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_IV), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_IV_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_V), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_V_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_VI), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_VI_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_VII), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_VII_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_VIII), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_VIII_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_IX), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_IX_BIT));
                    mSim1ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_X), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_X_BIT));
                   if (FeatureSupport.isSupported(FeatureSupport.FK_LTE_SUPPORT)) {
                        initLteArray(mSim1ModeArray);
                    }
            }
            mSim2ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_EGSM900), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_EGSM900_BIT)); 
                mSim2ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_DCS1800), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_DCS1800_BIT));
                mSim2ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_PCS1900), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_PCS1900_BIT));
                mSim2ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_GSM850), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_GSM850_BIT));
	}else if(mCapabilitySimId.equals(mRes.getString(R.string.radio_sim_2))){
            mSim2ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_EGSM900), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_EGSM900_BIT)); 
                mSim2ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_DCS1800), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_DCS1800_BIT));
                mSim2ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_PCS1900), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_PCS1900_BIT));
                mSim2ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_GSM850), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_GSM850_BIT));
            if(modemType ==TDSCDMA){
                mSim2ModeArray.add(new BandModeMap(
                             mRes.getString(R.string.TDD_UMTS_BAND_I), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_I_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_II), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_II_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_III), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_III_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_IV), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_IV_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_V), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_V_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.TDD_UMTS_BAND_VI), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_VI_BIT));
                if (FeatureSupport.isSupported(FeatureSupport.FK_LTE_SUPPORT)) {
                    initLteArray(mSim2ModeArray);
                }
            }else if(modemType == WCDMA){
                   mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_I), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_I_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_II), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_II_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_III), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_III_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_IV), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_IV_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_V), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_V_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_VI), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_VI_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_VII), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_VII_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_VIII), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_VIII_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_IX), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_IX_BIT));
                    mSim2ModeArray.add(new BandModeMap(
                            mRes.getString(R.string.BandSel_UMTS_BAND_X), hspa,
                            INDEX_UMTS_BAND, BandModeContent.UMTS_BAND_X_BIT));
                   if (FeatureSupport.isSupported(FeatureSupport.FK_LTE_SUPPORT)) {
                        initLteArray(mSim2ModeArray);
                    }
            }
            mSim1ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_EGSM900), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_EGSM900_BIT)); 
                mSim1ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_DCS1800), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_DCS1800_BIT));
                mSim1ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_PCS1900), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_PCS1900_BIT));
                mSim1ModeArray.add(new BandModeMap(
                     mRes.getString(R.string.BandSel_GSM_GSM850), gsm,
                     INDEX_GSM_BAND, BandModeContent.GSM_GSM850_BIT));
	}
        mIsThisAlive = true ;
    }
    
    private void initLteArray(ArrayList<BandModeMap> mModeArray){
    	String lte ="4G";
        String[] labels = mRes.getStringArray(R.array.band_mode_lte_fdd);
        for (int i = 0; i < labels.length; i++){
            mModeArray.add(new BandModeMap(labels[i], lte, INDEX_LTE_FDD_BAND, i));
        }
        labels = mRes.getStringArray(R.array.band_mode_lte_tdd);
        for (int i = 0; i < labels.length; i++){
            mModeArray.add(new BandModeMap(labels[i], lte, INDEX_LTE_TDD_BAND, i));
        }
    }
    
    /**
     * Query Modem supported band modes.
     */
    private void querySupportMode() {
        final String[] modeString = {BandModeContent.QUERY_SUPPORT_COMMAND,
                BandModeContent.SAME_COMMAND};
        Log.v(Utils.TAG, "AT String:" + modeString[0]);
        if(mIsGemini){
            sendATCommand(modeString, BandModeContent.EVENT_QUERY_SIM1_SUPPORTED, mPhone1);
            sendATCommand(modeString, BandModeContent.EVENT_QUERY_SIM2_SUPPORTED, mPhone2);
        }else{
            sendATCommand(modeString, BandModeContent.EVENT_QUERY_SIM1_SUPPORTED, mPhone);
        }
        
    }
    
    /**
     * Query Modem is being used band modes.
     */
    private void sendATCommand(String[] atCommand, int msg, Phone mPhone) {
        mPhone.invokeOemRilRequestStrings(atCommand, mResponseHander.obtainMessage(msg));
    }
    
    private void setMode(AsyncResult aSyncResult, int msg) {
        final String[] result = (String[]) aSyncResult.result;

        for (final String value : result) {
            Log.v(Utils.TAG, "--.>" + value);
            final String splitString = value.substring(BandModeContent.SAME_COMMAND
                    .length());
            final String[] getDigitalVal = splitString.split(",");

            if (getDigitalVal != null && getDigitalVal.length > 1) {
                long[] values = new long[INDEX_BAND_MAX];
                for (int i = 0; i < values.length; i++) {
                    if (getDigitalVal.length <= i || getDigitalVal[i] == null) {
                        values[i] = 0;
                        continue;
                    }
                    try {
                        values[i] = Long.valueOf(getDigitalVal[i].trim());
                    } catch (NumberFormatException e) {
                        values[i] = 0;
                    }
                }
                if (msg == BandModeContent.EVENT_QUERY_SUPPORTED
                        || msg == BandModeContent.EVENT_QUERY_SIM1_SUPPORTED) {
                    setSupportedMode(values,mSim1ModeArray,msg);
                } else if(msg == BandModeContent.EVENT_QUERY_SIM2_SUPPORTED) {
                    setSupportedMode(values,mSim2ModeArray,msg);
                    //setCurrentMode(values);
                    //saveDefaultValueIfNeed(values);
                }
            }
        }
    }
    
    /**
     * @param values
     *            the integer values from the modem
     */
    private void setSupportedMode(final long[] values , ArrayList<BandModeMap> mModeArray, int msg) {
    	String gsmString = "2G: ";
    	String hspaString = "3G: ";
    	String lteString = "4G: ";
    	String mBandSupport ="";
    	for (final BandModeMap m : mModeArray) {
        	boolean flag =true;
            if ((values[m.mIndex] & (1L << m.mBit)) == 0) {
               // m.mChkBox.setEnabled(false);
            } else {
                if(msg == BandModeContent.EVENT_QUERY_SUPPORTED
                        || msg == BandModeContent.EVENT_QUERY_SIM1_SUPPORTED){
                	Log.d(Utils.TAG, "sim1: "+m.mModeName+" type:"+m.mModeType);
                	if(m.mModeType.equals("2G")){
                		gsmString+= m.mModeName+", ";
                	}else if(m.mModeType.equals("3G")){
                		hspaString += m.mModeName+", ";
                	}else if(m.mModeType.equals("4G")){
                		lteString += m.mModeName+", ";
                	}
                     mSim1BandInfo += m.mModeName+", ";
                }else if(msg == BandModeContent.EVENT_QUERY_SIM2_SUPPORTED){
                	Log.d(Utils.TAG, "sim2: "+m.mModeName+" type:"+m.mModeType);
                	if(m.mModeType.equals("2G")){
                		gsmString+= m.mModeName+", ";
                	}else if(m.mModeType.equals("3G")){
                		hspaString += m.mModeName+", ";
                	}else if(m.mModeType.equals("4G")){
                		lteString += m.mModeName+", ";
                	}
                	
                    mSim2BandInfo += m.mModeName+", ";
                }

            }
        }
    	
    	if(gsmString.length()>4){
    		mBandSupport += gsmString;
    	}
    	
    	if(hspaString.length()>4){
    		mBandSupport +="\n";
    		mBandSupport +=hspaString;
    	}
    	
    	if(lteString.length()>4){
    		mBandSupport +="\n";
    		mBandSupport +=lteString;
    	}

        Log.d(Utils.TAG, "mBandSupport: "+gsmString+"\n"+hspaString+"\n"+lteString+"\n");
        
       if(msg == BandModeContent.EVENT_QUERY_SUPPORTED
                        || msg == BandModeContent.EVENT_QUERY_SIM1_SUPPORTED){
           mSIM1BandSupport.setSummary(mBandSupport);
           mBandSupport = "";
        }else if(msg == BandModeContent.EVENT_QUERY_SIM2_SUPPORTED){
           mSIM2BandSupport.setSummary(mBandSupport);
           mBandSupport = "";
        }

    }
    
    public void updateUI() {
        showNetworkInfo();
        mTimer.cancel();
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mUiHandler.sendEmptyMessage(MSG_UPDATE_UI);
            }
        }, TOTAL_TIMER, TOTAL_TIMER);
    }
    
    private void showNetworkInfo() {
        //Log.d(Utils.TAG, "showNetworkInfo()");
        String text = "";
        String raw = mNetworkInfo.get("data");
        int type = Integer.parseInt(mNetworkInfo.get("type"));
        String info = new String(UrcParser.parse(11, type,
                raw == null ? null : raw.toCharArray()));
        text += info;
	//Log.v(Utils.TAG, "info = "+text);
        mCurrentBand.setSummary(text);
    }
    
    private void registerNetwork(){
        String[] atCommand = {"AT+EINFO?", "+EINFO"};
        sendATCommand(atCommand, MSG_NW_INFO);
    }
    
    private void sendATCommand(String[] atCommand, int msg) {
        Log.d(Utils.TAG, "sendATCommand :" + atCommand[0]);
        mPhone.invokeOemRilRequestStrings(atCommand, mATCmdHander.obtainMessage(msg));
    }
}
