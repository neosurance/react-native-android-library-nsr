package eu.neosurance.sdk;

public class NSRLog {
	static public boolean enabled = true;

	public static void i(String string) {
		if (enabled) android.util.Log.i(NSR.TAG, string);
	}

	public static void e(String string) {
		android.util.Log.e(NSR.TAG, string);
	}

	public static void e(String string, Throwable t) {
		android.util.Log.e(NSR.TAG, string, t);
	}

	public static void d(String string) {
		if (enabled) android.util.Log.d(NSR.TAG, string);
	}

	public static void v(String string) {
		if (enabled) android.util.Log.v(NSR.TAG, string);
	}

	public static void w(String string) {
		if (enabled) android.util.Log.w(NSR.TAG, string);
	}
}
