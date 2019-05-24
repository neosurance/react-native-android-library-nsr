package eu.neosurance.sdk;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import eu.neosurance.utils.NSRUtils;

public class NSRFences {

	private GeofencingClient fenceClient = null;
	private PendingIntent fenceIntent = null;
	private NSR nsr = null;

	protected NSRFences(NSR nsr) {
		this.nsr = nsr;
	}

	private synchronized void initFence() {
		if (fenceClient == null) {
			NSRLog.d("initFence");
			fenceClient = LocationServices.getGeofencingClient(nsr.ctx);
		}
	}

	protected void traceFence() {
		if (fenceIntent != null) {
			NSRLog.d("traceFence already done");
			return;
		}
		NSRLog.d("traceFence");
		try {
			boolean fine = ActivityCompat.checkSelfPermission(nsr.ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
			JSONObject conf = NSRUtils.getConf(nsr.ctx.getApplicationContext());
			JSONArray fences = getFences();
			if (fine && conf != null && fences != null && NSRUtils.getBoolean(conf.getJSONObject("fence"), "enabled")) {
				initFence();
				List<Geofence> fenceList = new ArrayList();
				for (int i = 0; i < fences.length(); i++) {
					JSONObject fence = fences.getJSONObject(i);
					NSRLog.d("adding fence: " + fence.toString());
					fenceList.add(new Geofence.Builder()
									.setRequestId(fence.getString("id"))
									.setCircularRegion(fence.getDouble("latitude"), fence.getDouble("longitude"), (float) fence.getDouble("radius"))
									.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
									.setLoiteringDelay(fence.getInt("delay") * 1000)
									.setExpirationDuration(Geofence.NEVER_EXPIRE)
									.build());
				}
				GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
				builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL | GeofencingRequest.INITIAL_TRIGGER_ENTER);
				builder.addGeofences(fenceList);
				NSRLog.d("addGeofences");
				fenceIntent = PendingIntent.getBroadcast(nsr.ctx, 0, new Intent(nsr.ctx, NSRFenceCallback.class), PendingIntent.FLAG_UPDATE_CURRENT);
				fenceClient.addGeofences(builder.build(), fenceIntent).addOnFailureListener(new OnFailureListener() {
					public void onFailure(Exception e) {
						NSRLog.e("addGeofences failed", e);
						fenceIntent = null;
					}
				});
			}
		} catch (JSONException e) {
			NSRLog.e("traceFence", e);
		}
	}

	protected synchronized void stopTraceFence() {
		if (fenceClient != null && fenceIntent != null) {
			NSRLog.d("stopTraceFence");
			Task t = fenceClient.removeGeofences(fenceIntent);
			int i = 0;
			while (!t.isComplete()) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
				}
				i++;
			}
			NSRLog.d("stopTraceFence iterations: " + i);
			fenceIntent = null;
		}
	}

	private JSONArray getFences() {
		try {
			if (NSRUtils.getSharedPreferences(nsr.ctx).contains("fences"))
				return new JSONArray(NSRUtils.getSharedPreferences(nsr.ctx).getString("fences", "[]"));
			else
				return null;
		} catch (JSONException e) {
			return null;
		}
	}

	private void setFences(JSONArray fences) {
		SharedPreferences.Editor editor = NSRUtils.getSharedPreferences(nsr.ctx).edit();
		if (fences != null) {
			editor.putString("fences", fences.toString());
		} else {
			editor.remove("fences");
		}
		editor.commit();
	}

	protected void activateFences(JSONArray fences) {
		NSRLog.d("activateFences");
		if (fences != null) {
			synchronized (this) {
				JSONArray storedFences = getFences();
				String currentFences = storedFences != null ? storedFences.toString() : "";
				if (!currentFences.equals(fences.toString())) {
					stopTraceFence();
					setFences(fences);
					traceFence();
				}
			}
		}
	}

	protected void removeFences() {
		NSRLog.d("removeFences");
		synchronized (this) {
			setFences(null);
			stopTraceFence();
		}
	}

}
