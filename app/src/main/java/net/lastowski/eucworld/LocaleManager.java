package net.lastowski.eucworld;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import net.lastowski.eucworld.utils.SettingsUtil;
import java.util.Locale;

public class LocaleManager {

    public LocaleManager(Context context) { }

    public static Context setLocale(Context c) {
        String lang = SettingsUtil.getLang(c);
        if (!lang.equals("default")) {
            Locale locale = new Locale(lang);
            Configuration config = new Configuration(c.getResources().getConfiguration());
            Locale.setDefault(locale);
            config.setLocale(locale);
            return c.createConfigurationContext(config);
        }
        else
            return c;
    }

    public static Locale getLocale(Resources res) {
        Configuration config = res.getConfiguration();
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? config.getLocales().get(0) : config.locale;
    }

}
