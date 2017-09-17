package com.app.trackathon;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity implements View.OnClickListener,  GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private Button homeButton;
    private Button newWorkoutButton;
    private Button signOutButton;
    private Button refreshButton;

    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    DatabaseReference databaseReference;

    private User user = new User();

    int timerSeconds, timerMinutes, timerMilliSeconds;
    private double totalDistanceTraveled = 0;

    private String[] historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        homeButton = (Button) findViewById(R.id.home_button);
        homeButton.setOnClickListener(this);
        newWorkoutButton = (Button) findViewById(R.id.new_workout_button);
        newWorkoutButton.setOnClickListener(this);
        refreshButton = (Button) findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(this);
        signOutButton = (Button) findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);

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

        loadHistory();

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
    public void onClick(View view) {
        if (view.getId() == R.id.sign_out_button) {
            signOut();
        } else if (view.getId() == R.id.new_workout_button) {
            Intent i = new Intent(getApplicationContext(), WorkoutActivity.class);
            startActivity(i);
            finish();
        } else if (view.getId() == R.id.home_button) {
            Intent i = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(i);
            finish();
        }  else if (view.getId() == R.id.refresh_button) {
            Intent i = new Intent(getApplicationContext(), HistoryActivity.class);
            startActivity(i);
            finish();
        }
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

    public void loadHistory() {
        databaseReference.child("users").child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<User> genericTypeIndicator = new GenericTypeIndicator<User>() {
                    @Override
                    public int hashCode() {
                        return super.hashCode();
                    }
                };
                user = dataSnapshot.getValue(genericTypeIndicator);
                if (user == null) {
                    //no user to display
                } else {
                    showHistory();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });;
    }

    public void showHistory() {
        historyList = new String[user.getWorkouts().size()];
        Log.d("here", "asfa");
        for(int i = 0; i < user.getWorkouts().size(); i++){
            historyList[i] = user.getWorkouts().get(i).getWorkoutID();
        }
        for(int i = 0; i < historyList.length; i++) {
            Log.d("history"+i, historyList[i]);
        }
        ListAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, historyList);
        ListView workoutsHistory = (ListView) findViewById(R.id.workouts_history);
        workoutsHistory.setAdapter(adapter);
        workoutsHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String content = historyList[position];
                int time = (int) user.getWorkouts().get(position).getTime();
                timerMinutes = time/60000;
                timerSeconds = (time/1000) % 60;
                timerMilliSeconds = time % 1000;
                totalDistanceTraveled = user.getWorkouts().get(position).getDistance();
                ArrayList<CustomLatLng> customRoutePoints = user.getWorkouts().get(position).getRoutePoints();
                ArrayList<LatLng> routePoints = new ArrayList<LatLng>();
                for(int j = 0; j < customRoutePoints.size(); j++){
                    LatLng latLng = new LatLng(customRoutePoints.get(j).getLatitude(), customRoutePoints.get(j).getLongitude());
                    routePoints.add(latLng);
                }
                Intent showContent = new Intent(getApplicationContext(), HistoryViewerActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("timerMinutes", timerMinutes);
                bundle.putInt("timerSeconds", timerSeconds);
                bundle.putInt("timerMilliSeconds", timerMilliSeconds);
                bundle.putDouble("totalDistanceTraveled", totalDistanceTraveled);
                showContent.putExtras(bundle);
                showContent.putExtra("routePoints", routePoints);
                startActivity(showContent);
                finish();
            }
        });
    }
}
