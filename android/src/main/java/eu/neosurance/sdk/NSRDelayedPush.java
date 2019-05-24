package eu.neosurance.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

public class NSRDelayedPush extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			NSR.getInstance(context).showPush(new JSONObject(intent.getExtras().getString("push")));
		} catch (Exception e) {
		}
	}
}
