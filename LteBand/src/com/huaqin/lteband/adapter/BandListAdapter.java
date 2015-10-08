package com.huaqin.lteband.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.huaqin.lteband.provider.QueryListener;
import com.huaqin.lteband.provider.SimStateInfo;
import com.huaqin.lteband.provider.TodoAsyncQuery;
import com.huaqin.lteband.util.DBUtils;
import com.huaqin.lteband.util.LogUtils;
import com.huaqin.lteband.util.Utils;
import com.huaqin.lteband.R;
public class BandListAdapter extends BaseAdapter implements QueryListener{
	private static final String TAG = "BandListAdapter";
	
	public static final int TYPE_BAND_HEADER = 0;
    public static final int TYPE_BAND_ITEM = TYPE_BAND_HEADER + 1;
    public static final int TYPE_BAND_FOOTER = TYPE_BAND_ITEM + 1;
    
    private Comparator<SimStateInfo> mComparator = new Comparator<SimStateInfo>() {
        public int compare(SimStateInfo info1, SimStateInfo info2) {
            final long dueDate1 = Long.parseLong(info1.getCreateTime());
            final long dueDate2 = Long.parseLong(info2.getCreateTime());;
            int result = 0;
            if (dueDate1 == dueDate2) {
                result = 0;
            } else if (dueDate2 == 0 || (dueDate1 != 0 && dueDate2 > dueDate1)) {
                result = -1;
            } else {
                result = 1;
            }
            return result;
        }
    };
    
 // the data display in list.
    private ArrayList<SimStateInfo> mBandDataSource = new ArrayList<SimStateInfo>();

    private Context mContext = null;
    private LayoutInflater mInflater = null;
    private TodoAsyncQuery mAsyncQuery = null;

    private View mBandHeaderView = null;
    private View mBandFooterView = null;

    private boolean mBandExpand = true;

    // assume the Adapter should loading items form DB first.
    private boolean mIsLoadingData = false;
    // True means that mTodosDataSource | mDonesDataSource needs to update data from DB.
    private boolean mIsDataDirty = false;
    
    public BandListAdapter(Context context) {
        mContext = context;
        mAsyncQuery = TodoAsyncQuery.getInstatnce(context.getApplicationContext());
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // start query form DB.
        startQuery(null);
    }
    
    static class HeaderHolder {
        TextView mHeaderTitle = null;
        ImageView mBtnExpand = null;
    }

    static class FooterHolder {
        TextView mFooterHelper = null;
    }

    static class ViewHolder {
        TextView mStateInfoText = null;
        TextView mSlotId = null;
        TextView mCreateTime = null;
    }
    
    public void setBandExpand(boolean expand) {
		LogUtils.d(TAG, "setTodosExpand()");
        if (mBandExpand != expand) {
        	mBandExpand = expand;
            notifyDataSetChanged();
        }
    }
    
    public boolean isBandExpand() {
        return mBandExpand;
    }

    public ArrayList<SimStateInfo> getTodosDataSource() {
        return mBandDataSource;
    }

    public ArrayList<SimStateInfo> getDonesDataSource() {
        return mBandDataSource;
    }
	@Override
	public void startQuery(String selection) {
		// TODO Auto-generated method stub
		LogUtils.d(TAG, "startQuery query from DB. selection : " + selection);
        mIsLoadingData = true;
        mAsyncQuery.startQuery(0, this, TodoAsyncQuery.TODO_URI, null, selection, null, null);
	}

