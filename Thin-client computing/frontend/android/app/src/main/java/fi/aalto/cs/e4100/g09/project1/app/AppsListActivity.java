package fi.aalto.cs.e4100.g09.project1.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.coboltforge.dontmind.multivnc.COLORMODEL;
import com.coboltforge.dontmind.multivnc.ConnectionBean;
import com.coboltforge.dontmind.multivnc.VncCanvasActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fi.aalto.cs.e4100.g09.project1.AndroidUtils;
import fi.aalto.cs.e4100.g09.project1.ConnectionException;
import fi.aalto.cs.e4100.g09.project1.ConnectionUtils;
import fi.aalto.cs.e4100.g09.project1.Constants;
import fi.aalto.cs.e4100.g09.project1.ForbiddenException;
import fi.aalto.cs.e4100.g09.project1.R;
import fi.aalto.cs.e4100.g09.project1.ServerErrorException;

public class AppsListActivity extends LocationAwareActivity implements AdapterView.OnItemClickListener {
    private static final String LOG_TAG = AppsListActivity.class.getSimpleName();
    private static final String INTENT_DATA_APPS_LIST = "apps_list";
    private static final String INTENT_DATA_LOCATION = "last_location";
    private static final String INTENT_DATA_APP_TOKEN = "app_token";

    private static final Location tBuildingLocation;
    private static final int tBuildingProximityCircle;
    private static final String tBuildingPrioritizedApp;

    static {
        tBuildingLocation = new Location("");
        // 60.1869700N, 24.8215042E, circle 70m - https://maps.google.com/maps?q=60.1869700N%2C+24.8215042E
        tBuildingLocation.setLatitude(60.1869700d);
        tBuildingLocation.setLongitude(24.8215042d);
        tBuildingProximityCircle = 70; // radius in meters
        tBuildingPrioritizedApp = "openoffice";
    }

    private List<String> mListKeys;
    private List<String> mListLabels;
    private String mAppToken;
    private Location mLocation;
    private AppStartTask mAppStartTask = null;
    private View mProgressView;
    private ListView mListView;


    public static void start(Activity activity, String appToken, HashMap<String, String> appsList, Location location) {
        Intent intent = new Intent(activity, AppsListActivity.class);
        intent.putExtra(INTENT_DATA_APPS_LIST, appsList);
        intent.putExtra(INTENT_DATA_LOCATION, location);
        intent.putExtra(INTENT_DATA_APP_TOKEN, appToken);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps_list);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mListView = (ListView) findViewById(R.id.apps_list);
        mProgressView = findViewById(R.id.start_progress);

        Intent intent = getIntent();

        mAppToken = intent.getStringExtra(INTENT_DATA_APP_TOKEN);

        // get location (must be before apps list sorting!)
        mLocation = intent.getParcelableExtra(INTENT_DATA_LOCATION);
        if (mLocation == null) {
            mLocation = getLastLocation();
        }

        @SuppressWarnings("unchecked")
        Map<String, String> apps = (HashMap<String, String>) intent.getSerializableExtra(INTENT_DATA_APPS_LIST);
        if (apps == null) {
            throw new RuntimeException("Apps must be passed");
        }
        // order apps - location aware
        apps = sortApps(apps);
        mListKeys = new ArrayList<>(apps.size());
        mListLabels = new ArrayList<>(apps.size());
        for (Map.Entry<String, String> entry : apps.entrySet()) {
            mListKeys.add(entry.getKey());
            mListLabels.add(entry.getValue());
        }


        ListAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mListLabels);

        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(this);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
            mListView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mListView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mListView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public Map<String, String> sortApps(Map<String, String> apps) {
        final boolean nearTB = isNearTBuilding();
        Log.i(LOG_TAG, "The device is " + (nearTB ? "" : "NOT") + " near T Building, Otaniemi");

        List<Map.Entry<String, String>> list = new LinkedList<>(apps.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                if (o1.getKey().equals(o2.getKey())) {
                    return 0;
                }
                boolean priorityApp1 = o1.getKey().equals(tBuildingPrioritizedApp);
                boolean priorityApp2 = o2.getKey().equals(tBuildingPrioritizedApp);
                if (priorityApp1 == priorityApp2) {
                    return 0;
                }
                return (nearTB ? 1 : -1) * (priorityApp1 ? -1 : 1);
            }
        });

        Map<String, String> sortedApps = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : list) {
            sortedApps.put(entry.getKey(), entry.getValue());
        }
        return sortedApps;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String clickedAppCode = mListKeys.get(position);
        String clickedAppLabel = mListLabels.get(position);
        Log.d(LOG_TAG, "Clicked " + clickedAppLabel + " (" + clickedAppCode + ")");

        showProgress(true);
        mAppStartTask = new AppStartTask(clickedAppCode, mAppToken);
        mAppStartTask.execute((Void) null);

