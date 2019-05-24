package eu.neosurance.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import eu.neosurance.utils.NSRUtils;

public class NSRActivityWebView extends AppCompatActivity {
	private WebView webView;
	private String photoCallback;
	private NSR nsr;

	@SuppressLint("SetJavaScriptEnabled")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nsr = NSR.getInstance(getApplicationContext());
		nsr.registerWebView(this);

		try {
			String url = getIntent().getExtras().getString("url");
			webView = new WebView(this);
			if (Build.VERSION.SDK_INT >= 21) {
				WebView.setWebContentsDebuggingEnabled(NSRUtils.getBoolean(NSRUtils.getSettings(getApplicationContext()), "dev_mode"));
			}
			webView.addJavascriptInterface(this, "NSSdk");
			webView.getSettings().setJavaScriptEnabled(true);
			webView.getSettings().setAllowFileAccessFromFileURLs(true);
			webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
			webView.getSettings().setDomStorageEnabled(true);
			webView.setWebViewClient(new WebViewClient() {
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					if (url.endsWith(".pdf")) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.parse(url), "application/pdf");
						intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
						startActivity(intent);
					} else {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						startActivity(intent);
					}
					return true;
				}
			});
			webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
			setContentView(webView);
			webView.loadUrl(url);
			idle();
		} catch (Exception e) {
			NSRLog.e(e.getMessage(), e);
		}
	}

	public synchronized void navigate(final String url) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				try {
					if (webView != null) {
						webView.loadUrl(url);
					}
				} catch (Throwable e) {
				}
			}
		});
	}

	public void eval(final String code) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				try {
					if (webView != null && Build.VERSION.SDK_INT >= 21) {
						webView.evaluateJavascript(code, null);
					}
				} catch (Throwable e) {
				}
			}
		});
	}

	@JavascriptInterface
	public void postMessage(final String json) {
		try {
			final JSONObject body = new JSONObject(json);
			if (body.has("log")) {
				NSRLog.d(body.getString("log"));
			}
			if (body.has("event") && body.has("payload")) {
				nsr.sendEvent(body.getString("event"), body.getJSONObject("payload"));
			}
			if (body.has("crunchEvent") && body.has("payload")) {
				nsr.crunchEvent(body.getString("crunchEvent"), body.getJSONObject("payload"),NSR.ctx);
			}
			if (body.has("archiveEvent") && body.has("payload")) {
				nsr.archiveEvent(body.getString("archiveEvent"), body.getJSONObject("payload"));
			}
			if (body.has("action")) {
				nsr.sendAction(body.getString("action"), body.getString("code"), body.getString("details"));
			}
			if (body.has("what")) {
				String what = body.getString("what");
				if ("init".equals(what) && body.has("callBack")) {
					nsr.authorize(new NSRAuth() {
						public void authorized(boolean authorized) throws Exception {
							JSONObject settings = NSRUtils.getSettings(getApplicationContext());
							JSONObject message = new JSONObject();
							message.put("api", settings.getString("base_url"));
							message.put("token", NSRUtils.getToken(getApplicationContext()));
							message.put("lang", NSRUtils.getLang(getApplicationContext()));
							message.put("deviceUid", NSRUtils.getDeviceUid(getApplicationContext()));
							eval(body.getString("callBack") + "(" + message.toString() + ")");
						}
					});
				}
				if ("close".equals(what)) {
					finish();
				}
				if ("photo".equals(what) && body.has("callBack")) {
					takePhoto(body.getString("callBack"));
				}
				if ("location".equals(what) && body.has("callBack")) {
					getLocation(body.getString("callBack"));
				}
				if ("user".equals(what) && body.has("callBack")) {
					eval(body.getString("callBack") + "(" + NSRUser.getUser(getApplicationContext()).toJsonObject(true).toString() + ")");
				}
				if ("showApp".equals(what)) {
					if (body.has("params")) {
						nsr.showApp(body.getJSONObject("params"));
					} else {
						nsr.showApp();
					}
				}
				if ("showUrl".equals(what) && body.has("url")) {
					if (body.has("params")) {
						nsr.showUrl(body.getString("url"), body.getJSONObject("params"));
					} else {
						nsr.showUrl(body.getString("url"));
					}
				}
				if ("store".equals(what) && body.has("key") && body.has("data")) {
					NSRUtils.storeData(body.getString("key"), body.getJSONObject("data"),getApplicationContext());
				}
				if ("retrive".equals(what) && body.has("key") && body.has("callBack")) {
					JSONObject val = NSRUtils.retrieveData(body.getString("key"),getApplicationContext());
					eval(body.getString("callBack") + "(" + (val != null ? val.toString() : "null") + ")");
				}
				if ("retrieve".equals(what) && body.has("key") && body.has("callBack")) {
					JSONObject val = NSRUtils.retrieveData(body.getString("key"),getApplicationContext());
					eval(body.getString("callBack") + "(" + (val != null ? val.toString() : "null") + ")");
				}
				if ("callApi".equals(what) && body.has("callBack")) {
					nsr.authorize(new NSRAuth() {
						public void authorized(boolean authorized) throws Exception {
							if (!authorized) {
								JSONObject result = new JSONObject();
								result.put("status", "error");
								result.put("message", "not authorized");
								eval(body.getString("callBack") + "(" + result.toString() + ")");
								return;
							}
							JSONObject headers = new JSONObject();
							headers.put("ns_token", NSRUtils.getToken(getApplicationContext()));
							headers.put("ns_lang", NSRUtils.getLang(getApplicationContext()));
							nsr.getSecurityDelegate().secureRequest(getApplicationContext(), body.getString("endpoint"), body.has("payload") ? body.getJSONObject("payload") : null, headers, new NSRSecurityResponse() {
								public void completionHandler(JSONObject json, String error) throws Exception {
									if (error == null) {
										eval(body.getString("callBack") + "(" + json.toString() + ")");
									} else {
										NSRLog.e("secureRequest: " + error);
										JSONObject result = new JSONObject();
										result.put("status", "error");
										result.put("message", error);
										eval(body.getString("callBack") + "(" + result.toString() + ")");
									}
								}
							});
						}
					});
				}
				if ("geoCode".equals(what) && body.has("location") && body.has("callBack")) {
					if (Build.VERSION.SDK_INT >= 21) {
						Geocoder geocoder = new Geocoder(this, Locale.forLanguageTag(NSRUtils.getLang(getApplicationContext())));
						JSONObject location = body.getJSONObject("location");
						List<Address> addresses = geocoder.getFromLocation(location.getDouble("latitude"), location.getDouble("longitude"), 1);
						if (addresses != null && addresses.size() > 0) {
							Address adr = addresses.get(0);
							JSONObject address = new JSONObject();
							address.put("countryCode", adr.getCountryCode().toUpperCase());
							address.put("countryName", adr.getCountryName());
							String adrLine = adr.getAddressLine(0);
							address.put("address", adrLine != null ? adrLine : "");
							eval(body.getString("callBack") + "(" + address.toString() + ")");
						}
					}
				}
				if (nsr.getWorkflowDelegate() != null && "executeLogin".equals(what) && body.has("callBack")) {
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						public void run() {
							try {
								if (Build.VERSION.SDK_INT >= 21) {
									webView.evaluateJavascript(body.getString("callBack") + "(" + nsr.getWorkflowDelegate().executeLogin(getApplicationContext(), webView.getUrl()) + ")", null);
								}
							} catch (Throwable e) {
							}
						}
					});
				}
				if (nsr.getWorkflowDelegate() != null && "executePayment".equals(what) && body.has("payment")) {
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						public void run() {
							try {
								JSONObject paymentInfo = nsr.getWorkflowDelegate().executePayment(getApplicationContext(), body.getJSONObject("payment"), webView.getUrl());
								if (body.has("callBack") && Build.VERSION.SDK_INT >= 21) {
									webView.evaluateJavascript(body.getString("callBack") + "(" + (paymentInfo != null ? paymentInfo.toString() : "") + ")", null);
								}
							} catch (Throwable e) {
							}
						}
					});
				}
				if (nsr.getWorkflowDelegate() != null && "confirmTransaction".equals(what) && body.has("paymentInfo")) {
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						public void run() {
							try {
								nsr.getWorkflowDelegate().confirmTransaction(getApplicationContext(), body.getJSONObject("paymentInfo"));
							} catch (Throwable e) {
							}
						}
					});
				}
			}
		} catch (Exception e) {
			NSRLog.e("postMessage", e);
		}
	}

	private File imageFile() {
		File path = new File(Environment.getExternalStorageDirectory(), this.getPackageName());
		if (!path.exists()) {
			path.mkdir();
		}
		return new File(path, "nsr-photo.jpg");
	}

	private void takePhoto(final String callBack) {
		boolean camera = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
		boolean storage = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		if (camera && storage) {
			photoCallback = callBack;
			Intent mIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			if (mIntent.resolveActivity(this.getPackageManager()) != null) {
				mIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile()));
				this.startActivityForResult(mIntent, NSR.REQUEST_IMAGE_CAPTURE);
			}
		} else {
			List<String> permissionsList = new ArrayList<String>();
			if (!camera) {
				permissionsList.add(Manifest.permission.CAMERA);
			}
			if (!storage) {
				permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			}
			ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), NSR.PERMISSIONS_MULTIPLE_IMAGECAPTURE);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Build.VERSION.SDK_INT >= 21 && requestCode == NSR.REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				int orientation = new ExifInterface(imageFile().getAbsolutePath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
				int degree = 0;
				if (orientation == 6) {
					degree = 90;
				} else if (orientation == 3) {
					degree = 180;
				} else if (orientation == 8) {
					degree = 270;
				}
				Bitmap b = BitmapFactory.decodeFile(imageFile().getAbsolutePath());
				if (degree > 0) {
					Matrix matrix = new Matrix();
					matrix.postRotate(degree);
					b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
				}
				float k = 1;
				int maxSize = (b.getWidth() >= b.getHeight()) ? b.getWidth() : b.getHeight();
				if (maxSize > 1024) {
					k = (1024F / maxSize);
				}
				Bitmap.createScaledBitmap(b, Math.round(b.getWidth() * k), Math.round(b.getHeight() * k), false).compress(Bitmap.CompressFormat.JPEG, 60, baos);
				imageFile().delete();
				eval(photoCallback + "('data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP) + "')");
			} catch (Exception e) {
				NSRLog.e(e.getMessage(), e);
			}
		}
	}

	private void getLocation(final String callBack) {
		boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
		boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
		if (coarse || fine) {
			final FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
			LocationRequest locationRequest = LocationRequest.create();
			locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			locationRequest.setInterval(0);
			locationRequest.setNumUpdates(1);
			locationClient.requestLocationUpdates(locationRequest,
							new LocationCallback() {
								public void onLocationResult(LocationResult locationResult) {
									Location location = locationResult.getLastLocation();
									if (location != null) {
										locationClient.removeLocationUpdates(this);
										try {
											JSONObject locationAsJson = new JSONObject();
											locationAsJson.put("latitude", location.getLatitude());
											locationAsJson.put("longitude", location.getLongitude());
											locationAsJson.put("altitude", location.getAltitude());
											eval(callBack + "(" + locationAsJson.toString() + ")");
										} catch (JSONException e) {
										}
									}
								}
							}, null);
		} else {
			List<String> permissionsList = new ArrayList<String>();
			permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
			permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
			ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]), NSR.PERMISSIONS_MULTIPLE_ACCESSLOCATION);
		}
	}

	private void idle() {
		new Handler().postDelayed(new Runnable() {
			public void run() {
				try {
					if (webView != null && Build.VERSION.SDK_INT >= 21) {
						webView.evaluateJavascript("(function() { return (window.document.body.className.indexOf('NSR') == -1 ? false : true); })();", new ValueCallback<String>() {
							public void onReceiveValue(String value) {
								if ("true".equals(value)) {
									idle();
								} else {
									finish();
								}
							}
						});
					}
				} catch (Throwable e) {
				}
			}
		}, 15000);
	}

	public synchronized void finish() {
		nsr.clearWebView();
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				try {
					if (webView != null) {
						webView.stopLoading();
						webView.destroy();
						webView = null;
					}
				} catch (Throwable e) {
				}
			}
		});
		super.finish();
	}



	protected void onStop() {
		Log.d("VW", "NSRActivity onStop");
			super.onStop();
	}

	@Override
	protected void onStart() {
		Log.d("VW", "NSRActivity onStart");
		super.onStart();
	}

	@Override
	protected void onRestart() {
		Log.d("VW", "NSRActivity onRestart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		Log.d("VW", "NSRActivity onResume");
		super.onResume();
	}

	@Override
	protected void onPause() {
		Log.d("VW", "NSRActivity onPause");
		super.onPause();
	}

}
