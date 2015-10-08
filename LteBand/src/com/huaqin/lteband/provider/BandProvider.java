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

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.text.TextUtils;

import com.huaqin.lteband.provider.BandDatabaseHelper.BandColumn;
import com.huaqin.lteband.provider.BandDatabaseHelper.Tables;
import com.huaqin.lteband.util.LogUtils;
import com.huaqin.lteband.util.Utils;

import java.util.HashMap;

public class BandProvider extends ContentProvider {
	private static final String TAG = "BandProvider";
    protected static final String AUTHORITY = "com.huaqin.lteband";
    private BandDatabaseHelper mDbHelper;
    protected SQLiteDatabase mDb;
    private Context mContext;
    private ContentResolver mContentResolver;
    
    private static final int BAND = 33;
    private static final int BAND_ID = 34;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    private static final HashMap<String, String> TODO_PROJECTTION_MAP;
    private static final HashMap<String, String> COUNT_PROJECTION_MAP;
    static {
        URI_MATCHER.addURI(AUTHORITY, "band", BAND);
        URI_MATCHER.addURI(AUTHORITY, "band/#", BAND_ID);
        TODO_PROJECTTION_MAP = new HashMap<String, String>();
        TODO_PROJECTTION_MAP.put(BandColumn.ID, BandColumn.ID);
        TODO_PROJECTTION_MAP.put(BandColumn.SLOT_ID, BandColumn.SLOT_ID);
        TODO_PROJECTTION_MAP.put(BandColumn.SERVICE_STATE, BandColumn.SERVICE_STATE);
        TODO_PROJECTTION_MAP.put(BandColumn.CREATE_TIME, BandColumn.CREATE_TIME);
        
        /** Contains just BaseColumns._COUNT */
        COUNT_PROJECTION_MAP = new HashMap<String, String>();
        COUNT_PROJECTION_MAP.put(BaseColumns._COUNT, "COUNT(*)");
    }
    
    private static final String GENERIC_ID = "_id";
    protected static final String SQL_WHERE_ID = GENERIC_ID + "=?";

    @Override
    public boolean onCreate() {
    LogUtils.d(TAG, "onCreate()");
        return initialize();
    }

    private boolean initialize() {
		LogUtils.d(TAG, "initialize()");
        mContext = getContext();
        mContentResolver = mContext.getContentResolver();

        mDbHelper = (BandDatabaseHelper)getDatabaseHelper(mContext);
        mDb = mDbHelper.getWritableDatabase();

        return true;
    }
    
    protected BandDatabaseHelper getDatabaseHelper(final Context context) {
		LogUtils.d(TAG, "getDatabaseHelper()");
        return BandDatabaseHelper.getInstance(context);
    }

