package net.lastowski.eucworld;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import timber.log.Timber;


public class Eucworld extends Application {

    public LocaleManager localeManager;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            if (android.os.Debug.isDebuggerConnected())
                Timber.plant(new FileLoggingTree(getApplicationContext()));
            Timber.plant(new Timber.DebugTree());
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        localeManager = new LocaleManager(base);
        super.attachBaseContext(LocaleManager.setLocale(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleManager.setLocale(this);
    }

}
