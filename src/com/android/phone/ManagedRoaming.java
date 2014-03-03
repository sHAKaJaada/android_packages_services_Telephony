/*
 * Copyright (c) 2013 The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.MSimTelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;

public class ManagedRoaming extends Activity {
    private static final String LOG_TAG = "ManagedRoaming";

    private int mSubscription = 0;
    private boolean mIsMRDialogShown = false;

    // Key used to read and write the saved network selection numeric value
    private static final String NETWORK_SELECTION_KEY = "network_selection_key";
    private final int NETWORK_SCAN_ACTIVITY_REQUEST_CODE = 0;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = getIntent();
        int subscription = intent.getIntExtra(SUBSCRIPTION_KEY, PhoneGlobals
                .getInstance().getDefaultSubscription());
        createManagedRoamingDialog(subscription);
    }

    /*
     * Show Managed Roaming dialog if user preferred Network Selection mode is 'Manual'
     */
    private void createManagedRoamingDialog(int subscription) {
        Resources r = Resources.getSystem();
        String networkSelection = PreferenceManager.getDefaultSharedPreferences(ManagedRoaming.this)
                .getString(NETWORK_SELECTION_KEY, "");
        log(" Received Managed Roaming intent, networkSelection "
                + networkSelection + " Is Dialog Displayed " + mIsMRDialogShown
                + " sub = " + subscription);
        // networkSelection will be empty for 'Automatic' mode.
        if (!TextUtils.isEmpty(networkSelection) && !mIsMRDialogShown) {
            MSimTelephonyManager tm = MSimTelephonyManager.getDefault();
            int[] titleResource = { R.string.managed_roaming_title_sub1,
                    R.string.managed_roaming_title_sub2,
                    R.string.managed_roaming_title_sub3 };
            int title = R.string.managed_roaming_title;

            mSubscription = subscription;

            if (tm.isMultiSimEnabled() && (tm.getPhoneCount() > mSubscription)) {
                title = titleResource[mSubscription];
            }

            AlertDialog managedRoamingDialog = new AlertDialog.Builder(ManagedRoaming.this)
                    .setTitle(title)
                    .setMessage(R.string.managed_roaming_dialog_content)
                    .setPositiveButton(R.string.managed_roaming_dialog_ok_button,
                        onManagedRoamingDialogClick)
                    .setNegativeButton(R.string.managed_roaming_dialog_cancel_button,
                        onManagedRoamingDialogClick)
                    .create();

            managedRoamingDialog.setOnKeyListener(mManagedRoamingDialogOnKeyListener);
            mIsMRDialogShown = true;
            managedRoamingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            managedRoamingDialog.show();
        } else {
            finish();
        }
    }

    DialogInterface.OnClickListener onManagedRoamingDialogClick =
        new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    log("Launch network settings activity sub = " + mSubscription);
                    Intent networkSettingIntent = new Intent(Intent.ACTION_MAIN);
                    networkSettingIntent.setClassName("com.android.phone",
                            "com.android.phone.NetworkSetting");
                    networkSettingIntent.putExtra(SUBSCRIPTION_KEY, mSubscription);
                    startActivityForResult(networkSettingIntent,
                            NETWORK_SCAN_ACTIVITY_REQUEST_CODE);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    finish();
                    break;
                default:
                    Log.w(LOG_TAG, "received unknown button type: "+ which);
                    finish();
                    break;
            }
            mIsMRDialogShown = false;
        }
    };

    DialogInterface.OnKeyListener mManagedRoamingDialogOnKeyListener =
        new DialogInterface.OnKeyListener() {
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            // Handle the back key to reset the global variable.
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                log(" presed back key, reset local state");
                mIsMRDialogShown = false;
                dialog.dismiss();
                finish();
            }
            return false;
        }
    };

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        log("On activity result ");
        finish();
    }
}
