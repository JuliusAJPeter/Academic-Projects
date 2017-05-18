package fi.aalto.cs.e4100.g09.project2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */

public class AndroidUtils {
    private static final String LOG_TAG = AndroidUtils.class.getSimpleName();

    /**
     * Check if an app is installed.
     * Based on http://stackoverflow.com/a/18752247
     *
     * @param packageName    application id
     * @param packageManager android package manager
     * @return true if app is installed on system, false otherwise
     */
    public static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public static void goToPlayStore(String packageName, Activity fromActivity) {
        Uri uri;
        try {
            uri = Uri.parse("market://details?id=" + packageName);
            fromActivity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
            fromActivity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    public static void showErrorDialog(Context context, String title, String message) {
        try {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } catch (WindowManager.BadTokenException e) {
            /*don't show dialog*/
        }
    }

    public static String getRealPathFromURI(ContentResolver contentResolver, Uri contentURI) {
        String result;
        Cursor cursor = contentResolver.query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    /**
     * http://hmkcode.com/android-display-selected-image-and-its-real-path/
     *
     * @param contentResolver
     * @param uri
     * @return
     */
    public static String getRealPathFromURI_API19(ContentResolver contentResolver, Uri uri) {
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = {MediaStore.Images.Media.DATA};

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{id}, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }

        cursor.close();
        return filePath;
    }

    public static byte[] getBytesFromContentUri(ContentResolver contentResolver, Uri uri)
            throws IOException {
        InputStream inputStream = contentResolver.openInputStream(uri);

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        Log.d(LOG_TAG, isConnected ? "Device connected to internet" : "Device OFFLINE");

        return isConnected;
    }

    public static Bitmap decodeB64Bitmap(String imageB64) {
        byte[] decodedString;
        try {
            decodedString = Base64.decode(imageB64, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
            Log.d(LOG_TAG, "Base64: "+imageB64);
            return null;
        }
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

}
