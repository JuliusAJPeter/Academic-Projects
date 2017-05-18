package fi.aalto.cs.e4100.g09.project1;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;

/**
 *
 */

public class ConnectionUtils {
    private static final String LOG_TAG = ConnectionUtils.class.getSimpleName();

    public static Response doPostAuth(String url, Map<String,String> parameters, int readTimeoutMilliseconds)
            throws ConnectionException, ForbiddenException, ServerErrorException {
        URL urlInstance;
        try {
            urlInstance = new URL(url);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "MalformedURLException - this shouldn't happen... "+e.getMessage());
            throw new ConnectionException("Problem connecting to the backend server, please contact the developers.");
        }

        return doPostAuth(urlInstance, parameters, readTimeoutMilliseconds);
    }

    public static Response doPostAuth(URL url, Map<String,String> parameters, int readTimeoutMilliseconds)
            throws ConnectionException, ForbiddenException, ServerErrorException {

        OutputStream os = null;
        BufferedWriter writer = null;

        Response response = null;

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(readTimeoutMilliseconds);
            connection.connect();

            // send POST data
            Uri.Builder builder = new Uri.Builder();
            for (Map.Entry<String,String> e : parameters.entrySet()) {
                builder.appendQueryParameter(e.getKey(), e.getValue());
            }
            String query = builder.build().getEncodedQuery();

            os = connection.getOutputStream();
            writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();

            int code = connection.getResponseCode();
            Log.i(LOG_TAG, "Response code: " + code);

            if (code == HttpURLConnection.HTTP_FORBIDDEN || code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                throw new ForbiddenException("You are not authorized to access the specified resource.");
            } else if (code == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                throw new ServerErrorException("Server error");
            } else if (code != HttpURLConnection.HTTP_OK) {
                throw new ConnectionException("Unknown error");
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String content = "", line;
            while ((line = rd.readLine()) != null) {
                content += line + "\n";
            }
            Log.d(LOG_TAG, "Received: " + content);

            response = new Response(code, content);

        } catch (SocketTimeoutException e) {
            Log.e(LOG_TAG, "SocketTimeoutException: " + (e.getMessage() != null ? e.getMessage() : "Connection timeout"));
            throw new ConnectionException("No response from the server (connection timeout).");

        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage() != null ? e.getMessage() : "Unknown response from the server");
            throw new ConnectionException("Error connecting to the backend server, please try again later.");

        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {/*ignore*/}
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e) {/*ignore*/}
            }
        }

        return response;
    }

    public static class Response {
        private final int code;
        private final String content;

        public Response(int code, String content) {
            this.code = code;
            this.content = content;
        }

        public int getCode() {
            return code;
        }

        public String getContent() {
            return content;
        }
    }
}
