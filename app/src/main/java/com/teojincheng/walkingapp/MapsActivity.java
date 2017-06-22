package com.teojincheng.walkingapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private String TAG = "walkingApp";

    private GoogleMap mMap;

    private CameraPosition mCameraPosition;

    private LocationListener locListener;


    private GoogleApiClient mGoogleApiClient;

    private LocationManager locationManager;
    ArrayList<LatLng> list = new ArrayList<LatLng>();

    private Location mLastKnownLocation;
    private final LatLng mDefaultLocation = new LatLng(1.283333, 103.833333);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;
    boolean stopWatchRunning = false;

    StopWatch stopWatch;

    TextView textView;
    Button button;
    Button startButton;
    Button endButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        stopWatch = new StopWatch();

        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button2);
        startButton = (Button) findViewById(R.id.buttonStart);
        endButton = (Button) findViewById(R.id.buttonEnd);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopWatch.start();
                stopWatchRunning = true;


            }
        });


        endButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopWatch.stop();


            }
        });

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (stopWatchRunning) {
                                    textView.setText(String.valueOf(stopWatch.getElapsedTime()));

                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        updateLocationUI();

        getDeviceLocation();

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                double computedDistance = getDistance();
                textView.setText(String.valueOf(computedDistance));


            }

        });

    }


    @Override
    public void onConnected(Bundle connectionHint) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void getDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */


        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(mGoogleApiClient);


            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    list.add(new LatLng(location.getLatitude(), location.getLongitude()));

                    Polyline line = mMap.addPolyline(new PolylineOptions()
                            .addAll(list)
                            .width(5)
                            .color(Color.RED));


                }

                @Override
                public void onProviderDisabled(String provider) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onStatusChanged(String provider, int status,
                                            Bundle extras) {

                }
            });


        }

        // Set the map's camera position to the current location of the device.
        if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }


    }

    private double getDistance() {

        double totalDistance = 0;

        for (int i = 0; i < list.size() - 1; i++) {
            totalDistance = totalDistance + SphericalUtil.computeDistanceBetween(list.get(i), list.get(i + 1));
        }

        return totalDistance;

    }


}