package com.mspproject.aalto.maprunr;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import android.widget.SimpleAdapter;
import android.widget.ArrayAdapter;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UserActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String USERNAME = "com.mspproject.aalto.maprunr.username";
    TextView tUserUserName;
    String sUSERNAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tUserUserName = (TextView) findViewById(R.id.tUserUserName);
        Intent userIntent = getIntent();

        sUSERNAME = userIntent.getStringExtra(LoginActivity.USERNAME);
        if (sUSERNAME.equals(null)) {
            sUSERNAME = userIntent.getStringExtra(MapActivity.USERNAME);
            if (sUSERNAME.equals(null)){
                sUSERNAME = userIntent.getStringExtra(ViewActivity.USERNAME);
            }
        }
        tUserUserName.setText(("Hello ").concat(sUSERNAME));

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

        if (id == R.id.nav_start) {
            // Handle the camera action
            Intent mapIntent = new Intent(UserActivity.this, MapActivity.class);
            mapIntent.putExtra(USERNAME, sUSERNAME);
            startActivity(mapIntent);
        } else if (id == R.id.nav_logout) {
            logout();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        String url = Settings.BACKEND_URL + "/logout";
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonReq = new JsonObjectRequest(Request.Method.POST, url,
                new JSONObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObj) {
                startActivity(new Intent(UserActivity.this, LoginActivity.class));
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError volleyErr){
                Log.e("HTTP error", volleyErr.toString());
            }
        });
        queue.add(jsonReq);
    }

    private void fetchData() {
        String url = Settings.BACKEND_URL + "/userdata";
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonReq = new JsonObjectRequest(Request.Method.GET, url,
                new JSONObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObj) {
                ((TextView) findViewById(R.id.tUserUserName))
                        .setText(sUSERNAME + ", you control " + jsonObj.optInt("points") + " points!");
                createRunList(jsonObj.optJSONArray("recent"), "Recent activity", (ListView) findViewById(R.id.listRecent), false);
                createRunList(jsonObj.optJSONArray("highscore"), "Highscores", (ListView) findViewById(R.id.listHighscores), true);
                createPointHighscore(jsonObj.optJSONArray("pointsHighscore"));
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError volleyErr){
                Log.e("HTTP error", volleyErr.toString());
            }
        });
        queue.add(jsonReq);
    }

    private void createPointHighscore(JSONArray points) {
        List<Map<String, String>> data = new ArrayList<>();
        for (int i = 0; i < points.length(); i++) {
            JSONArray pointElement = points.optJSONArray(i);
            String username = pointElement.optString(0);
            int pointCount = pointElement.optInt(1);
            Map<String, String> obj = new HashMap<>(2);
            obj.put("username", username);
            obj.put("points", Integer.toString(pointCount) + " points!");
            data.add(obj);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2,
                new String[] { "username", "points" }, new int[] { android.R.id.text1,
                android.R.id.text2 });

        ((ListView) findViewById(R.id.listPointHighscores)).setAdapter(adapter);
    }

    private void createRunList(JSONArray runs, String title, ListView targetElement, boolean showUsername) {

        List<Map<String, String>> data = new ArrayList<>();
        for (int i = 0; i < runs.length(); i++) {
            JSONObject runElement = (JSONObject) runs.optJSONObject(i);
            String runTitle = runElement.optString("title");
            String distance = runElement.optString("distance");
            String username = runElement.optString("UserUsername");
            String id = Integer.toString(runElement.optInt("id"));
            String date = "Date not available";
            String[] dateArray = runElement.optString("createdAt").split("T");
            if(dateArray.length == 2) {
                date = dateArray[0];
            }
            Map<String, String> obj = new HashMap<>(3);
            obj.put("title", runTitle);
            obj.put("text", (showUsername ? username + " / " : "") + distance + "m / " + date);
            obj.put("id", id);
            data.add(obj);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2,
                new String[] { "title", "text" }, new int[] { android.R.id.text1,
                android.R.id.text2 });

        targetElement.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id){
                Map<String, String> map = (Map<String, String>) adapter.getItemAtPosition(position);
                Intent intent = new Intent(UserActivity.this, ViewActivity.class);
                intent.putExtra(USERNAME, sUSERNAME);
                intent.putExtra("id", map.get("id"));
                startActivity(intent);
            }
        });

        targetElement.setAdapter(adapter);
    }
}


