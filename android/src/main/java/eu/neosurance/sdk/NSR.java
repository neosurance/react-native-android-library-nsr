package eu.neosurance.sdk;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import eu.neosurance.utils.NSRShake;
import eu.neosurance.utils.NSRUtils;

public class NSR {

	public static final String TAG = "nsr";
	public static final int PERMISSIONS_MULTIPLE_ACCESSLOCATION = 0x2043;
	protected static final int PERMISSIONS_MULTIPLE_IMAGECAPTURE = 0x2049;
	protected static final int REQUEST_IMAGE_CAPTURE = 0x1702;

	public static NSR instance = null;
	public static NSREventWebView eventWebView = null;

	public static NSRSecurityDelegate securityDelegate = null;
	public static NSRWorkflowDelegate workflowDelegate = null;
	public static NSRPushDelegate pushDelegate = null;

	public static NSRActivityWebView activityWebView = null;

	public static NSRFences fences = null;
	public static Intent foregrounder = null;

	public static ActivityRecognitionClient activityClient = null;
	public static PendingIntent activityIntent = null;

	public String backgroundLocation = null;
	public static Context ctx = null;

	public NSREvent nsrEvent;

	public static String urlX = null;

	public static WifiManager wifiManager;
	public static JSONObject networks;

	protected NSR(Context ctx) {
		this.ctx = ctx.getApplicationContext();
		//NSRShake nsrShake = new NSRShake();
		//setNSRShake(ctx,nsrShake);
	}

	//********** GET_INSTANCE **********//

	public static NSR getInstance(Context ctx) {

		if (instance == null) {
			NSRLog.d("making instance...");
			instance = new NSR(ctx);
			if (!NSRUtils.gracefulDegradate()) {
				try {
					//instance.fences = new NSRFences(instance);

					String s = NSRUtils.getData("securityDelegateClass", ctx);
					if (s != null) {
						NSRLog.d("making securityDelegate... " + s);
						instance.setSecurityDelegate((NSRSecurityDelegate) Class.forName(s).newInstance());
					} else {
						NSRLog.d("making securityDelegate... NSRDefaultSecurity");
						instance.setSecurityDelegate(new NSRDefaultSecurity());
					}
					s = NSRUtils.getData("workflowDelegateClass", ctx);
					if (s != null) {
						NSRLog.d("making workflowDelegate... " + s);
						instance.setWorkflowDelegate((NSRWorkflowDelegate) Class.forName(s).newInstance());
					}
					s = NSRUtils.getData("pushDelegateClass", ctx);
					if (s != null) {
						NSRLog.d("making pushDelegateClass... " + s);
						instance.setPushDelegate((NSRPushDelegate) Class.forName(s).newInstance());
					}
					instance.initJob();
				} catch (Exception e) {
					NSRLog.e("init", e);
					NSRLog.d("makePristine....");
					SharedPreferences.Editor editor = NSRUtils.getSharedPreferences(ctx).edit();
					editor.remove("securityDelegateClass");
					editor.remove("workflowDelegateClass");
					editor.remove("pushDelegateClass");
					editor.remove("conf");
					editor.remove("settings");
					editor.remove("user");
					editor.remove("auth");
					editor.remove("appURL");
					editor.commit();

					instance.stopHardTraceLocation();
					instance.setHardTraceEnd(0);
					instance.stopTraceActivity();

					NSRLog.d("pristine!");
					instance.setSecurityDelegate(new NSRDefaultSecurity());
				}
			}
		}else
			instance.ctx = ctx.getApplicationContext();

		return instance;
	}

	//********** SETUP **********//

	public void setup(NSRSettings settings, JSONObject jsonShake, final NSRSecurityResponse responseHandler) {
		if (NSRUtils.gracefulDegradate()) {
			return;
		}

		NSRShake.setShakeLabelAndPayload(jsonShake);

		if (settings.getSecurityDelegate() != null) {
			setSecurityDelegate(settings.getSecurityDelegate());
		}
		if (settings.getWorkflowDelegate() != null) {
			setWorkflowDelegate(settings.getWorkflowDelegate());
		}
		if (settings.getPushDelegate() != null) {
			setPushDelegate(settings.getPushDelegate());
		}
		NSRLog.enabled = !settings.isDisableLog();

		try {
			NSRLog.d("setup: " + settings.toJsonObject());
			if (settings.getMiniappSkin() != null) {
				NSRUtils.storeData("skin", settings.getMiniappSkin(),ctx);
			}
			NSRUtils.setSettings(settings.toJsonObject(),ctx);
			
			JSONObject setupComplete = new JSONObject();
			try {
				setupComplete.put("result","SETUP COMPLETE!");				
			} catch (JSONException e) {
				e.printStackTrace();
			}			
			responseHandler.completionHandler(setupComplete, null);
			
		} catch (Exception e) {
			NSRLog.e("setup", e);
		}

	}

