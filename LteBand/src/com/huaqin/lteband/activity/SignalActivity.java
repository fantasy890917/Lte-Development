package com.huaqin.lteband.activity;

import com.huaqin.lteband.adapter.BandListAdapter;
import com.huaqin.lteband.util.LogUtils;
import com.huaqin.lteband.util.Utils;
import com.huaqin.lteband.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class SignalActivity extends Activity{
	
	private static final String TAG = "SignalActivity";
	
	/** Read all Todo infos from QB */
    private BandListAdapter mBandListAdapter = null;
    /** Show all Todo infos in ListView */
    private ListView mBandListView = null;
    /** Item click & long click listener */
    private AdapterViewListener mAdapterViewListener = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.items);
		initViews();
        //configureActionBar();
        LogUtils.d(TAG, "SignalActivity.onCreate() finished.");
	}
	
	private void initViews() {
		LogUtils.d(TAG, "initViews()");
		mBandListAdapter = new BandListAdapter(this);
		 mAdapterViewListener = new AdapterViewListener();
		mBandListView = (ListView) findViewById(R.id.list_band);
		mBandListView.setAdapter(mBandListAdapter);
		mBandListView.setOnItemClickListener(mAdapterViewListener);
    }
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		Log.d(Utils.TAG, "onCreateOptionsMenu()");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_clear, menu);
        return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		Log.d(Utils.TAG, "onOptionsItemSelected()");
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case R.id.btn_clear_data:
            mBandListAdapter.deleteAll();
            break;
        case R.id.btn_refresh:
        	refresh();
            break;
        default:
            break;
        }
        return true;
	}
	
	class AdapterViewListener implements AdapterView.OnItemClickListener,
	    AdapterView.OnItemLongClickListener {
		public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {
		    int viewType = mBandListAdapter.getItemViewType(position);
		    LogUtils.d(TAG, "onItemClick viewType =" + viewType + " position=" + position);
		
		    boolean selectDoneItem = false;
		    switch (viewType) {
		    case BandListAdapter.TYPE_BAND_HEADER:
		        mBandListAdapter.setBandExpand(!mBandListAdapter.isBandExpand());
		        break;
		    default:
		        break;
		    }
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			// TODO Auto-generated method stub
			return false;
		}
	}

	private void refresh(){
		onCreate(null);
	}

}
