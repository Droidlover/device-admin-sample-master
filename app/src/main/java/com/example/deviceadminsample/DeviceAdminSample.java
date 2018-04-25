/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.deviceadminsample;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * This activity provides a comprehensive UI for exploring and operating the DevicePolicyManager
 * api.  It consists of two primary modules:
 *
 * 1:  A device policy controller, implemented here as a series of preference fragments.  Each
 *     one contains code to monitor and control a particular subset of device policies.
 *
 * 2:  A DeviceAdminReceiver, to receive updates from the DevicePolicyManager when certain aspects
 *     of the device security status have changed.
 */
public class DeviceAdminSample extends PreferenceActivity {

    // Miscellaneous utilities and definitions
    private static final String TAG = "DeviceAdminSample";

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;




    // The following keys are used to find each preference item
    private static final String KEY_ENABLE_ADMIN = "key_enable_admin";

    private static final String KEY_SET_PASSWORD = "key_set_password";
    private static final String KEY_RESET_PASSWORD = "key_reset_password";


    private static final String KEY_CATEGORY_LOCK_WIPE = "key_category_lock_wipe";

    private static final String KEY_LOCK_SCREEN = "key_lock_screen";

    // Interaction with the DevicePolicyManager
    DevicePolicyManager mDPM;
    ComponentName mDeviceAdminSample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prepare to work with the DPM
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDeviceAdminSample = new ComponentName(this, DeviceAdminSampleReceiver.class);
       // mDPM.lockNow();
    }

    /**
     * We override this method to provide PreferenceActivity with the top-level preference headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.device_admin_headers, target);
    }

    /**
     * Helper to determine if we are an active admin
     */
    private boolean isActiveAdmin() {
        return mDPM.isAdminActive(mDeviceAdminSample);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return GeneralFragment.class.getName().equals(fragmentName)
                || LockWipeFragment.class.getName().equals(fragmentName);
    }

    /**
     * Common fragment code for DevicePolicyManager access.  Provides two shared elements:
     *
     *   1.  Provides instance variables to access activity/context, DevicePolicyManager, etc.
     *   2.  Provides support for the "set password" button(s) shared by multiple fragments.
     */
    public static class AdminSampleFragment extends PreferenceFragment
            implements OnPreferenceChangeListener, OnPreferenceClickListener{

        // Useful instance variables
        protected DeviceAdminSample mActivity;
        protected DevicePolicyManager mDPM;
        protected ComponentName mDeviceAdminSample;
        protected boolean mAdminActive;

        // Optional shared UI


        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Retrieve the useful instance variables
            mActivity = (DeviceAdminSample) getActivity();
            mDPM = mActivity.mDPM;
            mDeviceAdminSample = mActivity.mDeviceAdminSample;
            mAdminActive = mActivity.isActiveAdmin();

        }

        @Override
        public void onResume() {
            super.onResume();
            mAdminActive = mActivity.isActiveAdmin();
            //reloadSummaries();
            // Resetting the password via API is available only to active admins

        }

        /**
         * Called automatically at every onResume.  Should also call explicitly any time a
         * policy changes that may affect other policy values.
         */


//        protected void postReloadSummaries() {
//            getView().post(new Runnable() {
//                @Override
//                public void run() {
//                    reloadSummaries();
//                }
//            });
//        }

        @Override
        public boolean onPreferenceClick(Preference preference) {

            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            return false;
        }



    }

    /**
     * PreferenceFragment for "general" preferences.
     */
    public static class GeneralFragment extends AdminSampleFragment
            implements OnPreferenceChangeListener {
        // UI elements
        private CheckBoxPreference mEnableCheckbox;



        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.device_admin_general);
            mEnableCheckbox = (CheckBoxPreference) findPreference(KEY_ENABLE_ADMIN);
            mEnableCheckbox.setOnPreferenceChangeListener(this);



        }

        // At onResume time, reload UI with current values as required
        @Override
        public void onResume() {
            super.onResume();
            mEnableCheckbox.setChecked(mAdminActive);

        }



        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (super.onPreferenceChange(preference, newValue)) {
                return true;
            }
            if (preference == mEnableCheckbox) {
                boolean value = (Boolean) newValue;
                if (value != mAdminActive) {
                    if (value) {
                        // Launch the activity to have the user enable our admin.
                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdminSample);
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                mActivity.getString(R.string.add_admin_extra_app_text));
                        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
                        // return false - don't update checkbox until we're really active
                        return false;
                    } else {
                        mDPM.removeActiveAdmin(mDeviceAdminSample);
                        mAdminActive = false;
                    }
                }
            }
            return true;
        }

