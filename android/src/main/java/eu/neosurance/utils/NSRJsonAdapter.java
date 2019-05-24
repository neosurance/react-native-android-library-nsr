package eu.neosurance.utils;

import org.json.JSONException;
import org.json.JSONObject;
import eu.neosurance.sdk.NSRLog;
import eu.neosurance.sdk.NSRUser;

public class NSRJsonAdapter {

    public static void setNSRJson(JSONObject jsonObject) throws Exception {
        if (jsonObject.has("code"))
            NSRUser.code = jsonObject.getString("code");
        if (jsonObject.has("email"))
            NSRUser.email = jsonObject.getString("email");
        if (jsonObject.has("firstname"))
            NSRUser.firstname = jsonObject.getString("firstname");
        if (jsonObject.has("lastname"))
            NSRUser.lastname = jsonObject.getString("lastname");
        if (jsonObject.has("mobile"))
            NSRUser.mobile = jsonObject.getString("mobile");
        if (jsonObject.has("fiscalCode"))
            NSRUser.fiscalCode = jsonObject.getString("fiscalCode");
        if (jsonObject.has("gender"))
            NSRUser.gender = jsonObject.getString("gender");
        if (jsonObject.has("birthday"))
            NSRUser.birthday = NSRUtils.jsonStringToDate(jsonObject.getString("birthday"));
        if (jsonObject.has("address"))
            NSRUser.address = jsonObject.getString("address");
        if (jsonObject.has("zipCode"))
            NSRUser.zipCode = jsonObject.getString("zipCode");
        if (jsonObject.has("city"))
            NSRUser.city = jsonObject.getString("city");
        if (jsonObject.has("stateProvince"))
            NSRUser.stateProvince = jsonObject.getString("stateProvince");
        if (jsonObject.has("country"))
            NSRUser.country = jsonObject.getString("country");
        if (jsonObject.has("extra"))
            NSRUser.extra = jsonObject.getJSONObject("extra");
        if (jsonObject.has("locals"))
            NSRUser.locals = jsonObject.getJSONObject("locals");
    }

    public static JSONObject toJsonObject(boolean withLocals) {
        JSONObject jsonObject = new JSONObject();
        try {
            if (NSRUser.code != null)
                jsonObject.put("code", NSRUser.code);
            if (NSRUser.email != null)
                jsonObject.put("email", NSRUser.email);
            if (NSRUser.firstname != null)
                jsonObject.put("firstname", NSRUser.firstname);
            if (NSRUser.lastname != null)
                jsonObject.put("lastname", NSRUser.lastname);
            if (NSRUser.mobile != null)
                jsonObject.put("mobile", NSRUser.mobile);
            if (NSRUser.fiscalCode != null)
                jsonObject.put("fiscalCode", NSRUser.fiscalCode);
            if (NSRUser.gender != null)
                jsonObject.put("gender", NSRUser.gender);
            if (NSRUser.birthday != null)
                jsonObject.put("birthday", NSRUtils.dateToJsonString(NSRUser.birthday));
            if (NSRUser.address != null)
                jsonObject.put("address", NSRUser.address);
            if (NSRUser.zipCode != null)
                jsonObject.put("zipCode", NSRUser.zipCode);
            if (NSRUser.city != null)
                jsonObject.put("city", NSRUser.city);
            if (NSRUser.stateProvince != null)
                jsonObject.put("stateProvince", NSRUser.stateProvince);
            if (NSRUser.country != null)
                jsonObject.put("country", NSRUser.country);
            if (NSRUser.extra != null)
                jsonObject.put("extra", NSRUser.extra);
            if (withLocals && NSRUser.locals != null)
                jsonObject.put("locals", NSRUser.locals);
        } catch (JSONException e) {
            NSRLog.e("toJsonObject", e);
        }
        return jsonObject;
    }

}
