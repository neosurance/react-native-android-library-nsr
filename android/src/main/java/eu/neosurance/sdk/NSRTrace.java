package eu.neosurance.sdk;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import com.google.android.gms.location.ActivityRecognition;
import org.json.JSONException;
import org.json.JSONObject;
import eu.neosurance.utils.NSRUtils;

public class NSRTrace {

    public static synchronized void initActivity(Context ctx) {
        if (NSR.activityClient == null) {
            NSRLog.d("initActivity");
            NSR.activityClient = ActivityRecognition.getClient(ctx);
        }
    }

    public static synchronized void traceActivity(Context ctx) {
        NSRLog.d("traceActivity");
        try {
            JSONObject conf = NSRUtils.getConf(ctx);
            if (conf != null && NSRUtils.getBoolean(conf.getJSONObject("activity"), "enabled")) {
                initActivity(ctx);
                long time = conf.getLong("time") * 1000;
                NSRLog.d("requestActivityUpdates");

                NSR.activityIntent = PendingIntent.getBroadcast(ctx, 0, new Intent(ctx, NSRActivityCallback.class), PendingIntent.FLAG_UPDATE_CURRENT);
                NSR.activityClient.requestActivityUpdates(time, NSR.activityIntent);
            }
        } catch (JSONException e) {
            NSRLog.e("traceActivity", e);
        }
    }

    public static synchronized void stopTraceActivity() {
        if (NSR.activityClient != null && NSR.activityIntent != null) {
            NSRLog.d("stopTraceActivity");
            NSR.activityClient.removeActivityUpdates(NSR.activityIntent);
            NSR.activityIntent = null;
        }
    }

    public static String getLastActivity(Context ctx) {
        return NSRUtils.getData("lastActivity",ctx);
    }

    public static void setLastActivity(String lastActivity,Context ctx) {
        NSRUtils.setData("lastActivity", lastActivity, ctx);
    }

    public static void traceLocation(Context ctx) {
        NSRLog.d("traceLocation");
        try {
            JSONObject conf = NSRUtils.getConf(ctx);
            boolean fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if ((coarse || fine) && conf != null && NSRUtils.getBoolean(conf.getJSONObject("position"), "enabled")) {
                NSR.initBackground(3);
            }
        } catch (JSONException e) {
            NSRLog.e("traceLocation", e);
        }
    }

    public static void hardTraceLocation(Context ctx) {
        NSRLog.d("hardTraceLocation");

        try {
            JSONObject conf = NSRUtils.getConf(ctx);
            boolean fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (coarse || fine && conf != null && NSRUtils.getBoolean(conf.getJSONObject("position"), "enabled")) {
                if (NSR.isHardTraceLocation()) {
                    if (Build.VERSION.SDK_INT >= 26 && NSR.foregrounder == null) {
                        NSR.foregrounder = new Intent(ctx, NSRForeground.class);
                        ctx.startForegroundService(NSR.foregrounder);
                    }
                    NSRLog.d("hardTraceLocation reactivated");
                }
            }
        } catch (JSONException e) {
            NSRLog.e("hardTraceLocation", e);
        }
    }

    protected static synchronized void stopHardTraceLocation(Context ctx) {
        NSRLog.d("stopHardTraceLocation");
        if (Build.VERSION.SDK_INT >= 26 && NSR.foregrounder != null) {
            ctx.stopService(NSR.foregrounder);
            NSR.foregrounder = null;
        }
    }

    public static void accurateLocation(double meters, int duration, boolean extend, Context ctx) {
        NSRLog.d("accurateLocation");
        try {
            JSONObject conf = NSRUtils.getConf(ctx);
            boolean fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if ((coarse || fine) && conf != null && NSRUtils.getBoolean(conf.getJSONObject("position"), "enabled")) {
                NSRLog.d("accurateLocation in");
                setHardTraceMeters(meters, ctx);
                NSR.initBackground(NSR.getBackgroundTime());
                if (!isHardTraceLocation(ctx) || extend) {
                    setHardTraceEnd((int) (System.currentTimeMillis() / 1000) + duration, ctx);
                }
                if (Build.VERSION.SDK_INT >= 26 && NSR.foregrounder == null) {
                    NSR.foregrounder = new Intent(ctx, NSRForeground.class);
                    ctx.startForegroundService(NSR.foregrounder);
                }

            }
        } catch (JSONException e) {
            NSRLog.e("accurateLocation", e);
        }
    }

    public static void accurateLocationEnd(Context ctx) {
        NSRLog.d("accurateLocationEnd");
        stopHardTraceLocation(ctx);
        setHardTraceEnd(0,ctx);

    }

    public static void checkHardTraceLocation(Context ctx) {
        if (!isHardTraceLocation(ctx)) {
            stopHardTraceLocation(ctx);
            setHardTraceEnd(0,ctx);
        }
    }

    public static boolean isHardTraceLocation(Context ctx) {
        int hte = getHardTraceEnd(ctx);
        return (hte > 0 && (System.currentTimeMillis() / 1000) < hte);
    }

    public static int getHardTraceEnd(Context ctx) {
        try {
            String s = NSRUtils.getData("hardTraceEnd",ctx);
            return s != null ? Integer.parseInt(s) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void setHardTraceEnd(int hardTraceEnd,Context ctx) {
        NSRUtils.setData("hardTraceEnd", "" + hardTraceEnd, ctx);
    }

    protected double getHardTraceMeters(Context ctx) {
        try {
            String s = NSRUtils.getData("hardTraceMeters",ctx);
            return s != null ? Double.parseDouble(s) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void setHardTraceMeters(double hardTraceMeters,Context ctx) {
        NSRUtils.setData("hardTraceMeters", "" + hardTraceMeters,ctx);
    }

    //**** OPPORTUNISTIC_TRACE

    public static void opportunisticTrace(Context ctx) {
        NSR.tracePower();
        NSR.traceConnection();
        try {
            String locationAuth = "notAuthorized";
            boolean fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (coarse && fine)
                locationAuth = "authorized";
            else if (fine)
                locationAuth = "fine";
            else if (coarse)
                locationAuth = "coarse";
            String lastLocationAuth = getLastLocationAuth(ctx);
            if (!locationAuth.equals(lastLocationAuth)) {
                NSR.setLastLocationAuth(locationAuth);
                JSONObject payload = new JSONObject();
                payload.put("status", locationAuth);

                NSR.getInstance(ctx).sendEvent("locationAuth", payload);
            }

            String pushAuth = (NotificationManagerCompat.from(ctx).areNotificationsEnabled()) ? "authorized" : "notAuthorized";
            String lastPushAuth = getLastPushAuth(ctx);
            if (!pushAuth.equals(lastPushAuth)) {
                setLastPushAuth(pushAuth,ctx);
                JSONObject payload = new JSONObject();
                payload.put("status", pushAuth);

                NSR.getInstance(ctx).sendEvent("pushAuth", payload);
            }
        } catch (Exception e) {
        }
    }

    public static String getLastLocationAuth(Context ctx) {
        return NSRUtils.getData("locationAuth",ctx);
    }

    public static void setLastLocationAuth(String locationAuth, Context ctx) {
        NSRUtils.setData("locationAuth", locationAuth,ctx);
    }

    public static String getLastPushAuth(Context ctx) {
        return NSRUtils.getData("pushAuth",ctx);
    }

    public static void setLastPushAuth(String pushAuth, Context ctx) {
        NSRUtils.setData("pushAuth", pushAuth,ctx);
    }

}
