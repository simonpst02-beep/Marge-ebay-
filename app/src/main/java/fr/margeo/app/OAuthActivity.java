package fr.margeo.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Page de connexion eBay : l'utilisateur se connecte et accepte, puis on récupère
 * le code d'autorisation dans l'adresse de retour (l'appli ne voit jamais le mot de passe).
 */
public class OAuthActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.WHITE);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        w.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return verifier(request.getUrl());
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                verifier(Uri.parse(url));
            }
        });
        setContentView(w);
        w.loadUrl(getIntent().getStringExtra("url"));
    }

    /** Repère l'adresse de retour d'eBay (hors domaines eBay) et en extrait le code. */
    private boolean verifier(Uri u) {
        String hote = u.getHost() == null ? "" : u.getHost();
        if (hote.contains("ebay.")) return false;
        String code = null, ok = null;
        try {
            code = u.getQueryParameter("code");
            ok = u.getQueryParameter("isAuthSuccessful");
        } catch (Exception ignored) { }
        if (code != null && !code.isEmpty()) {
            setResult(RESULT_OK, new Intent().putExtra("code", code));
            finish();
            return true;
        }
        if ("false".equals(ok)) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
