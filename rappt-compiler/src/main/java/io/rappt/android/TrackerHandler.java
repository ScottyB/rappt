package io.rappt.android;


public class TrackerHandler extends Template{

    public String api;
    public String updateCall;

    public TrackerHandler(String api, String updateCall) {
        super("trackingUpdateHandler");
        this.api = api;
        this.updateCall = updateCall;
    }
}
