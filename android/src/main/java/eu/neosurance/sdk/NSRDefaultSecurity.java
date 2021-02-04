package eu.neosurance.sdk;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresApi;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import eu.neosurance.utils.NSRUtils;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class NSRDefaultSecurity implements NSRSecurityDelegate {

	public static String TAG = "NSRDefaultSecurity - NSRNetworkAdapter";
	private static NSRSecurityDelegate NSRnwa = null;
	private Context context = null;

	private static ExecutorService executorService = null;

	public void secureRequest(final Context ctx, final String endpoint, final JSONObject payload, final JSONObject headers, final NSRSecurityResponse completionHandler) throws Exception {
		try {
			final String url = NSRUtils.getSettings(ctx).getString("base_url") + endpoint;
			NSRLog.d(TAG + ": " + url);

			if(executorService == null) {
				executorService = Executors.newFixedThreadPool(1);
			}

			executorService.execute(new Runnable() {
				@Override
				public void run() {
					String method = (url.contains("?") && url.split("\\?").length > 1) ? "GET" : "POST";
					doInBackground(url, method, payload, headers, completionHandler);
				}
			});

		} catch (Exception e) {
			NSRLog.e(e.getMessage());
			throw e;
		}
	}

	private void doInBackground(final String urlString, String method, final JSONObject payload, final JSONObject headers, final NSRSecurityResponse completionHandler){

		try {

			URL url = new URL(urlString);
			HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
			urlConnection.setRequestProperty("Content-Type", "application/json");


			if(headers != null && headers.length() > 0){

				Iterator<String> keys = headers.keys();

				while(keys.hasNext()){
					String key = keys.next();
					String value = headers.getString(key);
					urlConnection.setRequestProperty(key,value);
				}

			}

			urlConnection.setConnectTimeout(120000);
			urlConnection.setReadTimeout(120000);

			Log.d(TAG,"ExecutorService - doInBackground - ConnectTimeout: 120sec, ReadTimeout: 120sec");

			urlConnection.setRequestMethod(method);
			urlConnection.connect();

			if(method.equals("POST") || method.equals("PUT")){
				OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
				out.write(payload.toString());
				out.close();
			}

			// Check the connection status.
			int statusCode = urlConnection.getResponseCode();
			String statusMsg = urlConnection.getResponseMessage();

			//CONNECTION_SUCCESS
			if (statusCode == 200) {

				InputStream it = new BufferedInputStream(urlConnection.getInputStream());
				InputStreamReader read = new InputStreamReader(it);
				BufferedReader buff = new BufferedReader(read);
				StringBuilder dta = new StringBuilder();
				String chunks;
				while ((chunks = buff.readLine()) != null) {
					dta.append(chunks);
				}
				String returndata = dta.toString();

				Bundle bundle = new Bundle();
				bundle.putString("jsonString",returndata);
				Message msg = new Message();
				msg.setData(bundle);
				//completionHandler.handleMessage(msg);

				NSRLog.d(TAG + " >> response: " + msg);
				completionHandler.completionHandler(new JSONObject(returndata), null);

			}
			//401, 500, etc...
			else if(statusCode != 200){
				String exceptionString = TAG + " >> statusCode: " + statusCode + ", statusMsg: " + statusMsg + " ";
				NSRLog.e(exceptionString);

				try {
					completionHandler.completionHandler(null, exceptionString);
				} catch (Exception ee) {
					NSRLog.e(ee.toString());
				}
			}

		} catch (ProtocolException e) {
			NSRLog.e(e.getMessage());

			try {
				completionHandler.completionHandler(null, e.getMessage());
			} catch (Exception ee) {
				NSRLog.e(ee.toString());
			}

		} catch (MalformedURLException e) {
			NSRLog.e(e.getMessage());

			try {
				completionHandler.completionHandler(null, e.getMessage());
			} catch (Exception ee) {
				NSRLog.e(ee.toString());
			}

		} catch (IOException e) {
			NSRLog.e(e.getMessage());

			try {
				completionHandler.completionHandler(null, e.getMessage());
			} catch (Exception ee) {
				NSRLog.e(ee.toString());
			}

		} catch (Exception e) {
			NSRLog.e(e.getMessage());

			try {
				completionHandler.completionHandler(null, e.getMessage());
			} catch (Exception ee) {
				NSRLog.e(ee.toString());
			}

		}

	}



}