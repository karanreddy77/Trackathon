package com.app.trackathon;


import java.util.HashMap;

public class Route {
    private String routeID;
    private HashMap<String, String> leaderBoard = new HashMap<String, String>();

    public String getRouteID() {
        return routeID;
    }

    public void setRouteID(String routeID) {
        this.routeID = routeID;
    }

    public HashMap<String, String> getLeaderBoard() {
        return leaderBoard;
    }

    public void setLeaderBoard(HashMap<String, String> leaderBoard) {
        this.leaderBoard = leaderBoard;
    }
}
