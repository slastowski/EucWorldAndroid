package net.lastowski.eucworld;

import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;

import net.lastowski.common.Value;
import net.lastowski.eucworld.utils.Constants;
import net.lastowski.eucworld.utils.SettingsUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import timber.log.Timber;

public class EucworldJSInterface {

    private Context context;

    EucworldJSInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public String jQuery() { return readAssetsContent(context,"jquery-3.4.1.min.js"); }

    @JavascriptInterface
    public String getResourceString(String name) {
        int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
        return (id > 0) ? context.getString(id) : "";
    }

    @JavascriptInterface
    public String getAppVersionName() { return BuildConfig.VERSION_NAME; }

    @JavascriptInterface
    public int getAppVersionCode() { return BuildConfig.VERSION_CODE; }

    @JavascriptInterface
    public String getAppId() { return Constants.appId(context); }

    @JavascriptInterface
    public String getApiKey() { return LivemapService.getApiKey(); }

    @JavascriptInterface
    public String getLocaleId() { return Locale.getDefault().toString(); }

    @JavascriptInterface
    public boolean useF() { return SettingsUtil.isUseF(context); }

    @JavascriptInterface
    public boolean useFt() { return SettingsUtil.isUseFt(context); }

    @JavascriptInterface
    public boolean useMi() { return SettingsUtil.isUseMi(context); }

    @JavascriptInterface
    public float toF(float v) { return Value.toFahrenheit(v); }

    @JavascriptInterface
    public float toFt(float v) { return Value.toFeet(v); }

    @JavascriptInterface
    public float toMi(float v) { return Value.toMiles(v); }

    @JavascriptInterface
    public void reload() { context.sendBroadcast(new Intent(Constants.ACTION_TOUR_RELOAD_WEBVIEW)); }

    @JavascriptInterface
    public void setTourHeaderAlpha(int v) {
        Intent intent = new Intent(Constants.ACTION_SET_TOUR_HEADER_VISIBILITY);
        intent.putExtra("v", v);
        context.sendBroadcast(intent);
    }

    @JavascriptInterface
    public int getTourStatus() { return LivemapService.getStatus(); }

    @JavascriptInterface
    public boolean getTourGpsSignalReceived() { return LivemapService.getLivemapGPS(); }

    @JavascriptInterface
    public double getTourDistance() { return LivemapService.getDistance(); }

    @JavascriptInterface
    public int getTourDuration() { return LivemapService.getRideTime(); }

    @JavascriptInterface
    public int getTourDurationRiding() { return LivemapService.getRidingTime(); }

    @JavascriptInterface
    public double getTourSpeed() { return LivemapService.getSpeed(); }

    @JavascriptInterface
    public double getTourSpeedAvg() { return LivemapService.getAverageSpeed(); }

    @JavascriptInterface
    public double getTourSpeedAvgRiding() { return LivemapService.getAverageRidingSpeed(); }

    @JavascriptInterface
    public double getTourSpeedMax() { return LivemapService.getTopSpeed(); }

    private String readAssetsContent(Context context, String name) {
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = context.getAssets().open(name);
            in = new BufferedReader(new InputStreamReader(is));
            String str;
            boolean isFirst = true;
            while ( (str = in.readLine()) != null ) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        }
        catch (IOException e) {
            Timber.e("Error opening asset %s", name);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                    Timber.e("Error closing asset %s", name);
                }
            }
        }
        return null;
    }
}
