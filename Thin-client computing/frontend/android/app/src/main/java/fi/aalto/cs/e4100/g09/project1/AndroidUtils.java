package fi.aalto.cs.e4100.g09.project1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.WindowManager;

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
            uri =  Uri.parse("market://details?id=" + packageName);
            fromActivity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
            fromActivity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    public static boolean isDeviceSupportingVnc(Activity activity) {
        Uri dummyVncUri = Uri.parse("vnc://vnc.example.com:5900/");
        Intent vncIntent = new Intent(android.content.Intent.ACTION_VIEW, dummyVncUri);

        PackageManager packageManager = activity.getPackageManager();
        if (vncIntent.resolveActivity(packageManager) != null) {
            Log.i(LOG_TAG, "VNC protocol supported!!");
            return true;
        } else {
            Log.w(LOG_TAG, "VNC protocol *NOT* supported!!");
            return false;
        }
    }

    public static void startVncViewer(Activity activity, String vncUrl) {
        Intent vncIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(vncUrl));
        vncIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(vncIntent);
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

}
