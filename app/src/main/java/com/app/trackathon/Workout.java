package com.app.trackathon;


import java.util.ArrayList;

public class Workout {
    private String workoutID;
    private double time;
    private double distance;
   // private String routeID;
    private ArrayList<CustomLatLng> routePoints = new ArrayList<>();

    public Workout() {
    }

    public Workout(String workoutID, double time, double distance) {
        this.workoutID = workoutID;
        this.time = time;
        this.distance = distance;
    }

    public String getWorkoutID() {
        return workoutID;
    }

    public void setWorkoutID(String workoutID) {
        this.workoutID = workoutID;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

   /* public String getRouteID() {
        return routeID;
    }

    public void setRouteID(String routeID) {
        this.routeID = routeID;
    }*/

    public ArrayList<CustomLatLng> getRoutePoints() {
        return routePoints;
    }

    public void setRoutePoints(ArrayList<CustomLatLng> routePoints) {
        this.routePoints = routePoints;
    }
}
