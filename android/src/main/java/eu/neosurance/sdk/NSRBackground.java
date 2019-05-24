package eu.neosurance.sdk;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import org.json.JSONException;
import org.json.JSONObject;
import eu.neosurance.utils.NSRUtils;


public class NSRBackground extends BroadcastReceiver {
	@Override
	public void onReceive(Context ctx, Intent intent) {
		final NSR nsr = NSR.getInstance(ctx);

		try {
			NSRLog.d("NSRBackground in");
			JSONObject conf = NSRUtils.getConf(ctx);
			boolean fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
			boolean coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
			if ((coarse || fine) && conf != null && NSRUtils.getBoolean(conf.getJSONObject("position"), "enabled")) {
				NSRBackground.initBackground(NSRBackground.getBackgroundTime(ctx),ctx);
				final FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(ctx);
				LocationRequest locationRequest = LocationRequest.create();
				locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
				locationRequest.setInterval(0);
				locationRequest.setNumUpdates(1);
				locationClient.requestLocationUpdates(locationRequest, new NSRLocationCallback(nsr, locationClient), Looper.getMainLooper());
				NSRLog.d("NSRBackground locrequested");
			}
		} catch (Exception e) {
			nsr.initBackground(30);
			NSRLog.e("NSRBackground err:", e);
		}
	}

	public static void initBackground(int delay, Context ctx) {
		if (Build.VERSION.SDK_INT >= 21) {
			NSRLog.d("initBackground....");
			PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, 1, new Intent(ctx, NSRBackground.class), PendingIntent.FLAG_UPDATE_CURRENT);
			((AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE)).setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay * 1000, pendingIntent);
			NSRLog.d("initBackground in " + delay);
		}
	}

	public static int getBackgroundTime(Context ctx) {
		try {
			int time = NSRUtils.getConf(ctx).getInt("time");
			if (NSRTrace.isHardTraceLocation(ctx)) {
				time = Math.max(15, Math.min((int) Math.round(NSR.getHardTraceMeters() / 3), time));
			}
			return time;
		} catch (JSONException e) {
			NSRLog.e("getBackgroundTime", e);
			return 0;
		}
	}

}
