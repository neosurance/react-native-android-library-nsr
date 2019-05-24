package eu.neosurance.sdk;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import org.json.JSONObject;

import io.ionic.starter.R;

public class NSRPush{

    public JSONObject jsonPush;
    public Context ctx;
    public JSONObject settings;
    public NSRPushDelegate pushDelegate;

    private static final String CHANNEL_ID = "NSRNotification";

    NSRPush(JSONObject push, Context ctx,JSONObject settings, NSRPushDelegate pushDelegate){

        this.jsonPush = push;
        this.ctx = ctx;
        this.settings = settings;
        this.pushDelegate = pushDelegate;
    }

    public void buildAndShowDelayedPush(String pid, JSONObject push, int delay){

        if (Build.VERSION.SDK_INT >= 21) {
            Intent intent = new Intent(ctx, NSRDelayedPush.class);
            intent.putExtra("push", push.toString());
            PendingIntent pushIntent = PendingIntent.getBroadcast(ctx, pid.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT);
            ((AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE)).setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay * 1000, pushIntent);
        }

    }

    public void buildAndShowPush(){
        try {

           if(Build.VERSION.SDK_INT >= 26) {
                NotificationManager notificationManager = ctx.getSystemService(NotificationManager.class);
                NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (channel == null) {
                    channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH);
                    channel.setSound(Uri.parse("android.resource://" + ctx.getPackageName() + "/" + R.raw.push), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
                    notificationManager.createNotificationChannel(channel);
                }
            }
            NotificationCompat.Builder notification = new NotificationCompat.Builder(ctx, CHANNEL_ID);
            notification.setSound(Uri.parse("android.resource://" + ctx.getPackageName() + "/" + R.raw.push));
            try {
                notification.setSmallIcon(this.settings.getInt("push_icon"));
            } catch (Exception e) {
                notification.setSmallIcon(R.drawable.nsr_logo);
            }
            if (this.jsonPush.has("title") && this.jsonPush.getString("title").trim() != "") {
                notification.setContentTitle(this.jsonPush.getString("title"));
            }
            notification.setContentText(this.jsonPush.getString("body"));
            notification.setStyle(new NotificationCompat.BigTextStyle().bigText(this.jsonPush.getString("body")));
            notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            notification.setPriority(NotificationCompat.PRIORITY_HIGH);

            notification.setAutoCancel(true);
            String url = this.jsonPush.has("url") ? this.jsonPush.getString("url") : null;
            PendingIntent pendingIntent = null;
            if (url != null && !"".equals(url)) {
                if (this.pushDelegate != null) {
                    pendingIntent = this.pushDelegate.makePendingIntent(ctx, this.jsonPush);
                } else {
                    pendingIntent = PendingIntent.getActivity(ctx, (int) System.currentTimeMillis(), makeActivityWebView(url), PendingIntent.FLAG_UPDATE_CURRENT);
                }
            }
            if (pendingIntent != null) {
                notification.setContentIntent(pendingIntent);
            }
            NotificationManagerCompat.from(ctx).notify((int) System.currentTimeMillis(), notification.build());


        } catch (Exception e) {
        }
    }

    public static void killPush(String pid,Context currentCtx) {
        if (Build.VERSION.SDK_INT >= 21) {
            ((AlarmManager) currentCtx.getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getBroadcast(currentCtx, pid.hashCode(), new Intent(currentCtx, NSRDelayedPush.class), PendingIntent.FLAG_ONE_SHOT));
        }
    }

    protected Intent makeActivityWebView(String url) throws Exception {
        NSRLog.d("showUrl makeActivityWebView " + ctx);
        Intent intent = new Intent(ctx, NSRActivityWebView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra("url", url);
        return intent;
    }

}
