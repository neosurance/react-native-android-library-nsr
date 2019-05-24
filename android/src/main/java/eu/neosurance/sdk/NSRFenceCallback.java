package eu.neosurance.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import org.json.JSONObject;
import java.util.List;
import eu.neosurance.utils.NSRUtils;

public class NSRFenceCallback extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		NSRLog.d("NSRFenceCallback");
		final NSR nsr = NSR.getInstance(context);
		JSONObject conf = NSRUtils.getConf(context);
		if (conf == null)
			return;
		GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
		if (geofencingEvent.hasError()) {
			NSRLog.d("NSRFenceCallback: " + geofencingEvent.getErrorCode());
			nsr.stopTraceFence();
		} else {
			try {
				nsr.opportunisticTrace();
				Location location = geofencingEvent.getTriggeringLocation();

				NSRLog.d("NSRFenceCallback: " + location);
				List<Geofence> fencesList = geofencingEvent.getTriggeringGeofences();
				for (Geofence g : fencesList) {
					NSRLog.d("NSRFenceCallback sending: " + g.getRequestId());
					JSONObject payload = new JSONObject();
					payload.put("latitude", location.getLatitude());
					payload.put("longitude", location.getLongitude());
					payload.put("altitude", location.getAltitude());
					payload.put("fence", fenceType(geofencingEvent));
					payload.put("id", g.getRequestId());
					nsr.crunchEvent("fence", payload,context);
					NSRLog.d("NSRFenceCallback sent: " + g.getRequestId());
				}
			} catch (Exception e) {
				NSRLog.e("NSRFenceCallback", e);
			}

		}
	}

	private String fenceType(GeofencingEvent geofencingEvent) {
		switch (geofencingEvent.getGeofenceTransition()) {
			case Geofence.GEOFENCE_TRANSITION_DWELL:
				return "dwell";
			case Geofence.GEOFENCE_TRANSITION_ENTER:
				return "enter";
			case Geofence.GEOFENCE_TRANSITION_EXIT:
				return "exit";
		}
		return null;
	}
}