    @Override
    public String getType(Uri uri) {
    LogUtils.d(TAG, "getType()");
        int match = URI_MATCHER.match(uri);
        switch (match) {
            case BAND:
                return "vnd.android.cursor.dir/band";
            case BAND_ID:
                return "vnd.android.cursor.item/band";
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
    LogUtils.d(TAG, "insert() uri =" +uri.toString());
        final int match = URI_MATCHER.match(uri);
        long id = 0;
        switch (match) {
            case BAND:
                id = mDbHelper.todosInsert(values);
                break;
            case BAND_ID:
                throw new UnsupportedOperationException("Cannot insert into that URL: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
        
        if (id < 0) {
            return null;
        }

        return ContentUris.withAppendedId(uri, id);
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
    LogUtils.d(TAG, "delete()");
        final int match = URI_MATCHER.match(uri);
        switch (match) {
            case BAND:
                //TODO:check the result.
                return mDb.delete(Tables.BAND, selection, selectionArgs);
            case BAND_ID:
                long id = ContentUris.parseId(uri);
                return mDb.delete(Tables.BAND, SQL_WHERE_ID, new String[] {String.valueOf(id)});
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
            LogUtils.d(TAG, "query()--Uri ="+uri+" projecttion ="+projection
				+" selection= "+selection+"selectionArgs ="+selectionArgs
				+" sortOrder ="+sortOrder);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String groupBy = null;
        String limit = null; // Not currently implemented
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
    
        final int match = URI_MATCHER.match(uri);
        switch (match) {
            case BAND:
                //TODO:check the result.
                qb.setTables(Tables.BAND);
                qb.setProjectionMap(TODO_PROJECTTION_MAP);
                selection = appendAccountFromParameterToSelection(selection, uri);
                break;
            case BAND_ID:
                qb.setTables(Tables.BAND);
                qb.setProjectionMap(TODO_PROJECTTION_MAP);
                selectionArgs = insertSelectionArg(selectionArgs, uri.getPathSegments().get(1));
                qb.appendWhere(SQL_WHERE_ID);
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
		LogUtils.d(TAG,"selection ="+selection);
        // run the query
        return query(db, qb, projection, selection, selectionArgs, sortOrder, groupBy, limit);
    }
    
    private Cursor query(final SQLiteDatabase db, SQLiteQueryBuilder qb, String[] projection,
            String selection, String[] selectionArgs, String sortOrder, String groupBy,
            String limit) {
		
        if (projection != null && projection.length == 1
                && BaseColumns._COUNT.equals(projection[0])) {
            qb.setProjectionMap(COUNT_PROJECTION_MAP);
        }

        final Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null,
                sortOrder, limit);
        if (c != null) {
            // TODO: is this the right notification Uri?
            c.setNotificationUri(mContentResolver, CalendarContract.Events.CONTENT_URI);
        }
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        final int match = URI_MATCHER.match(uri);
        switch (match) {
        case BAND:
            // TODO:check the result.
            return mDb.update(Tables.BAND, values, selection, selectionArgs);
        case BAND_ID:
            long id = ContentUris.parseId(uri);
            return mDb.update(Tables.BAND, values, SQL_WHERE_ID, new String[] { String.valueOf(id) });
        default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }
    
    //=====================
    //Local function
    //=====================
    
    private String appendAccountFromParameterToSelection(String selection, Uri uri) {
    LogUtils.d(TAG, "appendAccountFromParameterToSelection()");
        final String accountName = QueryParameterUtils.getQueryParameter(uri,
                CalendarContract.EventsEntity.ACCOUNT_NAME);
        final String accountType = QueryParameterUtils.getQueryParameter(uri,
                CalendarContract.EventsEntity.ACCOUNT_TYPE);
        if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
            final StringBuilder sb = new StringBuilder();
            sb.append(Calendars.ACCOUNT_NAME + "=")
                    .append(DatabaseUtils.sqlEscapeString(accountName))
                    .append(" AND ")
                    .append(Calendars.ACCOUNT_TYPE)
                    .append(" = ")
                    .append(DatabaseUtils.sqlEscapeString(accountType));
            return appendSelection(sb, selection);
        } else {
            return selection;
        }
    }
    
    /**
     * Inserts an argument at the beginning of the selection arg list.
     *
     * The {@link android.database.sqlite.SQLiteQueryBuilder}'s where clause is
     * prepended to the user's where clause (combined with 'AND') to generate
     * the final where close, so arguments associated with the QueryBuilder are
     * prepended before any user selection args to keep them in the right order.
     */
    private String[] insertSelectionArg(String[] selectionArgs, String arg) {
    LogUtils.d(TAG, "insertSelectionArg()");
        if (selectionArgs == null) {
            return new String[] {arg};
        } else {
            int newLength = selectionArgs.length + 1;
            String[] newSelectionArgs = new String[newLength];
            newSelectionArgs[0] = arg;
            System.arraycopy(selectionArgs, 0, newSelectionArgs, 1, selectionArgs.length);
            return newSelectionArgs;
        }
    }
    
    private String appendSelection(StringBuilder sb, String selection) {
		LogUtils.d(TAG, "appendSelection()");
        if (!TextUtils.isEmpty(selection)) {
            sb.append(" AND (");
            sb.append(selection);
            sb.append(')');
        }
        return sb.toString();
    }

}
