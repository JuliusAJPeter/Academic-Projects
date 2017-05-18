package fi.aalto.cs.e4100.g09.project1.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import fi.aalto.cs.e4100.g09.project1.AndroidUtils;
import fi.aalto.cs.e4100.g09.project1.ConnectionException;
import fi.aalto.cs.e4100.g09.project1.ConnectionUtils;
import fi.aalto.cs.e4100.g09.project1.Constants;
import fi.aalto.cs.e4100.g09.project1.ForbiddenException;
import fi.aalto.cs.e4100.g09.project1.R;
import fi.aalto.cs.e4100.g09.project1.ServerErrorException;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends LocationAwareActivity {

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    private final Activity mActivity = this;

    // UI references
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: remove, just for testing
//        HashMap<String,String> apps = new HashMap<>(1);
//        apps.put("t","TEST APP");
//        AppsListActivity.start(this, apps);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        Button emailSignInButton = (Button) findViewById(R.id.email_sign_in_button);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        emailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showErrorDialog(String title, String message) {
        AndroidUtils.showErrorDialog(this, title, message);
    }

    /**
     * Login task used to authenticate the user.
     */
    public class UserLoginTask extends AsyncTask<Object, Object, HashMap<String, String>> {

        private final String mUsername;
        private final String mPasswordHash;
        private String mAppToken = null;
        private String errorMsg;
        private boolean incorrectPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            // cannot use DigestUtils.sha1Hex(password)  cf. http://stackoverflow.com/a/9284092
            mPasswordHash = new String(Hex.encodeHex(DigestUtils.sha(password)));
        }

        @Override
        protected HashMap<String, String> doInBackground(Object... params) {
            Log.i(LOG_TAG, "Trying to log in as " + mUsername + ":" + mPasswordHash +
                    " from " + (getLastLocation() == null ? "unknown location" : getLastLocation().toString()));

            errorMsg = null;
            incorrectPassword = false;
            String url = Constants.URL_LOGIN;
            Map<String, String> parameters = new HashMap<>(2);
            parameters.put("username", mUsername);
            parameters.put("password", mPasswordHash);
            ConnectionUtils.Response response;

            try {
                response = ConnectionUtils.doPostAuth(url, parameters, 5000); // 5sec
            } catch (ConnectionException e) {
                errorMsg = "Error connecting to the server, please try again later.";
                return null;
            } catch (ForbiddenException e) {
                incorrectPassword = true;
                errorMsg = "Unable to login.";
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

                if (jo.has("token")) {
                    mAppToken = jo.getString("token");
                } else {
                    return null;
                }

            } catch (JSONException e1) {
                Log.e(LOG_TAG, "Error occurred: " + e1.getMessage());
                return null;
            }

            url = Constants.URL_LIST;
            parameters = new HashMap<>(1);
            parameters.put("appToken", mAppToken);
            try {
                response = ConnectionUtils.doPostAuth(url, parameters, 5000); // 5sec
            } catch (ConnectionException e) {
                errorMsg = "Error connecting to the server, please try again later.";
                return null;
            } catch (ForbiddenException e) {
                errorMsg = "You are not authorized to access this resource.";
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
                JSONArray ja = new JSONArray(content);

                HashMap<String, String> apps = new HashMap<>(ja.length());
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    String key = jo.getString("name");
                    apps.put(key, jo.getString("label"));
                }

                return apps;

            } catch (JSONException e1) {
                Log.e(LOG_TAG, "Error occurred: " + e1.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(final HashMap<String, String> appsList) {
            mAuthTask = null;
            showProgress(false);

            if (appsList != null) {
                AppsListActivity.start(mActivity, mAppToken, appsList, getLastLocation());

            } else {
                if (incorrectPassword) {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                } else {
                    showErrorDialog("Error", errorMsg != null ? errorMsg : "Unknown error occurred");
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

}
