package eu.neosurance.sdk;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.google.android.gms.location.ActivityRecognitionClient;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONObject;

//import eu.neosurance.cordova.NSRCordovaInterface;
//import eu.neosurance.utils.NSRShake;
import java.io.IOException;
import java.util.Properties;

import eu.neosurance.utils.NSRUtils;

public class NSR {

	protected static final String TAG = "nsr";
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
					instance.fences = new NSRFences(instance);

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
					editor.remove("securityDelegateClass"); editor.remove("workflowDelegateClass"); editor.remove("pushDelegateClass");
					editor.remove("conf"); editor.remove("settings"); editor.remove("user"); editor.remove("auth"); editor.remove("appURL");
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

	public void setup(NSRSettings settings){ //, JSONObject jsonShake) {
		if (NSRUtils.gracefulDegradate()) {
			return;
		}

		//NSRShake.setShakeLabelAndPayload(jsonShake);

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
		} catch (Exception e) {
			NSRLog.e("setup", e);
		}
	}

	//********** INIT_JOB **********//

	public static void initJob() {
		NSRLog.d("initJob");
		try {
			stopTraceFence();
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
		traceFence();
		traceLocation();
		hardTraceLocation();
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

	public static void loginExecuted(String url, CallbackContext NSR_LoginExecutedCallback) {
		urlX = url;
		NSRUser.loginExecuted(urlX,ctx,NSR_LoginExecutedCallback);
	}

	public static void paymentExecuted(JSONObject paymentInfo, String url, CallbackContext NSR_PaymentExecutedCallback) {
		urlX = url;
		NSRUser.paymentExecuted(paymentInfo,urlX,NSR_PaymentExecutedCallback);
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

/*
	public void setNSRShake(Context ctx, NSRShake nsrShake){
		nsrShake.setNSRShake(ctx);
	}

	public static void NSROnResume() {
		NSRShake.NSRShakeOnResume();
	}

	public static void NSROnStop() {
		NSRShake.NSRShakeOnStop();
	}
*/

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

	public void showApp(CallbackContext callbackContext) {

		String urlTmp = NSRUtils.getAppURL(ctx);
		if (urlTmp != null)
			showUrl(urlTmp, null, callbackContext);
	}

	public void showApp(JSONObject params, CallbackContext callbackContext) {

		String urlTmp = NSRUtils.getAppURL(ctx);
		if (urlTmp != null)
			showUrl(urlTmp, params, callbackContext);
	}

	public static void showUrl(String url, CallbackContext callbackContext) {
		urlX = url;
		showUrl(url, null, callbackContext);
	}

	public static synchronized void showUrl(String url, JSONObject params, CallbackContext callbackContext) {
		urlX = url;
		if(url != null && url.trim().length() > 0)
			NSRUtils.showUrl(url,params,ctx, callbackContext);
		else if(callbackContext != null)
			callbackContext.error("No url found");
	}

	public static Intent makeActivityWebView(String url) throws Exception {
		urlX = url;
		return NSRUtils.makeActivityWebView(urlX,ctx);
	}

}
