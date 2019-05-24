package eu.neosurance.sdk;

import android.app.Activity;
import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import eu.neosurance.sdk_ext.NSRActivity;
import eu.neosurance.sdk_ext.WFDelegate;
import eu.neosurance.utils.NSRUtils;

import io.ionic.starter.R;

public class NSRSettings {
	private String baseUrl;
	private String code;
	private String secretKey;
	private String lang = Locale.getDefault().getLanguage();
	private int pushIcon = R.drawable.nsr_logo;
	private boolean disableLog = false;
	private boolean devMode = false;
	private JSONObject miniappSkin;

	public static NSRSettings settings = null;

	public static void setNSRSettings(Context ctx){

		if(settings == null){
			Properties config = new Properties();
			try {
				config.load(ctx.getAssets().open("config.properties"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			settings = new NSRSettings();
			settings.setBaseUrl(config.getProperty("base_url"));
			settings.setCode(config.getProperty("code"));
			settings.setSecretKey(config.getProperty("secret_key"));
			settings.setDevMode(true);
		}

	}

	public JSONObject getJSONNSRSettings(){
		JSONObject json = new JSONObject();
		try {
			json.put("base_url",settings.getBaseUrl());
			json.put("code",settings.getCode());
			json.put("secret_key",settings.getSecretKey());
			json.put("dev_mode",settings.isDevMode());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return json;
	}

	//********** NSR_SECURITY_DELEGATE **********//

	public static NSRSecurityDelegate getSecurityDelegate() {
		return NSR.securityDelegate;
	}

	public static void setSecurityDelegate(NSRSecurityDelegate securityDelegate, Context ctx) {
		NSR.securityDelegate = securityDelegate;

		if (NSRUtils.gracefulDegradate())
			return;

		NSRUtils.setData("securityDelegateClass", securityDelegate.getClass().getName(),ctx);
		NSR.securityDelegate = securityDelegate;
	}

	//********** NSR_WORKFLOW_DELEGATE **********//

	public static NSRWorkflowDelegate getWorkflowDelegate() {
		return NSR.workflowDelegate;
	}

	public static void setWorkflowDelegate(NSRWorkflowDelegate workflowDelegate, Context ctx) {

		if (NSRUtils.gracefulDegradate())
			return;

		NSR.workflowDelegate = workflowDelegate;
		NSRUtils.setData("workflowDelegateClass", NSR.workflowDelegate.getClass().getName(), ctx);

	}

	//********** NSR_PUSH_DELEGATE **********//

	public static NSRPushDelegate getPushDelegate() {
		return NSR.pushDelegate;
	}

	public static void setPushDelegate(NSRPushDelegate pushDelegate) {
		NSR.pushDelegate = pushDelegate;
	}

	//********** GETTERS & SETTERS **********//

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public int getPushIcon() {
		return pushIcon;
	}

	public void setPushIcon(int pushIcon) {
		this.pushIcon = pushIcon;
	}

	public boolean isDisableLog() {
		return disableLog;
	}

	public void setDisableLog(boolean disableLog) {
		this.disableLog = disableLog;
	}

	public boolean isDevMode() {
		return devMode;
	}

	public void setDevMode(boolean devMode) {
		this.devMode = devMode;
	}

	public JSONObject getMiniappSkin() {
		return miniappSkin;
	}

	public void setMiniappSkin(JSONObject miniappSkin) {
		this.miniappSkin = miniappSkin;
	}

	public JSONObject toJsonObject() {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("base_url", baseUrl);
			jsonObject.put("code", code);
			jsonObject.put("secret_key", secretKey);
			jsonObject.put("ns_lang", lang);
			jsonObject.put("push_icon", pushIcon);
			jsonObject.put("dev_mode", devMode);
			jsonObject.put("disable_log", disableLog);
		} catch (JSONException e) {
			NSRLog.e("toJsonObject", e);
		}
		return jsonObject;
	}
}
//new
