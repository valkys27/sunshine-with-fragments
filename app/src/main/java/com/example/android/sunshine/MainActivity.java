/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;

import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.SunshineDateUtils;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements
        ListFragment.OnItemChangeListener {

    private static final String BACK_STACK_ROOT_TAG = "root_fragment";
    private static final String SCREENS_IN_THE_STAG_TAG = "screens";

    private final String TAG = MainActivity.class.getSimpleName();

    private int screensInTheStack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);
        getSupportActionBar().setElevation(0f);
        screensInTheStack = (savedInstanceState == null) ?
                0 : savedInstanceState.getInt(SCREENS_IN_THE_STAG_TAG, 0);
        Uri uri = getIntent().getData();
        if (uri == null)
            replaceFragment(new ListFragment());
        else
            replaceFragment(DetailFragment.newInstance(uri));
        getIntent().setData(null);
    }

    @Override
    public void onBackPressed() {
        if (screensInTheStack > 0) {
            screensInTheStack--;
            super.onBackPressed();
        }
        else if (getSupportFragmentManager().findFragmentByTag(ListFragment.TAG) == null)
            replaceFragment(new ListFragment());
        else
            finish();
    }

    private void replaceFragment(Fragment fragment) {
        int orientation = getResources().getConfiguration().orientation;
        int smallestScreenWidthDp = getResources().getConfiguration().smallestScreenWidthDp;
        FragmentManager fm = getSupportFragmentManager();
        DetailFragment oldDetailFragment = (DetailFragment) fm.findFragmentByTag(DetailFragment.TAG);
        FragmentTransaction transaction = fm.beginTransaction();
        if (fragment instanceof ListFragment) {
            if (smallestScreenWidthDp >= 600 && orientation == Configuration.ORIENTATION_LANDSCAPE
                    && oldDetailFragment == null) {
                long date = SunshineDateUtils.normalizeDate(System.currentTimeMillis());
                DetailFragment detailFragment = DetailFragment
                        .newInstance(WeatherContract.WeatherEntry.buildWeatherUriWithDate(date));
                transaction.replace(R.id.detail_fragment, detailFragment, DetailFragment.TAG);
                getIntent().setData(detailFragment.getUri());
            }
            if (screensInTheStack > 0) {
                screensInTheStack--;
                fm.popBackStack(BACK_STACK_ROOT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            else {
                transaction.replace(R.id.main_fragment, fragment, ListFragment.TAG);
                if (oldDetailFragment != null || fm.findFragmentByTag(ListFragment.TAG) == null)
                    transaction.addToBackStack(BACK_STACK_ROOT_TAG);
            }
        } else if (orientation != Configuration.ORIENTATION_LANDSCAPE || smallestScreenWidthDp < 600) {
            transaction.replace(R.id.main_fragment, fragment, DetailFragment.TAG);
            if (screensInTheStack == 0) {
                screensInTheStack++;
                transaction.addToBackStack(null);
            }
        } else  {
            transaction.replace(R.id.detail_fragment, fragment, DetailFragment.TAG);
            if (oldDetailFragment == null) {
                screensInTheStack++;
                transaction.addToBackStack(null);
            }
        }
        transaction.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        DetailFragment detailFragment;
        if ((detailFragment = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DetailFragment.TAG)) != null)
            getIntent().setData(detailFragment.getUri());
        outState.putInt(SCREENS_IN_THE_STAG_TAG, screensInTheStack);
        super.onSaveInstanceState(outState);
    }

    /**
     * This is where we inflate and set up the menu for this Activity.
     *
     * @param menu The options menu in which you place your items.
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.forecast, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    /**
     * Callback invoked when a menu item was selected from this Activity's menu.
     *
     * @param item The menu item that was selected by the user
     *
     * @return true if you handle the menu click here, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Uses the URI scheme for showing a location found on a map in conjunction with
     * an implicit Intent. This super-handy Intent is detailed in the "Common Intents" page of
     * Android's developer site:
     *
     * @see "http://developer.android.com/guide/components/intents-common.html#Maps"
     * <p>
     * Protip: Hold Command on Mac or Control on Windows and click that link to automagically
     * open the Common Intents page
     */
    private void openPreferredLocationInMap() {
        double[] coords = SunshinePreferences.getLocationCoordinates(this);
        String posLat = Double.toString(coords[0]);
        String posLong = Double.toString(coords[1]);
        Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
        }
    }

    @Override
    public void onItemChange(Uri uri) {
        int orientation = getResources().getConfiguration().orientation;
        int smallestScreenWidthDp = getResources().getConfiguration().smallestScreenWidthDp;
        ListFragment.OnItemChangeListener onItemChangeListener =
                (ListFragment.OnItemChangeListener) getSupportFragmentManager().findFragmentByTag(DetailFragment.TAG);
        if (onItemChangeListener != null && orientation == Configuration.ORIENTATION_LANDSCAPE &&
                smallestScreenWidthDp >= 600) {
            onItemChangeListener.onItemChange(uri);
            return;
        }
        replaceFragment(DetailFragment.newInstance(uri));
    }
}
