package eu.neosurance.sdk;

import android.content.Context;

import org.json.JSONObject;

public interface NSRWorkflowDelegate {
	boolean executeLogin(Context ctx, String url);

	JSONObject executePayment(Context ctx, JSONObject payment, String url);

	void confirmTransaction(Context ctx, JSONObject paymentInfo);
}