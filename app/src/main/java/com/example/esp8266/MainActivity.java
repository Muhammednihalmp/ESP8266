package com.example.esp8266;

import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String ESP_IP = "http://192.168.4.1";
    private boolean isLedOn = false;
    private Button btnToggle;
    private TextView tvStatus;
    private TextView tvConnection;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager connectivityManager;
    private Network espNetwork = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnToggle = findViewById(R.id.btnToggle);
        tvStatus = findViewById(R.id.tvStatus);
        tvConnection = findViewById(R.id.tvConnection);

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        // Force app to use WiFi network (not mobile data)
        bindToWifiNetwork();

        btnToggle.setOnClickListener(v -> toggleLed());
    }

    // This forces Android to route traffic through WiFi even if mobile data is active
    private void bindToWifiNetwork() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                espNetwork = network;
                mainHandler.post(() ->
                        tvConnection.setText("WiFi connected ✓")
                );
            }

            @Override
            public void onLost(Network network) {
                espNetwork = null;
                mainHandler.post(() ->
                        tvConnection.setText("WiFi disconnected ✗")
                );
            }
        });
    }

    private void toggleLed() {
        btnToggle.setEnabled(false);
        tvStatus.setText("Sending...");

        String endpoint = isLedOn ? "/off" : "/on";

        executor.execute(() -> {
            String result = sendRequest(ESP_IP + endpoint);
            mainHandler.post(() -> {
                btnToggle.setEnabled(true);
                if (result != null) {
                    isLedOn = !isLedOn;
                    updateUI();
                } else {
                    tvStatus.setText("❌ Cannot reach ESP8266\n\nMake sure:\n• Connected to ESP8266_LED WiFi\n• ESP8266 is powered on");
                }
            });
        });
    }

    private void updateUI() {
        if (isLedOn) {
            btnToggle.setText("Turn OFF");
            tvStatus.setText("💡 LED is ON");
            btnToggle.setBackgroundColor(Color.parseColor("#2196F3"));
        } else {
            btnToggle.setText("Turn ON");
            tvStatus.setText("⚫ LED is OFF");
            btnToggle.setBackgroundColor(Color.parseColor("#9E9E9E"));
        }
    }

    private String sendRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn;

            // Use the bound WiFi network if available
            if (espNetwork != null) {
                conn = (HttpURLConnection) espNetwork.openConnection(url);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setUseCaches(false);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200 ? "OK" : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}