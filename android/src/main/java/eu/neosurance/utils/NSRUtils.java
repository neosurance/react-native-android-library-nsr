package eu.neosurance.utils;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;

import org.apache.cordova.CallbackContext;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import eu.neosurance.sdk.NSR;
import eu.neosurance.sdk.NSRActivityWebView;
import eu.neosurance.sdk.NSREventWebView;
import eu.neosurance.sdk.NSRLog;

import eu.neosurance.sdk.NSRSettings;
import io.ionic.starter.BuildConfig;

public class NSRUtils {

    public static final String PREFS_NAME = "NSRSDK";
    protected static final String TAG = "nsr";
    private final static byte[] K = Base64.decode("Ux44AGRuanL0y7qQDeasT3", Base64.NO_WRAP);
    private final static byte[] I = Base64.decode("ycB4AGR7a0fhoFXbpoHy43", Base64.NO_WRAP);

    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public static String getOs() {
        return "Android";
    }

    public static SharedPreferences getSharedPreferences(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE);
    }

    public static JSONObject getJSONData(String key, Context ctx) {
        try {
            return new JSONObject(getData(key,ctx));
        } catch (Exception e) {
            return null;
        }
    }

    public static void setJSONData(String key, JSONObject value, Context ctx) {
        setData(key, (value != null) ? value.toString() : null, ctx);
    }

    public static String getData(String key, Context ctx) {
        if (getSharedPreferences(ctx).contains(key)) {
            try {
                return tod(getSharedPreferences(ctx).getString(key, ""));
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static void setData(String key, String value, Context ctx) {
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        if (value != null) {
            try {
                editor.putString(key, toe(value));
            } catch (Exception e) {
            }
        } else {
            editor.remove(key);
        }
        editor.commit();
    }

    public static String toe(String input) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(Arrays.copyOf(K, 16), "AES"), new IvParameterSpec(Arrays.copyOf(I, 16)));
        return Base64.encodeToString(cipher.doFinal(input.getBytes()), Base64.NO_WRAP);
    }

    public static String tod(String input) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Arrays.copyOf(K, 16), "AES"), new IvParameterSpec(Arrays.copyOf(I, 16)));
        return new String(cipher.doFinal(Base64.decode(input, Base64.NO_WRAP)));
    }

    public static void storeData(String key, JSONObject data, Context ctx) {
        setJSONData("WV_" + key, data, ctx);
    }

    public static JSONObject retrieveData(String key, Context ctx) {
        return getJSONData("WV_" + key, ctx);
    }

    public static Date jsonStringToDate(String s) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static String dateToJsonString(Date date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDeviceUid(Context ctx) {
        return Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getPushToken(Context ctx) {
        try {
            if(getSettings(ctx) != null)
                return getSettings(ctx).has("push_token") ? getSettings(ctx).getString("push_token") : null;
            else
                return null;
        } catch (Exception e) {
            NSRLog.e("getPushToken", e);
            return null;
        }
    }

    public static String getLang(Context ctx) {
        try {
            if(ctx != null && getSettings(ctx) != null)
                return getSettings(ctx).has("ns_lang") ? getSettings(ctx).getString("ns_lang") : null;
            else
                return null;
        } catch (Exception e) {
            NSRLog.e("getLang", e);
            return null;
        }
    }

    public static JSONObject getSettings(Context ctx) {
        return getJSONData("settings", ctx);
    }

    public static JSONObject getConf(Context ctx) {
        return getJSONData("conf", ctx);
    }

    public static void setConf(JSONObject conf, Context ctx) {
        setJSONData("conf", conf, ctx);
    }

    public static void setSettings(JSONObject settings, Context ctx) {
        setJSONData("settings", settings, ctx);
    }

    public static JSONObject getAuth(Context ctx) {
        return getJSONData("auth", ctx);
    }

    public static void setAuth(JSONObject auth, Context ctx) {
        setJSONData("auth", auth, ctx);
    }

    public static String getToken(Context ctx) {
        try {
            JSONObject authTmp = getAuth(ctx);
            return authTmp.has("token") ? authTmp.getString("token") : null;
        } catch (Exception e) {
            NSRLog.e("getToken", e);
            return null;
        }
    }

    public static boolean isLogEnabled(Context ctx) {
        return !getBoolean(getSettings(ctx), "disable_log");
    }


    public static String getAppURL(Context ctx) {
        return getData("appURL",ctx);
    }

    public static void setAppURL(String appURL, Context ctx) {
        setData("appURL", appURL, ctx);
    }

    public static boolean getBoolean(JSONObject obj, String key) {
        try {
            return (obj.get(key) instanceof Boolean) ? obj.getBoolean(key) : (obj.getInt(key) != 0);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean gracefulDegradate() {
        return Build.VERSION.SDK_INT < 21;
    }


    public static void askPermissions(Activity activity) {

        NSRUtils.setData("permission_requested", "*",activity.getApplicationContext());
        List<String> permissionsList = new ArrayList<String>();
        if (ActivityCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(activity.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions(activity, permissionsList.toArray(new String[permissionsList.size()]), NSR.PERMISSIONS_MULTIPLE_ACCESSLOCATION);
        }

    }

    public static synchronized void showUrl(String url, JSONObject params, Context ctx, CallbackContext callbackContext) {
        if (NSRUtils.gracefulDegradate()) {
            if(callbackContext != null)
                callbackContext.error("NSRUtils - showUrl - gracefulDegradate");
            return;
        }
        try {
            NSRLog.d("showUrl: " + url);
            if (params != null && params.length() > 0) {
                Iterator<String> keys = params.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    url += ((url.indexOf('?') < 0) ? "?" : "&") + key + "=" + URLEncoder.encode(params.getString(key), "UTF-8");
                }
            }

            if(callbackContext != null)
                callbackContext.success(url);
            
            if (NSR.activityWebView != null) {
                NSR.activityWebView.navigate(url);
            } else {
                ctx.startActivity(NSRUtils.makeActivityWebView(url,ctx));
            }
        } catch (Exception e) {
            NSRLog.e("showUrl", e);
        }
    }



    public static Intent makeActivityWebView(String url, Context ctx) throws Exception {
        NSRLog.d("showUrl makeActivityWebView " + ctx);
        Intent intent = new Intent(ctx, NSRActivityWebView.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra("url", url);
        return intent;
    }

    public static void registerWebView(NSRActivityWebView activityWebViewTmp) {
        if (NSR.activityWebView != null)
            NSR.activityWebView.finish();
        NSR.activityWebView = activityWebViewTmp;
    }

    public static boolean synchEventWebView(Context ctx) {

        if (NSRUtils.getBoolean(NSRUtils.getConf(ctx), "local_tracking")) {
            if (NSR.eventWebView == null) {
                NSRLog.d("Making NSREventWebView");
                NSR.eventWebView = new NSREventWebView(ctx, NSR.getInstance(ctx));
                return true;
            } else {
                NSR.eventWebView.synch();
            }
        } else if (NSR.eventWebView != null) {
            NSR.eventWebView.finish();
            NSR.eventWebView = null;
        }

        return false;
    }

}
