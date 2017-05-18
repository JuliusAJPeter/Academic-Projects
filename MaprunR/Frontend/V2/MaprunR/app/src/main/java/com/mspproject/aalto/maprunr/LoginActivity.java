package com.mspproject.aalto.maprunr;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    public static final String USERNAME = "com.mspproject.aalto.maprunr.username";
    Button bLogin;
    Button bRegister;
    EditText eUserName;
    EditText ePassword;
    TextView tUserNameError;
    TextView tPasswordError;
    String sUserName;
    String sPassword;
    String responseStatus;
    String responseError;
    int backButtonCount = 0;

    JSONObject loginJSON = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bLogin = (Button) findViewById(R.id.bLogin);
        bRegister = (Button) findViewById(R.id.bRegister);
        eUserName = (EditText) findViewById(R.id.eUserName);
        ePassword = (EditText) findViewById(R.id.ePassword);
        tUserNameError = (TextView) findViewById(R.id.tUserNameError);
        tPasswordError = (TextView) findViewById(R.id.tPasswordError);

        bLogin.setOnClickListener(this);
        bRegister.setOnClickListener(this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onClick(View view) {
        sUserName = eUserName.getText().toString();
        sPassword = ePassword.getText().toString();

        switch (view.getId()) {
            case R.id.bLogin:
                if (validate()) {
                    authenticate(sUserName, sPassword);
                }
                break;

            case R.id.bRegister:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
        }
    }

    public boolean validate() {
        if (sUserName.isEmpty()) {
            tUserNameError.setText(("Username cannot be blank"));
            tUserNameError.setTextColor(Color.RED);
            eUserName.requestFocus();
            return false;
        } else {
            tUserNameError.setText("");
            if (sPassword.isEmpty()) {
                tPasswordError.setText(("Password cannot be ignored"));
                tPasswordError.setTextColor(Color.RED);
                ePassword.requestFocus();
                return false;
            }
            return true;
        }
    }

    public void authenticate(String authUser, String authPass) {
        String url = Settings.BACKEND_URL + "/login";
        try {
            loginJSON.put("loginUser", authUser);
            loginJSON.put("loginPass", authPass);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonReq = new JsonObjectRequest(Request.Method.POST, url, loginJSON, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObj) {
                Log.e("DEBUG", jsonObj.toString());
                try {
                    responseStatus = jsonObj.getString("status");
                    responseError = jsonObj.getString("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (responseStatus.equals("success")) {
                    Intent userIntent = new Intent(LoginActivity.this, UserActivity.class);
                    userIntent.putExtra(USERNAME, sUserName);
                    startActivity(userIntent);
                } else {
                    AlertDialog.Builder loginError = new AlertDialog.Builder(LoginActivity.this);
                    loginError.setMessage(responseError);
                    loginError.setCancelable(true);
                    loginError.setPositiveButton("OK", null);
                    loginError.show();
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
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
            if(backButtonCount >= 1)
            {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            else
            {
                Toast.makeText(this, "Press the back button once again to close the application.", Toast.LENGTH_SHORT).show();
                backButtonCount++;
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_about_us) {
            // Handle the camera action
        } else if (id == R.id.nav_loginExit) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
