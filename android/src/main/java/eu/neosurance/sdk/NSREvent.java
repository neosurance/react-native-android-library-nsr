package eu.neosurance.sdk;

import android.content.Context;
import android.os.Build;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.TimeZone;

import eu.neosurance.cordova.NSRCordovaInterface;
import eu.neosurance.utils.NSRUtils;

public class NSREvent{

    protected static final String PREFS_NAME = "NSRSDK";
    protected static final String TAG = "nsr";
    private final static byte[] K = Base64.decode("Ux44AGRuanL0y7qQDeasT3", Base64.NO_WRAP);
    private final static byte[] I = Base64.decode("ycB4AGR7a0fhoFXbpoHy43", Base64.NO_WRAP);

    public static NSRSecurityDelegate securityDelegate;
    public Context ctx;
    public static NSREventWebView eventWebView;

    public NSREvent(NSRSecurityDelegate securityDelegate, Context ctx, NSREventWebView eventWebView){
        this.securityDelegate = securityDelegate;
        this.ctx = ctx;
        this.eventWebView = eventWebView;
    }

    public static void archiveEvent(final String event, final JSONObject payload, final Context ctx, final NSRSecurityDelegate securityDelegate) {
        if (NSRUtils.gracefulDegradate()) {
            return;
        }
        NSRLog.d("archiveEvent - event: " + event + " payload: " + payload);
        try {
            NSR.authorize(new NSRAuth() {
                public void authorized(boolean authorized) throws Exception {
                    if (!authorized) {
                        return;
                    }
                    JSONObject eventPayLoad = new JSONObject();
                    eventPayLoad.put("event", event);
                    eventPayLoad.put("timezone", TimeZone.getDefault().getID());
                    eventPayLoad.put("event_time", System.currentTimeMillis());
                    eventPayLoad.put("payload", new JSONObject());

                    JSONObject devicePayLoad = new JSONObject();
                    devicePayLoad.put("uid", NSRUtils.getDeviceUid(ctx));

                    JSONObject userPayLoad = new JSONObject();
                    userPayLoad.put("code", NSRUser.getUser(ctx).getCode());

                    JSONObject requestPayload = new JSONObject();
                    requestPayload.put("event", eventPayLoad);
                    requestPayload.put("user", userPayLoad);
                    requestPayload.put("device", devicePayLoad);
                    requestPayload.put("snapshot", snapshot(event, payload,ctx));

                    JSONObject headers = new JSONObject();
                    String token = NSRUtils.getToken(ctx);
                    NSRLog.d("archiveEvent token: " + token);
                    headers.put("ns_token", token);
                    headers.put("ns_lang", NSRUtils.getLang(ctx));

                    NSRLog.d("requestPayload: " + requestPayload.toString());

                    securityDelegate.secureRequest(ctx, "archiveEvent", requestPayload, headers, new NSRSecurityResponse() {
                        public void completionHandler(JSONObject json, String error) throws Exception {
                            if (error != null) {
                                NSRLog.e("archiveEvent secureRequest: " + error);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            NSRLog.e("archiveEvent", e);
        }
    }

    public void sendEvent(final String event, final JSONObject payload) {
        if (NSRUtils.gracefulDegradate()) {
            return;
        }
        NSRLog.d("sendEvent - event: " + event + " payload: " + payload);
        try {
            NSR.authorize(new NSRAuth() {
                public void authorized(boolean authorized) throws Exception {
                    if (!authorized) {
                        return;
                    }
                    snapshot(event, payload, ctx);
                    JSONObject eventPayLoad = new JSONObject();
                    eventPayLoad.put("event", event);
                    eventPayLoad.put("timezone", TimeZone.getDefault().getID());
                    eventPayLoad.put("event_time", System.currentTimeMillis());
                    eventPayLoad.put("payload", payload);

                    JSONObject devicePayLoad = new JSONObject();
                    devicePayLoad.put("uid", NSRUtils.getDeviceUid(ctx));
                    String pushToken = NSRUtils.getPushToken(ctx);
                    if (pushToken != null) {
                        devicePayLoad.put("push_token", pushToken);
                    }
                    devicePayLoad.put("os", NSRUtils.getOs());
                    devicePayLoad.put("version", "[sdk:" + NSRUtils.getVersion() + "] " + Build.VERSION.RELEASE + " " + Build.VERSION_CODES.class.getFields()[Build.VERSION.SDK_INT].getName());
                    devicePayLoad.put("model", Build.MODEL);

                    JSONObject requestPayload = new JSONObject();
                    requestPayload.put("event", eventPayLoad);
                    requestPayload.put("user", NSRUser.getUser(ctx).toJsonObject(false));
                    requestPayload.put("device", devicePayLoad);
                    if (NSRUtils.getBoolean(NSRUtils.getConf(ctx), "send_snapshot")) {
                        requestPayload.put("snapshot", snapshot(ctx));
                    }

                    JSONObject headers = new JSONObject();
                    String token = NSRUtils.getToken(ctx);
                    NSRLog.d("sendEvent token: " + token);
                    headers.put("ns_token", token);
                    headers.put("ns_lang", NSRUtils.getLang(ctx));

                    NSRLog.d("requestPayload: " + requestPayload.toString());

                    securityDelegate.secureRequest(ctx, "event", requestPayload, headers, new NSRSecurityResponse() {
                        public void completionHandler(JSONObject json, String error) throws Exception {
                            if (error == null) {
                                if (json.has("pushes")) {
                                    boolean skipPush = !json.has("skipPush") || NSRUtils.getBoolean(json, "skipPush");
                                    JSONArray pushes = json.getJSONArray("pushes");
                                    if (!skipPush) {
                                        if (pushes.length() > 0) {
                                            JSONObject push = pushes.getJSONObject(0);
                                            NSR.showPush(push);
                                            if (NSRUtils.getBoolean(NSRUtils.getConf(ctx), "local_tracking")) {
                                                localCrunchEvent("pushed", push);
                                            }
                                        }
                                    } else {
                                        if (pushes.length() > 0) {
                                            JSONObject notification = pushes.getJSONObject(0);
                                            NSRLog.d(notification.toString());
                                            NSR.showUrl(notification.getString("url"), NSRCordovaInterface.NSR_ShowAppCallback);
                                        }
                                    }
                                }
                            } else {
                                NSRLog.e("sendEvent secureRequest: " + error);
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            NSRLog.e("sendEvent", e);
        }
    }

    public void crunchEvent(final String event, final JSONObject payload, final Context ctx) {
        if (NSRUtils.gracefulDegradate()) {
            return;
        }

        if (NSRUtils.getBoolean(NSRUtils.getConf(ctx), "local_tracking")) {
            NSRLog.d("crunchEvent: " + event + " payload: " + payload.toString());
            snapshot(event, payload,ctx);
            localCrunchEvent(event, payload);
        } else {
            sendEvent(event, payload);
        }
    }

    public void localCrunchEvent(final String event, final JSONObject payload) {
        if (eventWebView == null) {
            NSRLog.d("localCrunchEvent Making NSREventWebView");
        }
        NSRLog.d("localCrunchEvent call eventWebView");
        eventWebView.crunchEvent(event, payload);
    }

    public static void resetCruncher() {
        if (eventWebView != null) {
            eventWebView.reset();
        }
    }


    public static synchronized JSONObject snapshot(final String event, final JSONObject payload, final Context ctx) {
        JSONObject snapshot = snapshot(ctx);
        try {
            snapshot.put(event, payload);
        } catch (Exception e) {
        }
        NSRUtils.setJSONData("snapshot", snapshot, ctx);
        return snapshot;
    }

    public static synchronized JSONObject snapshot(final Context ctx) {
        JSONObject snapshot = NSRUtils.getJSONData("snapshot",ctx);
        if (snapshot == null) {
            snapshot = new JSONObject();
        }
        return snapshot;
    }

}
