package com.mspproject.aalto.maprunr;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONObject;

public class ViewActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    public static final String USERNAME = "com.mspproject.aalto.maprunr.username";
    TextView tUserUserName;
    String sUSERNAME;
    String ID;
    GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent viewIntent = getIntent();
        sUSERNAME = viewIntent.getStringExtra(ViewActivity.USERNAME);
        ID = viewIntent.getStringExtra("id");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        //ImageView drawerImage = (ImageView) headerView.findViewById(R.id.drawer_image);
        TextView tUsername = (TextView) headerView.findViewById(R.id.tProfileName);
        //drawerImage.setImageDrawable(R.drawable.ic_user);
        tUsername.setText(sUSERNAME);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.viewMap);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        fetchData();
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

        if (id == R.id.vBack) {
            Intent mapIntent = new Intent(ViewActivity.this, UserActivity.class);
            mapIntent.putExtra(USERNAME, sUSERNAME);
            startActivity(mapIntent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void fetchData() {
        String url = Settings.BACKEND_URL + "/run/" + ID;
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
        PolygonOptions pOptions = new PolygonOptions();
        for (int i = 0; i < points.length(); i++) {
            JSONObject point = points.optJSONObject(i);
            LatLng latLng = new LatLng(point.optDouble("lat"), point.optDouble("lng"));
            pOptions.add(latLng);
            if(i == 0) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        }
        mMap.addPolygon(pOptions
                .fillColor(Color.argb(120, 135, 206, 250))
                .strokeColor(Color.parseColor("#87CEFA"))
                .strokeWidth(1));
    }
}
