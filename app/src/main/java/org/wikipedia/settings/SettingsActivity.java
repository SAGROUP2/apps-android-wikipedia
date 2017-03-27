package org.wikipedia.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;

public class SettingsActivity extends SingleFragmentActivity<SettingsFragment>
        implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    public static final int ACTIVITY_REQUEST_SHOW_SETTINGS = 1;
    public static final int ACTIVITY_RESULT_LANGUAGE_CHANGED = 1;

    public static Intent newIntent(@NonNull Context ctx) {
        return new Intent(ctx, SettingsActivity.class);
    }

    @Override
    public SettingsFragment createFragment() {
        return SettingsFragment.newInstance();
    }

    @Override
    protected void setTheme() {
        setActionBarTheme();
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat,
                                           PreferenceScreen preferenceScreen) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);
        ft.add(R.id.fragment_container, fragment, preferenceScreen.getKey());
        ft.addToBackStack(preferenceScreen.getKey());
        ft.commit();
        return true;
    }
}
