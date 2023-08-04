package com.example.maps_app;

public class DataHolder {
    private static DataHolder instance;
    private String data;

    private DataHolder() {
        // Private constructor to prevent instantiation from outside
    }

    public static DataHolder getInstance() {
        if (instance == null) {
            instance = new DataHolder();
        }
        return instance;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}