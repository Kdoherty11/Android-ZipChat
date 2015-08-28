package com.kdoherty.zipchat.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.kdoherty.zipchat.R;

/**
 * Created by kevin on 6/8/15.
 */
public final class LocationManager {

    private LocationManager() {
    }


    public static void checkLocation(final Activity activity) {
        if (!isLocationEnabled(activity)) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setMessage(activity.getString(R.string.location_not_enabled));
            dialog.setPositiveButton(activity.getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivity(myIntent);
                }
            });

            dialog.setNegativeButton(activity.getString(R.string.cancel_location_dialog), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // Do nothing if user cancels
                }
            });

            dialog.show();

        }
    }

    private static boolean isLocationEnabled(Activity activity) {
        android.location.LocationManager lm = (android.location.LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = false;
        boolean isNetworkEnabled = false;
        try {
            isGpsEnabled = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            // isGpsEnabled is still false
        }
        try {
            isNetworkEnabled = lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            // isNetworkEnabled is still false
        }
        return isGpsEnabled || isNetworkEnabled;
    }

    public static double getDistance(double thisLat, double thisLong, double otherLat, double otherLong) {
        final int earthRadius = 6371;
        return Math.acos(Math.sin(Math.toRadians(thisLat)) * Math.sin(Math.toRadians(otherLat)) + Math.cos(Math.toRadians(thisLat)) * Math.cos(Math.toRadians(otherLat)) * Math.cos(Math.toRadians(thisLong) - Math.toRadians(otherLong))) * earthRadius * 1000;
    }

    public static LatLng computeOffset(LatLng from, double distance, double heading) {
        distance /= 6371009.0D;
        heading = Math.toRadians(heading);
        double fromLat = Math.toRadians(from.latitude);
        double fromLng = Math.toRadians(from.longitude);
        double cosDistance = Math.cos(distance);
        double sinDistance = Math.sin(distance);
        double sinFromLat = Math.sin(fromLat);
        double cosFromLat = Math.cos(fromLat);
        double sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * Math.cos(heading);
        double dLng = Math.atan2(sinDistance * cosFromLat * Math.sin(heading), cosDistance - sinFromLat * sinLat);
        return new LatLng(Math.toDegrees(Math.asin(sinLat)), Math.toDegrees(fromLng + dLng));
    }

    public static Circle setRoomCircle(Context context, GoogleMap map, LatLng center, int radius) {
        Resources res = context.getResources();
        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(radius)
                .fillColor(res.getColor(R.color.create_room_map_circle_fill))
                .strokeColor(res.getColor(R.color.zipchat_blue))
                .strokeWidth(6f);

        Circle circle = map.addCircle(circleOptions);

        int minRadius = 100;
        radius = Math.max(radius, minRadius);
        LatLngBounds bounds = new LatLngBounds.Builder().
                include(computeOffset(center, radius, 0)).
                include(computeOffset(center, radius, 90)).
                include(computeOffset(center, radius, 180)).
                include(computeOffset(center, radius, 270)).build();
        int paddingPx = 50;
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx));

        return circle;
    }
}
