
package org.ohmage.service;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.gson.stream.CustomJsonWriter;

import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.ohmage.AccountHelper;
import org.ohmage.ConfigHelper;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.UploadResponse;
import org.ohmage.PreferenceStore;
import org.ohmage.logprobe.Analytics;
import org.ohmage.logprobe.Log;
import org.ohmage.logprobe.LogProbe.Status;
import org.ohmage.probemanager.DbContract.BaseProbeColumns;
import org.ohmage.probemanager.DbContract.Probe;
import org.ohmage.probemanager.DbContract.Probes;
import org.ohmage.probemanager.DbContract.Responses;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;

public class ProbeUploadService extends WakefulIntentService {
    private static final String TAG = "ProbeUploadService";

    /** Extra to tell the upload service if it is running in the background **/
    public static final String EXTRA_BACKGROUND = "is_background";

    /** Extra to tell the upload service to only upload one probe **/
    public static final String EXTRA_OBSERVER_ID = "extra_observer_id";

    /**
     * Extra to tell the upload service to only upload the probe with this
     * version. Ignored if {@link ProbeUploadService#EXTRA_OBSERVER_ID} is not
     * specified.
     */
    public static final String EXTRA_OBSERVER_VERSION = "extra_observer_version";

    /** Uploaded in batches based on the size of the points **/
    private final long BATCH_SIZE = 1024 * 32;

    public static final String PROBE_UPLOAD_STARTED = "org.ohmage.PROBE_UPLOAD_STARTED";
    public static final String PROBE_UPLOAD_FINISHED = "org.ohmage.PROBE_UPLOAD_FINISHED";
    public static final String PROBE_UPLOAD_ERROR = "org.ohmage.PROBE_UPLOAD_ERROR";

    public static final String RESPONSE_UPLOAD_STARTED = "org.ohmage.RESPONSE_UPLOAD_STARTED";
    public static final String RESPONSE_UPLOAD_FINISHED = "org.ohmage.RESPONSE_UPLOAD_FINISHED";
    public static final String RESPONSE_UPLOAD_ERROR = "org.ohmage.RESPONSE_UPLOAD_ERROR";

    public static final String PROBE_UPLOAD_SERVICE_FINISHED = "org.ohmage.PROBE_UPLOAD_SERVICE_FINISHED";

    public static final String EXTRA_PROBE_ERROR = "extra_probe_error";

    private static boolean mRunning;

    private OhmageApi mApi;

    private boolean isBackground;

    /**
     * Set to true if there was any data which uploaded
     */
    private boolean mUploadedData = false;

    private AccountHelper mAccount;
    private PreferenceStore mPrefs;

    private String mObserverId = null;

    private String mObserverVersion = null;

    public ProbeUploadService() {
        super(TAG);
    }

    public static boolean isRunning() {
        return mRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Analytics.service(this, Status.ON);
        Log.d(TAG, "batch size:" + BATCH_SIZE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRunning = false;
        Analytics.service(this, Status.OFF);
    }

    @Override
    protected void doWakefulWork(Intent intent) {
        mRunning = true;

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        mAccount = new AccountHelper(ProbeUploadService.this);
        mPrefs = new PreferenceStore(this);

        if (mApi == null)
            setOhmageApi(new OhmageApi(this));

        isBackground = intent.getBooleanExtra(EXTRA_BACKGROUND, false);

        mObserverId = intent.getStringExtra(EXTRA_OBSERVER_ID);
        if (mObserverId != null)
            mObserverVersion = intent.getStringExtra(EXTRA_OBSERVER_VERSION);

        Log.v(TAG, "upload probes");
        ProbesUploader probesUploader = new ProbesUploader();
        probesUploader.upload();
        Log.v(TAG, "upload responses");
        ResponsesUploader responsesUploader = new ResponsesUploader();
        responsesUploader.upload();

        // If there were no internal errors, we can say it was successful
        if (probesUploader.uploadedData() || responsesUploader.uploadedData())
            mPrefs.edit().putLastProbeUploadTimestamp(System.currentTimeMillis()).commit();

        sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_SERVICE_FINISHED));