	//********** INIT_JOB **********//

	public static void initJob() {
		NSRLog.d("initJob");
		try {
			//stopTraceFence();
			stopHardTraceLocation();
			stopTraceActivity();

			if (!synchEventWebView()) {
				continueInitJob();
			}
		} catch (Exception e) {
			NSRLog.e("initJob", e);
		}
	}

	//********** CONTINUE_INIT_JOB **********//

	public static void continueInitJob() {
		traceActivity();
		traceLocation();
		hardTraceLocation();
		//traceFence();
		//traceNetworks();
	}

	//********** TRACE ACTIVITY **********//

	public static synchronized void initActivity() {
		NSRTrace.initActivity(ctx);
	}

	public static void traceActivity() {
		NSRTrace.traceActivity(ctx);
	}

	public static synchronized void stopTraceActivity() {
		NSRTrace.stopTraceActivity();
	}

	public String getLastActivity() {
		return NSRTrace.getLastActivity(ctx);
	}

	protected void setLastActivity(String lastActivity) {
		NSRTrace.setLastActivity(lastActivity,ctx);
	}

	//********** TRACE_FENCE **********//

	public static synchronized void traceFence() {
		fences.traceFence();
	}

	public static synchronized void stopTraceFence() {
		fences.stopTraceFence();
	}

	protected void activateFences(JSONArray fenceArray) {
		fences.activateFences(fenceArray);
	}

	protected void removeFences() {
		fences.removeFences();
	}

	//********** TRACE LOCATION **********//

	public static void traceLocation() {
		NSRTrace.traceLocation(ctx);
	}

	public static void hardTraceLocation() {
		NSRTrace.hardTraceLocation(ctx);
	}

	protected static synchronized void stopHardTraceLocation() {
		NSRTrace.stopHardTraceLocation(ctx);
	}

	//********** OPPORTUNISTIC_TRACE **********//

	public void opportunisticTrace() {
		NSRTrace.opportunisticTrace(ctx);
	}

	//********** TRACE NETWORKS **********//

	public static void traceNetworks(){
		NSRTrace.traceNetworks(ctx);
	}


	public static String getLastLocationAuth() {
		return NSRTrace.getLastLocationAuth(ctx);
	}

	public static void setLastLocationAuth(String locationAuth) {
		NSRTrace.setLastLocationAuth(locationAuth,ctx);
	}

	protected String getLastPushAuth() {
		return NSRTrace.getLastPushAuth(ctx);
	}

	protected void setLastPushAuth(String pushAuth) {
		NSRTrace.setLastPushAuth(pushAuth,ctx);
	}

	//********** CONNECTION **********//

	public static void traceConnection() {
		NSRConnection.traceConnection(securityDelegate,ctx);
	}

	protected String getLastConnection() {
		return NSRUtils.getData("lastConnection",ctx);
	}

	protected void setLastConnection(String lastConnection) {
		NSRUtils.setData("lastConnection", lastConnection, ctx);
	}

	//********** ACTION **********//

	public void sendAction(final String name, final String policyCode, final String details) {
		NSRAction.sendAction(name,policyCode,details,ctx);
	}

	//********** TRACE POWER **********//

	public static void tracePower() {
		NSRPower nsrPower = new NSRPower();
		nsrPower.tracePower(securityDelegate,ctx);
	}

	protected String getLastPower() {
		NSRPower nsrPower = new NSRPower();
		return nsrPower.getLastPower(ctx);
	}

	protected void setLastPower(String lastPower) {
		NSRPower nsrPower = new NSRPower();
		nsrPower.setLastPower(ctx,lastPower);
	}

	protected int getLastPowerLevel() {
		NSRPower nsrPower = new NSRPower();
		return nsrPower.getLastPowerLevel(ctx);
	}

	protected void setLastPowerLevel(int lastPower) {
		NSRPower nsrPower = new NSRPower();
		nsrPower.setLastPowerLevel(ctx,lastPower);
	}

	//********** TRACE ACCURATE LOCATION **********//

	public void accurateLocation(double meters, int duration, boolean extend) {
		NSRTrace.accurateLocation(meters,duration,extend,ctx);
	}

	public static int getBackgroundTime() {
		return NSRBackground.getBackgroundTime(ctx);
	}

	public void accurateLocationEnd() {
		NSRTrace.accurateLocationEnd(ctx);
	}

	protected void checkHardTraceLocation() {
		NSRTrace.checkHardTraceLocation(ctx);
	}

	public static boolean isHardTraceLocation() {
		return NSRTrace.isHardTraceLocation(ctx);
	}

	public static int getHardTraceEnd() {
		return NSRTrace.getHardTraceEnd(ctx);
	}

