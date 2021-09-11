package com.mazsoft.capacitor.plugin.app.fused.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssNavigationMessage;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

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
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    // The location received from the LocationManager
    Location lastLocation; // last known location

    private Map<String, PluginCall> watchingCalls = new HashMap<>();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

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
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        if(locationManager == null) {
            call.reject("Proveedores de ubicación deshabilitados.", "2");
            call.release(bridge);
            return;
        }

        // Removes location updates for this class
        locationManager.removeUpdates(this);

        // Verify if use High Accuracy
        boolean enableHighAccuracy = call.getBoolean("enableHighAccuracy", false);

        // Getting the Location Provider that will be used
        String providerToUse = enableHighAccuracy ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;

        // Verifying the Location Provider availability
        if (!locationManager.isProviderEnabled(providerToUse)) {
            // Removes location updates for this class
            locationManager.removeUpdates(this);

            locationManager = null;

            call.reject("Proveedor de ubicación deshabilitado.", "2");
            call.release(bridge);

            return;
        }

        Log.d(locationTag, "Getting user location from provider: " + (enableHighAccuracy ? "GPS_PROVIDER" : "NETWORK_PROVIDER"));

        // Requesting location updates
        locationManager.requestSingleUpdate(providerToUse, this, null);

        // Getting the Last Known Location
        Location location = locationManager.getLastKnownLocation(providerToUse);

        // Verifying the returned location
        if (location == null) {
            call.reject("Ubicación no disponible.", "2");
        } else {
            call.resolve(getJSObjectForLocation(location));
        }

        call.release(bridge);

        // Removes location updates for this class
        locationManager.removeUpdates(this);
        locationManager = null;

    }

    private void clearLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

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
