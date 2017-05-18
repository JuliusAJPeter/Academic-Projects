package fi.aalto.cs.e4100.g09.project1.app;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.Date;

/**
 * Activity connected to the Google Location API and simplifying access to the last known location.
 */
public abstract class LocationAwareActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String LOG_TAG = LocationAwareActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;


    protected Location getLastLocation() {
        //noinspection MissingPermission
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(LOG_TAG, "onConnected()");

        //noinspection MissingPermission
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (lastLocation == null) {
            Log.w(LOG_TAG, "last location is UNKNOWN!");
        } else {
            Log.d(LOG_TAG, String.format("last location: https://maps.google.com/maps?q=%f%%2C%f %s (at %s)",
                    lastLocation.getLatitude(), lastLocation.getLongitude(), lastLocation.toString(),
                    new Date(lastLocation.getTime()).toString()));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "onConnectionSuspended()");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "onConnectionFailed()");
    }
}
