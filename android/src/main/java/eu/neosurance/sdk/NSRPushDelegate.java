package eu.neosurance.sdk;

import android.app.PendingIntent;
import android.content.Context;

import org.json.JSONObject;

public interface NSRPushDelegate {
	PendingIntent makePendingIntent(Context ctx, JSONObject push);
}