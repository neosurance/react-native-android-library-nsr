package eu.neosurance.sdk;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import static eu.neosurance.sdk.NSRActivity.TAG;


public class WFDelegate implements NSRWorkflowDelegate {

	public static Context contextWF;

	@Override
	public boolean executeLogin(final Context ctx, final String url) {
		Log.d(TAG, "executeLogin");
		Log.d(TAG, "NSRActivity: " + eu.neosurance.sdk.NSRActivity.created + " " + eu.neosurance.sdk.NSRActivity.ready);

		setData(ctx, "login_url", url);
		contextWF = ctx;

		if (!eu.neosurance.sdk.NSRActivity.ready) {
			eu.neosurance.sdk.NSRActivity.ready = true;
			Log.d(TAG, "eu.neosurance.sdk.NSRActivity not ready");
			if (!eu.neosurance.sdk.NSRActivity.created) {
				Log.d(TAG, "eu.neosurance.sdk.NSRActivity launching");
				launchMainActivity(ctx);
			}
			new Handler().postDelayed(new Runnable() {
				public void run() {
					executeLogin(ctx, url);
				}
			}, 500);
		} else {
			Log.d(TAG, "eu.neosurance.sdk.NSRActivity ready");
			Intent intent = new Intent("WFStuff");
			intent.putExtra("message", "showLogin()");
			LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);

			JSONObject json = new JSONObject();
			try {

				json.put("url",url);
				SetNSRLoggedUrl(json);

			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		return true;
	}

	//SetNSRLoggedUrl
	public static void SetNSRLoggedUrl(JSONObject data) throws JSONException {
		Log.d(TAG, "SetNSRLoggedUrl");

		String url = data.getString("url");
		setData(contextWF, "login_url", url);

	}

	@Override
	public JSONObject executePayment(final Context ctx, final JSONObject payment, final String url) {
		Log.d(TAG, "executePayment");
		Log.d(TAG, "eu.neosurance.sdk.NSRActivity: " + eu.neosurance.sdk.NSRActivity.created + " " + eu.neosurance.sdk.NSRActivity.ready);

		setData(ctx, "payment_url", url);
		setData(ctx, "payment", payment.toString());

		if (!eu.neosurance.sdk.NSRActivity.ready) {
			Log.d(TAG, "eu.neosurance.sdk.NSRActivity not ready");
			if (!eu.neosurance.sdk.NSRActivity.created) {
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
				appPaymentHandler(payment);
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		return payment;
	}

	public static void appPaymentHandler(JSONObject payment) throws JSONException {
		Log.d(TAG, "appPaymentHandler");

		String url = payment.getString("payment_url");
		setData(contextWF, "payment_url", url);

	}

	@Override
	public void confirmTransaction(Context ctx, JSONObject paymentInfo) {

	}
	
	@Override
	public void keepAlive() {
		Log.d(TAG, "keepAlive");
	}
	
	@Override
	public void goTo(final Context ctx, final String area) {
		Log.d(TAG, "goTo: " + area);
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
		Intent intent = new Intent(ctx, eu.neosurance.sdk.NSRActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		ctx.startActivity(intent);
	}
}
