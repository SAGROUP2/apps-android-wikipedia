package org.wikipedia.settings;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;

import org.wikipedia.R;

abstract class PreferenceLoaderFragment extends PreferenceFragmentCompat
        implements PreferenceLoader {
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        loadPreferences();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the default white background in the view so as to avoid transparency
        view.setBackgroundColor(
                ContextCompat.getColor(getContext(), R.color.background_material_light));

    }
}
