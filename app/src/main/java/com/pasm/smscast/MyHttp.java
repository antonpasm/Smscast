package com.pasm.smscast;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;


// Не забыть добавить в manifest android:usesCleartextTraffic="true", чтобы работал http

class MyHttp {

    @SuppressWarnings("CanBeFinal")
    public static RetryPolicy myRetryPolicy = new DefaultRetryPolicy(3000, 2, 3);

    public static void logError(VolleyError error, String description, String url, Context context) {

        String tag = "" + context.getPackageName();
        NetworkResponse response = error.networkResponse;
        description = description == null ? "" : description + " ";
        Log.e(tag, description + error);
        Log.e(tag, "url=" + url);
        if (response != null) {
            Log.e(tag, "Status Code=" + response.statusCode);
            Log.e(tag, "Data=" + new String(response.data));
        }
    }

    public static void httpGET(Context context,
                               String url,
                               Response.Listener<String> listener,
                               Response.ErrorListener errorListener
                               ) {
        try {
            StringRequest request = new StringRequest(
                    Request.Method.GET,
                    url,
                    listener,
                    errorListener
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    // Закрывать соединение, т.к. отправим всего 1 запрос
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Connection", "close");
                    return headers;
                }
            };
            request.setRetryPolicy(myRetryPolicy);
            RequestQueue queue = Volley.newRequestQueue(context);
            queue.add(request);
        } catch (Exception e) {
            Log.e("" + context.getPackageName(), "exception in HttpGET. " + e.getMessage());
        }
    }

    public static void httpPOST(Context context,
                                String url,
                                Map<String, String> params,
                                Response.Listener<String> listener,
                                Response.ErrorListener errorListener) {
        try {
            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    listener,
                    errorListener
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    // Закрывать соединение, т.к. отправим всего 1 запрос
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Connection", "close");
                    return headers;
                }

                @Override
                protected Map<String, String> getParams() {
                    return params;
                }
            };
            request.setRetryPolicy(myRetryPolicy);
            RequestQueue queue = Volley.newRequestQueue(context);
            queue.add(request);
        } catch (Exception e) {
            Log.e("" + context.getPackageName(), "exception in HttpPOST. " + e.getMessage());
        }
    }

}
