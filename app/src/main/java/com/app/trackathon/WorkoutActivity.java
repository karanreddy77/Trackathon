////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                        //
//  WorkoutActivity.java - Trackathon                                                     //
//              Source file containing WorkoutActivity class with Fragment Activity       //
//  Language:        Java                                                                 //
//  Platform:        Android SDK                                                          //
//  Course No.:      ESE 543                                                              //
//  Assignment No.:  Final Project                                                        //
//  Author:          Reddem Karan Reddy, SBU ID: 111218499                                //
//                   Gollapudi Sathya, SBU ID: 111155154                                  //
////////////////////////////////////////////////////////////////////////////////////////////

package com.app.trackathon;

//------------------------------------------------------------------------------
// Importing Files
//------------------------------------------------------------------------------

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

//------------------------------------------------------------------------------
// Class implementation
//------------------------------------------------------------------------------
public class WorkoutActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private final String TAG = "Trackathon:HomePage:";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private GoogleApiClient mGoogleApiClient;
    private GoogleApiClient mGoogleApiClient2;
    private LocationRequest mLocationRequest;

    //Time values for timer
    private long startTime = 0L;
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;

    // Time values for pace calculation
    private long  lastTimeValue= 0L;
    private long  currentTimeValue =0L;
    long timeInMillisecondsForPace = 0L;
    long timeSwapBuffForPace = 0L;
    long updatedTimeForPace = 0L;

    //pace values
    private double maxPace = 0;
    private double calculatedPace = 0;

    private double distanceBetweenLatLngs = 0;
    private double timeBetweenLatLngs = 0;

    private Button startButton;
    private Button pauseButton;
    private Button resumeButton;
    private Button finishButton;
    private Button signOutButton;


    private TextView distanceValue;
    private TextView paceValue;
    private TextView timerValue;

    int timerSeconds, timerMinutes, timerMilliSeconds;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private final int REQUEST_LOCATION = 200;
    private final int REQUEST_CHECK_SETTINGS = 300;
    private final int REQUEST_GOOGLE_PLAY_SERVICE = 400;

    private Location mLastLocation;
    private LatLng previousLatLng;
    private LatLng currentLatLng;

    private Handler timerHandler = new Handler();
    private Handler distanceHandler = new Handler();
    private Handler paceHandler = new Handler();

    private boolean isWorkoutOngoing = false;
    private double totalDistanceTraveled = 0;

    private PendingResult<LocationSettingsResult> result;
    private LocationSettingsRequest.Builder builder;
    private ArrayList<LatLng> routePoints = new ArrayList<>();

    //------------------------------------------------------------------------------
    // Oncreate function called
    //------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);
        mGoogleApiClient2 = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        setUpMapIfNeeded();

        signOutButton = (Button) findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);
        startButton = (Button) findViewById(R.id.startWorkoutButton);
        startButton.setOnClickListener(this);
        pauseButton = (Button) findViewById(R.id.pauseWorkoutButton);
        pauseButton.setOnClickListener(this);
        resumeButton = (Button) findViewById(R.id.resumeWorkoutButton);
        resumeButton.setOnClickListener(this);
        finishButton = (Button) findViewById(R.id.finishWorkoutButton);
        finishButton.setOnClickListener(this);

        timerValue = (TextView) findViewById(R.id.timerValue);
        distanceValue = (TextView) findViewById(R.id.distanceValue);
        paceValue = (TextView) findViewById(R.id.paceValue);

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new android.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(isWorkoutOngoing == true) {
                    LatLng mapPoint = new LatLng(location.getLatitude(), location.getLongitude());
                    if(routePoints.size() == 0) {
                        previousLatLng = mapPoint;
                    }
                    currentLatLng =  mapPoint;
                    routePoints.add(mapPoint);
                    drawPolyLineOnMap(routePoints);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });

        // [START config_signin]
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
    }
    //------------------------------------------------------------------------------
    // Location update onconnected
    //------------------------------------------------------------------------------
    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = createLocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second

        builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient2, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                final LocationSettingsStates mState = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()){
                    case LocationSettingsStatusCodes.SUCCESS:
                        /*int tempPermissionCoarse = ActivityCompat.checkSelfPermission(Home.this, Manifest.permission.ACCESS_COARSE_LOCATION);
                        int tempPermissionFine = ActivityCompat.checkSelfPermission(Home.this, Manifest.permission.ACCESS_FINE_LOCATION);*/
                        ActivityCompat.requestPermissions(WorkoutActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient2);
                        if(mMap != null){
                            if(mLastLocation != null) {
                                LatLng locationMarker = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                                //routePoints.add(locationMarker);
                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(locationMarker, 17);
                                //mMap.addMarker(new MarkerOptions().position(locationMarker).title("Current Location"));
                                //mMap.moveCamera(CameraUpdateFactory.newLatLng(locationMarker));
                                mMap.animateCamera(cameraUpdate);
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult() and check the result in onActivityResult().
                            status.startResolutionForResult(WorkoutActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });

    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    //------------------------------------------------------------------------------
    // on resume
    //------------------------------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        //setUpMapIfNeeded();
        int resultReturned = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if(resultReturned != ConnectionResult.SUCCESS){
            GoogleApiAvailability.getInstance().getErrorDialog(WorkoutActivity.this, ConnectionResult.SERVICE_MISSING, REQUEST_GOOGLE_PLAY_SERVICE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
        mGoogleApiClient2.connect();
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
        mGoogleApiClient2.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection has failed");
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.sign_out_button) {
            signOut();
        } else if (view.getId() == R.id.startWorkoutButton) {
            startButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            startWorkout();
        } else if (view.getId() == R.id.pauseWorkoutButton) {
            pauseButton.setVisibility(View.GONE);
            finishButton.setVisibility(View.VISIBLE);
            resumeButton.setVisibility(View.VISIBLE);
            pauseWorkout();
        } else if (view.getId() == R.id.resumeWorkoutButton) {
            resumeButton.setVisibility(View.GONE);
            finishButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            startWorkout();
        } else if (view.getId() == R.id.finishWorkoutButton) {;
            finishWorkout();
        }
    }

    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        Intent i = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(i);
                        finish();
                    }
                });
    }

    private void startWorkout() {
        isWorkoutOngoing = true;
        startTime = SystemClock.uptimeMillis();
        //start threads
        timerHandler.postDelayed(updateTimerThread, 0);
        distanceHandler.postDelayed(updateDistanceThread, 1000);
        paceHandler.postDelayed(updatePaceThread, 1000);
    }

    private void pauseWorkout() {
        isWorkoutOngoing = false;
        timeSwapBuff += timeInMilliseconds;
        //stop threads
        timerHandler.removeCallbacks(updateTimerThread);
        distanceHandler.removeCallbacks(updateDistanceThread);
        paceHandler.removeCallbacks(updatePaceThread);
    }

    private void finishWorkout() {
        Intent i = new Intent(getApplicationContext(), SaveWorkoutActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("timerMinutes", timerMinutes);
        bundle.putInt("timerSeconds", timerSeconds);
        bundle.putInt("timerMilliSeconds", timerMilliSeconds);
        bundle.putDouble("totalDistanceTraveled", totalDistanceTraveled);
        i.putExtras(bundle);
        i.putExtra("routePoints", routePoints);
        startActivity(i);
        finish();
    }

    private Runnable updateTimerThread = new Runnable() {
        @Override
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            timerSeconds = (int) (updatedTime/1000);
            timerMinutes = timerSeconds /60;
            timerSeconds = timerSeconds % 60;
            timerMilliSeconds = (int) (updatedTime % 1000);
            timerValue.setText(""+ timerMinutes + ":" + String.format("%02d", timerSeconds) + ":" + String.format("%03d", timerMilliSeconds));

            timerHandler.postDelayed(this, 0);
        }
    };

    private Runnable updateDistanceThread = new Runnable() {
        @Override
        public void run() {
            if(currentLatLng != null && previousLatLng != null) {
                distanceCalculation();
            }
            distanceHandler.postDelayed(this, 1000);
        }
    };

    private Runnable updatePaceThread = new Runnable() {
        @Override
        public void run() {
            timeInMillisecondsForPace = SystemClock.uptimeMillis() - startTime;
            updatedTimeForPace = timeSwapBuffForPace + timeInMillisecondsForPace;
            currentTimeValue = updatedTimeForPace;
            paceCalculation();
            paceHandler.postDelayed(this, 1000);
        }
    };

    //------------------------------------------------------------------------------
    // set up map
    //------------------------------------------------------------------------------
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        ActivityCompat.requestPermissions(WorkoutActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient2);
                        if (mMap != null) {
                            LatLng locationMarker = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(locationMarker).title("Current Location"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(locationMarker));
                        }
                        break;
                    case  Activity.RESULT_CANCELED:
                        // user does not want to update setting. Handle it in a way that it will to affect your app functionality
                        Toast.makeText(WorkoutActivity.this, "User did not update location setting", Toast.LENGTH_LONG).show();
                        break;
                }
                break;
        }
    }

    public void drawPolyLineOnMap(ArrayList<LatLng> list) {
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(Color.RED);
        polyOptions.width(5);
        polyOptions.addAll(list);
        mMap.clear();
        mMap.addPolyline(polyOptions);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : list) {
            builder.include(latLng);
        }
        final LatLngBounds bounds = builder.build();
        //BOUND_PADDING is an int to specify padding of bound.. try 100.
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);
        mMap.animateCamera(cu);
    }

    public void distanceCalculation() {
        float[] distanceResults = new float[1];
        Location.distanceBetween(currentLatLng.latitude, currentLatLng.longitude, previousLatLng.latitude, previousLatLng.longitude, distanceResults);
        distanceBetweenLatLngs = (distanceResults[0]/1000);
        distanceBetweenLatLngs = threeDecimalPlaces(distanceBetweenLatLngs).doubleValue();

        totalDistanceTraveled += distanceBetweenLatLngs;
        distanceValue.setText("" + threeDecimalPlaces(totalDistanceTraveled) + " Distance(km)");

        previousLatLng = currentLatLng;
    }

    public void paceCalculation() {
        timeBetweenLatLngs = currentTimeValue - lastTimeValue;
        double distanceTraveled = (distanceBetweenLatLngs * 1000);
        int secs = (int) (timeBetweenLatLngs/1000);
        lastTimeValue = currentTimeValue;

        if(secs != 0){
            calculatedPace = distanceTraveled / secs;
            paceValue.setText("" +twoDecimalPlaces(calculatedPace) + " Speed(mps)");
            if (calculatedPace > maxPace) {
                maxPace = calculatedPace;
            }
        }
    }

    private static final int Three_DECIMAL_PLACES = 3;
    private static java.math.BigDecimal threeDecimalPlaces(final double d) {
        return new java.math.BigDecimal(d).setScale(Three_DECIMAL_PLACES,
                java.math.RoundingMode.HALF_UP);
    }

    private static final int DECIMAL_PLACES = 2;
    private static java.math.BigDecimal twoDecimalPlaces(final double d) {
        return new java.math.BigDecimal(d).setScale(DECIMAL_PLACES,
                java.math.RoundingMode.HALF_UP);
    }
}
