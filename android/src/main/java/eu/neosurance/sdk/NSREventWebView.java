package eu.neosurance.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;
import eu.neosurance.utils.NSRUtils;

public class NSREventWebView {
	public static WebView webView = null;
	private Context ctx;
	private NSR nsr;


	@SuppressLint("SetJavaScriptEnabled")
	public NSREventWebView(final Context ctx, final NSR nsr) {
		try {
			this.ctx = ctx;
			this.nsr = nsr;
			final NSREventWebView eventWebView = this;
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				public void run() {
					webView = new WebView(ctx);
					if (Build.VERSION.SDK_INT >= 21) {
						WebView.setWebContentsDebuggingEnabled(NSRUtils.getBoolean(NSRUtils.getSettings(ctx), "dev_mode"));
					}
					webView.addJavascriptInterface(eventWebView, "NSR");
					webView.getSettings().setJavaScriptEnabled(true);
					webView.getSettings().setAllowFileAccessFromFileURLs(true);
					webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
					webView.getSettings().setDomStorageEnabled(true);
					webView.loadUrl("file:///android_asset/eventCruncher.html?ns_lang=" + NSRUtils.getLang(ctx) + "&ns_log=" + NSRUtils.isLogEnabled(ctx));
				}
			});

		} catch (Exception e) {
			NSRLog.e(e.getMessage(), e);
		}
	}

	public void synch() {
		eval("EVC.synch()");
	}

	public void reset() {
		eval("localStorage.clear();EVC.synch()");
	}

	protected void crunchEvent(final String event, final JSONObject payload) {
		try {
			JSONObject nsrEvent = new JSONObject();
			nsrEvent.put("event", event);
			nsrEvent.put("payload", payload);
			eval("EVC.innerCrunchEvent(" + nsrEvent.toString() + ")");
		} catch (JSONException e) {
			NSRLog.e("crunchEvent", e);
		}
	}

	public static void eval(final String code) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				try {
					if (webView != null && Build.VERSION.SDK_INT >= 21) {
						webView.evaluateJavascript(code, null);
					}
				} catch (Throwable e) {
				}
			}
		});
	}

	@JavascriptInterface
	public void postMessage(final String json) {
		try {
			final JSONObject body = new JSONObject(json);
			if (body.has("log")) {
				NSRLog.d(body.getString("log"));
			}
			if (body.has("event") && body.has("payload")) {
				nsr.sendEvent(body.getString("event"), body.getJSONObject("payload"));
			}
			if (body.has("archiveEvent") && body.has("payload")) {
				nsr.archiveEvent(body.getString("archiveEvent"), body.getJSONObject("payload"));
			}
			if (body.has("action")) {
				nsr.sendAction(body.getString("action"), body.getString("code"), body.getString("details"));
			}
			if (body.has("push")) {
				if (body.has("delay")) {
					nsr.showPush(body.has("id") ? body.getString("id") : Integer.toString((int) SystemClock.elapsedRealtime()), body.getJSONObject("push"), body.getInt("delay"));
				} else {
					nsr.showPush(body.getJSONObject("push"));
				}
			}
			if (body.has("killPush")) {
				nsr.killPush(body.getString("killPush"),ctx);
			}
			if (body.has("what")) {
				String what = body.getString("what");
				if ("continueInitJob".equals(what)) {
					nsr.continueInitJob();
				}
				if ("init".equals(what) && body.has("callBack")) {
					nsr.authorize(new NSRAuth() {
						public void authorized(boolean authorized) throws Exception {
							if (authorized) {
								JSONObject message = new JSONObject();
								message.put("api", NSRUtils.getSettings(ctx).getString("base_url"));
								message.put("token", NSRUtils.getToken(ctx));
								message.put("lang", NSRUtils.getLang(ctx));
								message.put("deviceUid", NSRUtils.getDeviceUid(ctx));
								eval(body.getString("callBack") + "(" + message.toString() + ")");
							}
						}
					});
				}
				if ("token".equals(what) && body.has("callBack")) {
					nsr.authorize(new NSRAuth() {
						public void authorized(boolean authorized) throws Exception {
							if (authorized) {
								eval(body.getString("callBack") + "('" + NSRUtils.getToken(ctx) + "')");
							}
						}
					});
				}
				if ("user".equals(what) && body.has("callBack")) {
					eval(body.getString("callBack") + "(" + NSRUser.getUser(ctx).toJsonObject(true).toString() + ")");
				}
				if ("geoCode".equals(what) && body.has("location") && body.has("callBack")) {
					Geocoder geocoder = null;
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
						geocoder = new Geocoder(ctx, Locale.forLanguageTag(NSRUtils.getLang(ctx)));
					}
					JSONObject location = body.getJSONObject("location");
					List<Address> addresses = geocoder.getFromLocation(location.getDouble("latitude"), location.getDouble("longitude"), 1);
					if (addresses != null && addresses.size() > 0) {
						Address adr = addresses.get(0);
						JSONObject address = new JSONObject();
						address.put("countryCode", adr.getCountryCode().toUpperCase());
						address.put("countryName", adr.getCountryName());
						String adrLine = adr.getAddressLine(0);
						address.put("address", adrLine != null ? adrLine : "");
						eval(body.getString("callBack") + "(" + address.toString() + ")");
					}
				}
				if ("store".equals(what) && body.has("key") && body.has("data")) {
					NSRUtils.storeData(body.getString("key"), body.getJSONObject("data"),ctx);
				}
				if ("retrive".equals(what) && body.has("key") && body.has("callBack")) {
					JSONObject val = NSRUtils.retrieveData(body.getString("key"),ctx);
					eval(body.getString("callBack") + "(" + (val != null ? val.toString() : "null") + ")");
				}
				if ("retrieve".equals(what) && body.has("key") && body.has("callBack")) {
					JSONObject val = NSRUtils.retrieveData(body.getString("key"),ctx);
					eval(body.getString("callBack") + "(" + (val != null ? val.toString() : "null") + ")");
				}
				if ("callApi".equals(what) && body.has("callBack")) {
					nsr.authorize(new NSRAuth() {
						public void authorized(boolean authorized) throws Exception {
							if (!authorized) {
								JSONObject result = new JSONObject();
								result.put("status", "error");
								result.put("message", "not authorized");
								eval(body.getString("callBack") + "(" + result.toString() + ")");
								return;
							}
							JSONObject headers = new JSONObject();
							headers.put("ns_token", NSRUtils.getToken(ctx));
							headers.put("ns_lang", NSRUtils.getLang(ctx));
							nsr.getSecurityDelegate().secureRequest(ctx, body.getString("endpoint"), body.has("payload") ? body.getJSONObject("payload") : null, headers, new NSRSecurityResponse() {
								public void completionHandler(JSONObject json, String error) throws Exception {
									if (error == null) {
										eval(body.getString("callBack") + "(" + json.toString() + ")");
									} else {
										NSRLog.e("secureRequest: " + error);
										JSONObject result = new JSONObject();
										result.put("status", "error");
										result.put("message", error);
										eval(body.getString("callBack") + "(" + result.toString() + ")");
									}
								}
							});
						}
					});
				}
				if ("accurateLocation".equals(what) && body.has("meters") && body.has("duration")) {
					boolean extend = NSRUtils.getBoolean(body, "extend");
					nsr.accurateLocation(body.getDouble("meters"), body.getInt("duration"), extend);
				}
				if ("accurateLocationEnd".equals(what)) {
					nsr.accurateLocationEnd();
				}
				if ("activateFences".equals(what) && body.has("fences")) {
					NSRLog.d("activatingFences");
					nsr.activateFences(body.getJSONArray("fences"));
				}
				if ("removeFences".equals(what)) {
					nsr.removeFences();
				}
			}
		} catch (Exception e) {
			NSRLog.e("postMessage", e);
		}
	}

	public synchronized void finish() {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				try {
					if (webView != null) {
						webView.stopLoading();
						webView.destroy();
						webView = null;
					}
				} catch (Throwable e) {
				}
			}
		});
	}
}