//        private void postUpdateDpmDisableFeatures() {
//            getView().post(new Runnable() {
//                @Override
//                public void run() {
//                    mDPM.setKeyguardDisabledFeatures(mDeviceAdminSample,
//                            createKeyguardDisabledFlag());
//                    String component = mTrustAgentComponent.getText();
//                    if (component != null) {
//                        ComponentName agent = ComponentName.unflattenFromString(component);
//                        if (agent != null) {
//                            String featureString = mTrustAgentFeatures.getText();
//                            if (featureString != null) {
//                                PersistableBundle bundle = new PersistableBundle();
//                                bundle.putStringArray("features", featureString.split(","));
//                                mDPM.setTrustAgentConfiguration(mDeviceAdminSample, agent, bundle);
//                            }
//                        } else {
//                            Log.w(TAG, "Invalid component: " + component);
//                        }
//                    }
//                }
//            });
//        }


        /** Updates the device capabilities area (dis/enabling) as the admin is (de)activated */

    }










    /**
     * PreferenceFragment for "lock screen & wipe" preferences.
     */
    public static class LockWipeFragment extends AdminSampleFragment
            implements OnPreferenceChangeListener, OnPreferenceClickListener {
        private PreferenceCategory mLockWipeCategory;

        private PreferenceScreen mLockScreen;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.device_admin_lock_wipe);

            mLockWipeCategory = (PreferenceCategory) findPreference(KEY_CATEGORY_LOCK_WIPE);

            mLockScreen = (PreferenceScreen) findPreference(KEY_LOCK_SCREEN);


            mLockScreen.setOnPreferenceClickListener(this);

        }

        @Override
        public void onResume() {
            super.onResume();
            mLockWipeCategory.setEnabled(mAdminActive);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (super.onPreferenceClick(preference)) {
                return true;
            }
            if (preference == mLockScreen) {

                mDPM.lockNow();
                return true;
            }
            return false;
        }


        /**
         * Sample implementation of a DeviceAdminReceiver.  Your controller must provide one,
         * although you may or may not implement all of the methods shown here.
         * <p>
         * All callbacks are on the UI thread and your implementations should not engage in any
         * blocking operations, including disk I/O.
         */}
        public static class DeviceAdminSampleReceiver extends DeviceAdminReceiver {
            void showToast(Context context, String msg) {
                String status = context.getString(R.string.admin_receiver_status, msg);
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == ACTION_DEVICE_ADMIN_DISABLE_REQUESTED) {
                    abortBroadcast();
                }
                super.onReceive(context, intent);
            }

            @Override
            public void onEnabled(Context context, Intent intent) {
                showToast(context, context.getString(R.string.admin_receiver_status_enabled));
            }

            @Override
            public CharSequence onDisableRequested(Context context, Intent intent) {
                return context.getString(R.string.admin_receiver_status_disable_warning);
            }

            @Override
            public void onDisabled(Context context, Intent intent) {
                showToast(context, context.getString(R.string.admin_receiver_status_disabled));
            }

            @Override
            public void onPasswordChanged(Context context, Intent intent) {
                showToast(context, context.getString(R.string.admin_receiver_status_pw_changed));
            }

            @Override
            public void onPasswordFailed(Context context, Intent intent) {
                showToast(context, context.getString(R.string.admin_receiver_status_pw_failed));
            }

            @Override
            public void onPasswordSucceeded(Context context, Intent intent) {
                showToast(context, context.getString(R.string.admin_receiver_status_pw_succeeded));
            }

            @Override
            public void onPasswordExpiring(Context context, Intent intent) {
                DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                        Context.DEVICE_POLICY_SERVICE);
                long expr = dpm.getPasswordExpiration(
                        new ComponentName(context, DeviceAdminSampleReceiver.class));
                long delta = expr - System.currentTimeMillis();
                boolean expired = delta < 0L;
                String message = context.getString(expired ?
                        R.string.expiration_status_past : R.string.expiration_status_future);
                showToast(context, message);
                Log.v(TAG, message);
            }
        }

    }