package fi.aalto.cs.e4100.g09.project2;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Tools for OCR reading from images.
 */
public class OcrConverter {
    private static final String LOG_TAG = OcrConverter.class.getSimpleName();
    private static final String DEFAULT_LANGUAGE = "eng";
    private static final String TESSBASE_PATH = "tessdata";
    private final String tessBaseParentPath;


    public OcrConverter(AssetManager assets, String cacheDir) {
        try {
            loadTrainedData(assets, cacheDir);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to prepare trained data: "+e.getLocalizedMessage());
            throw new RuntimeException("Unable to prepare trained data: "+e.getLocalizedMessage());
        }

        // Tess requires the last character of the path to be a slash
        // see @link TessBaseAPI.init(String,String)
        tessBaseParentPath = cacheDir + (cacheDir.endsWith("/") ? "" : "/");
    }

    private void loadTrainedData(AssetManager assets, String cacheDir) throws IOException {
        File f;
        InputStream is;
        int size;
        byte[] buffer;

        File dir = new File(cacheDir + "/" + TESSBASE_PATH);

        if (!dir.exists()) {
            dir.mkdir();
        }

        String[] list = assets.list(TESSBASE_PATH);
        Log.d(LOG_TAG, Arrays.toString(list));

        for (String p : list) {
            Log.d(LOG_TAG, cacheDir + "/" + TESSBASE_PATH + "/" + p);
            f = new File(cacheDir + "/" + TESSBASE_PATH + "/" + p);

            if (!f.exists()) {
                is = assets.open(TESSBASE_PATH + "/" + p);
                size = is.available();
                buffer = new byte[size];
                is.read(buffer);
                is.close();

                FileOutputStream fos = new FileOutputStream(f);
                fos.write(buffer);
                fos.close();
            }
        }
    }


    public String detectText(Bitmap bitmap) {

        final TessBaseAPI baseApi = new TessBaseAPI();
//        baseApi.setDebug(true);
        boolean success = baseApi.init(tessBaseParentPath, DEFAULT_LANGUAGE);
        if (success) {
            Log.i(LOG_TAG, "Successfully initialized Tesseract OCR engine");
        } else {
            Log.e(LOG_TAG, "Failed to initialize Tesseract OCR engine");
        }


//        Bitmap bitmap = prepareBitmap(image);
        baseApi.setImage(bitmap);

        Log.v(LOG_TAG, "Starting OCR");
        String text = baseApi.getUTF8Text();
        Log.v(LOG_TAG, "Finished OCR: " + text);

        baseApi.end();
        bitmap.recycle();

        return text;
    }

    /**
     * Prepare the picture for processing. Without this, the OCR process took many minutes and
     * produced gibberish.
     * Based on https://github.com/GautamGupta/Simple-Android-OCR/blob/master/src/com/datumdroid/android/ocr/simple/SimpleAndroidOCRActivity.java
     *
     * @param fileImage
     * @return modified bitmap
     */
    private static Bitmap prepareBitmap(File fileImage) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;

        Bitmap bitmap = BitmapFactory.decodeFile(fileImage.getPath(), options);

        try {
            ExifInterface exif = new ExifInterface(fileImage.getPath());
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            int rotate = 0;
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            if (rotate != 0) {
                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            }

            // Convert to ARGB_8888, required by tess
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        } catch (IOException e) {
            Log.e(LOG_TAG, "Couldn't correct orientation: " + e.toString());
        }

        return bitmap;
    }

}
