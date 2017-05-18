package com.mspproject.aalto.maprunr;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final String USERNAME = "com.mspproject.aalto.maprunr.username";
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    String sUSERNAME, mTitle;
    GoogleMap mMap;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLocationCoordinates;
    Marker mCurrLocationMarker;
    List<Location> mCoordinates = new ArrayList<>();
    double totalDistance;
    JSONObject pointsJSON = new JSONObject();
    JSONArray latLng = new JSONArray();
    String responseStatus;
    String responseError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Intent userIntent = getIntent();
        sUSERNAME = userIntent.getStringExtra(UserActivity.USERNAME);

        View headerView = navigationView.getHeaderView(0);
        //ImageView drawerImage = (ImageView) headerView.findViewById(R.id.drawer_image);
        TextView tUsername = (TextView) headerView.findViewById(R.id.tProfileName);
        //drawerImage.setImageDrawable(R.drawable.ic_user);
        tUsername.setText(sUSERNAME);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_finish) {
            // Handle the camera action
            if (CheckCoordinates()) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Congratulations!");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    builder.setMessage(Html.fromHtml("Covered area conquered by running <b>"
                            + String.valueOf(Math.round(totalDistance)) + "m</b>.", Html.FROM_HTML_MODE_LEGACY));
                } else {
                    builder.setMessage(Html.fromHtml("Covered area conquered by running <b>"
                            + String.valueOf(Math.round(totalDistance)) + "m</b>."));
                }
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getUserTitle();
                    }
                });
                builder.show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Attention!")
                        .setMessage("Sorry... You are not close enough to the starting point to conquer!")
                        .setPositiveButton("OK", null)
                        .create()
                        .show();
            }
        } else if (id == R.id.nav_map_back) {
            //startActivity(new Intent(this, UserActivity.class));
            Intent userIntent = new Intent(MapActivity.this, UserActivity.class);
            userIntent.putExtra(USERNAME, sUSERNAME);
            startActivity(userIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void getUserTitle() {
        android.app.AlertDialog.Builder dialogTitle = new android.app.AlertDialog.Builder(this);
        dialogTitle.setTitle("Save conquered area as...");
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog, null);
        dialogTitle.setView(dialogView);
        final EditText mInput = (EditText) dialogView.findViewById(R.id.mTitle);
        dialogTitle.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mTitle = mInput.getText().toString();
                NavigationView nView = (NavigationView) findViewById(R.id.nav_view);
                Menu mMenu = nView.getMenu();
                MenuItem nFinish = mMenu.findItem(R.id.nav_finish);
                nFinish.setEnabled(false);
                saveArea();
            }
        });
        dialogTitle.show();
    }

    public void saveArea() {
        String url = Settings.BACKEND_URL + "/save";
        try {
            pointsJSON.put("username", sUSERNAME);
            pointsJSON.put("title", mTitle);
            pointsJSON.put("distance", Math.round(totalDistance));
            pointsJSON.put("arrayLen", mCoordinates.size());
            pointsJSON.put("latLng", latLng);
            Log.i("poinstJson", pointsJSON.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonReq = new JsonObjectRequest(Request.Method.POST, url, pointsJSON, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObj) {
                Log.e("DEBUG", jsonObj.toString());
                try {
                    responseStatus = jsonObj.getString("status");
                    responseError = jsonObj.getString("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (responseStatus.equals("failure")) {
                    AlertDialog.Builder registrationError = new AlertDialog.Builder(MapActivity.this);
                    registrationError.setMessage(responseError);
                    registrationError.setCancelable(true);
                    registrationError.setPositiveButton("OK", null);
                    registrationError.show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyErr) {
                Log.e("HTTP error", volleyErr.toString());
            }
        });
        queue.add(jsonReq);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                mMap.setMyLocationEnabled(true);
                buildGoogleApiClient();

            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.setMyLocationEnabled(true);
            buildGoogleApiClient();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000); //1 second
        mLocationRequest.setFastestInterval(1000); //1 second
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //Place current location marker
        Location mStartLoc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mStartLoc != null) {
            mMap.clear();
            mapMarker(mMap, mStartLoc, "Starting point", true);
            saveLocation(mStartLoc);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        fetchPoints();
    }

    public void saveLocation(Location mLocation) {
        mCoordinates.add(mLocation);
        //Log.i("Coordinates", mLocation.toString());
        mLocationCoordinates = mLocation;
        try {
            JSONObject pointsArray = new JSONObject();
            pointsArray.put("lat", mLocationCoordinates.getLatitude());
            pointsArray.put("lng", mLocationCoordinates.getLongitude());
            latLng.put(pointsArray);
        } catch(JSONException e){
            e.printStackTrace();
        }

    }

    public boolean CheckCoordinates() {
        Log.i("Start", mCoordinates.get(0).toString());
        Log.i("Finish", mCoordinates.get(mCoordinates.size() - 1).toString());
        double distance = findDistance(0, mCoordinates.size() - 1);
        //distance = Math.pow(distance, 2);
        Log.i("Distance in meters", String.valueOf(distance));
        if (mCoordinates.size() < 3) {
            return false;
        }
        if (distance < 50) {
            PolygonOptions pOptions = new PolygonOptions();
            for (int i = 0; i < mCoordinates.size(); i++) {
                pOptions.add(new LatLng(getCoordinates(i).getLatitude(), getCoordinates(i).getLongitude()));
            }
            mMap.addPolygon(pOptions
                    .fillColor(Color.argb(120, 135, 206, 250))
                    .strokeColor(Color.parseColor("#87CEFA"))
                    .strokeWidth(1));
            for (int i = 0; i < mCoordinates.size() - 1; i++) {
                totalDistance = totalDistance + findDistance(i, i + 1);
            }
            return true;
        } else {
            return false;
        }
    }

    public double findDistance(int startPoint, int endPoint) {
        double longDistance = Math.toRadians(mCoordinates.get(endPoint).getLongitude() -
                mCoordinates.get(startPoint).getLongitude());
        double latDistance = Math.toRadians(mCoordinates.get(endPoint).getLatitude() -
                mCoordinates.get(startPoint).getLatitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(mCoordinates.get(endPoint).getLatitude())) *
                Math.cos(Math.toRadians(mCoordinates.get(startPoint).getLatitude()))
                * Math.sin(longDistance / 2) * Math.sin(longDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c * 1000;
    }

    public Location getCoordinates(int index) {
        return mCoordinates.get(index);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        /*if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }*/
        //Place current location marker
        mapMarker(mMap, location, "Intermediate", false);
        //mCurrLocationMarker.setVisible(false);
        saveLocation(location);
        //Draw route
        /*
        Polyline line = mMap.addPolyline(new PolylineOptions()
                .add(new LatLng(getCoordinates(mCoordinates.size() - 1).getLatitude(), getCoordinates(mCoordinates.size() - 1).getLongitude())
                        , new LatLng(location.getLatitude(), location.getLongitude()))
                .width(5)
                .color(Color.RED));

        if (mCoordinates.size() > 2){
            Polygon polygon = mMap.addPolygon(new PolygonOptions()
                    .add(new LatLng(getCoordinates(0).getLatitude(), getCoordinates(0).getLongitude()),
                            new LatLng(getCoordinates(mCoordinates.size()-1).getLatitude(), getCoordinates(mCoordinates.size()-1).getLongitude()),
                            new LatLng(getCoordinates(mCoordinates.size()-2).getLatitude(), getCoordinates(mCoordinates.size()-2).getLongitude()),
                            new LatLng(getCoordinates(mCoordinates.size()-3).getLatitude(), getCoordinates(mCoordinates.size()-3).getLongitude()))
                    .fillColor(Color.parseColor("#87CEFA"))
                    .strokeColor(Color.parseColor("#87CEFA"))
                    .strokeWidth(1));
        }
        */
        /*
        //optionally, stop location updates if only current location is needed
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }*/
    }

    public void mapMarker(GoogleMap mGoogleMap, Location mLocation, String mTitle, boolean setZoom) {
        LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(mTitle);
        //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);
        if (mTitle.equals("Intermediate")) {
            mCurrLocationMarker.remove();
        }

        //move map camera
        if(setZoom) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
        } else {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapActivity.this,
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                //return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void fetchPoints() {
        String url = Settings.BACKEND_URL + "/points";
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonReq = new JsonObjectRequest(Request.Method.GET, url,
                new JSONObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObj) {
                drawPoints(jsonObj.optJSONArray("points"));
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError volleyErr){
                Log.e("HTTP error", volleyErr.toString());
            }
        });
        queue.add(jsonReq);
    }

    private void drawPoints(JSONArray points) {
        for(int i = 0; i < points.length(); i++) {
            JSONObject point = points.optJSONObject(i);
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(point.optDouble("lat"), point.optDouble("lng")));
            String username = point.optString("UserUsername");
            if(!username.equals("null")) {
                options.title(username);
            }
            float color;
            if(username.equals(sUSERNAME)) {
                color = BitmapDescriptorFactory.HUE_GREEN;
            } else {
                color = BitmapDescriptorFactory.HUE_YELLOW;
            }
            options.icon(BitmapDescriptorFactory.defaultMarker(color));
            mMap.addMarker(options);
        }
    }
}
