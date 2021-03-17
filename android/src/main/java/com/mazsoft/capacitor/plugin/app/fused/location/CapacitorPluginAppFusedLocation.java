package com.mazsoft.capacitor.plugin.app.fused.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

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
public class CapacitorPluginAppFusedLocation extends Plugin {

    static final String locationTag = "capacitor_plugin_app_fused_location";
    String[] locationPermissions = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };

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
        clearLocationUpdates();

        boolean enableHighAccuracy = call.getBoolean("enableHighAccuracy", false);

        int timeout = call.getInt("timeout", 10000);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());

        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        boolean networkEnabled = false;

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            call.error("Error checking NETWORK_PROVIDER availability.");
            return;
        }

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setMaxWaitTime(timeout);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        if (!enableHighAccuracy) {
            // int priority = networkEnabled ? LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY : LocationRequest.PRIORITY_LOW_POWER;
            locationRequest.setPriority(networkEnabled ? LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY : LocationRequest.PRIORITY_LOW_POWER);
        } else {
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        locationCallback =
            new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (call.getMethodName().equals("getCurrentPosition")) {
                        clearLocationUpdates();
                    }

                    if (locationResult == null) {
                        return;
                    }

                    for (Location location : locationResult.getLocations()) {
                        if (location == null) {
                            call.error("location unavailable");
                        } else {
                            call.success(getJSObjectForLocation(location));
                        }
                    }
                }

                @Override
                public void onLocationAvailability(LocationAvailability availability) {
                    if (!availability.isLocationAvailable()) {
                        call.error("location unavailable");

                        clearLocationUpdates();
                    }
                }
            };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void clearLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }
}
