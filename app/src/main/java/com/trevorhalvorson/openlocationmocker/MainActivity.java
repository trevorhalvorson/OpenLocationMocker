package com.trevorhalvorson.openlocationmocker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;

public class MainActivity extends AppCompatActivity implements LocationEngineListener,
        OnMapReadyCallback, MapboxMap.OnMapLongClickListener {
    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_LOCATION = 1337;
    private static final String MOCK_LOCATION_PROVIDER = "OpenLocationMocker";

    private LocationManager mLocationManager;

    private LocationEngine mLocationEngine;

    private MapView mMapView;
    private MapboxMap mMap;

    // Lifecycle callbacks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(MainActivity.this,
                // YOUR MAPBOX ACCESS TOKEN HERE
                getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_main);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.getAllProviders().contains(MOCK_LOCATION_PROVIDER)) {
            try {
                mLocationManager.addTestProvider(MOCK_LOCATION_PROVIDER,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        0,
                        0
                );
            } catch (SecurityException securityException) {
                Toast.makeText(MainActivity.this, getString(R.string.request_mock_location_provider),
                        Toast.LENGTH_LONG).show();
            }
        }

        mLocationEngine = LocationSource.getLocationEngine(MainActivity.this);
        mLocationEngine.activate();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mMapView = (MapView) findViewById(R.id.map_view);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        } else {
            mLocationEngine.requestLocationUpdates();
            mLocationEngine.getLastLocation();
            mLocationEngine.addLocationEngineListener(this);
            showLocationOnMap();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        if (!mLocationManager.isProviderEnabled(MOCK_LOCATION_PROVIDER)) {
            Toast.makeText(MainActivity.this, getString(R.string.request_mock_location_provider),
                    Toast.LENGTH_LONG).show();
        } else {
            mLocationManager.setTestProviderEnabled(MOCK_LOCATION_PROVIDER, true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (mLocationEngine != null) {
            mLocationEngine.removeLocationUpdates();
            mLocationEngine.removeLocationEngineListener(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    // Android callbacks

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_my_location:
                moveToMyLocation();
                return true;
            default:
                return false;
        }
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION:
                if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    mLocationEngine.getLastLocation();
                    mLocationEngine.addLocationEngineListener(this);
                    showLocationOnMap();
                }
                break;
        }
    }

    // Location callbacks

    @Override
    public void onConnected() {
        showLocationOnMap();
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    // Map callbacks

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        mMap = mapboxMap;
        mMap.setOnMapLongClickListener(this);
    }

    @Override
    public void onMapLongClick(@NonNull LatLng point) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(point));

        if (mLocationManager.isProviderEnabled(MOCK_LOCATION_PROVIDER)) {
            Location mockGpsLocation = new Location(LocationManager.GPS_PROVIDER);
            mockGpsLocation.setLongitude(point.getLongitude());
            mockGpsLocation.setLatitude(point.getLatitude());
            mockGpsLocation.setAltitude(0.0);
            mockGpsLocation.setBearing(0f);
            mockGpsLocation.setSpeed(0f);
            mockGpsLocation.setAccuracy(1f);
            mockGpsLocation.setTime(System.nanoTime());
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                mockGpsLocation.setElapsedRealtimeNanos(System.nanoTime());
            }
            mLocationManager.setTestProviderLocation(MOCK_LOCATION_PROVIDER, mockGpsLocation);

            Location mockNetworkLocation = new Location(LocationManager.NETWORK_PROVIDER);
            mockNetworkLocation.setLongitude(point.getLongitude());
            mockNetworkLocation.setLatitude(point.getLatitude());
            mockNetworkLocation.setAltitude(0.0);
            mockNetworkLocation.setBearing(0f);
            mockNetworkLocation.setSpeed(0f);
            mockNetworkLocation.setAccuracy(1f);
            mockNetworkLocation.setTime(System.nanoTime());
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                mockNetworkLocation.setElapsedRealtimeNanos(System.nanoTime());
            }
            mLocationManager.setTestProviderLocation(MOCK_LOCATION_PROVIDER, mockNetworkLocation);
        } else {
            Toast.makeText(MainActivity.this, getString(R.string.request_mock_location_provider),
                    Toast.LENGTH_LONG).show();
        }
    }

    // private methods

    private void showLocationOnMap() {
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void moveToMyLocation() {
        if (mMap != null) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_LOCATION);
            } else {
                Location location = mLocationEngine.getLastLocation();
                if (location != null) {
                    CameraPosition position = new CameraPosition.Builder()
                            .target(new LatLng(mLocationEngine.getLastLocation()))
                            .zoom(17)
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
                } else {
                    Toast.makeText(MainActivity.this, "Fetching location",
                            Toast.LENGTH_SHORT).show();
                    mLocationEngine.requestLocationUpdates();
                }
            }
        }
    }
}
