package com.adrian.osmdroidlocmessenger.messagemanagement;

import java.io.Serializable;
import java.util.UUID;


public class LocationMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private String messageID;
    private String userID;
    private String text;
    private double latitude;
    private double longitude;
    private int likeCount;

    public LocationMessage(String userID, String text, double latitude, double longitude) {
        this.messageID = UUID.randomUUID().toString();
        this.userID = userID;
        this.latitude = latitude;
        this.longitude = longitude;
        this.text = text;
        this.likeCount = 0;
    }

    public LocationMessage(String messageID, String userID, String text, double latitude, double longitude, int likeCount) {
        this.messageID = messageID;
        this.userID = userID;
        this.latitude = latitude;
        this.longitude = longitude;
        this.text = text;
        this.likeCount = likeCount;
    }

    public String getMessageID() {
        return messageID;
    }

    public String getUserID() {
        return userID;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getText() {
        return this.text;
    }

    public int getLikeCount() {
        return likeCount;
    }
}
