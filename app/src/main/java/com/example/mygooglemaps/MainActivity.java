package com.example.mygooglemaps;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mygooglemaps.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final int DEFAULT_ZOOM = 15;
    private final double DONGGUK_LAT = 37.558398;
    private final double DONGGUK_LNG = 126.998271;

    public static final int PERMISSION_REQUEST_CODE = 9001;
    private static final int PLAY_SERVICES_ERROR_CODE = 9002;
    public static final int GPS_REQUEST_CODE = 9003;
    public static final String TAG = "MapDebug";
    private boolean mLocationPermissionGranted;

    private Button mBtnLocate;
    private TextView mOutputText;
    private GoogleMap mGoogleMap;
    private Button mBtnBatchLocation;
    private EditText mSearchAddress;
    private FusedLocationProviderClient mLocationClient;
    private LocationCallback mLocationCallback;

    public DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mSearchAddress = findViewById(R.id.et_address);
        mBtnLocate = findViewById(R.id.btn_locate);
        mOutputText = findViewById(R.id.tv_location);
        mBtnLocate.setOnClickListener(this::geoLocate);
        mBtnBatchLocation = findViewById(R.id.btn_batch_location);


        mBtnBatchLocation.setOnClickListener(this::batchLocationButtonClicked);

        initGoogleMap();

        mLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                if (locationResult == null) {
                    return;
                }

                Location location = locationResult.getLastLocation();

                Toast.makeText(MainActivity.this, location.getLatitude() + " \n" +
                        location.getLongitude(), Toast.LENGTH_SHORT).show();

                mOutputText.setText(location.getLatitude() + " : " + location.getLongitude());

                gotoLocation(location.getLatitude(), location.getLongitude());
                Date dt = new Date();
                showMarker(location.getLatitude(), location.getLongitude());


            }
        };

        Log.i("ABC", "맵실행");

        dbManager = DBManager.getInstance(this);

       // getLOC();

        //showMarker(37.0,127.0);


    }

    private void batchLocationButtonClicked(View view) {
        Intent intent = new Intent(MainActivity.this, BatchLocationActivity.class);
        startActivity(intent);
    }

    private void geoLocate(View view) {
        hideSoftKeyboard(view);

        String locationName = mSearchAddress.getText().toString();

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);

            if (addressList.size() > 0) {
                Address address = addressList.get(0);

                gotoLocation(address.getLatitude(), address.getLongitude());
                Date dt = new Date();

                showMarker(address.getLatitude(), address.getLongitude());

                Toast.makeText(this, address.getLocality(), Toast.LENGTH_SHORT).show();

                Log.d(TAG, "geoLocate: Locality: " + address.getLocality());
            }

            for (Address address : addressList) {
                Log.d(TAG, "geoLocate: Address: " + address.getAddressLine(address.getMaxAddressLineIndex()));
            }


        } catch (IOException e) {


        }


    }

    private void showMarker(double lat, double lng) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(lat, lng));
        mGoogleMap.addMarker(markerOptions);
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void initGoogleMap() {

        if (isServicesOk()) {
            if (isGPSEnabled()) {
                if (checkLocationPermission()) {
                    Toast.makeText(this, "Ready to Map", Toast.LENGTH_SHORT).show();

                    SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map_fragment_container);

                    supportMapFragment.getMapAsync(this);
                } else {
                    requestLocationPermission();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: map is showing on the screen");

        mGoogleMap = googleMap;
        gotoLocation(DONGGUK_LAT, DONGGUK_LNG);
//        mGoogleMap.setMyLocationEnabled(true);

        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(true);

        String[] columns = new String[]{"_id", "lat",
                "lng", "time"};
        Cursor c = getContentResolver().query(MyContentProvider.CONTENT_URI, columns, null,
                null, null, null);

        ArrayList<Double> LAT = new ArrayList<>();
        ArrayList<Double> LNG = new ArrayList<>();
        ArrayList<String> TIM = new ArrayList<>();
        int max = 0;
        if (c != null) {
            while (c.moveToNext()) {
                int id = c.getInt(0);
                Double lat = c.getDouble(1);
                Double lng = c.getDouble(2);
                String time = c.getString(3);

                Log.i("bbbb", Double.toString(lat) + Double.toString(lng) + time);
                LAT.add(lat);
                LNG.add(lng);
                TIM.add(time);


                max++;
                Log.i("bbbb", Integer.toString(max));
            }

            Log.i("cccc", "for 1");
            for (int i = 1; i < max-1; i++) {
                Log.i("cccc", "for 2");
                MarkerOptions makerOptions = new MarkerOptions();
                Log.i("cccc", "for 3");
                makerOptions
                        .position(new LatLng(LAT.get(i), LNG.get(i))).title(TIM.get(i))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        .alpha(0.5f);
                Log.i("cccc", "for 4");
                // 2. 마커 생성 (마커를 나타냄)

                mGoogleMap.addMarker(makerOptions);

                Log.i("cccc", "for 5");
                Log.i("cccc", "for out");
            }
            c.close();


        }

    }

    private void gotoLocation(double lat, double lng) {

        LatLng latLng = new LatLng(lat, lng);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM);

        mGoogleMap.moveCamera(cameraUpdate);
//        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

    }

    private boolean isGPSEnabled() {

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (providerEnabled) {
            return true;
        } else {

            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("GPS Permissions")
                    .setMessage("GPS is required for this app to work. Please enable GPS.")
                    .setPositiveButton("Yes", ((dialogInterface, i) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, GPS_REQUEST_CODE);
                    }))
                    .setCancelable(false)
                    .show();

        }

        return false;
    }

    private boolean checkLocationPermission() {

        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isServicesOk() {

        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();

        int result = googleApi.isGooglePlayServicesAvailable(this);

        if (result == ConnectionResult.SUCCESS) {
            return true;
        } else if (googleApi.isUserResolvableError(result)) {
            Dialog dialog = googleApi.getErrorDialog(this, result, PLAY_SERVICES_ERROR_CODE, task ->
                    Toast.makeText(this, "Dialog is cancelled by User", Toast.LENGTH_SHORT).show());
            dialog.show();
        } else {
            Toast.makeText(this, "Play services are required by this application", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {

            case R.id.maptype_none: {
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                break;
            }
            case R.id.maptype_normal: {
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            }
            case R.id.maptype_satelite: {
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            }
            case R.id.maptype_terrain: {
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
            }
            case R.id.maptype_hybrid: {
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            }
            case R.id.current_location: {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    break;
                }
                mLocationClient.getLastLocation().addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        Location location = task.getResult();
                        gotoLocation(location.getLatitude(), location.getLongitude());
                        Date dt = new Date();
                        showMarker(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.d(TAG, "getCurrentLocation: Error: " + task.getException().getMessage());
                    }

                });
                break;
            }

        }

        return super.onOptionsItemSelected(item);
    }

    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationClient.getLastLocation().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                Location location = task.getResult();
                gotoLocation(location.getLatitude(), location.getLongitude());
            } else {
                Log.d(TAG, "getCurrentLocation: Error: " + task.getException().getMessage());
            }

        });

    }

    private void getLocationUpdates() {

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Log.d(TAG, "run: done");

                if (ActivityCompat.checkSelfPermission(
                        MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "run: done");

                    return;
                }
                mLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper());

            }
        }).start();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GPS_REQUEST_CODE) {

            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (providerEnabled) {
                Toast.makeText(this, "GPS is enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "GPS not enabled. Unable to show user location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this, "Connected to Location Services", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLocationCallback != null) {
            mLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    public void getLOC() {
        String[] columns = new String[]{"_id", "lat",
                "lng", "time"};
        Cursor c = getContentResolver().query(MyContentProvider.CONTENT_URI, columns, null,
                null, null, null);

        ArrayList<Double> LAT = new ArrayList<>();
        ArrayList<Double> LNG = new ArrayList<>();
        ArrayList<String> TIM = new ArrayList<>();
        int max = 0;
        if (c != null) {
            while (c.moveToNext()) {
                int id = c.getInt(0);
                Double lat = c.getDouble(1);
                Double lng = c.getDouble(2);
                String time = c.getString(3);

                Log.i("bbbb", Double.toString(lat) + Double.toString(lng) + time);
                LAT.add(lat);
                LNG.add(lng);
                TIM.add(time);


                max++;
                Log.i("bbbb", Integer.toString(max));
            }

            Log.i("cccc", "for 1");
            for (int i = 1; i < max-1; i++) {
                Log.i("cccc", "for 2");
                MarkerOptions makerOptions = new MarkerOptions();
                Log.i("cccc", "for 3");
                makerOptions.position(new LatLng(LAT.get(i), LNG.get(i)));

                Log.i("cccc", "for 4");
                // 2. 마커 생성 (마커를 나타냄)

                mGoogleMap.addMarker(makerOptions);

                Log.i("cccc", "for 5");
                Log.i("cccc", "for out");
            }
            c.close();


        }
    }


}