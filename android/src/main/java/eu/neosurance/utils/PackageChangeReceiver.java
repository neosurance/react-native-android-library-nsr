package eu.neosurance.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.File;
import static eu.neosurance.utils.NSRUtils.TAG;
import static eu.neosurance.utils.NSRUtils.getSharedPreferences;

public class PackageChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {

        clearAllSharedPreferences(ctx);

    }

    public static void clearAllSharedPreferences(Context ctx){

        Log.d(TAG,"...clearing all shared preferences...");

        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        //editor.clear().apply();
        editor.clear().commit();


        File dataDir = null;
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            dataDir = ctx.getDataDir();
        }
        File appWebViewDir = new File(dataDir.getPath() + "/app_webview/");
        deleteDir(appWebViewDir);

    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}