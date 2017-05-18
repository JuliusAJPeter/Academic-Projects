package fi.aalto.cs.e4100.g09.project2;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Client to communicate with the cloud service.
 */
public class CloudClient {
    private static final String LOG_TAG = CloudClient.class.getSimpleName();
    private static final int WRITE_TIMEOUT_SEC = 30;
    private static final int READ_TIMEOUT_SEC = 60;

    private static CloudClient singleton;

    private final Context mContext;
    private final InputStream mServerCertificate;
    private final ContentResolver mContentResolver;
    private final Urls mUrls;
    private OkHttpClient mHttpClient;
    private String mAppToken;

    private List<Long> processingTime;
    private List<Long> bytesTransferred;
    private Long bytesTransferredTotal;

    private CloudClient(Context context, InputStream serverCertificate) {
        this.mContext = context;
        this.mServerCertificate = serverCertificate;
        this.mContentResolver = context.getContentResolver();
        this.mUrls = Urls.getInstance(context.getResources().getString(R.string.base_url));
    }

    private CloudClient(Context context, InputStream serverCertificate, String appToken) {
        this(context, serverCertificate);
        this.mAppToken = appToken;
    }

    public static CloudClient getInstance(Context context) {
        if (singleton == null) {
            singleton = new CloudClient(
                    context,
                    context.getResources().openRawResource(R.raw.server_cert)
            );
        }
        return singleton;
    }

    public static CloudClient getInstance(Context context, String appToken) {
        if (singleton == null) {
            singleton = new CloudClient(
                    context,
                    context.getResources().openRawResource(R.raw.server_cert),
                    appToken
            );
        }
        return singleton;
    }

    public boolean hasToken() {
        return (mAppToken != null && !mAppToken.isEmpty());
    }

    public String login(String username, String password) throws CloudConnectionException, ForbiddenException {

        String url = mUrls.login();

        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        Response response;
        try {
            response = getHttpClient().newCall(request).execute();
            Log.d(LOG_TAG, response.toString());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
            throw new CloudConnectionException(e);
        }

        if (!response.isSuccessful()) {
            int code = response.code();
            if (code == HttpURLConnection.HTTP_FORBIDDEN || code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new ForbiddenException("You are not authorized to access the specified resource.");
            } else if (code == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                throw new CloudConnectionException("Server error");
            } else if (code != HttpURLConnection.HTTP_OK) {
                throw new CloudConnectionException("Unknown error");
            }
        }

        try {
            String content = response.body().string().trim();
            JSONObject jo = new JSONObject(content);

            if (jo.has("token")) {
                // set the token for the client!
                mAppToken = jo.getString("token");
                return mAppToken;
            } else {
                return null;
            }

        } catch (JSONException | IOException e) {
            Log.e(LOG_TAG, "Error occurred: " + e.getMessage());
            throw new CloudConnectionException(e);
        }
    }

