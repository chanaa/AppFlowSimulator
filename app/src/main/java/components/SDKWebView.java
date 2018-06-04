package components;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;

public class SDKWebView extends WebView {

    public interface SDKWebViewClientListener {

        void challengeHtmlDataEntered(String htmlData);

    }

    private SDKWebViewClientListener sdkWebViewClientListener;

    private class SDKWebViewClient extends WebViewClient {

        public static final String HtmlVerify = "HTTPS://EMV3DS/challenge";

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (sdkWebViewClientListener == null){
                return false;
            }

            boolean intercept = url.toLowerCase().startsWith(HtmlVerify.toLowerCase());

            if (intercept){
                sdkWebViewClientListener.challengeHtmlDataEntered(url.substring(url.indexOf('?')+1));

                return true;
            }

            return false;
        }


        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (sdkWebViewClientListener == null){
                return false;
            }

            boolean intercept = request.getUrl().toString().toLowerCase().startsWith(HtmlVerify.toLowerCase());

            if (intercept){
                sdkWebViewClientListener.challengeHtmlDataEntered(request.getUrl().getQuery());

                return true;
            }

            return false;
        }

        @Override
        public void onLoadResource(WebView view, String url) {
        }

        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          String url) {
            boolean intercept = url.toLowerCase().startsWith(HtmlVerify.toLowerCase());

            if (intercept){
                return new WebResourceResponse("text/html", "UTF-8", new
                        ByteArrayInputStream(new byte[0]));
            }

            return null;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          WebResourceRequest request) {

            boolean intercept = request.getUrl().toString().toLowerCase().startsWith(HtmlVerify.toLowerCase());

            if (intercept){
                return new WebResourceResponse("text/html", "UTF-8", new
                        ByteArrayInputStream(new byte[0]));
            }

            return null;

        }
    }

    public void setSdkWebViewClientListener(SDKWebViewClientListener sdkWebViewClientListener){
        this.sdkWebViewClientListener = sdkWebViewClientListener;
    }

    public SDKWebView(Context context) {
        this(context, null);
    }

    public SDKWebView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        super.getSettings().setJavaScriptEnabled(false);
        super.getSettings().setAllowContentAccess(false);
        super.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        SDKWebViewClient sdkWebViewClient = new SDKWebViewClient();
        setWebViewClientInternal(sdkWebViewClient);
    }

    @Override
    public WebSettings getSettings() {
        return null;
    }

    @Override
    public void addJavascriptInterface(Object object, String name) {
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
    }

    protected void setWebViewClientInternal(WebViewClient client) {
        super.setWebViewClient(client);
    }
}