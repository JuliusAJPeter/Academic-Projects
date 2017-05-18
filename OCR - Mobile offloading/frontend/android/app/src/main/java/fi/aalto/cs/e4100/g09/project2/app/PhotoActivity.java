package fi.aalto.cs.e4100.g09.project2.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import fi.aalto.cs.e4100.g09.project2.CloudClient;
import fi.aalto.cs.e4100.g09.project2.R;

public class PhotoActivity extends AppCompatActivity {
    private static final String LOG_TAG = PhotoActivity.class.getSimpleName();

    private static final String INTENT_DATA_ID = "ocr_id";

    private CloudClient mClient;

    public static void start(Activity activity, String id) {
        Intent intent = new Intent(activity, PhotoActivity.class);
        intent.putExtra(INTENT_DATA_ID, id);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mClient = CloudClient.getInstance(this);

        Intent intent = getIntent();

        final String id = intent.getStringExtra(INTENT_DATA_ID);
        if (id != null) {
            DownloadTask task = new DownloadTask();
            task.execute(id);
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... ids) {
            String id = ids[0];
            Log.i(LOG_TAG, "Downloading Image " + id);

            try {
                return mClient.getSourceImage(id);
            } catch (CloudClient.CloudConnectionException e) {
                Log.e(LOG_TAG, e.getLocalizedMessage());
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result == null) {
                // TODO: notify error
            } else {
                onDownloadSuccess(result);
            }
        }

        @Override
        protected void onCancelled() {
//            showProgress(false);
        }
    }

    private void onDownloadSuccess(Bitmap result) {
        ImageView iv = (ImageView) findViewById(R.id.image);
        iv.setImageBitmap(result);
    }

}
