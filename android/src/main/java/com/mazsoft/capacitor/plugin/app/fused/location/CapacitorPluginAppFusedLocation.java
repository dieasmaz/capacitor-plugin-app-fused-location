package com.mazsoft.capacitor.plugin.app.fused.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;

import java.util.HashMap;
import java.util.Map;

@NativePlugin(
        permissions = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
        permissionRequestCode = CapacitorPluginAppFusedLocationConstants.CAPACITOR_PLUGIN_APP_FUSED_LOCATION_REQUEST
)
public class CapacitorPluginAppFusedLocation extends Plugin implements LocationListener {

    static final String locationTag = "CapPlugAppFusedLocation";
    String[] locationPermissions = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 0 meters

    // The minimum time between updates in milliseconds
    private static final int MIN_TIME_BW_UPDATES = 5000; // 5 seconds

    // The location received from the LocationManager
    Location lastLocation; // last known location

    private Map<String, PluginCall> watchingCalls = new HashMap<>();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // The main location manager
    private LocationManager locationManager = null;

    @PluginMethod
    public void getCurrentPosition(PluginCall call) {
        if (!hasRequiredPermissions()) {
            saveCall(call);
            pluginRequestAllPermissions();
        } else {
            sendLocation(call);
        }
    }

    private void sendLocation(PluginCall call) {
        requestLocationUpdates(call);
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void watchPosition(PluginCall call) {
        call.save();
        if (!hasRequiredPermissions()) {
            saveCall(call);
            pluginRequestAllPermissions();
        } else {
            startWatch(call);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startWatch(PluginCall call) {
        requestLocationUpdates(call);
        watchingCalls.put(call.getCallbackId(), call);
    }

    @SuppressWarnings("MissingPermission")
    @PluginMethod
    public void clearWatch(PluginCall call) {
        String callbackId = call.getString("id");
        if (callbackId != null) {
            PluginCall removed = watchingCalls.remove(callbackId);
            if (removed != null) {
                removed.release(bridge);
            }
        }
        if (watchingCalls.size() == 0) {
            clearLocationUpdates();
        }
        call.success();
    }

    /**
     * Process a new location item and send it to any listening calls
     * @param location
     */
    private void processLocation(Location location) {
        for (Map.Entry<String, PluginCall> watch : watchingCalls.entrySet()) {
            watch.getValue().success(getJSObjectForLocation(location));
        }
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != CapacitorPluginAppFusedLocationConstants.CAPACITOR_PLUGIN_APP_FUSED_LOCATION_REQUEST) {
            return;
        }

        PluginCall savedCall = getSavedCall();
        if (savedCall == null) {
            return;
        }

        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                savedCall.error("User denied location permission");
                return;
            }
        }

        if (savedCall.getMethodName().equals("getCurrentPosition")) {
            sendLocation(savedCall);
        } else if (savedCall.getMethodName().equals("watchPosition")) {
            startWatch(savedCall);
        } else {
            savedCall.resolve();
            savedCall.release(bridge);
        }
    }

    private JSObject getJSObjectForLocation(Location location) {
        JSObject ret = new JSObject();
        JSObject coords = new JSObject();
        ret.put("coords", coords);
        ret.put("timestamp", location.getTime());
        coords.put("latitude", location.getLatitude());
        coords.put("longitude", location.getLongitude());
        coords.put("accuracy", location.getAccuracy());
        coords.put("altitude", location.getAltitude());
        if (Build.VERSION.SDK_INT >= 26) {
            coords.put("altitudeAccuracy", location.getVerticalAccuracyMeters());
        }
        coords.put("speed", location.getSpeed());
        coords.put("heading", location.getBearing());

        return ret;
    }

    @SuppressWarnings("MissingPermission")
    private void requestLocationUpdates(final PluginCall call) {

        // getting LocationManager Service
        this.locationManager = (LocationManager) bridge.getContext().getSystemService(Context.LOCATION_SERVICE);

        if(locationManager == null) {
            call.reject("Proveedores de ubicación deshabilitados.", "2");
            call.release(bridge);
            return;
        }

        // Getting the time out
        int timeout = call.getInt("timeout", MIN_TIME_BW_UPDATES);

        // getting GPS status
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        // Verifying the Location Provider availability
        if (!isGPSEnabled && !isNetworkEnabled) {
            // Removes location updates for this class
            locationManager.removeUpdates(this);
            locationManager = null;

            call.reject("Proveedor(es) de ubicación deshabilitado(s).", "2");
            call.release(bridge);

            return;
        }

        Log.d(locationTag, "Getting user location from providers: GPS_PROVIDER and/or NETWORK_PROVIDER");

        // First get location from Network Provider
        if (isNetworkEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    timeout,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    this);

            Log.d(locationTag, "NETWORK_PROVIDER");
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if(networkLocation != null) {
                lastLocation = networkLocation;
            }
        }

        // if GPS Enabled get lat/long using GPS Services
        if (isGPSEnabled && lastLocation == null) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    timeout,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    this);

            Log.d(locationTag, "GPS_PROVIDER");
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if(gpsLocation != null) {
                lastLocation = gpsLocation;
            }
        }

        // Verifying the returned location
        if (lastLocation == null) {
            call.reject("Ubicación no disponible.", "2");
        } else {
            call.resolve(getJSObjectForLocation(lastLocation));
        }

        call.release(bridge);

        // Removes location updates for this class
        //locationManager.removeUpdates(this);
        //locationManager = null;
    }

    public void clearLocationUpdates() {
        /*
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
         */

        if(locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(locationTag, "onLocationChanged " + (location == null ? "NULL": location.toString()));
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
}
