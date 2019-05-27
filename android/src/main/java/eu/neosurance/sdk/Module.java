package eu.neosurance.sdk;

import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Module extends ReactContextBaseJavaModule {

  private static final String DURATION_SHORT_KEY = "SHORT";
  private static final String DURATION_LONG_KEY = "LONG";
  public static ReactApplicationContext ctx;

  public Module(ReactApplicationContext reactContext) {
    super(reactContext);
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
  public void show(final String message, final int duration) {

    getReactApplicationContext().runOnUiQueueThread(new Runnable() {
      @Override
      public void run() {
        //Toast.makeText(getReactApplicationContext(), message, duration).show();

        ctx = getReactApplicationContext();
        NSRSettings settings = new NSRSettings();
        settings.setDisableLog(false);
        settings.setDevMode(true);
        settings.setBaseUrl("https://sbsdk.neosurancecloud.net/api/v1.0/");
        settings.setCode("bppb");
        settings.setSecretKey("pass");
        settings.setPushIcon(R.drawable.nsr_logo);
        //settings.setWorkflowDelegate(new WFDelegate(),ctx);
        NSR.getInstance(ctx).setup(settings, new JSONObject());
        NSR.getInstance(ctx).askPermissions(((ReactApplicationContext) ctx).getCurrentActivity());

        NSRUser user = new NSRUser();
        user.setEmail("mario@rossi.com");
        user.setCode("mario@rossi.com");
        user.setFirstname("Mario");
        user.setLastname("Rossi");
        user.setFiscalCode("RSSMRA85T01F205P");
        JSONObject locals = new JSONObject();
        try {
          locals.put("email","mario@rossi.com");
          locals.put("firstname","Mario");
          locals.put("lastname","Rossi");
          locals.put("fiscalCode","RSSMRA85T01F205P");
          locals.put("pushToken","fake-push");
          user.setLocals(locals);
        } catch (JSONException e) {
          e.printStackTrace();
        }

        NSR.getInstance(ctx).registerUser(user);

        try {
          Thread.sleep(10000);
          Log.d("MODULE", "sendEvent");
          JSONObject payload = new JSONObject();
          payload.put("iata", "LIN");
          NSR.getInstance(ctx).sendEvent("inAirport", payload);
        } catch (Exception e) {
          Log.e("MODULE", "sendEvent exception: " + e.getMessage());
        }

      }
    });


  }

  @ReactMethod
  public void showLog(String message) {
    Log.d("MODULE NSR",">>>>> " + message);
  }

  @ReactMethod
  public void setup() {

      getReactApplicationContext().runOnUiQueueThread(new Runnable() {
            @Override
            public void run() {

              Toast.makeText(getReactApplicationContext(), "RUNNING SETUP...", Toast.LENGTH_LONG).show();

              ctx = getReactApplicationContext();
              NSRSettings settings = new NSRSettings();
              settings.setDisableLog(false);
              settings.setDevMode(true);
              settings.setBaseUrl("https://sbsdk.neosurancecloud.net/api/v1.0/");
              settings.setCode("bppb");
              settings.setSecretKey("pass");
              settings.setPushIcon(R.drawable.nsr_logo);
              //settings.setWorkflowDelegate(new WFDelegate(),ctx);
              NSR.getInstance(ctx).setup(settings, new JSONObject());
              NSR.getInstance(ctx).askPermissions(((ReactApplicationContext) ctx).getCurrentActivity());

            }
      });

  }

  @ReactMethod
  public void registerUser(final String msg) {

    getReactApplicationContext().runOnUiQueueThread(new Runnable() {
        @Override
        public void run() {

            Toast.makeText(getReactApplicationContext(), "RUNNING " + msg + "...", Toast.LENGTH_LONG).show();

            NSRUser user = new NSRUser();
            user.setEmail("mario@rossi.com");
            user.setCode("mario@rossi.com");
            user.setFirstname("Mario");
            user.setLastname("Rossi");
            user.setFiscalCode("RSSMRA85T01F205P");
            JSONObject locals = new JSONObject();

            try {
                locals.put("email","mario@rossi.com");
                locals.put("firstname","Mario");
                locals.put("lastname","Rossi");
                locals.put("fiscalCode","RSSMRA85T01F205P");
                locals.put("pushToken","fake-push");
                user.setLocals(locals);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            NSR.getInstance(ctx).registerUser(user);

        }
    });

  }

  @ReactMethod
  public void sendTrialEvent() {

    getReactApplicationContext().runOnUiQueueThread(new Runnable() {
        @Override
        public void run() {

            Toast.makeText(getReactApplicationContext(), "RUNNING SEND TRIAL EVENT...", Toast.LENGTH_LONG).show();

            try {
                Log.d("MODULE", "sendEvent");
                JSONObject payload = new JSONObject();
                payload.put("fake", "1");
                NSR.getInstance(ctx).sendEvent("trg1", payload);
            }catch (Exception e) {
                Log.e("MODULE", "sendEvent exception: " + e.getMessage());
            }

        }
    });

  }

  @ReactMethod
  public void showList() {

      getReactApplicationContext().runOnUiQueueThread(new Runnable() {
          @Override
          public void run() {

              Toast.makeText(getReactApplicationContext(), "RUNNING SHOW LIST...", Toast.LENGTH_LONG).show();

              NSR.getInstance(ctx).showApp();

          }
      });

  }


}
