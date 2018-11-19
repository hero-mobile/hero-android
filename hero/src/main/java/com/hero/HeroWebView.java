/**
 * BSD License
 * Copyright (c) Hero software.
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.

 * Neither the name Hero nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific
 * prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.hero;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hero.depandency.ContextUtils;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by liuguoping on 15/9/24.
 */
@SuppressLint("SetJavaScriptEnabled")
public class HeroWebView extends WebView implements IHero {
    static final String TAG = "HeroWebView";
    public final static boolean NEED_VERIFY_URL_HOST = false;
    public final static String FRAGMENT_TAG_KEY = "fragment_tag";
    private HeroFragment parentFragment = null;
    private JSONArray hijackUrlArray;
    private String mUrl;
    private String postData;
    private String method;
    private static final String METHOD_POST = HttpPost.METHOD_NAME;

    public HeroWebView(Context context) {
        super(context);
        // On some phones SecurityException will be thrown for lacking WRITE_SECURE_SETTINGS permission,
        // but the method doesn't need a secure permission at all, furthermore, we can't ask for
        // the WRITE_SECURE_SETTINGS permission because it's a system permission
        try {
            this.getSettings().setJavaScriptEnabled(true);


            //add
            this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
        this.addJavascriptInterface(this, "native");
//        this.addJavascriptInterface(this,"npc");
//        if (Build.VERSION.SDK_INT < 17) {
//            this.removeJavascriptInterface("searchBoxJavaBridge_");
//        }
        String userAgent = this.getSettings().getUserAgentString();
        userAgent += " Android/" + ContextUtils.getSystemVersion() + " hero-android/" + ContextUtils.getVersionCode(this.getContext()) + " imei/" + ContextUtils.getIMEI(context) + " androidId/" + ContextUtils.getAndroidId(context);
        userAgent += " Brand/" + ContextUtils.getDeviceBrand() +" Model/" + ContextUtils.getDeviceName();
        if (HeroApplication.getInstance() != null) {
            String extraUA = HeroApplication.getInstance().getExtraUserAgent();
            if (!TextUtils.isEmpty(extraUA)) {
                userAgent += " " + extraUA;
            }
        }
        this.getSettings().setUserAgentString(userAgent);
        this.getSettings().setDomStorageEnabled(true);
        String appCachePath = getContext().getCacheDir().getAbsolutePath();
        this.getSettings().setAppCachePath(appCachePath);
        this.getSettings().setAllowFileAccess(true);
        this.getSettings().setAppCacheEnabled(true);
        this.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        this.setDownloadListener(new MyWebViewDownLoadListener());

        final Context theContext = context;
        this.setWebViewClient(new WebViewClient() {



            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
//                Log.i("test test","vonLoadResourceonLoadResourceonLoadResource");
//
//                try {
//                    InputStream home = getResources().getAssets().open("hero-provider.js");
//                    String content = null;
//                    try {
//                        content = inputStreamTOString(home);
//                    } catch (Exception e) {
//                        Log.d("Error", e.getMessage());
//                    }
//                    view.loadUrl("javascript:" + content);
////                    view.loadData(content, "text/html;charset=UTF-8", null);
//                } catch (IOException IOE) {
//
//                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!isUrlAuthenticated(url)) {
                    return true;
                }

                if (hijackUrlArray != null) {
                    try {
                        JSONObject jsonObject = shouldHijackUrl(url);
                        if (jsonObject != null) {
                            JSONObject event = new JSONObject();
                            event.put("name", HeroView.getName(view));
                            event.put("url", jsonObject.optString("url"));
                            HeroView.sendActionToContext(theContext, event);

                            if (jsonObject.has("isLoad")) {
                                // isLoad: this url need to be loaded
                                if (!jsonObject.getBoolean("isLoad")) {
                                    return true;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if(!url.startsWith("http:") || url.startsWith("https:") ) {
                    return false;
                }

                view.loadUrl(url);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                Log.i(TAG, "onReceivedError " + error);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                InputStream page404 = getResources().openRawResource(R.raw.page_404);
                if (page404 != null) {
                    String content = null;
                    try {
                        content = inputStreamTOString(page404);
                    } catch (Exception e) {
                        Log.d("Error", e.getMessage());
                    }
                    view.loadData(content, "text/html;charset=UTF-8", null);
                }

                try {
                    JSONObject object = new JSONObject("{common:'webViewDidFinishLoad'}");
                    HeroView.sendActionToContext(getContext(), object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                Log.i(TAG, "onReceivedHttpError " + errorResponse);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (view.getParent() != null && parentFragment != null) {
                    parentFragment.showToolBar(true);
                }else{
                    try {
                        HeroFragment.evaluateJavaScript(view, HeroFragment.VIEW_WILL_APPEAR_EXPRESSION);

                        JSONObject object = new JSONObject("{common:'webViewDidFinishLoad'}");
                        HeroView.sendActionToContext(getContext(), object);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                setWindowAttribute();
            }

            @Override
            public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                if ((event.getKeyCode() == KeyEvent.KEYCODE_BACK) && view.canGoBack()) {
                    view.goBack();
                    return true;
                }
                return false;
            }
        });
        this.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                return super.onJsConfirm(view, url, message, result);
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }

            @Override
            public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                return super.onJsBeforeUnload(view, url, message, result);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (view.getParent() != null && parentFragment != null) {
                    if (!TextUtils.isEmpty(title) && !title.contains(".html")) {
                        parentFragment.showToolBar(true);
                        parentFragment.setTitle(title);
                    }
                }
            }


            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                    super.onProgressChanged(view, newProgress);
            }
        });
    }

    public static String inputStreamTOString(InputStream in) throws Exception{
        int BUFFER_SIZE = 1024;

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while((count = in.read(data,0,BUFFER_SIZE)) != -1)
            outStream.write(data, 0, count);

        data = null;
        return new String(outStream.toByteArray(),"UTF-8");
    }

    public HeroWebView(Context context, int initColor) {
        this(context);
        this.setBackgroundColor(initColor);
    }

    @JavascriptInterface
    public void on(String jsonStr) {
        try {
            Object json = new JSONTokener(jsonStr).nextValue();
            if (parentFragment != null && parentFragment.getTag() != null) {
                if (json instanceof JSONObject) {
                    ((JSONObject) json).put(FRAGMENT_TAG_KEY, parentFragment.getTag());
                } else if (json instanceof JSONArray) {
                    JSONObject tag = new JSONObject();
                    tag.put(FRAGMENT_TAG_KEY, parentFragment.getTag());
                    ((JSONArray) json).put(tag);
                }
            }
            if (this.getContext() instanceof IHeroContext) {
                ((IHeroContext) this.getContext()).on(json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void npc(String jsonStr) {
        try {
            Object json = new JSONTokener(jsonStr).nextValue();
            if (parentFragment != null && parentFragment.getTag() != null) {
                if (json instanceof JSONObject) {
                    ((JSONObject) json).put(FRAGMENT_TAG_KEY, parentFragment.getTag());
                } else if (json instanceof JSONArray) {
                    JSONObject tag = new JSONObject();
                    tag.put(FRAGMENT_TAG_KEY, parentFragment.getTag());
                    ((JSONArray) json).put(tag);
                }
            }
            if (this.getContext() instanceof IHeroContext) {
                ((IHeroContext) this.getContext()).on(json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadUrl(String url) {
        if (BuildConfig.DEBUG && url.startsWith("http")) {
            if (url.contains("?")) {
                url = url + "&test=true";
            } else {
                url = url + "?test=true";
            }
        }
        if (!isUrlAuthenticated(url)) {
            return;
        }
        Map header = null;
        if (HeroApplication.getInstance() != null) {
            header = HeroApplication.getInstance().getExtraHttpHeader();
        }
        if (header != null && header.size() > 0) {
            super.loadUrl(url, header);
        } else {
            super.loadUrl(url);
        }
    }

    @Override
    public void on(JSONObject jsonObject) throws JSONException {
        HeroView.on(this, jsonObject);
        if (jsonObject.has("url")) {
            Object urlObject = jsonObject.get("url");
            if (urlObject instanceof JSONObject) {
                method = ((JSONObject) urlObject).optString("method");
                mUrl = ((JSONObject) urlObject).getString("url");
                postData = ((JSONObject) urlObject).optString("data");
                // if post data not null but method is not specified, think it as POST
                if (!TextUtils.isEmpty(postData) && TextUtils.isEmpty(method)) {
                    method = METHOD_POST;
                }
                if (METHOD_POST.equals(method)) {
                    byte data[] = TextUtils.isEmpty(postData) ? new byte[1] : postData.getBytes();
                    this.postUrl(mUrl, data);
                } else {
                    this.loadUrl(mUrl);
                }
            } else {
                mUrl = jsonObject.getString("url");
                this.loadUrl(mUrl);
            }
        }
        if (jsonObject.has("innerHtml")) {
            this.loadData(jsonObject.getString("innerHtml"), "text/html;charset=UTF-8", null);
        }
        if (jsonObject.has("hijackURLs")) {
            hijackUrlArray = jsonObject.optJSONArray("hijackURLs");
        }
    }

    @Override
    public void reload() {
        if (!TextUtils.isEmpty(postData) && METHOD_POST.equals(method) && !TextUtils.isEmpty(mUrl)) {
            this.postUrl(mUrl, postData.getBytes());
            return;
        }
        super.reload();
    }

    // if it's in the hijack list, return the object, else return null
    private JSONObject shouldHijackUrl(String url) throws JSONException {
        if (hijackUrlArray == null || hijackUrlArray.length() == 0) {
            return null;
        }
        for (int i = 0; i < hijackUrlArray.length(); i ++) {
            JSONObject item = hijackUrlArray.getJSONObject(i);
            if (TextUtils.equals(url, item.optString("url"))) {
                return item;
            }
        }
        return null;
    }

    public void setFragment(HeroFragment fragment) {
        parentFragment = fragment;
    }

    private class MyWebViewDownLoadListener implements DownloadListener {

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                                    long contentLength) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        }

    }

    private boolean isUrlAuthenticated(String url) {
        if (NEED_VERIFY_URL_HOST) {
            Context application = getContext().getApplicationContext();
            if (application instanceof HeroApplication) {
                String urlDomain = HeroApplication.getDomainAddress(url);
                String host = ((HeroApplication)application).getHomeAddress();
                String hostDomain = HeroApplication.getDomainAddress(host);
                if (urlDomain.equals(hostDomain)) {
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    private void setWindowAttribute() {
        Context context = getContext();
        int scrHeightDp = HeroView.px2dip(context, HeroView.getScreenHeight(context));
        int scrWidthDp = HeroView.px2dip(context, HeroView.getScreenWidth(context));
        String script = String.format("window.deviceWidth=%d;window.deviceHeight=%d",scrWidthDp,scrHeightDp);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                this.evaluateJavascript(script, null);
            } catch (IllegalStateException e) {
                this.loadUrl("javascript:" + script);
            }
        } else {
            this.loadUrl("javascript:" + script);
        }
    }
}
