package eu.neosurance.sdk;

import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import org.json.JSONObject;
import eu.neosurance.utils.NSRUtils;

public class NSRLocationCallback extends LocationCallback {
	private NSR nsr;
	private FusedLocationProviderClient locationClient;

	public NSRLocationCallback(NSR nsr, FusedLocationProviderClient locationClient) {
		this.nsr = nsr;
		this.locationClient = locationClient;
	}

	public void onLocationResult(LocationResult locationResult) {
		JSONObject conf = NSRUtils.getConf(nsr.ctx);
		if (conf == null)
			return;
		nsr.opportunisticTrace();
		nsr.checkHardTraceLocation();
		Location lastLocation = locationResult.getLastLocation();
		if (lastLocation != null) {
			try {
				if (locationClient != null) {
					locationClient.removeLocationUpdates(this);
				}
				NSRLog.d("NSRLocationCallback: " + lastLocation);
				String backgroundLocation = lastLocation.getLatitude() + "|" + lastLocation.getLongitude();
				if (!backgroundLocation.equals(nsr.getBackgroundLocation())) {
					nsr.setBackgroundLocation(backgroundLocation);
					NSRLog.d("NSRLocationCallback sending");
					JSONObject payload = new JSONObject();
					payload.put("latitude", lastLocation.getLatitude());
					payload.put("longitude", lastLocation.getLongitude());
					payload.put("altitude", lastLocation.getAltitude());
					nsr.crunchEvent("position", payload,nsr.ctx);
					NSRLog.d("NSRLocationCallback sent");
				} else {
					NSRLog.d("NSRLocationCallback already sent: " + backgroundLocation);
				}
			} catch (Exception e) {
				NSRLog.e("NSRLocationCallback", e);
			}
		}
	}
}
