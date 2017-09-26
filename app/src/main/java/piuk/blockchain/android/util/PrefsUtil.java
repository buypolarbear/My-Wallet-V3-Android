package piuk.blockchain.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PrefsUtil implements PersistentPrefs {

    private SharedPreferences preferenceManager;

    public PrefsUtil(Context context) {
        preferenceManager = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public String getValue(String name, String defaultValue) {
        return preferenceManager.getString(name,
                (defaultValue == null || defaultValue.isEmpty()) ? "" : defaultValue);
    }

    @Override
    public void setValue(String name, String value) {
        Editor editor = preferenceManager.edit();
        editor.putString(name, (value == null || value.isEmpty()) ? "" : value);
        editor.apply();
    }

    @Override
    public int getValue(String name, int defaultValue) {
        return preferenceManager.getInt(name, defaultValue);
    }

    @Override
    public void setValue(String name, int value) {
        Editor editor = preferenceManager.edit();
        editor.putInt(name, (value < 0) ? 0 : value);
        editor.apply();
    }

    @Override
    public void setValue(String name, long value) {
        Editor editor = preferenceManager.edit();
        editor.putLong(name, (value < 0L) ? 0L : value);
        editor.apply();
    }

    @Override
    public long getValue(String name, long defaultValue) {

        long result;
        try {
            result = preferenceManager.getLong(name, defaultValue);
        } catch (Exception e) {
            result = (long) preferenceManager.getInt(name, (int) defaultValue);
        }

        return result;
    }

    @Override
    public boolean getValue(String name, boolean defaultValue) {
        return preferenceManager.getBoolean(name, defaultValue);
    }

    @Override
    public void setValue(String name, boolean value) {
        Editor editor = preferenceManager.edit();
        editor.putBoolean(name, value);
        editor.apply();
    }

    @Override
    public boolean has(String name) {
        return preferenceManager.contains(name);
    }

    @Override
    public void removeValue(String name) {
        Editor editor = preferenceManager.edit();
        editor.remove(name);
        editor.apply();
    }

    @Override
    public void clear() {
        Editor editor = preferenceManager.edit();
        editor.clear();
        editor.apply();
    }
}