//        // start external VNC viewer
//        String vncUrl = "vnc://myserver.example.com:5900/C24bit/MYPASSWORd";
//        AndroidUtils.startVncViewer(this, vncUrl);

//        startVncActivity();
    }

    private boolean isNearTBuilding() {
        if (mLocation == null) {
            mLocation = getLastLocation();
            if (mLocation == null) {
                return false;
            }
        }

        float distanceToTBuilding = mLocation.distanceTo(tBuildingLocation);
        Log.d(LOG_TAG, "Distance to T Building: " + distanceToTBuilding);
        return distanceToTBuilding < tBuildingProximityCircle;
    }

    private void startVncActivity(String address, int port, String password) {
        String username = "";
        COLORMODEL colorModel = COLORMODEL.C24bit;


        ConnectionBean conn = new ConnectionBean();

        conn.setAddress(address);
        conn.set_Id(0); // is new!!
        conn.setPort(port);
        conn.setUserName(username);
        conn.setPassword(password);
        conn.setKeepPassword(false);
        conn.setUseLocalCursor(true); // always enable
        conn.setColorModel(colorModel.nameString());
        conn.setUseRepeater(false);

        Log.i(LOG_TAG, "Starting NEW connection " + conn.toString());
        Intent intent = new Intent(this, VncCanvasActivity.class);
        intent.putExtra(com.coboltforge.dontmind.multivnc.Constants.CONNECTION, conn.Gen_getValues());
        startActivity(intent);
    }

    private void showErrorDialog(String title, String message) {
        AndroidUtils.showErrorDialog(this, title, message);
    }

    /**
     * Login task used to authenticate the user.
     */
    public class AppStartTask extends AsyncTask<Object, Object, VncConnectionDetails> {

        private final String mAppName;
        private final String mAppToken;
        private String errorMsg;

        AppStartTask(String appName, String appToken) {
            mAppName = appName;
            mAppToken = appToken;
        }

        @Override
        protected VncConnectionDetails doInBackground(Object... params) {
            Log.i(LOG_TAG, "Requesting to start app " + mAppName);

            errorMsg = null;
            String url = Constants.URL_START;
            Map<String, String> parameters = new HashMap<>(2);
            parameters.put("appName", mAppName);
            parameters.put("appToken", mAppToken);
            ConnectionUtils.Response response; // 30 sec
            try {
                response = ConnectionUtils.doPostAuth(url, parameters, 60000);
            } catch (ConnectionException e) {
                errorMsg = "Error connecting to the server, please try again later.";
                return null;
            } catch (ForbiddenException e) {
                errorMsg = "You are not authorized to use this resource.";
                return null;
            } catch (ServerErrorException e) {
                errorMsg = "The remote server experienced an error.";
                return null;
            }

            if (response == null) {
                return null;
            }

            try {
                String content = response.getContent().trim();
                JSONObject jo = new JSONObject(content);

                if (jo.has("ip") && jo.has("port_a") && jo.has("pass")) {
                    return new VncConnectionDetails(
                            jo.getString("ip"),
                            Integer.parseInt(jo.getString("port_a")),
                            jo.getString("pass"));
                } else {
                    return null;
                }

            } catch (JSONException e1) {
                Log.e(LOG_TAG, "Error occurred: " + e1.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final VncConnectionDetails connDetails) {
            mAppStartTask = null;
            showProgress(false);

            if (connDetails != null) {
                startVncActivity(
                        connDetails.ip,
                        connDetails.port,
                        connDetails.pass
                );

            } else {
                showErrorDialog("Error", errorMsg != null ? errorMsg : "Unknown error occurred");
            }
        }

        @Override
        protected void onCancelled() {
            mAppStartTask = null;
            showProgress(false);
        }
    }

    private static class VncConnectionDetails {
        final String ip;
        final int port;
        final String pass;

        VncConnectionDetails(String ip, int port, String pass) {
            this.ip = ip;
            this.port = port;
            this.pass = pass;
        }
    }

}
