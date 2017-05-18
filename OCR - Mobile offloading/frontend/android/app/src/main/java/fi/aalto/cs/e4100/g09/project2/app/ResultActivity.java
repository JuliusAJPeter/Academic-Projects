package fi.aalto.cs.e4100.g09.project2.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fi.aalto.cs.e4100.g09.project2.R;

public class ResultActivity extends AppCompatActivity {
    private static final String LOG_TAG = ResultActivity.class.getSimpleName();

    private static final String INTENT_DATA_OCR_TEXT = "ocr_text";
    private static final String INTENT_DATA_CAMERA = "ocr_camera";
    private static final String INTENT_DATA_ID = "ocr_id";
    private static final String INTENT_DATA_CREATION_DATE = "ocr_timestamp";

    private TextView mOcrTextView;
    private TextView mOcrDate;
    private final Activity mActivity = this;

    public static void start(Activity activity, String ocrText, boolean fromCamera) {
        Intent intent = new Intent(activity, ResultActivity.class);
        intent.putExtra(INTENT_DATA_OCR_TEXT, ocrText);
        intent.putExtra(INTENT_DATA_CAMERA, fromCamera);
        activity.startActivity(intent);
    }

    public static void start(Activity activity, String ocrText, String id, Date creationDate, boolean fromCamera) {
        Intent intent = new Intent(activity, ResultActivity.class);
        intent.putExtra(INTENT_DATA_OCR_TEXT, ocrText);
        intent.putExtra(INTENT_DATA_ID, id);
        intent.putExtra(INTENT_DATA_CREATION_DATE, creationDate);
        intent.putExtra(INTENT_DATA_CAMERA, fromCamera);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mOcrTextView = (TextView) findViewById(R.id.text);
        mOcrDate = (TextView) findViewById(R.id.date);
        Button mSaveButton = (Button) findViewById(R.id.save_button);
        Button mRetakeButton = (Button) findViewById(R.id.retake_button);

        Intent intent = getIntent();

        final String ocrText = intent.getStringExtra(INTENT_DATA_OCR_TEXT);
        mOcrTextView.setText(ocrText);

        final String id = intent.getStringExtra(INTENT_DATA_ID);
        if (id != null) {
            Button sourceButton = (Button) findViewById(R.id.source_button);
            sourceButton.setVisibility(View.VISIBLE);
            sourceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PhotoActivity.start(mActivity, id);
                }
            });
        }

        final Date timestamp = (Date) intent.getSerializableExtra(INTENT_DATA_CREATION_DATE);
        if (timestamp != null) {
            DateFormat f = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.getDefault());
            mOcrDate.setText("Created on " + f.format(timestamp));
            mOcrDate.setVisibility(View.VISIBLE);
        }

        boolean fromCamera = intent.getBooleanExtra(INTENT_DATA_CAMERA, false);
        mRetakeButton.setVisibility(fromCamera ? View.VISIBLE : View.GONE);
        mRetakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                retakePhoto();
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveText(ocrText);
            }
        });
    }

    private void retakePhoto() {
        // a bit of a hack
        PhotosActivity.requestPhotoRetake();
        finish();
    }

    private void saveText(String ocrText) {
        Log.d(LOG_TAG, "save text");

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss'.txt'");
        File file = new File(getFilesDir(), format.format(new Date()));

        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(ocrText.getBytes());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
            e.printStackTrace();
            return;
        }

        Toast.makeText(this, "Text was saved to file " + file.toString(), Toast.LENGTH_LONG).show();
    }

}
