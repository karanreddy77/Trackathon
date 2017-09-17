package com.app.trackathon;

import java.util.ArrayList;

public class User {
    private String userID;
    private ArrayList<Workout> workouts = new ArrayList<Workout>();

    public User() {
    }

    public User(String userID, ArrayList<Workout> workouts) {
        this.userID = userID;
        this.workouts = workouts;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public ArrayList<Workout> getWorkouts() {
        return workouts;
    }

    public void setWorkouts(ArrayList<Workout> workouts) {
        this.workouts = workouts;
    }

}
