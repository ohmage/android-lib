
package org.ohmage.async;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class BitmapCompressTask extends AsyncTask<Void, Void, Void> {

    private final Bitmap mBitmap;
    private final File mFile;

    public BitmapCompressTask(Context context, Bitmap bitmap, File file) {
        super();
        mBitmap = bitmap;
        mFile = file;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            mBitmap.compress(CompressFormat.PNG, 90, new FileOutputStream(mFile));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
