package eu.neosurance.sdk;

import android.content.Context;
import android.os.Build;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;

import eu.neosurance.utils.NSRJsonAdapter;
import eu.neosurance.utils.NSRUtils;
import static eu.neosurance.sdk.NSR.initJob;

public class NSRUser {
	public static String code,email,firstname,lastname,mobile,fiscalCode,gender,address,zipCode,city,stateProvince,country;
	public static Date birthday;
	public static JSONObject extra,locals;

	public NSRUser() {

	}

	public static NSRUser getUser(Context ctx) {
		try {
			JSONObject user = NSRUtils.getJSONData("user",ctx);
			return user != null ? new NSRUser(user) : null;
		} catch (Exception e) {
			NSRLog.e("getUser", e);
			return null;
		}
	}

	public static void setUser(NSRUser user, Context ctx) {
		NSRUtils.setJSONData("user", user == null ? null : user.toJsonObject(true),ctx);
	}

	public static void forgetUser(Context ctx) {
		if (NSRUtils.gracefulDegradate()) {
			return;
		}
		NSRLog.d("forgetUser");
		NSRUtils.setConf(null,ctx);
		NSRUtils.setAuth(null,ctx);
		NSRUtils.setAppURL(null,ctx);
		NSRUser.setUser(null,ctx);
		initJob();
	}

	//*** JSON_ADAPTER
	public NSRUser(JSONObject jsonObject) throws Exception {
		NSRJsonAdapter.setNSRJson(jsonObject);
	}

	//*** JSON_ADAPTER
	public JSONObject toJsonObject(boolean withLocals) {
		return NSRJsonAdapter.toJsonObject(withLocals);
	}

	//********** GETTERS & SETTERS **********//

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getFiscalCode() {
		return fiscalCode;
	}

	public void setFiscalCode(String fiscalCode) {
		this.fiscalCode = fiscalCode;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getStateProvince() {
		return stateProvince;
	}

	public void setStateProvince(String stateProvince) {
		this.stateProvince = stateProvince;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public JSONObject getExtra() {
		return extra;
	}

	public void setExtra(JSONObject extra) {
		this.extra = extra;
	}

	public JSONObject getLocals() {
		return locals;
	}

	public void setLocals(JSONObject locals) {
		this.locals = locals;
	}

	//********** AUTHORIZE **********//

	public static void authorize(final NSRAuth delegate, final Context ctx) throws Exception {
		NSRLog.d("authorize");
		JSONObject auth = NSRUtils.getAuth(ctx);
		if (auth != null && (auth.getLong("expire") - System.currentTimeMillis()) > 0) {
			delegate.authorized(true);
		} else {
			NSRUser user = NSRUser.getUser(ctx);
			JSONObject settings = NSRUtils.getSettings(ctx);
			if (user != null && settings != null) {
				try {
					JSONObject payload = new JSONObject();
					payload.put("user_code", user.getCode());
					payload.put("code", settings.getString("code"));
					payload.put("secret_key", settings.getString("secret_key"));

					JSONObject sdkPayload = new JSONObject();
					sdkPayload.put("version", NSRUtils.getVersion());
					sdkPayload.put("dev", NSRUtils.getBoolean(settings, "dev_mode"));
					sdkPayload.put("os", NSRUtils.getOs());
					payload.put("sdk", sdkPayload);

					NSR.securityDelegate.secureRequest(ctx, "authorize", payload, null, new NSRSecurityResponse() {
						public void completionHandler(JSONObject response, String error) throws Exception {
							if (error == null) {
								JSONObject auth = response.getJSONObject("auth");
								NSRLog.d("authorize auth: " + auth);
								NSRUtils.setAuth(auth,ctx);

								JSONObject oldConf = NSRUtils.getConf(ctx);
								JSONObject conf = response.getJSONObject("conf");
								NSRLog.d("authorize conf: " + conf);
								NSRUtils.setConf(conf,ctx);

								String appUrl = response.getString("app_url");
								NSRLog.d("authorize appUrl: " + appUrl);
								NSRUtils.setAppURL(appUrl,ctx);

								if (NSR.needsInitJob(conf, oldConf)) {
									NSRLog.d("authorize needsInitJob");
									initJob();
								} else {
									NSR.synchEventWebView();
								}
								delegate.authorized(true);
							} else {
								delegate.authorized(false);
							}
						}
					});
				} catch (Exception e) {
					NSRLog.e("authorize", e);
					delegate.authorized(false);
				}
			}
		}
	}

	//********** REGISTER_USER **********//

	public static void registerUser(NSRUser user, final Context ctx) {
		if (NSRUtils.gracefulDegradate()) {
			return;
		}
		NSRLog.d("registerUser");
		try {
			NSRUtils.setAuth(null,ctx);
			NSRUser.setUser(user,ctx);
			authorize(new NSRAuth() {
				public void authorized(boolean authorized) throws Exception {
					NSRLog.d("registerUser: " + (authorized ? "" : "not ") + "authorized!");
					if (authorized && NSRUtils.getBoolean(NSRUtils.getConf(ctx), "send_user")) {
						NSRLog.d("sendUser");
						try {
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
							requestPayload.put("user", NSRUser.getUser(ctx).toJsonObject(false));
							requestPayload.put("device", devicePayLoad);

							JSONObject headers = new JSONObject();
							String token = NSRUtils.getToken(ctx);
							NSRLog.d("sendUser token: " + token);
							headers.put("ns_token", token);
							headers.put("ns_lang", NSRUtils.getLang(ctx));

							NSRLog.d("requestPayload: " + requestPayload.toString());

							NSR.getSecurityDelegate().secureRequest(ctx, "register", requestPayload, headers, new NSRSecurityResponse() {
								public void completionHandler(JSONObject json, String error) throws Exception {
									if (error != null) {
										NSRLog.e("sendUser secureRequest: " + error);
									}
								}
							});
						} catch (Exception e) {
							NSRLog.e("sendUser", e);
						}
					}
				}
			},ctx);
		} catch (Exception e) {
			NSRLog.e("registerUser", e);
		}
	}

	public static void loginExecuted(String url, Context ctx) {
		if (NSRUtils.gracefulDegradate())
			return;
		try {
			NSRLog.d("loginExecuted " + ctx);
			JSONObject params = new JSONObject();
			params.put("loginExecuted", "yes");
			NSR.showUrl(url, params);
		} catch (Exception e) {
			NSRLog.e("loginExecuted", e);
		}
	}

	public static void paymentExecuted(JSONObject paymentInfo, String url) {
		if (NSRUtils.gracefulDegradate())
			return;
		try {
			JSONObject params = new JSONObject();
			params.put("paymentExecuted", paymentInfo.toString());
			NSR.showUrl(url, params);
		} catch (Exception e) {
			NSRLog.e("paymentExecuted", e);
		}
	}

}
