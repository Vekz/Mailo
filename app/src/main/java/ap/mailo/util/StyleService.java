package ap.mailo.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import ap.mailo.R;

public class StyleService {
    public static void setPreferedStyle(Context context)
    {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String themeResource_str = mSharedPreferences.getString(context.getString(R.string.pref_theme), "Theme.Mailo");
        int resId = context.getResources().getIdentifier(themeResource_str, "style", context.getPackageName());
        context.setTheme(resId);
    }
}
