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
import android.content.DialogInterface;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    EditText eNewUserName;
    EditText eNewPassword;
    EditText eNewUserEmail;
    TextView tNewUserNameError;
    TextView tNewUserEmailError;
    TextView tNewPasswordError;
    Button bRegister;
    String sNewUserName;
    String sNewPassword;
    String sNewUserEmail;
    String responseStatus;
    String responseError;

    JSONObject registerJSON = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        eNewUserName = (EditText) findViewById(R.id.eNewUserName);
        eNewPassword = (EditText) findViewById(R.id.eNewPassword);
        eNewUserEmail = (EditText) findViewById(R.id.eNewUserEmail);
        tNewUserNameError = (TextView) findViewById(R.id.tNewUserNameError);
        tNewUserEmailError = (TextView) findViewById(R.id.tNewUserEmailError);
        tNewPasswordError = (TextView) findViewById(R.id.tNewPasswordError);
        bRegister = (Button) findViewById(R.id.bRegister);

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
        sNewUserName = eNewUserName.getText().toString();
        sNewPassword = eNewPassword.getText().toString();
        sNewUserEmail = eNewUserEmail.getText().toString();

        switch (view.getId()) {
            case R.id.bRegister:
                if (validate()) {
                    registerUser(sNewUserName, sNewUserEmail, sNewPassword);
                }
                break;
        }
    }

    public boolean validate() {
        if (sNewUserName.isEmpty()) {
            tNewUserNameError.setText(("Username cannot be blank"));
            tNewUserNameError.setTextColor(Color.RED);
            eNewUserName.requestFocus();
            return false;
        } else if (sNewUserEmail.isEmpty()) {
            tNewUserNameError.setText("");
            tNewUserEmailError.setText(("User email cannot be blank"));
            tNewUserEmailError.setTextColor(Color.RED);
            eNewUserEmail.requestFocus();
            return false;
        } else if (sNewPassword.isEmpty()) {
            tNewUserNameError.setText("");
            tNewUserEmailError.setText("");
            tNewPasswordError.setText(("Password cannot be ignored"));
            tNewPasswordError.setTextColor(Color.RED);
            eNewPassword.requestFocus();
            return false;
        } else {
            return true;
        }
    }

    public void registerUser(String newUser, String newEmail, String newPassord) {
        String url = Settings.BACKEND_URL + "/register";
        try {
            registerJSON.put("newUser", newUser);
            registerJSON.put("newEmail", newEmail);
            registerJSON.put("newPassword", newPassord);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonReq = new JsonObjectRequest(Request.Method.POST, url, registerJSON, new Response.Listener<JSONObject>() {
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
                    AlertDialog.Builder registrationError = new AlertDialog.Builder(RegisterActivity.this);
                    registrationError.setMessage("Registration success. Please try login with your username.");
                    registrationError.setCancelable(true);
                    registrationError.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        }
                    });
                    registrationError.show();
                } else {
                    AlertDialog.Builder registrationError = new AlertDialog.Builder(RegisterActivity.this);
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

        if (id == R.id.nav_Rback) {
            startActivity(new Intent(this, LoginActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