    public String requestOcr(Uri... images) throws CloudConnectionException {
        if (mAppToken == null) {
            throw new CloudConnectionException("App Token is empty!");
        }

        Log.i(LOG_TAG, "Sending request with appToken " + mAppToken + ", file " + images[0].toString());
        processingTime = new ArrayList<>(images.length);
        bytesTransferred = new ArrayList<>(images.length);

        OkHttpClient client = getHttpClient();

        String url = mUrls.imageOcr();
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("appToken", mAppToken);

        int i = 0;
        for (Uri uri : images) {
            int bytes = addFormDataPart(builder, "ocrPhoto", "image" + i, uri);
            bytesTransferred.add(i, (long) bytes);
            Log.d(LOG_TAG, "Bytes size for sending image" + i + ": " + bytes + " B");

            i++;
        }

        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();

        Response response;
        String responseContents;
        try {
            bytesTransferredTotal = request.body().contentLength();
            response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                Log.e(LOG_TAG, "Unsuccessful response code " + response.code());
                Log.e(LOG_TAG, "Response: " + response.body().string());
                throw new IOException("Unexpected code " + response);
            }

            ResponseBody body = response.body();
            responseContents = body.string().trim();
            bytesTransferredTotal += body.contentLength();
            body.close();

            JSONArray ja = new JSONArray(responseContents);
//            Log.d(LOG_TAG, "Returned: " + ja.toString());

            JSONObject jo;
            String text = "";
            for (i = 0; i < ja.length(); i++) {
                jo = ja.getJSONObject(i);
                int bytes = jo.toString().length();
                bytesTransferred.set(i, bytesTransferred.get(i) + bytes);
                Log.d(LOG_TAG, "Bytes size for receiving image" + i + ": " + bytes + " B");
                Log.d(LOG_TAG, "Total byte size for image" + i + ": " + bytesTransferred.get(i) + " B");

                if (jo.has("thumbnail") && jo.has("text") && jo.has("time")) {
                    text += jo.getString("text") + "\n";
                    String time = jo.getString("time"); // has suffix ms
                    processingTime.add(i, 1000000 * Long.parseLong(time.substring(0, time.length() - 2)));
                    Log.d(LOG_TAG, "time: " + time + " -> " + processingTime.get(i));

                } else {
                    return null;
                }
            }
            return text.trim();

        } catch (IOException | JSONException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
            throw new CloudConnectionException(e);
        }
    }

    public List<Long> getLastOcrProcessingTime() {
        return processingTime;
    }

    public List<Long> getLastOcrTransferBytes() {
        return bytesTransferred;
    }

    public Long getLastTotalTransferBytes() {
        return bytesTransferredTotal;
    }

    private int addFormDataPart(MultipartBody.Builder builder, String name, String filename, Uri uri) {
        Log.d(LOG_TAG, "Uri: " + uri);

        String contentType = mContentResolver.getType(uri);
        Log.d(LOG_TAG, "Content Type: " + contentType);

        MediaType mediaType;
        RequestBody requestBody;
        int bytesLength;

        // sometimes it doesn't work with URIs (picture captured)
        if (contentType == null) {
            File file = new File(uri.getPath());
            Log.d(LOG_TAG, "Created a file instead, exists? " + Boolean.toString(file.exists()));
            String extension = MimeTypeMap.getFileExtensionFromUrl(file.getPath());
            if (extension != null) {
                contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            mediaType = MediaType.parse(contentType);
            requestBody = RequestBody.create(mediaType, file);
            bytesLength = (int) file.length();
        } else {
            mediaType = MediaType.parse(contentType);
            byte[] contents;
            try {
                contents = AndroidUtils.getBytesFromContentUri(mContentResolver, uri);
                bytesLength = contents.length;
                requestBody = RequestBody.create(mediaType, contents);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getLocalizedMessage());
                return 0;
            }
        }

        builder.addFormDataPart(name, filename, requestBody);
        return bytesLength;
    }

    public List<HistoryAdapter.OcrResult> getHistory() throws CloudConnectionException {

        String url = mUrls.history();
        Request request = new Request.Builder()
                .url(url + "?appToken=" + mAppToken)
                .build();

        Response response;
        try {
            response = getHttpClient().newCall(request).execute();
            Log.d(LOG_TAG, response.toString());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
            throw new CloudConnectionException(e);
        }

        if (!response.isSuccessful()) {
            try {
                Log.e(LOG_TAG, response.body().string());
            } catch (IOException e) {/*ignore*/}
            throw new CloudConnectionException("Unexpected error code: " + response.code());
        }

        try {
            String contents = response.body().string().trim();

            // cache contents
            SharedPreferences cache = mContext.getSharedPreferences("CACHE", 0);
            SharedPreferences.Editor editor = cache.edit();
            editor.putString("history_json", contents);
            editor.commit();

            return parseHistoryJson(contents);

        } catch (JSONException | IOException e) {
            Log.e(LOG_TAG, "Error occurred: " + e.getMessage());
            throw new CloudConnectionException(e);
        }
    }

    public List<HistoryAdapter.OcrResult> getHistoryFromCache() throws CloudConnectionException {
        SharedPreferences cache = mContext.getSharedPreferences("CACHE", 0);
        String contentsJson = cache.getString("history_json", null);

        if (contentsJson == null) {
            return null;
        }

        try {
            return parseHistoryJson(contentsJson);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error occurred: " + e.getMessage());
            throw new CloudConnectionException(e);
        }
    }

    private List<HistoryAdapter.OcrResult> parseHistoryJson(String contents) throws JSONException {
        JSONArray ja = new JSONArray(contents);

        JSONObject jo;
        List<HistoryAdapter.OcrResult> history = new ArrayList<>();
        for (int i = 0; i < ja.length(); i++) {
            jo = ja.getJSONObject(i);
//            Log.d(LOG_TAG, "? "+jo.has("thumbnail") + jo.has("text") + jo.has("createTs"));
            if (jo.has("thumbnail") && jo.has("text") && jo.has("sourceID") && jo.has("createTs")) {
                String createTs = jo.getString("createTs");
                //                                         11-12-2016-14.16.50.456
                SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss.SSS");
                f.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
                Date date;
                try {
                    date = f.parse(createTs);
                } catch (ParseException e) {
                    Log.e(LOG_TAG, "Unable to parse date string " + createTs + ", reason: " + e.getLocalizedMessage());
                    date = null;
                }
                Log.d(LOG_TAG, date.toString());
                history.add(0, new HistoryAdapter.OcrResult(
                        jo.getString("thumbnail"),
                        jo.getString("text"),
                        jo.getString("sourceID"),
                        date
                ));
            }
        }

        return history;
    }

    public Bitmap getSourceImage(String id) throws CloudConnectionException {
        String url = mUrls.source();
        Request request = new Request.Builder()
                .url(url + "?appToken=" + mAppToken+"&id="+id)
                .build();

        Response response;
        try {
            response = getHttpClient().newCall(request).execute();
            Log.d(LOG_TAG, response.toString());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getLocalizedMessage());
            throw new CloudConnectionException(e);
        }

        if (!response.isSuccessful()) {
            try {
                Log.e(LOG_TAG, response.body().string());
            } catch (IOException e) {/*ignore*/}
            throw new CloudConnectionException("Unexpected error code: " + response.code());
        }

        try {
            String contents = response.body().string().trim();

            JSONArray ja = new JSONArray(contents);
            JSONObject jo = ja.getJSONObject(0);
            if (jo.has("source")) {
                return AndroidUtils.decodeB64Bitmap(jo.getString("source"));
            }

        } catch (JSONException | IOException e) {
            Log.e(LOG_TAG, "Error occurred: " + e.getMessage());
            throw new CloudConnectionException(e);
        }

        return null;
    }

    private SSLContext getSslContext() throws CloudConnectionException {
        try {
            return getSslContextForCertificate(mServerCertificate);
        } catch (GeneralSecurityException | IOException e) {
            throw new CloudConnectionException(e);
        }
    }

    private static SSLContext getSslContextForCertificate(InputStream certificateContents)
            throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate ca;
        try {
            ca = cf.generateCertificate(certificateContents);
            Log.i(LOG_TAG, "Certificate=" + ((X509Certificate) ca).getSubjectDN());
        } finally {
            certificateContents.close();
        }

        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Create an SSLContext that uses our TrustManager
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);

        return context;
    }

    private OkHttpClient getHttpClient() throws CloudConnectionException {
        if (mHttpClient == null) {
            SSLContext sslContext = getSslContext();

            mHttpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory()) // TODO ?
                    .hostnameVerifier(new NullHostNameVerifier())
                    .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .build();
        }

        return mHttpClient;
    }

    private static class NullHostNameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            Log.i("RestUtilImpl", "Approving certificate for " + hostname);
            return true;
        }
    }

    public static class CloudConnectionException extends Exception {
        public CloudConnectionException(String message) {
            super(message);
        }

        public CloudConnectionException(Throwable cause) {
            super(cause);
        }
    }

}
