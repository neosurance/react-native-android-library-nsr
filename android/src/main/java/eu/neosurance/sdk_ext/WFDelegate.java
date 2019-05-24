package eu.neosurance.sdk_ext;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.cordova.CordovaArgs;
import org.json.JSONException;
import org.json.JSONObject;

import eu.neosurance.cordova.NSRCordovaInterface;
import eu.neosurance.sdk.NSRWorkflowDelegate;

import static eu.neosurance.sdk_ext.NSRActivity.TAG;


public class WFDelegate implements NSRWorkflowDelegate {

	@Override
	public boolean executeLogin(final Context ctx, final String url) {
		Log.d(TAG, "executeLogin");
		Log.d(TAG, "NSRActivity: " + eu.neosurance.sdk_ext.NSRActivity.created + " " + eu.neosurance.sdk_ext.NSRActivity.ready);

		setData(ctx, "login_url", url);

		if (!eu.neosurance.sdk_ext.NSRActivity.ready) {
			eu.neosurance.sdk_ext.NSRActivity.ready = true;
			Log.d(TAG, "eu.neosurance.sdk_ext.NSRActivity not ready");
			if (!eu.neosurance.sdk_ext.NSRActivity.created) {
				Log.d(TAG, "eu.neosurance.sdk_ext.NSRActivity launching");
				launchMainActivity(ctx);
			}
			new Handler().postDelayed(new Runnable() {
				public void run() {
					executeLogin(ctx, url);
				}
			}, 500);
		} else {
			Log.d(TAG, "eu.neosurance.sdk_ext.NSRActivity ready");
			Intent intent = new Intent("WFStuff");
			intent.putExtra("message", "showLogin()");
			LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);

			JSONObject json = new JSONObject();
			try {

				json.put("url",url);
				NSRCordovaInterface.SetNSRLoggedUrl(json);

			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		return true;
	}

	@Override
	public JSONObject executePayment(final Context ctx, final JSONObject payment, final String url) {
		Log.d(TAG, "executePayment");
		Log.d(TAG, "eu.neosurance.sdk_ext.NSRActivity: " + eu.neosurance.sdk_ext.NSRActivity.created + " " + eu.neosurance.sdk_ext.NSRActivity.ready);

		setData(ctx, "payment_url", url);
		if (!eu.neosurance.sdk_ext.NSRActivity.ready) {
			Log.d(TAG, "eu.neosurance.sdk_ext.NSRActivity not ready");
			if (!eu.neosurance.sdk_ext.NSRActivity.created) {
				launchMainActivity(ctx);
			}
			new Handler().postDelayed(new Runnable() {
				public void run() {
					executePayment(ctx, payment, url);
				}
			}, 500);
		} else {
			Log.d(TAG, "wfReceiver OK");
			Intent intent = new Intent("WFStuff");
			intent.putExtra("message", "showPay()");
			LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
			try {
				payment.put("payment_url",url);
				NSRCordovaInterface.appPaymentHandler(payment);
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		return payment;
	}

	@Override
	public void confirmTransaction(Context ctx, JSONObject paymentInfo) {

	}

	public static String getData(Context ctx, String key) {
		SharedPreferences sp = ctx.getSharedPreferences("DemoIngPrefs", Application.MODE_PRIVATE);
		if (sp.contains(key)) {
			return sp.getString(key, "");
		} else {
			return null;
		}
	}

	public static void setData(Context ctx, String key, String value) {
		SharedPreferences sp = ctx.getSharedPreferences("DemoIngPrefs", Application.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		if (value != null) {
			editor.putString(key, value);
		} else {
			editor.remove(key);
		}
		editor.commit();
	}

	private void launchMainActivity(final Context ctx) {
		Intent intent = new Intent(ctx, eu.neosurance.sdk_ext.NSRActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ctx.startActivity(intent);
	}
}
