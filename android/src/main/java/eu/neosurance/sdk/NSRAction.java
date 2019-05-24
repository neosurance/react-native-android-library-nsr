package eu.neosurance.sdk;

import android.content.Context;
import org.json.JSONObject;
import java.util.TimeZone;
import eu.neosurance.utils.NSRUtils;

public class NSRAction {

    public static void sendAction(final String name, final String policyCode, final String details, final Context ctx) {
        if (NSRUtils.gracefulDegradate()) {
            return;
        }
        NSRLog.d("sendAction - name: " + name + " policyCode: " + policyCode + " details: " + details);
        try {
            NSR.authorize(new NSRAuth() {
                public void authorized(boolean authorized) throws Exception {
                    JSONObject requestPayload = new JSONObject();

                    requestPayload.put("action", name);
                    requestPayload.put("code", policyCode);
                    requestPayload.put("details", details);
                    requestPayload.put("timezone", TimeZone.getDefault().getID());
                    requestPayload.put("action_time", System.currentTimeMillis());

                    JSONObject headers = new JSONObject();
                    String token = NSRUtils.getToken(ctx);
                    NSRLog.d("sendAction token: " + token);
                    headers.put("ns_token", token);
                    headers.put("ns_lang", NSRUtils.getLang(ctx));

                    NSR.getSecurityDelegate().secureRequest(ctx, "trace", requestPayload, headers, new NSRSecurityResponse() {
                        public void completionHandler(JSONObject json, String error) throws Exception {
                            if (error != null) {
                                NSRLog.e("sendAction: " + error);
                            } else {
                                NSRLog.d("sendAction: " + json.toString());
                            }
                        }
                    });
                }
            });

        } catch (Exception e) {
            NSRLog.e("sendAction", e);
        }
    }

}
