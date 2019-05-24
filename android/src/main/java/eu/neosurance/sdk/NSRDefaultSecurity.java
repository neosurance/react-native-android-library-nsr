package eu.neosurance.sdk;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import org.json.JSONObject;
import java.util.Iterator;
import eu.neosurance.utils.NSRUtils;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class NSRDefaultSecurity implements NSRSecurityDelegate {

	public void secureRequest(final Context ctx, final String endpoint, final JSONObject payload, final JSONObject headers, final NSRSecurityResponse completionHandler) throws Exception {
		try {
			String url = NSRUtils.getSettings(ctx).getString("base_url") + endpoint;
			NSRLog.d("NSRDefaultSecurity: " + url);
			AsynchRequest asynchRequest = new AsynchRequest(url, payload, headers, completionHandler);
			asynchRequest.execute();
		} catch (Exception e) {
			NSRLog.e(e.getMessage());
			throw e;
		}
	}

	private class AsynchRequest extends AsyncTask<Void, Void, Object> {
		private String url;
		private JSONObject payload;
		private JSONObject headers;
		private NSRSecurityResponse completionHandler;

		public AsynchRequest(final String url, final JSONObject payload, final JSONObject headers, final NSRSecurityResponse completionHandler) {
			this.url = url;
			this.payload = payload;
			this.headers = headers;
			this.completionHandler = completionHandler;
		}

		protected Object doInBackground(Void... params) {
			NSRHttpRunner httpRunner = null;
			try {
				httpRunner = new NSRHttpRunner(url);
				if (payload != null)
					httpRunner.payload(payload.toString(), "application/json");

				if (headers != null) {
					Iterator<String> keys = headers.keys();
					while (keys.hasNext()) {
						String key = keys.next();
						httpRunner.header(key, headers.getString(key));
					}
				}
				String response = httpRunner.read();
				NSRLog.d("NSRDefaultSecurity response:" + response);
				completionHandler.completionHandler(new JSONObject(response), null);
			} catch (Exception e) {
				try {
					if (httpRunner != null) {
						NSRLog.e("MSG:" + httpRunner.getMessage());
						NSRLog.e("Error:" + e.getMessage());
					}
					completionHandler.completionHandler(null, e.toString());
				} catch (Exception ee) {
					NSRLog.e(ee.toString());
				}
			}
			return null;
		}
	}
}