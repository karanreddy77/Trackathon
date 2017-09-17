////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                        //
//  SaveWorkoutActivity.java - Trackathon                                                     //
//              Source file containing SaveWorkoutActivity class with Fragment Activity       //
//  Language:        Java                                                                 //
//  Platform:        Android SDK                                                          //
//  Course No.:      ESE 543                                                              //
//  Assignment No.:  Final Project                                                        //
//  Author:          Reddem Karan Reddy, SBU ID: 111218499                                //
//                   Gollapudi Sathya, SBU ID: 111155154                                  //
////////////////////////////////////////////////////////////////////////////////////////////

package com.app.trackathon;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class SaveWorkoutActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private TextView timeTraveled;
    private TextView distanceTraveled;

    private Button signOutButton;
    private Button discardButton;
    private Button saveButton;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    DatabaseReference databaseReference;

    private double timeVal = 0L;
    private double distanceVal = 0L;
    private ArrayList<LatLng> routePoints = new ArrayList<>();
    private ArrayList<CustomLatLng> customRoutePoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_workout);

        signOutButton = (Button) findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);
        discardButton = (Button) findViewById(R.id.discardWorkoutButton);
        discardButton.setOnClickListener(this);
        saveButton = (Button) findViewById(R.id.saveWorkoutButton);
        saveButton.setOnClickListener(this);

        timeTraveled = (TextView) findViewById(R.id.totalTime);
        distanceTraveled = (TextView) findViewById(R.id.totalDistance);

        Bundle bundle = getIntent().getExtras();
        int mins = bundle.getInt("timerMinutes");
        int secs = bundle.getInt("timerSeconds");
        int millisecs = bundle.getInt("timerMilliSeconds");
        timeVal = (mins*60*1000) + (secs*1000) + millisecs;
        distanceVal = bundle.getDouble("totalDistanceTraveled");

        timeTraveled.setText(""+ mins + ":" + String.format("%02d", secs) + ":" + String.format("%03d", millisecs));
        distanceTraveled.setText(threeDecimalPlaces(distanceVal) + " km");

        //set up map
        routePoints = (ArrayList<LatLng>) getIntent().getSerializableExtra("routePoints");
        for(int i=0; i < routePoints.size(); i++){
            CustomLatLng customLatLng = new CustomLatLng(routePoints.get(i).latitude, routePoints.get(i).longitude);
            customRoutePoints.add(customLatLng);
        }
        setUpMap();

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

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.sign_out_button) {
            signOut();
        } else if (view.getId() == R.id.discardWorkoutButton) {
            Intent i = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(i);
            finish();
        } else if (view.getId() == R.id.saveWorkoutButton) {
            saveData();
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

    private void setUpMap() {
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
        //mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        if(routePoints.size() > 0) {
            drawPolyLineOnMap(routePoints);
            mMap.addMarker(new MarkerOptions()
                    .position(routePoints.get(0))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.addMarker(new MarkerOptions()
                    .position(routePoints.get(routePoints.size()-1))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
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

    private static final int Three_DECIMAL_PLACES = 3;
    private static java.math.BigDecimal threeDecimalPlaces(final double d) {
        return new java.math.BigDecimal(d).setScale(Three_DECIMAL_PLACES,
                java.math.RoundingMode.HALF_UP);
    }

    public void saveData() {
        databaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<HashMap<String, User>> genericTypeIndicator = new GenericTypeIndicator<HashMap<String, User>>() {
                    @Override
                    public int hashCode() {
                        return super.hashCode();
                    }
                };
                HashMap<String, User> users = dataSnapshot.getValue(genericTypeIndicator);
                if(users == null) {
                    //add user
                    User newUser = newUser();
                    databaseReference.child("users").child(newUser.getUserID()).setValue(newUser);
                } else {
                    //get user
                    saveWorkoutForUser();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(intent);
        finish();
    }

    public void saveWorkoutForUser() {
        databaseReference.child("users").child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        GenericTypeIndicator<User> genericTypeIndicator = new GenericTypeIndicator<User>() {
                            @Override
                            public int hashCode() {
                                return super.hashCode();
                            }
                        };
                        User user = dataSnapshot.getValue(genericTypeIndicator);
                        if(user == null) {
                            User newUser = newUser();
                            databaseReference.child("users").child(newUser.getUserID()).setValue(newUser);
                        } else {
                            updateUser(user);
                            databaseReference.child("users").child(user.getUserID()).setValue(user);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );
    }

    public User newUser() {
        User newUser = new User();
        newUser.setUserID(mAuth.getCurrentUser().getUid());
        ArrayList<Workout> newUserWorkouts = newUser.getWorkouts();
        Workout newWorkout = new Workout("Workout1", timeVal, distanceVal);
        newWorkout.setRoutePoints(customRoutePoints);
        newUserWorkouts.add(newWorkout);
        newUser.setWorkouts(newUserWorkouts);
        return newUser;
    }

    public void updateUser(User user) {
        ArrayList<Workout> newUserWorkouts = user.getWorkouts();
        Workout newWorkout = new Workout("Workout"+(newUserWorkouts.size()+1), timeVal, distanceVal);
        newWorkout.setRoutePoints(customRoutePoints);
        newUserWorkouts.add(newWorkout);
        user.setWorkouts(newUserWorkouts);
    }
}
