package eu.neosurance.sdk;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;
import eu.neosurance.utils.NSRUtils;

public class NSRSettings {
	private String baseUrl;
	private String code;
	private String secretKey;
	private String lang = Locale.getDefault().getLanguage();
	private int pushIcon = R.drawable.nsr_logo;
	private boolean disableLog = false;
	private boolean devMode = false;
	private JSONObject miniappSkin;

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
