package projet.fst.ma.localisationn;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_ID = 100;
    private static final String LOG_TAG = "GeoTracker";
    private static final String SYNC_URL = "http://10.0.2.2/localisation/createPosition.php";

    private TextView latitudeDisplay, longitudeDisplay;
    private RequestQueue syncQueue;
    private LocationManager geoService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeDisplay = findViewById(R.id.txt_lat_lx);
        longitudeDisplay = findViewById(R.id.txt_lon_lx);
        Button openMapBtn = findViewById(R.id.btn_map_lx);

        syncQueue = Volley.newRequestQueue(getApplicationContext());
        geoService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        openMapBtn.setOnClickListener(v -> startActivity(new Intent(this, MapsActivity.class)));

        checkHardwarePermissions();
    }

    private void checkHardwarePermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQ_ID);
        } else {
            activateGpsStream();
        }
    }

    @SuppressLint("MissingPermission")
    private void activateGpsStream() {
        geoService.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, 0,
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location currentPos) {
                        double lat = currentPos.getLatitude();
                        double lon = currentPos.getLongitude();
                        latitudeDisplay.setText(getString(R.string.label_latitude) + ": " + String.format(Locale.getDefault(), "%.6f", lat));
                        longitudeDisplay.setText(getString(R.string.label_longitude) + ": " + String.format(Locale.getDefault(), "%.6f", lon));
                        pushPositionToServer(lat, lon);
                    }
                }
        );
    }

    private void pushPositionToServer(final double lat, final double lon) {
        StringRequest syncRequest = new StringRequest(
                Request.Method.POST,
                SYNC_URL,
                response -> {
                    Log.d(LOG_TAG, "Réponse : " + response);
                    Toast.makeText(getApplicationContext(), "Position envoyée : " + response, Toast.LENGTH_SHORT).show();
                },
                (VolleyError error) -> {
                    Log.e(LOG_TAG, "Erreur : " + error.toString());
                    Toast.makeText(getApplicationContext(), "Erreur réseau", Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> dataMap = new HashMap<>();
                dataMap.put("latitude", String.valueOf(lat));
                dataMap.put("longitude", String.valueOf(lon));
                dataMap.put("date_position", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                dataMap.put("imei", deviceId != null ? deviceId : "000000");
                return dataMap;
            }
        };
        syncQueue.add(syncRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            activateGpsStream();
        } else {
            Toast.makeText(this, "Permission GPS requise", Toast.LENGTH_LONG).show();
        }
    }
}