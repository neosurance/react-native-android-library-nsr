package eu.neosurance.sdk_ext;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Properties;

import eu.neosurance.cordova.NSRCordovaInterface;
import eu.neosurance.sdk.NSR;
import eu.neosurance.sdk.NSRSettings;
import eu.neosurance.sdk.NSRUser;

import static eu.neosurance.cordova.NSRCordovaInterface.NSR_LoginExecutedCallback;


public class NSRActivity extends CordovaActivity {
	public final static String TAG = "sdk_ext_";
	private WebView mainView;
	private BroadcastReceiver wfReceiver;
	private Properties config;
	public static boolean created = false;
	public static boolean ready = true;
	public static Context demoContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		created = true;
		config = new Properties();
		try {
			config.load(this.getAssets().open("config.properties"));

			Log.d(TAG, "NsrActivity onCreate");
			super.onCreate(savedInstanceState);

			this.demoContext = getApplicationContext();

			WebView.setWebContentsDebuggingEnabled(true);
			mainView = new WebView(this);
			mainView.getSettings().setJavaScriptEnabled(true);
			mainView.getSettings().setDomStorageEnabled(true);
			mainView.getSettings().setAllowFileAccessFromFileURLs(true);
			mainView.getSettings().setAllowUniversalAccessFromFileURLs(true);
			mainView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
			mainView.addJavascriptInterface(this, "demo");

			mainView.setWebViewClient(new WebViewClient() {
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					if (url.endsWith(".pdf") || url.contains(".pdf?")) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse(url), "application/pdf");
						intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
						startActivity(intent);
					} else {
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						startActivity(browserIntent);
					}
					return true;
				}
			});


			//mainView.loadUrl("file:///android_asset/app.html");
			setContentView(mainView);
			//setup();
			//wfReceiver = new WFReceiver(this);
			//LocalBroadcastManager.getInstance(this).registerReceiver(wfReceiver, new IntentFilter("WFStuff"));

			Intent intent = new Intent(demoContext, io.ionic.starter.MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			demoContext.startActivity(intent);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void setup() {

		Log.d(TAG, "setup ctx: " + getApplicationContext());
		NSRSettings settings = new NSRSettings();
		settings.setBaseUrl(config.getProperty("base_url"));
		settings.setCode(config.getProperty("code"));
		settings.setSecretKey(config.getProperty("secret_key"));
		settings.setDevMode(true);
		settings.setWorkflowDelegate(new WFDelegate(),getApplicationContext());
		NSR.getInstance(this).askPermissions(this);

		//SHAKE SETTINGS
		/*
		JSONObject jsonShake = new JSONObject();
		try {
			jsonShake.put("label","inAirport");
			jsonShake.put("payload",new JSONObject());
			NSR.getInstance(this).setup(settings);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		*/

	}

	public void appLogin(){

		Log.d(TAG, "appLogin");
		String url = WFDelegate.getData(this, "login_url");
		if (url != null) {
			NSR.getInstance(this).loginExecuted(url, NSRCordovaInterface.NSR_LoginExecutedCallback);
			WFDelegate.setData(this, "login_url", null);
		}

	}

	public void appPayment() {

		Log.d(TAG, "appPayment");
		try {
			String url = WFDelegate.getData(this, "payment_url");
			if (url != null) {
				JSONObject paymentInfo = new JSONObject();
				paymentInfo.put("transactionCode", "fakeTransactionCode");
				paymentInfo.put("iban", "fakeClientIban");
				NSR.getInstance(this).paymentExecuted(paymentInfo, url, NSRCordovaInterface.NSR_PaymentExecutedCallback);
				WFDelegate.setData(this, "payment_url", null);
			}
		} catch (Exception e) {
			Log.e(TAG, "appPayment", e);
		}

	}

	protected void eval(final String code) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				if(mainView != null)
					mainView.evaluateJavascript(code, null);

			}
		});
	}

	@JavascriptInterface
	public void postMessage(final String json) {
		try {
			final JSONObject body = new JSONObject(json);
			if (body.has("log")) {
				Log.i(TAG, body.getString("log"));
			}
			if (body.has("event") && body.has("payload")) {
				Log.i(TAG, "event: " + body.getString("event"));
				NSR.getInstance(this).sendEvent(body.getString("event"), body.getJSONObject("payload"));
			}
			if (body.has("action")) {
				NSR.getInstance(this).sendAction(body.getString("action"), body.getString("code"), body.getString("details"));
			}
			//if (body.has("test")) {
			//	NSR.getInstance(this).showUrl("https://neosuranceprd.s3.eu-west-1.amazonaws.com/apps/02ac1983d6383b035a7d20bdcca25fc3/app.html?code=1PcnUFR8jFNSF8jDOe");
			//}
			if (body.has("what")) {
				String what = body.getString("what");
				if ("ready".equals(what)) {
					ready = true;
				}
				if ("registerUser".equals(what) && body.has("user")) {
					NSR.getInstance(this).registerUser(new NSRUser(body.getJSONObject("user")));
				}
				if ("loginExecuted".equals(what)) {
					appLogin();
				}
				if ("paymentExecuted".equals(what)) {
					appPayment();
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "postMessage", e);
		}
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "NsrActivity onDestroy");
		ready = false;
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				try {
					if (mainView != null) {
						LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(wfReceiver);
						mainView.stopLoading();
						mainView.destroy();
						mainView = null;
					}
				} catch (Throwable e) {
					Log.d(TAG,e.getMessage());
				}
			}
		});
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "NsrActivity onStart");
		super.onStart();
	}

	@Override
	protected void onRestart() {
		Log.d(TAG, "NsrActivity onRestart");
		super.onRestart();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "NsrActivity onPause");
		super.onPause();
	}


	@Override
	protected void onResume() {
		Log.d(TAG, "NsrActivity onResume");
		super.onResume();

		//NSR.NSROnResume();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "NsrActivity onStop");

		//NSR.NSROnStop();

		super.onStop();
	}

}
