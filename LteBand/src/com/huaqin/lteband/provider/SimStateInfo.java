/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.huaqin.lteband.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.Time;

import com.huaqin.lteband.provider.BandDatabaseHelper.BandColumn;
import com.huaqin.lteband.util.DBUtils;
import com.huaqin.lteband.util.LogUtils;

import java.io.Serializable;

/**
 * Like a record in DB.
 */
public class SimStateInfo implements Serializable {
    private static final String TAG = "SimStateInfo";

    private String mId = null;
    private String mSlotId = null;
    private String mServiceState = null;
    private String mCreateTime = null;


    public SimStateInfo() {
    }

    /**
     * extract cursor content, to fill a new TodoInfo object, then return it.
     * 
     * @param cursor
     *            an valid cursor. if not valid, empty TodoInfo will be created
     * @return TodoInfo
     */
    public static SimStateInfo makeTodoInfoFromCursor(Cursor cursor) {
    LogUtils.d(TAG, "makeTodoInfoFromCursor().");
        String id;
        String slotId;
        String serviceState;
        String createTime;

        SimStateInfo simInfo = new SimStateInfo();

        id = DBUtils.getColumnByName(BandColumn.ID, cursor);
        slotId = DBUtils.getColumnByName(BandColumn.SLOT_ID, cursor);
        serviceState = DBUtils.getColumnByName(BandColumn.SERVICE_STATE, cursor);
        createTime = DBUtils.getColumnByName(BandColumn.CREATE_TIME, cursor);
        
        simInfo.mId = id;
        simInfo.mSlotId = slotId ;
        simInfo.mServiceState = serviceState;
        simInfo.mCreateTime = createTime;
		LogUtils.d(TAG, "simInfo=="+simInfo);
        return simInfo;
    }

    
    

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public ContentValues makeContentValues() {
        ContentValues values = new ContentValues();
        values.put(BandColumn.ID, mId);
        values.put(BandColumn.SLOT_ID, mSlotId);
        values.put(BandColumn.SERVICE_STATE, mServiceState);
        values.put(BandColumn.CREATE_TIME, mCreateTime);
        return values;
    }

 

    public String getId() {
		return mId;
	}

	public void setId(String mId) {
		this.mId = mId;
	}

	public String getSlotId() {
		return mSlotId;
	}

	public void setSlotId(String mSlotId) {
		this.mSlotId = mSlotId;
	}

	public String getServiceState() {
		return mServiceState;
	}

	public void setServiceState(String mServiceState) {
		this.mServiceState = mServiceState;
	}

	public String getCreateTime() {
		return mCreateTime;
	}

	public void setCreateTime(String mCreateTime) {
		this.mCreateTime = mCreateTime;
	}

	public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("------ TodoInfo--------\n");
        sb.append("id = ").append(getId()).append(" status=").append(getSlotId()).append("\n");
        sb.append("title = ").append(getServiceState());
        sb.append("dueDate = ").append(getCreateTime());
        return sb.toString();
    }

}
