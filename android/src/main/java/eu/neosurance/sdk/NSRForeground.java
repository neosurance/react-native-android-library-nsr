package eu.neosurance.sdk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import org.json.JSONException;
import eu.neosurance.utils.NSRUtils;

import io.ionic.starter.R;

public class NSRForeground extends Service {
	protected static final String SILENT_ID = "Background Execution";

	public NSRForeground() {
		super();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		NSRLog.d("NSRForeground onCreate");
		if (Build.VERSION.SDK_INT >= 26) {
			final NotificationManager notificationManager = getSystemService(NotificationManager.class);
			NotificationCompat.Builder notification = new NotificationCompat.Builder(this, SILENT_ID);
			notification.setSound(null);
			try {
				notification.setSmallIcon(NSRUtils.getSettings(eu.neosurance.sdk_ext.NSRActivity.demoContext).getInt("push_icon"));
			} catch (Exception e) {
				notification.setSmallIcon(R.drawable.nsr_logo);
			}
			try {

				String foregroundPushText = NSRUtils.getConf(eu.neosurance.sdk_ext.NSRActivity.demoContext).getString("foreground_push");
				if (foregroundPushText != null) {
					NSRLog.d("NSRForeground text: " + foregroundPushText);
					notification.setContentText(foregroundPushText);
					notification.setStyle(new NotificationCompat.BigTextStyle().bigText(foregroundPushText));
				}
			} catch (JSONException e) {
			}

			Notification n = notification.build();
			int id = (int) System.currentTimeMillis() / 1000;

			NotificationChannel channel = new NotificationChannel(SILENT_ID, SILENT_ID, NotificationManager.IMPORTANCE_LOW);
			channel.setSound(null, null);
			notificationManager.createNotificationChannel(channel);
			startForeground(id, n);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