	protected void setHardTraceEnd(int hardTraceEnd) {
		NSRUtils.setData("hardTraceEnd", "" + hardTraceEnd, ctx);
	}

	public static double getHardTraceMeters() {
		return NSRTrace.getHardTraceEnd(ctx);
	}

	protected void setHardTraceMeters(double hardTraceMeters) {
		NSRTrace.setHardTraceMeters(hardTraceMeters,ctx);
	}

	//********** EVENTS **********//

	public void crunchEvent(final String event, final JSONObject payload, final Context ctx) {
		if(this.nsrEvent == null)
			this.nsrEvent = new NSREvent(securityDelegate, ctx, eventWebView);
		this.nsrEvent.crunchEvent(event,payload,ctx);
	}

	private void localCrunchEvent(final String event, final JSONObject payload) {
		if(this.nsrEvent == null)
			this.nsrEvent = new NSREvent(securityDelegate, ctx, eventWebView);
		this.nsrEvent.localCrunchEvent(event,payload);
	}

	public void sendEvent(final String event, final JSONObject payload){
		if(this.nsrEvent == null)
			this.nsrEvent = new NSREvent(securityDelegate, ctx, eventWebView);
		nsrEvent.sendEvent(event,payload);
	}

	public void archiveEvent(final String event, final JSONObject payload) {
		NSREvent.archiveEvent(event,payload,ctx,securityDelegate);
	}

	public static void resetCruncher() {
		NSREvent.resetCruncher();
	}

	//********** SNAPSHOT **********//

	public synchronized JSONObject snapshot(final String event, final JSONObject payload, final Context ctx) {
		JSONObject snapshot = snapshot(ctx);
		try {
			snapshot.put(event, payload);
		} catch (Exception e) {
		}
		NSRUtils.setJSONData("snapshot", snapshot, ctx);
		return snapshot;
	}

	public synchronized JSONObject snapshot(final Context ctx) {
		JSONObject snapshot = NSRUtils.getJSONData("snapshot",ctx);
		if (snapshot == null) {
			snapshot = new JSONObject();
		}
		return snapshot;
	}

	//********** LOGIN AND PAYMENT **********//

	public static void loginExecuted(String url) {
		urlX = url;
		NSRUser.loginExecuted(urlX,ctx);
	}

	public static void paymentExecuted(JSONObject paymentInfo, String url) {
		urlX = url;
		NSRUser.paymentExecuted(paymentInfo,urlX);
	}

	//********** PUSH NOTIFICATIONS (NSRPush) **********//

	protected void showPush(String pid, final JSONObject push, int delay) {
		NSRPush nsrPush = new NSRPush(push,ctx,NSRUtils.getSettings(ctx),pushDelegate);
		nsrPush.buildAndShowDelayedPush(pid,push,delay);
	}

	protected void killPush(String pid, Context currentCtx) {
		NSRPush.killPush(pid,currentCtx);
	}

	public static void showPush(JSONObject push) {
		if (NSRUtils.gracefulDegradate()) {
			return;
		}
		NSRPush nsrPush = new NSRPush(push,ctx,NSRUtils.getSettings(ctx),pushDelegate);
		nsrPush.buildAndShowPush();
	}

	//********** SHAKE **********//

	public void setNSRShake(Context ctx, NSRShake nsrShake){
		nsrShake.setNSRShake(ctx);
	}

	public static void NSROnResume() {
		NSRShake.NSRShakeOnResume();
	}

	public static void NSROnStop() {
		NSRShake.NSRShakeOnStop();
	}

	//********** NSR_USER **********//

	public void askPermissions(Activity activity) {
		NSRUtils.askPermissions(activity);
	}

	public void registerUser(NSRUser user) {
		NSRUser.registerUser(user,ctx);
	}

	public void forgetUser() {
		NSRUser.forgetUser(ctx);
	}

	public static void authorize(final NSRAuth delegate) throws Exception {
		NSRUser.authorize(delegate,ctx);
	}

	//********** BACKGROUND **********//

	public String getBackgroundLocation() {
		return backgroundLocation;
	}

	protected void setBackgroundLocation(String backgroundLocation) {
		this.backgroundLocation = backgroundLocation;
	}

	public static void initBackground(int delay) {
		NSRBackground.initBackground(delay,ctx);
	}

	//********** WEB_VIEW **********//

	public static boolean synchEventWebView() {
		return NSRUtils.synchEventWebView(ctx);
	}

	public static boolean needsInitJob(JSONObject conf, JSONObject oldConf) throws Exception {
		return (oldConf == null) || !oldConf.toString().equals(conf.toString()) || (eventWebView == null && NSRUtils.getBoolean(conf, "local_tracking"));
	}

