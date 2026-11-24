package fr.margeo.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int fond = Color.parseColor("#07080c");
        getWindow().setStatusBarColor(fond);
        getWindow().setNavigationBarColor(fond);

        web = new WebView(this);
        web.setBackgroundColor(fond);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "Native");

        // Les liens vers eBay s'ouvrent dans le navigateur ou l'appli eBay
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("http")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                    return true;
                }
                return false;
            }
        });

        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    @Override
    public void onBackPressed() {
        // Ferme d'abord un panneau ouvert, sinon quitte l'appli
        web.evaluateJavascript("window.onBack ? window.onBack() : false", value -> {
            if (!"true".equals(value)) retourSysteme();
        });
    }

    private void retourSysteme() {
        super.onBackPressed();
    }

    /** Appels HTTP faits par Android (pas de blocage CORS comme dans un navigateur). */
    class Bridge {
        /** Numéro de version installé (= numéro de compilation GitHub). */
        @JavascriptInterface
        public int version() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            } catch (Exception e) {
                return 0;
            }
        }

        @JavascriptInterface
        public void http(final String id, final String method, final String url,
                         final String headers, final String body) {
            new Thread(() -> {
                int code = 0;
                String text;
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setRequestMethod(method);
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(25000);
                    JSONObject h = new JSONObject(headers == null || headers.isEmpty() ? "{}" : headers);
                    Iterator<String> keys = h.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        c.setRequestProperty(k, h.getString(k));
                    }
                    if (body != null && !body.isEmpty()) {
                        c.setDoOutput(true);
                        OutputStream o = c.getOutputStream();
                        o.write(body.getBytes("UTF-8"));
                        o.close();
                    }
                    code = c.getResponseCode();
                    InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    text = in == null ? "" : lire(in);
                } catch (Exception e) {
                    text = "Erreur réseau : " + e.getMessage();
                }
                final String js = "window.__native(" + JSONObject.quote(id) + "," + code + ","
                        + JSONObject.quote(text) + ")";
                runOnUiThread(() -> web.evaluateJavascript(js, null));
            }).start();
        }
    }

    private static String lire(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = r.readLine()) != null) sb.append(l).append('\n');
        r.close();
        return sb.toString();
    }
}
