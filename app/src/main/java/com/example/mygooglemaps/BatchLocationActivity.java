package com.example.mygooglemaps;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.List;
//TODO 배치 로케이션 액티비티에서 서비스 스타트 누르면 서비스가 시작되고, dgu.db가 열리면서 반환받은 위치정보와 시간을 db에 저장한다.
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class BatchLocationActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = "MyTag";
    private TextView mOutputText;
    private Button mBtnLocationRequest, mBtnStartService, mBtnStopService;
    private FusedLocationProviderClient mLocationClient;
    private LocationCallback mLocationCallback;
    public DBManager dbManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_location);
        dbManager = DBManager.getInstance(this);
        mOutputText = findViewById(R.id.tv_output);
        mBtnLocationRequest = findViewById(R.id.btn_location_request);
        mBtnStartService = findViewById(R.id.btn_start_service);
        mBtnStopService = findViewById(R.id.btn_stop_service);

        mLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                if (locationResult == null) {
                    Log.d(TAG, "onLocationResult: location error");
                    return;
                }

                List<Location> locations = locationResult.getLocations();

                LocationResultHelper helper = new LocationResultHelper(BatchLocationActivity.this, locations);

                helper.showNotification();

                helper.saveLocationResults();

                Toast.makeText(BatchLocationActivity.this, "Location received: " + locations.size(), Toast.LENGTH_SHORT).show();

                mOutputText.setText(helper.getLocationResultText());

//                Log.d(TAG, "onLocationResult: " + location.getLatitude() + " \n" +
//                        location.getLongitude());


            }
        };

        mBtnLocationRequest.setOnClickListener(this::requestBatchLocationUpdates);
        mBtnStartService.setOnClickListener(this::startLocationService);
        mBtnStopService.setOnClickListener(this::stopLocationService);

    }

    private void startLocationService(View view) {
        //start background location service

        Intent intent = new Intent(this, MyBackgroundLocationService.class);
        ContextCompat.startForegroundService(this, intent);
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();


    }

    private void stopLocationService(View view) {
        //stop background location service

        Intent intent = new Intent(this, MyBackgroundLocationService.class);

        stopService(intent);


        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();

    }

    private void requestBatchLocationUpdates(View view) {

        dbManager.deleteAll();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(4000);

        locationRequest.setMaxWaitTime(15 * 1000);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);

    }

    @Override
    protected void onPause() {
        super.onPause();

//        mLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOutputText.setText(LocationResultHelper.getSavedLocationResults(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals(LocationResultHelper.KEY_LOCATION_RESULTS)) {
            mOutputText.setText(LocationResultHelper.getSavedLocationResults(this));
        }

    }
}