	public static void registerWebView(NSRActivityWebView activityWebViewTmp) {
		NSRUtils.registerWebView(activityWebViewTmp);
	}

	protected void clearWebView() {
		activityWebView = null;
	}

	//********** NSR_SECURITY_DELEGATE **********//

	public static NSRSecurityDelegate getSecurityDelegate() {
		return NSRSettings.getSecurityDelegate();
	}

	public static void setSecurityDelegate(NSRSecurityDelegate securityDelegate) {
		NSRSettings.setSecurityDelegate(securityDelegate,ctx);
	}

	//********** NSR_WORK_FLOW_DELEGATE **********//

	public static NSRWorkflowDelegate getWorkflowDelegate() {
		return NSRSettings.getWorkflowDelegate();
	}

	public static void setWorkflowDelegate(NSRWorkflowDelegate workflowDelegate) {
		NSRSettings.setWorkflowDelegate(workflowDelegate, ctx);
	}

	//********** NSR_PUSH_DELEGATE **********//

	protected NSRPushDelegate getPushDelegate() {
		return NSRSettings.getPushDelegate();
	}

	public void setPushDelegate(NSRPushDelegate pushDelegate) {
		NSRSettings.setPushDelegate(pushDelegate);
	}

	//********** UTILS **********//

	public void policies(final JSONObject criteria, final NSRSecurityResponse responseHandler){
		NSRUtils.policies(criteria, responseHandler, ctx);
	}

	public void closeView(){
		NSRUtils.closeView();
	}

	public void showApp() {
		if (NSRUtils.getAppURL(ctx) != null)
			showUrl(NSRUtils.getAppURL(ctx), null);
	}

	public void showApp(JSONObject params) {
		if (NSRUtils.getAppURL(ctx) != null)
			showUrl(NSRUtils.getAppURL(ctx), params);
	}

	public static void showUrl(String url) {
		urlX = url;
		showUrl(url, null);
	}

	public static synchronized void showUrl(String url, JSONObject params) {
		urlX = url;
		NSRUtils.showUrl(url,params,ctx);
	}

	public static Intent makeActivityWebView(String url) throws Exception {
		urlX = url;
		return NSRUtils.makeActivityWebView(urlX,ctx);
	}

	public boolean isAppOnForeground(Context context,String appPackageName) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		if (appProcesses == null) {
			return false;
		}
		final String packageName = appPackageName;
		for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
				//                Log.e("app",appPackageName);
				return true;
			}
		}
		return false;
	}

	public String getCurrentSsid(Context context) {
		String ssid = null;
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo.isConnected()) {
			final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
				ssid = connectionInfo.getSSID();
			}
		}
		return ssid;
	}

	public void getWifiNetworks(){

		wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);

		BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent intent) {
				boolean success = intent.getBooleanExtra(
						WifiManager.EXTRA_RESULTS_UPDATED, false);
				if (success) {
					scanSuccess();
				} else {
					// scan failure handling
					scanFailure();
				}
			}
		};

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		ctx.registerReceiver(wifiScanReceiver, intentFilter);

		boolean success = wifiManager.startScan();
		if (!success) {
			// scan failure handling
			scanFailure();
		}

	}

	public void scanSuccess() {
		List<ScanResult> results = wifiManager.getScanResults();

		networks = new JSONObject();
		JSONArray netsArray = new JSONArray();

		for(ScanResult net : results){

			Log.d(TAG,"\n\nNET: " + net);
			JSONObject netjson = new JSONObject();
			try {
				netjson.put("BSSID",net.BSSID);
				netjson.put("SSID",net.SSID);
				netjson.put("capabilities",net.capabilities);
				netjson.put("centerFreq0",net.centerFreq0);
				netjson.put("centerFreq1",net.centerFreq1);
				netjson.put("channelWidth",net.channelWidth);
				netjson.put("frequency",net.frequency);
				netjson.put("level",net.level);
				netjson.put("operatorFriendlyName",net.operatorFriendlyName);
				netjson.put("timestamp",net.timestamp);
				netjson.put("venueName",net.venueName);

				//netjson.put("distanceCm",net.distanceCm);
				//netjson.put("distanceSdCm",net.distanceSdCm);
				//netjson.put("seen",net.seen);
				//netjson.put("untrusted",net.untrusted);
				//netjson.put("wifiSsid",net.wifiSsid);

				netsArray.put(netjson);

			} catch (JSONException e) {
				e.printStackTrace();
			}

		}

		try {
			networks.put("networks",netsArray);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		NSR.getInstance(ctx).sendEvent("changeNetworksState", networks);

	}

	public void scanFailure() {
		// handle failure: new scan did NOT succeed
		// consider using old scan results: these are the OLD results!
		List<ScanResult> results = wifiManager.getScanResults();
	}

}