package eu.neosurance.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import org.json.JSONObject;
import eu.neosurance.utils.NSRUtils;

public class NSRPower {

    public void tracePower(NSRSecurityDelegate securityDelegate, Context ctx) {
        NSRLog.d("tracePower");
        try {
            JSONObject conf = NSRUtils.getConf(ctx);
            if (conf != null && NSRUtils.getBoolean(conf.getJSONObject("power"), "enabled")) {
                Intent batteryStatus = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int powerLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                String power = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) > 0 ? "plugged" : "unplugged";
                if (!power.equals(getLastPower(ctx)) || Math.abs(powerLevel - getLastPowerLevel(ctx)) >= 5) {
                    JSONObject payload = new JSONObject();
                    payload.put("type", power);
                    payload.put("level", powerLevel);
                    NSREvent nsrEvent = new NSREvent(securityDelegate,ctx,NSR.eventWebView);
                    nsrEvent.crunchEvent("power", payload,ctx);
                    setLastPower(ctx, power);
                    setLastPowerLevel(ctx, powerLevel);
                }
            }
        } catch (Exception e) {
            NSRLog.e("tracePower", e);
        }
    }

    protected String getLastPower(Context ctx) {
        return NSRUtils.getData("lastPower",ctx);
    }

    protected void setLastPower(Context ctx, String lastPower) {
        NSRUtils.setData("lastPower", lastPower, ctx);
    }

    protected int getLastPowerLevel(Context ctx) {
        try {
            String s = NSRUtils.getData("lastPowerLevel",ctx);
            return s != null ? Integer.parseInt(s) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    protected void setLastPowerLevel(Context ctx, int lastPower) {
        NSRUtils.setData("lastPowerLevel", "" + lastPower, ctx);
    }

}
