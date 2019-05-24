package eu.neosurance.sdk;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.json.JSONObject;
import eu.neosurance.utils.NSRUtils;

public class NSRConnection {

    public static void traceConnection(NSRSecurityDelegate securityDelegate, Context ctx) {
        NSRLog.d("traceConnection");
        try {
            JSONObject conf = NSRUtils.getConf(ctx);
            if (conf != null && NSRUtils.getBoolean(conf.getJSONObject("connection"), "enabled")) {
                String connection = null;
                NetworkInfo info = ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    switch (info.getType()) {
                        case ConnectivityManager.TYPE_WIFI:
                            connection = "wi-fi";
                            break;
                        case ConnectivityManager.TYPE_MOBILE:
                            connection = "mobile";
                    }
                }
                if (connection != null && !connection.equals(getLastConnection(ctx))) {
                    JSONObject payload = new JSONObject();
                    payload.put("type", connection);

                    NSREvent nsrEvent = new NSREvent(securityDelegate,ctx,NSR.eventWebView);
                    nsrEvent.crunchEvent("connection", payload, ctx);
                    setLastConnection(connection,ctx);
                }
            }
        } catch (Exception e) {
            NSRLog.e("traceConnection", e);
        }
    }

    public static String getLastConnection(Context ctx) {
        return NSRUtils.getData("lastConnection",ctx);
    }

    public static void setLastConnection(String lastConnection, Context ctx) {
        NSRUtils.setData("lastConnection", lastConnection, ctx);
    }

}
