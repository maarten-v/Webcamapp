package com.webcam.maartenv.webcamapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.loopj.android.http.*;

import java.security.KeyStore;

import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;
import cz.msebera.android.httpclient.Header;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String url = sharedPref.getString("pref_url", "");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ImageView screenshot = (ImageView) findViewById(R.id.screenshot);

        AsyncHttpClient client = createClient(this);

        client.get(url+"axis-cgi/jpg/image.cgi", new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length);
                screenshot.setImageBitmap(bitmap);
                hideProgessbar();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                hideProgessbar();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static AsyncHttpClient createClient(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String username = sharedPref.getString("pref_username", "");
        String password = sharedPref.getString("pref_password", "");

        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(username,password);

        KeyStore trustStore;
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            client.setSSLSocketFactory(sf);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return client;
    }

    public void webcamOn(View view) {
        Log.i("webcamOn", "start");
        showProgressbar();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String url = sharedPref.getString("pref_url", "");
        String eventid = sharedPref.getString("pref_event_id", "");
        connect(url+"axis-cgi/param.cgi?action=update&Event.E"+eventid+".Starttime=00:00&Event.E"+eventid+".Duration=24:00");
    }

    public void webcamOff(View view) {
        Log.i("webcamOff", "start");
        showProgressbar();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String url = sharedPref.getString("pref_url", "");
        String eventid = sharedPref.getString("pref_event_id", "");
        connect(url+"axis-cgi/param.cgi?action=update&Event.E"+eventid+".Starttime=01:00&Event.E"+eventid+".Duration=7:00");
    }

    public void status(View view) {
        Log.i("status", "start");
        showProgressbar();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String url = sharedPref.getString("pref_url", "");
        String eventid = sharedPref.getString("pref_event_id", "");
        connect(url+"axis-cgi/param.cgi?action=list&group=Event.E"+eventid);
    }

    public void refreshImage(View view) {
        showProgressbar();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String url = sharedPref.getString("pref_url", "");
        final ImageView screenshot = (ImageView) findViewById(R.id.screenshot);

        AsyncHttpClient client = createClient(this);

        client.get(url + "axis-cgi/jpg/image.cgi", new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length);
                screenshot.setImageBitmap(bitmap);
                hideProgessbar();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                TextView statusview = (TextView) findViewById(R.id.textStatus);
                statusview.setText("mislukt");
                hideProgessbar();
            }
        });
    }

    public void connect(String url) {
        Log.i("connect", "start");
        final TextView statusview = (TextView) findViewById(R.id.textStatus);
        statusview.setText("");
        AsyncHttpClient client = createClient(this);

        client.get(url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                String response = new String(responseBody);

                if (response.equals("OK")) {
                    status(null);
                } else {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String eventid = sharedPref.getString("pref_event_id", "");
                    String[] lines = response.split(System.getProperty("line.separator"));
                    String result = null;
                    for(int i =0; i < lines.length; i++) {
                        if (lines[i].startsWith("root.Event.E"+eventid+".Starttime") ) {
                            String[] split = lines[i].split("=");
                            result = "Starttijd: " + split[1] + "\n" ;
                        } else if (lines[i].startsWith("root.Event.E"+eventid+".Duration")) {
                            String[] split = lines[i].split("=");
                            result = result + "Duur: " + split[1] ;
                        }
                    }
                    statusview.setText(result);
                    hideProgessbar();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                TextView statusview = (TextView) findViewById(R.id.textStatus);
                statusview.setText("Mislukt :(");
                hideProgessbar();
            }
        });
    }

    public void showProgressbar() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgessbar() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
    }
}