	@Override
	public void onQueryComplete(int token, Cursor cursor) {
		// TODO Auto-generated method stub
		LogUtils.d(TAG, "onQueryComplete.");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                SimStateInfo simInfo = SimStateInfo.makeTodoInfoFromCursor(cursor);
                mBandDataSource.add(simInfo);
            } while (cursor.moveToNext());
        }
        /**M: close cursor*/
        if(cursor != null){
            cursor.close();
        }

        if (mBandDataSource.size() > 1) {
            Collections.sort(mBandDataSource, mComparator);
        }
        //updateHeaderNumberText();
        notifyDataSetChanged();
        mIsLoadingData = false;
	}

	@Override
	public void startDelete(SimStateInfo info) {
		// TODO Auto-generated method stub
		LogUtils.d(TAG, "startDelete(). info = " + info);
        DBUtils.writeAdapterDataToDB(this, info, mAsyncQuery, DBUtils.OPERATOR_DELETE);
	}

	@Override
	public void onDeleteComplete(int token, int result) {
		// TODO Auto-generated method stub
		if (result <= 0) {
            LogUtils.e(TAG, "onDeleteComplete() failed : " + result);
            DBUtils.prompt(mContext, R.string.operator_failed);
            mIsDataDirty = true;
        }
	}

	@Override
	public void startUpdate(SimStateInfo info) {
		// TODO Auto-generated method stub
		LogUtils.d(TAG, "startUpdate() : ");
        DBUtils.writeAdapterDataToDB(this, info, mAsyncQuery, DBUtils.OPERATOR_UPDATE);
	}

	@Override
	public void onUpdateComplete(int token, int result) {
		// TODO Auto-generated method stub
		if (result <= 0) {
            LogUtils.e(TAG, "onUpdateComplete() result : " + result);
            DBUtils.prompt(mContext, R.string.operator_failed);
            mIsDataDirty = true;
        }
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
		LogUtils.d(TAG, "onInsertComplete() uri : " + uri);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		int count = 1; // Todos' Header
        if (mBandDataSource.isEmpty()) {
            count++;
        } else {
            count += (mBandExpand ? mBandDataSource.size() : 0);
        }
        
		LogUtils.d(TAG, "getCount().--count="+count);
        return count;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		LogUtils.d(TAG, "getItem().--position="+position);
        if (mBandExpand) {
            int index = position - 1;
            if (index < 0) {
                return null;
            } else if (index < mBandDataSource.size()) {
                return mBandDataSource.get(index);
            }
        }
        return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		LogUtils.d(TAG, "getItemId().--position="+position);
        return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		LogUtils.d(TAG, "getView()--position="+position);
        int viewType = getItemViewType(position);
        switch (viewType) {
        case TYPE_BAND_HEADER:
            updateBandHeaderView();
            return mBandHeaderView;
        case TYPE_BAND_FOOTER:
            updateBandFooterView();
            return mBandFooterView;

        case TYPE_BAND_ITEM:
        default:
            return getItemView(position, convertView, parent);
        }
	}
	
	@Override
    public int getItemViewType(int position) {
    	
        int viewType = -1;
        final int mBandDataSize = mBandDataSource.size();
        final int mBandShowSize = mBandExpand ? mBandDataSource.size() : 0;
        if (position == 0) {
            viewType = TYPE_BAND_HEADER;
        } else if (position == 1 && mBandDataSize == 0) {
            viewType = TYPE_BAND_FOOTER;
        } else if (position <= mBandShowSize) {
            viewType = TYPE_BAND_ITEM;
        }
		LogUtils.d(TAG, "getItemViewType()---viewType ="+viewType);
        return viewType;
    }
	
	private void updateBandHeaderView() {
		LogUtils.d(TAG, "updateBandHeaderView()");
        StringBuffer todosTitle = new StringBuffer();
        todosTitle.append(mContext.getResources().getString(R.string.band_title));
        todosTitle.append(" (").append(mBandDataSource.size()).append(")");
        HeaderHolder bandHolder = null;
        if (mBandHeaderView == null) {
        	bandHolder = new HeaderHolder();
        	mBandHeaderView = mInflater.inflate(R.layout.list_header, null);
        	bandHolder.mHeaderTitle = (TextView) mBandHeaderView.findViewById(R.id.list_title);
        	bandHolder.mBtnExpand = (ImageView) mBandHeaderView.findViewById(R.id.btn_expand);
        	mBandHeaderView.setTag(bandHolder);
        } else {
        	bandHolder = (HeaderHolder) mBandHeaderView.getTag();
        }
        bandHolder.mHeaderTitle.setText(todosTitle.toString());
        if (mBandExpand) {
        	bandHolder.mBtnExpand.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_expand_open));
        } else {
        	bandHolder.mBtnExpand.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_expand_close));
        }

        // no items in todos list, set the button transparent and un-clickable
        if (mBandDataSource.isEmpty()) {
        	bandHolder.mBtnExpand.setAlpha(0f);
        	bandHolder.mBtnExpand.setClickable(true);
        } else {
        	bandHolder.mBtnExpand.setAlpha(1f);
        	bandHolder.mBtnExpand.setClickable(false);
        }

        //int detaultColor = mContext.getResources().getColor(R.color.ListHeaderBgColor);
        //Utils.updateViewBackgroud(mContext, mTodosHeaderView, detaultColor);
    }

    private void updateBandFooterView() {
		LogUtils.d(TAG, "updateBandFooterView()");
        String todosInfo = mContext.getResources().getString(R.string.todos_empty_info);
        FooterHolder bandFooterHolder = null;
        if (mBandFooterView == null) {
        	bandFooterHolder = new FooterHolder();
        	mBandFooterView = mInflater.inflate(R.layout.list_footer, null);
        	bandFooterHolder.mFooterHelper = (TextView) mBandFooterView
                    .findViewById(R.id.footer_info);
        	mBandFooterView.setTag(bandFooterHolder);
        } else {
        	bandFooterHolder = (FooterHolder) mBandFooterView.getTag();
        }
        bandFooterHolder.mFooterHelper.setText(todosInfo);
        updateFooterViewState(mBandFooterView);
    }
    
    public void updateFooterViewState(View footerView) {
		LogUtils.d(TAG, "updateFooterViewState()");
        footerView.setEnabled(true);
        //footerView.setAlpha(ALPHA_ENABLE);
    }
    
    private View getItemView(int position, View convertView, ViewGroup parent) {
		LogUtils.d(TAG, "getItemView()");
        ViewHolder holder = null;
        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            convertView = mInflater.inflate(R.layout.list_item, parent, false);
            holder = new ViewHolder();
            holder.mStateInfoText = (TextView) convertView.findViewById(R.id.item_state_info);
            holder.mCreateTime = (TextView) convertView.findViewById(R.id.item_create_time);
            holder.mSlotId = (TextView) convertView.findViewById(R.id.item_slot_id);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SimStateInfo info = null;
        String slotId =null;
        String createTime = null;
        String serviceState = null;
        if (mBandExpand) {
            int index = position - 1;
            if (index >= 0 && index < mBandDataSource.size()) { // Todo item
                info = mBandDataSource.get(index);
                createTime = DBUtils.getDateText(mContext, Long.parseLong(info.getCreateTime()));
                slotId = info.getSlotId();
                serviceState = info.getServiceState();
            }
        }

        holder.mStateInfoText.setText(serviceState);
        holder.mCreateTime.setText(createTime);
        holder.mSlotId.setText(slotId);

        return convertView;
    }
    
    public void deleteAll(){
    	if(mBandDataSource.size()> 0){
    		mBandDataSource.clear();
        	mAsyncQuery.startDelete(0, this, TodoAsyncQuery.TODO_URI, null, null);
        	notifyDataSetChanged();
    	}
    	
    }
    

}
