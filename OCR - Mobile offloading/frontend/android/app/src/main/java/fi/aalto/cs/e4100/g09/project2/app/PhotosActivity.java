package fi.aalto.cs.e4100.g09.project2.app;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.aalto.cs.e4100.g09.project2.AndroidUtils;
import fi.aalto.cs.e4100.g09.project2.CloudClient;
import fi.aalto.cs.e4100.g09.project2.HistoryAdapter;
import fi.aalto.cs.e4100.g09.project2.OcrConverter;
import fi.aalto.cs.e4100.g09.project2.R;

public class PhotosActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String LOG_TAG = PhotosActivity.class.getSimpleName();

    private static final String INTENT_DATA_APP_TOKEN = "app_token";
    private static final int REQUEST_IMAGE_CAPTURE = 53291;
    private static final int REQUEST_IMAGE_PICK = 53292;
    private static boolean photoRetakeRequested = false;

    private final Activity mActivity = this;
    private CloudClient mClient;
    private View mProgressView;
    private Spinner mModeSelector;
    private LinearLayout mContent;
    private ListView mHistoryList;

    private String mCurrentPhotoPath;
    private boolean mFromCamera = false;
    private boolean mOnline = false;


    public static void start(Activity activity, String appToken) {
        Intent intent = new Intent(activity, PhotosActivity.class);
        intent.putExtra(INTENT_DATA_APP_TOKEN, appToken);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mModeSelector = (Spinner) findViewById(R.id.mode_selector);
        Button mGalleryButton = (Button) findViewById(R.id.gallery_button);
        Button mTakePhotoButton = (Button) findViewById(R.id.photo_button);
        mContent = (LinearLayout) findViewById(R.id.content);
        mHistoryList = (ListView) findViewById(R.id.history_list);
        mProgressView = findViewById(R.id.photos_progress);

        Intent intent = getIntent();

        String appToken = intent.getStringExtra(INTENT_DATA_APP_TOKEN);

        String[] operatingModes = getResources().getStringArray(R.array.operating_modes);
        mClient = CloudClient.getInstance(this, appToken);
        if (AndroidUtils.isConnected(this)) {
            mOnline = true;
            if (!mClient.hasToken()) {
                finish(); // back to login
            }
        } else {
            mOnline = false;
        }

        if (mOnline) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, operatingModes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mModeSelector.setAdapter(adapter);
            mModeSelector.setSelection(1);
        } else {
            // work offline
            operatingModes = Arrays.copyOfRange(operatingModes, 0, 1);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, operatingModes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mModeSelector.setAdapter(adapter);
            mModeSelector.setSelection(0);
            Toast.makeText(this, "No internet connection, working only offline", Toast.LENGTH_LONG).show();
        }

        mHistoryList.setOnItemClickListener(this);

        mGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Gallery button pressed");
                pickPhotos();
            }
        });
        mTakePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Take photo button pressed");
                takePhoto();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (photoRetakeRequested) {
            photoRetakeRequested = false;
            takePhoto();
        }

        if (mOnline) {
            GetHistoryTask historyTask = new GetHistoryTask();
            historyTask.execute();
        } else {
            List<HistoryAdapter.OcrResult> history = null;
            try {
                history = mClient.getHistoryFromCache();
            } catch (CloudClient.CloudConnectionException e) {/*ignore - empty*/}
            if (history != null) {
                onHistorySuccess(history);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mContent.setVisibility(show ? View.GONE : View.VISIBLE);
            mContent.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mContent.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mContent.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        HistoryAdapter.OcrResult item = (HistoryAdapter.OcrResult) parent.getItemAtPosition(position);
        Log.d(LOG_TAG, "Clicked " + item.text + " at position " + position);

        ResultActivity.start(this, item.text, item.id, item.timestamp, false); // TODO: pass more data - some ID, creation time, ...
    }

    private void showErrorDialog(String title, String message) {
        AndroidUtils.showErrorDialog(this, title, message);
    }

    private void pickPhotos() {

        // we need permission
        askForReadExternalStoragePermission();

        Intent pickPicturesIntent = new Intent();
        pickPicturesIntent.setType("image/*");
        pickPicturesIntent.setAction(Intent.ACTION_GET_CONTENT);
        pickPicturesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        if (pickPicturesIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(pickPicturesIntent,
                    "select multiple images"), REQUEST_IMAGE_PICK);
        }
    }

    /**
     * See https://developer.android.com/training/camera/photobasics.html
     */
    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(LOG_TAG, ex.getLocalizedMessage());
                showErrorDialog("Error occurred while taking picture", ex.getLocalizedMessage());
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /**
     * See https://developer.android.com/training/camera/photobasics.html
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "JPEG_" + Long.toString(System.currentTimeMillis());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "...and we're back! requestCode: " + requestCode + ", resultCode: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_CANCELED) {
                Log.i(LOG_TAG, "Picture capturing cancelled");
            } else if (resultCode == RESULT_OK) {
                File imgFile = new File(mCurrentPhotoPath);
                Log.d(LOG_TAG, imgFile.toString() + " - exists? " + Boolean.toString(imgFile.exists()));
                Uri uri = Uri.parse(imgFile.toString());
                Log.d(LOG_TAG, uri.toString() + " - exists? " + uri.getPath());
                onPictureCaptured(uri);
            }
        } else if (requestCode == REQUEST_IMAGE_PICK) {
            if (resultCode == RESULT_CANCELED) {
                Log.i(LOG_TAG, "Picture picking cancelled");

            } else if (resultCode == RESULT_OK) {
                if (data == null) {
                    Log.e(LOG_TAG, "No data received from image picking");
                    return;
                }

                List<Uri> images = new ArrayList<>();

                // if multiple images selected
                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        Uri uri = item.getUri();
                        Log.d(LOG_TAG, "Uri: " + uri);
                        images.add(uri);
                    }
                }
                // if single image selected
                else if (data.getData() != null) {
                    Uri uri = data.getData();
                    Log.d(LOG_TAG, "Uri: " + uri);
                    images.add(uri);
                }

                onPictureSelected(images.toArray(new Uri[images.size()]));
            }
        }
    }

    private void onPictureCaptured(Uri image) {
        Log.i(LOG_TAG, "Image captured! Image: " + image);
        mFromCamera = true;
        submitOcr(image);
    }

    private void onPictureSelected(Uri... images) {
        Log.i(LOG_TAG, "Image selected! First of images(" + images.length + "): " + images[0]);
        mFromCamera = false;
        submitOcr(images);
    }

    private void submitOcr(Uri... images) {
        String selectedMode = (String) mModeSelector.getSelectedItem();
        selectedMode = selectedMode.toLowerCase();

        if (selectedMode.contains("local")) {
            performLocalOcr(images);
        } else if (selectedMode.contains("remote")) {
            performRemoteOcr(images);
        } else if (selectedMode.contains("benchmark")) {
            performBenchmark(images);
        } else {
            AndroidUtils.showErrorDialog(this, "ERROR", "UNKNOWN MODE: " + selectedMode);
        }
    }

    private void performRemoteOcr(Uri... images) {
        showProgress(true);
        RemoteOcrTask task = new RemoteOcrTask();
        task.execute(images);
    }

    private void performLocalOcr(Uri... images) {
        showProgress(true);
        LocalOcrTask task = new LocalOcrTask();
        task.execute(images);
    }

    private void performBenchmark(Uri... images) {
        showProgress(true);
        BenchmarkTask task = new BenchmarkTask();
        task.execute(images);
    }

    private void onOcrSuccess(String result) {
        Log.i(LOG_TAG, "OCR success. Result: " + result);

        // TODO: reload history list ?

        ResultActivity.start(this, result, mFromCamera);
    }

    private void onOcrError(String msg) {
        Log.e(LOG_TAG, "OCR error: " + msg);
        showErrorDialog("Error", "Unable to perform OCR on the image. Please try again.\n\n" + msg);
    }

    private void onBenchmarkSuccess(BenchmarkActivity.BenchmarkStats stats) {
        Log.i(LOG_TAG, "Benchmark success. Result: " + stats);

        BenchmarkActivity.start(this, stats);
    }

    private void onHistorySuccess(List<HistoryAdapter.OcrResult> history) {
        ListAdapter adapter = new HistoryAdapter(mActivity, history);
        mHistoryList.setAdapter(adapter);
    }


    public static void requestPhotoRetake() {
        photoRetakeRequested = true;
    }

    /**
     * Login task used to authenticate the user.
     */
    public class RemoteOcrTask extends AsyncTask<Uri, Void, String> {

        private String errorMsg;

        RemoteOcrTask() {
        }

        @Override
        protected String doInBackground(Uri... images) {
            errorMsg = null;
            try {
                return mClient.requestOcr(images);
            } catch (CloudClient.CloudConnectionException e) {
                errorMsg = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            showProgress(false);

            if (result != null) {
                onOcrSuccess(result);
            } else {
                onOcrError(errorMsg);
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }
    }

    public class LocalOcrTask extends AsyncTask<Uri, String, String> implements
            TessBaseAPI.ProgressNotifier {

//        private String errorMsg;

        LocalOcrTask() {
        }

        @Override
        protected String doInBackground(Uri... images) {
            Log.i(LOG_TAG, "Performing local OCR with images " + Arrays.toString(images));

            OcrConverter ocr = new OcrConverter(getAssets(), getCacheDir().getPath());

            String text = "";
            for (Uri u : images) {
                Bitmap bitmap;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), u);
                } catch (IOException e) {
                    Log.e(LOG_TAG, e.getLocalizedMessage());
                    try {
                        File file = new File(u.getPath());
                        bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                    } catch (IOException e1) {
                        Log.e(LOG_TAG, e1.getLocalizedMessage());
                        e1.printStackTrace();
                        return null;
                    }
                }
                Log.d(LOG_TAG, "Bitmap: "+bitmap.toString());
                String ocrText = ocr.detectText(bitmap);
                Log.i(LOG_TAG, "OCR(" + u.toString() + "): " + ocrText);
                text += ocrText + "\n";
            }

            Log.d(LOG_TAG, "OCR result: " + text);

            return text;
        }

        @Override
        public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {
            Log.i(LOG_TAG, progressValues.toString());
        }

        @Override
        protected void onPostExecute(String result) {
            showProgress(false);

            if (result != null) {
                onOcrSuccess(result);
            } else {
                onOcrError("");
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }
    }

    public class BenchmarkTask extends AsyncTask<Uri, String, BenchmarkActivity.BenchmarkStats> {

        BenchmarkTask() {
        }

        @Override
        protected BenchmarkActivity.BenchmarkStats doInBackground(final Uri... images) {
            Log.i(LOG_TAG, "Performing benchmark with images " + Arrays.toString(images));

            final BenchmarkActivity.BenchmarkStats stats = new BenchmarkActivity.BenchmarkStats();

            Thread[] threads = new Thread[2];
            threads[0] = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "BENCHMARK: starting local OCR");

                    long ntTotal = System.nanoTime();

                    OcrConverter ocr = new OcrConverter(getAssets(), getCacheDir().getPath());

                    for (Uri u : images) {
                        Bitmap bitmap;
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), u);
                        } catch (IOException e) {
                            Log.e(LOG_TAG, e.getLocalizedMessage());
                            break;
                        }

                        long ntOcr = System.nanoTime();
                        ocr.detectText(bitmap);
                        stats.addProcessingTimeLocal(System.nanoTime() - ntOcr);
                    }

                    stats.setTotalProcessingTimeLocal(System.nanoTime() - ntTotal);
                    Log.i(LOG_TAG, "BENCHMARK: local OCR finished");
                }
            });
            threads[1] = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "BENCHMARK: starting remote OCR");
                    long ntTotal = System.nanoTime();
                    try {
                        mClient.requestOcr(images);
                        stats.getProcessingTimeRemote().addAll(mClient.getLastOcrProcessingTime());
                        stats.getExchangedBytesRemote().addAll(mClient.getLastOcrTransferBytes());

                        // TODO: individual stats !!!

                    } catch (CloudClient.CloudConnectionException e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                    stats.setTotalExchangedBytesRemote(mClient.getLastTotalTransferBytes());
                    stats.setTotalProcessingTimeRemote(System.nanoTime() - ntTotal);
                    Log.i(LOG_TAG, "BENCHMARK: remote OCR finished");
                }
            });

            threads[0].start();
            threads[1].start();
            try {
                threads[0].join();
                threads[1].join();
            } catch (InterruptedException e) {/*ignore for now*/}

            return stats;
        }

        @Override
        protected void onPostExecute(BenchmarkActivity.BenchmarkStats result) {
            showProgress(false);

            if (result != null) {
                onBenchmarkSuccess(result);
            } else {
                onOcrError("");
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }
    }

    public class GetHistoryTask extends AsyncTask<Void, String, List<HistoryAdapter.OcrResult>> {

        GetHistoryTask() {
        }

        @Override
        protected List<HistoryAdapter.OcrResult> doInBackground(Void... images) {
            Log.i(LOG_TAG, "Downloading OCR history");

            List<HistoryAdapter.OcrResult> history;
            try {
                history = mClient.getHistory();
            } catch (CloudClient.CloudConnectionException e) {
                Log.e(LOG_TAG, e.getLocalizedMessage());
                e.printStackTrace();
                return null;
            }

            return history;
        }

        @Override
        protected void onPostExecute(List<HistoryAdapter.OcrResult> result) {
            if (result == null) {
                // TODO: toast-notify error
            } else {
                onHistorySuccess(result);
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }


    }

    private void askForReadExternalStoragePermission() {
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private void askForPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int granted = checkSelfPermission(permission);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, 123);
            }
        }
    }
}
