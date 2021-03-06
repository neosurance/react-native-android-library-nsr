package eu.neosurance.sdk;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import eu.neosurance.utils.NSRUtils;

public class Module extends ReactContextBaseJavaModule {

  private static final String DURATION_SHORT_KEY = "SHORT";
  private static final String DURATION_LONG_KEY = "LONG";
  public static ReactApplicationContext ctx;

  public Module(ReactApplicationContext reactContext) {
    super(reactContext);
  }
  
  public static void sendEvent(ReactApplicationContext reactContext, String eventName, WritableMap params) {
	  reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
  }

  @Override
  public String getName() {
    return "Neosurance";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put(DURATION_SHORT_KEY, Toast.LENGTH_SHORT);
    constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
    return constants;
  }

  @ReactMethod
  public String getTitle(){
      return "NSR SDK React Native Android!";
  }


  @ReactMethod
  public void setup(final String settingsTmp, final Callback callback) {

      ctx = getReactApplicationContext();
      ctx.runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {				

                try {
                    //Toast.makeText(ctx, "RUNNING SETUP...", Toast.LENGTH_LONG).show();
					Log.d("Module", "setup");

                    JSONObject settingsJson = new JSONObject(settingsTmp);

                    NSRSettings settings = new NSRSettings();
                    settings.setDisableLog(Boolean.parseBoolean(settingsJson.getString("disable_log")));
                    settings.setDevMode(Boolean.parseBoolean(settingsJson.getString("dev_mode")));
                    settings.setBaseUrl(settingsJson.getString("base_url"));
                    settings.setCode(settingsJson.getString("code"));
                    settings.setSecretKey(settingsJson.getString("secret_key"));

                    settings.setPushIcon(R.drawable.nsr_logo);
                    settings.setWorkflowDelegate(new WFDelegate(),ctx);
                    NSR.getInstance(ctx).setup(settings, new JSONObject(), new NSRSecurityResponse() {
                        public void completionHandler(JSONObject json, String error) throws Exception {
                            if (error == null) {
                                Log.d("Module", "setup response");
                                Log.d("Module", json.toString());
								
								callback.invoke(json.toString());
								
                            } else {
                                Log.e("Module", "setup error: " + error);
								callback.invoke(error);
                            }
                        }
                    });
					
                    NSR.getInstance(ctx).askPermissions(((ReactApplicationContext) ctx).getCurrentActivity());

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
      });

  }

  @ReactMethod
  public void registerUser(final String userTmp, final Callback callback) {

      ctx = getReactApplicationContext();
      ctx.runOnUiQueueThread(new Runnable() {
        @Override
        public void run() {

            try {
                JSONObject userJson = new JSONObject(userTmp);
                //Toast.makeText(ctx, "RUNNING " + userJson.getString("method") + "...", Toast.LENGTH_LONG).show();
				Log.d("Module", "registerUser");
				
                NSRUser user = new NSRUser();
                user.setEmail(userJson.getString("email"));
                user.setCode(userJson.getString("code"));
                user.setFirstname(userJson.getString("firstname"));
                user.setLastname(userJson.getString("lastname"));
				
	            user.setAddress(userJson.getString("address"));
	            user.setZipCode(userJson.getString("cap"));
	            user.setCity(userJson.getString("city"));
	            user.setStateProvince(userJson.getString("province"));				
                user.setFiscalCode(userJson.getString("fiscalCode"));

				if(userJson.getJSONObject("locals").length() > 0){
                	JSONObject locals = new JSONObject(userJson.getString("locals"));
					user.setLocals(locals);
				}	
										
				callback.invoke(userTmp);

                NSR.getInstance(ctx).registerUser(user);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    });

  }

  @ReactMethod
  public void sendTrialEvent(final String event) {

      ctx = getReactApplicationContext();
      ctx.runOnUiQueueThread(new Runnable() {
        @Override
        public void run() {

            //Toast.makeText(ctx, "RUNNING SEND TRIAL EVENT...", Toast.LENGTH_LONG).show();
			Log.d("Module", "sendEvent");

            try {
                JSONObject eventJson = new JSONObject(event);

                Log.d("MODULE", "sendEvent");
                JSONObject payload = new JSONObject(eventJson.getString("payload"));

                NSR.getInstance(ctx).sendEvent(eventJson.getString("event"), payload);
            }catch (Exception e) {
                Log.e("MODULE", "sendEvent exception: " + e.getMessage());
            }

        }
    });

  }

  @ReactMethod
  public void showApp() {

      ctx = getReactApplicationContext();
      ctx.runOnUiQueueThread(new Runnable() {
          @Override
          public void run() {

              //Toast.makeText(ctx, "RUNNING SHOW LIST...", Toast.LENGTH_LONG).show();
			  Log.d("Module", "showApp");

              NSR.getInstance(ctx).showApp();

          }
      });

  }

    @ReactMethod
    public void takePicture(final String event) {

        ctx = getReactApplicationContext();
        ctx.runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {

                //Toast.makeText(ctx, "RUNNING NSR CLAIM...", Toast.LENGTH_LONG).show();
				Log.d("Module", "takePicture");
				
                try {
                    ctx.startActivity(NSRUtils.makeActivityWebView(event,ctx));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    @ReactMethod
    public void appLogin() {

        ctx = getReactApplicationContext();
        ctx.runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {

                //Toast.makeText(ctx, "RUNNING LOGIN EXECUTED...", Toast.LENGTH_LONG).show();
				Log.d("Module", "appLogin");
				
                try {

                    String url = WFDelegate.getData(ctx,"login_url");
                    //NSR.getInstance(ctx).loginExecuted(url);
                    NSRUser.loginExecuted(url,ctx);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    @ReactMethod
    public void appPayment() {

        ctx = getReactApplicationContext();
        ctx.runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {

                //Toast.makeText(ctx, "RUNNING PAYMENT EXECUTED...", Toast.LENGTH_LONG).show();
				Log.d("Module", "appPayment");
				
                try {

                    String payment_url = WFDelegate.getData(ctx,"payment_url");
                    JSONObject paymentJson = new JSONObject(WFDelegate.getData(ctx,"payment"));

                    NSRUser.paymentExecuted(paymentJson,payment_url);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }
	
    @ReactMethod
    public void policies(final Callback callback) {

        ctx = getReactApplicationContext();
        ctx.runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {

                //Toast.makeText(ctx, "GETTING POLICIES...", Toast.LENGTH_LONG).show();
				Log.d("Module", "policies");
				
                try {

                    JSONObject criteria = new JSONObject();
                    criteria.put("available",true);
					
                    NSR.getInstance(ctx).policies(criteria, new NSRSecurityResponse() {
                        public void completionHandler(JSONObject json, String error) throws Exception {
                            if (error == null) {
                                Log.d("Module", "policies response");
                                Log.d("Module", json.toString());
								
								callback.invoke(json.toString());
								
                            } else {
                                Log.e("Module", "policies error: " + error);
								callback.invoke(error);
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }
	
    @ReactMethod
    public void closeView() {

        ctx = getReactApplicationContext();
        ctx.runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {

                //Toast.makeText(ctx, "CLOSING VIEW...", Toast.LENGTH_LONG).show();
				Log.d("Module", "closeView");
				
                try {
                    NSR.getInstance(ctx).closeView();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }


}
