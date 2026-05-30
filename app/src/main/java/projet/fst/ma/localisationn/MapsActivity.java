package projet.fst.ma.localisationn;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap m_Map;
    private RequestQueue request__Queuee;
    private LocationManager location__Managerr;
    private Marker current_Markerr;

    private static final String TAGGer = "MapsActivity";
    private static final String URL_GETT = "http://10.0.2.2/localisation/showPositions.php";
    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        request__Queuee = Volley.newRequestQueue(this);
        location__Managerr = (LocationManager) getSystemService(LOCATION_SERVICE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        m_Map = googleMap;

        try {
            boolean success = m_Map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Log.e(TAGGer, "Style parsing failed.");
            }
        } catch (Exception e) {
            Log.e(TAGGer, "Can't find style. Error: ", e);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            m_Map.setMyLocationEnabled(true);
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE
            );
        }

        loadMarkers();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!location__Managerr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                LatLng pos = new LatLng(lat, lng);
                if (current_Markerr == null) {
                    current_Markerr = m_Map.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("Ma position")
                            // 20.0f est une teinte proche du marron/orange foncé pour le marker
                            .icon(BitmapDescriptorFactory.defaultMarker(20.0f)));
                } else {
                    current_Markerr.setPosition(pos);
                }

                m_Map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        location__Managerr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, locationListener);
        location__Managerr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, locationListener);
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Votre GPS est désactivé, voulez-vous l'activer ?")
                .setCancelable(false)
                .setPositiveButton("Oui", (dialog, id) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Non", (dialog, id) -> dialog.cancel());
        builder.create().show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (m_Map != null) {
                    m_Map.setMyLocationEnabled(true);
                    startLocationUpdates();
                }
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadMarkers() {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, URL_GETT, null,
                response -> {
                    try {
                        JSONArray positions = response.getJSONArray("positions");
                        LatLng lastPoint = null;

                        for (int i = 0; i < positions.length(); i++) {
                            JSONObject pos = positions.getJSONObject(i);
                            LatLng latLng = new LatLng(pos.getDouble("latitude"), pos.getDouble("longitude"));
                            m_Map.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("Appareil ID: " + pos.optString("imei"))
                                    // Utilisation de la même teinte marron pour les marqueurs du serveur
                                    .icon(BitmapDescriptorFactory.defaultMarker(20.0f)));
                            lastPoint = latLng;
                        }

                        if (lastPoint != null) {
                            m_Map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPoint, 8));
                        }
                    } catch (JSONException e) {
                        Log.e(TAGGer, "JSON Error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAGGer, "Volley Error: " + error.getMessage())
        );
        request__Queuee.add(request);
    }
}