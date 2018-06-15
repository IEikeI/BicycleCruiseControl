package de.uni_hannover.hci.pcl.bicyclecruisecontrolmockapp.shortener;

// TODO: remove Apache library and use HttpURLConnection (Apache is deprecated since Android Marshmallow)  http://developer.android.com/preview/behavior-changes.html#behavior-apache-http-client

//import java.net.HttpURLConnection.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class BitlyShortenerAsyncTask extends ShortenerAsyncTask {

    // Constants
    private static final String kBitlyApiKey = "38bc9301550f6eeec36db33334701e3a551f580d";


    public BitlyShortenerAsyncTask(ShortenerListener listener) {
        super(listener);
    }

    @Override
    protected String doInBackground(String... urls) {
        try {
            String originalUrl = urls[0];
            String urlString = bitlyShorteningEndPoint(originalUrl);

            URL url = new URL ( urlString );
            URLConnection connection = url.openConnection();
            connection.connect();
            //HttpGet httpGet = new HttpGet(url);
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            //connection.setRequestProperty("Accept-Charset", charset);
            //HttpClient httpclient = new DefaultHttpClient();
            //HttpResponse response = httpclient.execute(httpGet);

            final int status = httpConnection.getResponseCode();
            if (status >= 200 && status < 300) {
              //  HttpEcntity entity = response.getEntity();
                InputStream response = connection.getInputStream();
                String data = response.toString();
                return data;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String bitlyShorteningEndPoint(String uri) throws UnsupportedEncodingException {
        String encodedUri = URLEncoder.encode(uri, "UTF-8");
        return String.format("https://api-ssl.bitly.com/v3/shorten?access_token=%s&longUrl=%s&format=txt", kBitlyApiKey, encodedUri);
    }
}
