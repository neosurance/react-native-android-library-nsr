package eu.neosurance.sdk_ext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WFReceiver extends BroadcastReceiver {
	private NSRActivity nsrActivity;

	public WFReceiver(NSRActivity nsrActivity) {
		super();
		this.nsrActivity = nsrActivity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String message = intent.getStringExtra("message");
		Log.d("WFReceiver", "Got message: " + message);
		nsrActivity.eval(message);
	}
}