        mRunning = false;
    }

    public void setOhmageApi(OhmageApi api) {
        mApi = api;
    }

    /**
     * Abstraction to upload object from the probes db. Uploads data in chunks
     * based on the {@link #getName(Cursor)} and {@link #getVersion(Cursor)}
     * values.
     * 
     * @author cketcham
     */
    public abstract class Uploader {

        protected abstract Uri getContentURI();

        protected abstract UploadResponse uploadCall(String serverUrl, String username,
                String password, String client, String name, String version, JsonContentBody data);

        protected abstract void uploadStarted();

        protected abstract void uploadFinished();

        protected abstract void uploadError(String string);

        /**
         * Creates json representation of probe
         * 
         * @param c
         * @return the probe json
         * @throws IOException
         */
        public abstract long createProbe(Cursor c, CustomJsonWriter writer) throws IOException;

        protected abstract int getVersionIndex();

        protected abstract int getNameIndex();

        protected abstract String getVersionColumn();

        protected abstract String getNameColumn();

        protected abstract String[] getProjection();

        public void upload() {

            uploadStarted();

            long start = System.currentTimeMillis();

            ArrayList<Probe> observers = queryObservers();

            for (Probe o : observers) {
                Log.d(TAG, "starting to upload " + o.observer_id + " v" + o.observer_version);
                uploadBatches(o, BATCH_SIZE);
            }

            Log.d(TAG, "total time: " + (System.currentTimeMillis() - start));

            uploadFinished();
        }

        /**
         * Query for a list of observers which have data
         * 
         * @return
         */
        private ArrayList<Probe> queryObservers() {
            String select = BaseProbeColumns.USERNAME + "=?";
            if (mObserverId != null)
                select += " AND " + getNameColumn() + "='" + mObserverId + "'";

            if (mObserverVersion != null)
                select += " AND " + getVersionColumn() + "='" + mObserverVersion + "'";

            Cursor observersCursor = getContentResolver().query(getContentURI(), new String[] {
                    "distinct " + getNameColumn(), getVersionColumn()
            }, select, new String[] {
                mAccount.getUsername()
            }, null);

            ArrayList<Probe> observers = new ArrayList<Probe>();

            while (observersCursor.moveToNext()) {
                observers
                        .add(new Probe(observersCursor.getString(0), observersCursor.getString(1)));
            }
            observersCursor.close();
            return observers;
        }

        /**
         * Queries the DB for probes which are up to a given size in length.
         * Adds the probes to the probes JsonArray.
         * 
         * @param o
         * @param size
         */
        protected void uploadBatches(Probe o, long size) {
            DeletingCursor c = queryProbe(o);

            int i = 0;

            while (i < c.getCount()) {
                long startTime = System.currentTimeMillis();
                ProbeWriterBody probeWriter = new ProbeWriterBody(this, c, size);
                if (!upload(o, probeWriter))
                    break;
                int uploadedCount = c.deleteMarkedIds(ProbeUploadService.this, getContentURI());
                mUploadedData |= uploadedCount > 0;
                Log.d(TAG,
                        "uploaded batch of " + uploadedCount + " points in: "
                                + (System.currentTimeMillis() - startTime));
                i += uploadedCount;
            }

            c.close();
        }

        private DeletingCursor queryProbe(Probe o) {
            return new DeletingCursor(getContentResolver().query(
                    getContentURI(),
                    getProjection(),
                    BaseProbeColumns.USERNAME + "=? AND " + getNameColumn() + "=? AND "
                            + getVersionColumn() + "=?", new String[] {
                            mAccount.getUsername(), o.observer_id, o.observer_version
                    }, null));
        }

        /**
         * Uploads probes to the server
         * 
         * @param probes the probe json
         * @param c the cursor object
         * @return false only if there was an error which indicates we shouldn't
         *         continue uploading
         */
        private boolean upload(Probe observer, JsonContentBody data) {

            String username = mAccount.getUsername();
            String hashedPassword = mAccount.getAuthToken();

            String observerId = observer.observer_id;
            String observerVersion = observer.observer_version;
            UploadResponse response = uploadCall(ConfigHelper.serverUrl(), username,
                    hashedPassword, OhmageApi.CLIENT_NAME, observerId, observerVersion, data);
            response.handleError(ProbeUploadService.this);

            if (response.getResult().equals(OhmageApi.Result.HTTP_ERROR)) {
                Log.d(TAG, "failed due to http error (" + BATCH_SIZE + ")");
            }

            if (response.getResult().equals(OhmageApi.Result.FAILURE)) {
                if (response.hasAuthError())
                    return false;
                uploadError(observerId + response.getErrorCodes().toString());
                Log.w(TAG,
                        "Some Probes failed to upload for " + observerId + " "
                                + response.getErrorCodes());
            } else if (!response.getResult().equals(OhmageApi.Result.SUCCESS)) {
                uploadError(null);
                return false;
            }
            return true;
        }

        public boolean uploadedData() {
            return mUploadedData;
        }
    }

    private interface ProbeQuery {
        static final String[] PROJECTION = new String[] {
                Probes._ID, Probes.OBSERVER_ID, Probes.OBSERVER_VERSION, Probes.STREAM_ID,
                Probes.STREAM_VERSION, Probes.PROBE_METADATA, Probes.PROBE_DATA
        };

        static final int OBSERVER_ID = 1;
        static final int OBSERVER_VERSION = 2;
        static final int STREAM_ID = 3;
        static final int STREAM_VERSION = 4;
        static final int PROBE_METADATA = 5;
        static final int PROBE_DATA = 6;
    }

    public class ProbesUploader extends Uploader {

        @Override
        protected String[] getProjection() {
            return ProbeQuery.PROJECTION;
        }

        @Override
        protected int getNameIndex() {
            return ProbeQuery.OBSERVER_ID;
        }

        @Override
        protected int getVersionIndex() {
            return ProbeQuery.OBSERVER_VERSION;
        }

        @Override
        public long createProbe(Cursor c, CustomJsonWriter writer) throws IOException {
            writer.beginObject();
            writer.name("stream_id").value(c.getString(ProbeQuery.STREAM_ID));
            writer.name("stream_version").value(c.getInt(ProbeQuery.STREAM_VERSION));

            String data = c.getString(ProbeQuery.PROBE_DATA);
            if (!TextUtils.isEmpty(data)) {
                writer.name("data").inLineValue(data);
            }
            String metadata = c.getString(ProbeQuery.PROBE_METADATA);
            if (!TextUtils.isEmpty(metadata)) {
                writer.name("metadata").inLineValue(metadata);
            }
            writer.endObject();
            return data.length() + metadata.length();
        }

        @Override
        protected void uploadStarted() {
            sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_STARTED));
        }

        @Override
        protected void uploadFinished() {
            sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_FINISHED));
        }

        @Override
        protected void uploadError(String error) {
            if (isBackground) {
                if (error != null)
                    NotificationHelper.showProbeUploadErrorNotification(ProbeUploadService.this,
                            error);
            } else {
                Intent broadcast = new Intent(ProbeUploadService.PROBE_UPLOAD_ERROR);
                if (error != null)
                    broadcast.putExtra(EXTRA_PROBE_ERROR, error);
                sendBroadcast(broadcast);
            }
        }

        @Override
        protected Uri getContentURI() {
            return Probes.CONTENT_URI;
        }

        @Override
        protected UploadResponse uploadCall(String serverUrl, String username, String password,
                String client, String observerId, String observerVersion, JsonContentBody dataWriter) {
            return mApi.observerUpload(ConfigHelper.serverUrl(), username, password,
                    OhmageApi.CLIENT_NAME, observerId, observerVersion, dataWriter);
        }

        @Override
        protected String getVersionColumn() {
            return Probes.OBSERVER_VERSION;
        }

        @Override
        protected String getNameColumn() {
            return Probes.OBSERVER_ID;
        }
    }

    private interface ResponseQuery {
        static final String[] PROJECTION = new String[] {
                Responses._ID, Responses.CAMPAIGN_URN, Responses.CAMPAIGN_CREATED,
                Responses.RESPONSE_DATA
        };

        static final int CAMPAIGN_URN = 1;
        static final int CAMPAIGN_CREATED = 2;
        static final int RESPONSE_DATA = 3;
    }

    public class ResponsesUploader extends Uploader {

        @Override
        protected String[] getProjection() {
            return ResponseQuery.PROJECTION;
        }

        @Override
        protected int getNameIndex() {
            return ResponseQuery.CAMPAIGN_URN;
        }

        @Override
        protected int getVersionIndex() {
            return ResponseQuery.CAMPAIGN_CREATED;
        }

        @Override
        public long createProbe(Cursor c, CustomJsonWriter writer) throws IOException {
            String data = c.getString(ResponseQuery.RESPONSE_DATA);
            writer.inLineValue(data);
            return data.length();
        }

        @Override
        protected void uploadStarted() {
            sendBroadcast(new Intent(ProbeUploadService.RESPONSE_UPLOAD_STARTED));
        }

        @Override
        protected void uploadFinished() {
            sendBroadcast(new Intent(ProbeUploadService.RESPONSE_UPLOAD_FINISHED));
        }

        @Override
        protected void uploadError(String error) {
            if (isBackground) {
                if (error != null)
                    NotificationHelper.showResponseUploadErrorNotification(ProbeUploadService.this,
                            error);
            } else {
                Intent broadcast = new Intent(ProbeUploadService.RESPONSE_UPLOAD_ERROR);
                if (error != null)
                    broadcast.putExtra(EXTRA_PROBE_ERROR, error);
                sendBroadcast(broadcast);
            }
        }

        @Override
        protected Uri getContentURI() {
            return Responses.CONTENT_URI;
        }

        @Override
        protected UploadResponse uploadCall(String serverUrl, String username, String password,
                String client, String campaignUrn, String campaignCreated,
                JsonContentBody dataWriter) {
            return mApi.surveyUpload(ConfigHelper.serverUrl(), username, password,
                    OhmageApi.CLIENT_NAME, campaignUrn, campaignCreated, dataWriter);
        }

        @Override
        protected String getVersionColumn() {
            return Responses.CAMPAIGN_CREATED;
        }

        @Override
        protected String getNameColumn() {
            return Responses.CAMPAIGN_URN;
        }
    }

    public static class ProbeWriterBody extends JsonContentBody {

        private final DeletingCursor mCursor;
        private long mSize;
        private final Uploader mUploader;

        public ProbeWriterBody(Uploader uploader, DeletingCursor c, long size) {
            mUploader = uploader;
            mCursor = c;
            mSize = size;
        }

        @Override
        protected void writeJson(CustomJsonWriter writer) {
            try {

                writer.beginArray();
                while (mSize > 0 && mCursor.moveToNext()) {
                    try {
                        mSize -= mUploader.createProbe(mCursor, writer);
                    } catch (IOException e) {
                        Log.e(TAG, "There was an error creating a probe");
                    }
                }
                writer.endArray();
                writer.flush();
            } catch (IOException e) {
                Log.e(TAG, "there was an error uploading probes", e);
            }
        }
    }

    public static class DeletingCursor implements Cursor {

        private final Cursor mCursor;
        private final LinkedList<Long> ids = new LinkedList<Long>();

        private final int mCount;
        private int mOffset = 0;

        /** The number of deleted points **/
        private int mDeleted;

        public DeletingCursor(Cursor c) {
            mCursor = c;
            mCount = mCursor.getCount();
        }

        public int deleteMarkedIds(Context context, Uri contentUri) {
            StringBuilder deleteString = new StringBuilder();

            // Deleting this batch of points. We can only delete
            // with a maximum expression tree depth of 1000
            int batch = 0;
            for (Long id : ids) {
                if (deleteString.length() != 0)
                    deleteString.append(" OR ");
                deleteString.append(BaseColumns._ID + "=" + id);
                batch++;

                // If we have 1000 Expressions or we are at the last
                // point, delete them
                if ((batch % (1000 - 2) == 0) || batch == ids.size()) {
                    context.getContentResolver().delete(contentUri, deleteString.toString(), null);
                    deleteString = new StringBuilder();
                }
            }
            int count = ids.size();
            mDeleted += count;
            ids.clear();
            return count;
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public int getPosition() {
            return mCursor.getPosition() + mOffset;
        }

        @Override
        public boolean move(int offset) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean moveToPosition(int position) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean moveToFirst() {
            if (!isBeforeFirst())
                throw new RuntimeException("DeletingCursor can only move forwards");
            return mCursor.moveToFirst();
        }

        @Override
        public boolean moveToLast() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean moveToNext() {
            if (!isAfterLast() && !isLast()
                    && mCursor.getPosition() + 1 > mCursor.getCount() - mDeleted) {
                requery();
            }
            if (mCursor.moveToNext()) {
                ids.add(getLong(0));
                return true;
            }
            return false;
        }

        @Override
        public boolean moveToPrevious() {
            throw new RuntimeException("DeletingCursor can only move forwards");
        }

        @Override
        public boolean isFirst() {
            return mCursor.isFirst();
        }

        @Override
        public boolean isLast() {
            return mCursor.isLast();
        }

        @Override
        public boolean isBeforeFirst() {
            return mCursor.isBeforeFirst() && ids.isEmpty() && mDeleted == 0;
        }

        @Override
        public boolean isAfterLast() {
            return mCursor.isAfterLast();
        }

        @Override
        public int getColumnIndex(String columnName) {
            return mCursor.getColumnIndex(columnName);
        }

        @Override
        public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
            return mCursor.getColumnIndexOrThrow(columnName);
        }

        @Override
        public String getColumnName(int columnIndex) {
            return mCursor.getColumnName(columnIndex);
        }

        @Override
        public String[] getColumnNames() {
            return mCursor.getColumnNames();
        }

        @Override
        public int getColumnCount() {
            return mCursor.getColumnCount();
        }

        @Override
        public byte[] getBlob(int columnIndex) {
            return mCursor.getBlob(columnIndex);
        }

        @Override
        public String getString(int columnIndex) {
            return mCursor.getString(columnIndex);
        }

        @Override
        public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
            mCursor.copyStringToBuffer(columnIndex, buffer);
        }

        @Override
        public short getShort(int columnIndex) {
            return mCursor.getShort(columnIndex);
        }

        @Override
        public int getInt(int columnIndex) {
            return mCursor.getInt(columnIndex);
        }

        @Override
        public long getLong(int columnIndex) {
            return mCursor.getLong(columnIndex);
        }

        @Override
        public float getFloat(int columnIndex) {
            return mCursor.getFloat(columnIndex);
        }

        @Override
        public double getDouble(int columnIndex) {
            return mCursor.getDouble(columnIndex);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public int getType(int columnIndex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                return mCursor.getType(columnIndex);
            throw new RuntimeException("getType() not available on this version of android");
        }

        @Override
        public boolean isNull(int columnIndex) {
            return mCursor.isNull(columnIndex);
        }

        @Override
        @Deprecated
        public void deactivate() {
            mCursor.deactivate();
        }

        @Override
        @Deprecated
        public boolean requery() {
            Log.d(TAG, "requery");
            int position = mCursor.getPosition();
            if (mCursor.requery()) {
                mCursor.move(position - mDeleted + 1);
                mOffset += position - (position - mDeleted);
                mDeleted = 0;
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            mCursor.close();
        }

        @Override
        public boolean isClosed() {
            return mCursor.isClosed();
        }

        @Override
        public void registerContentObserver(ContentObserver observer) {
            mCursor.registerContentObserver(observer);
        }

        @Override
        public void unregisterContentObserver(ContentObserver observer) {
            mCursor.unregisterContentObserver(observer);
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mCursor.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mCursor.unregisterDataSetObserver(observer);
        }

        @Override
        public void setNotificationUri(ContentResolver cr, Uri uri) {
            mCursor.setNotificationUri(cr, uri);
        }

        @Override
        public boolean getWantsAllOnMoveCalls() {
            return mCursor.getWantsAllOnMoveCalls();
        }

        @Override
        public Bundle getExtras() {
            return mCursor.getExtras();
        }

        @Override
        public Bundle respond(Bundle extras) {
            return mCursor.respond(extras);
        }
    }

    public abstract static class JsonContentBody extends AbstractContentBody {

        public JsonContentBody() {
            super("text/plain");
        }

        @Override
        public String getFilename() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getCharset() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getContentLength() {
            // TODO Auto-generated method stub
            return -1;
        }

        @Override
        public String getTransferEncoding() {
            return MIME.ENC_8BIT;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            CustomJsonWriter writer = new CustomJsonWriter(new OutputStreamWriter(out));
            writeJson(writer);
        }

        protected abstract void writeJson(CustomJsonWriter writer);
    }
}
