/* Copyright 2018 Urban Airship and Contributors */

package com.urbanairship.iam;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.urbanairship.Logger;
import com.urbanairship.UAirship;

/**
 * Helper class that keeps track of the schedule's adapter, assets, and execution callback.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
final class AdapterWrapper {
    public final String scheduleId;
    public final InAppMessage message;
    public volatile boolean isReady;
    public volatile boolean skipDisplay;
    public InAppMessageAdapter adapter;
    public boolean displayed = false;

    private boolean prepareCalled = false;

    AdapterWrapper(@NonNull String scheduleId, @NonNull InAppMessage message, @NonNull InAppMessageAdapter adapter) {
        this.scheduleId = scheduleId;
        this.message = message;
        this.adapter = adapter;
    }

    @InAppMessageAdapter.PrepareResult
    int prepare() {
        if (!AudienceChecks.checkAudience(UAirship.getApplicationContext(), message.getAudience())) {
            skipDisplay = true;
            Logger.debug("InAppMessageManager - Message audience conditions not met, skipping schedule: " + scheduleId);
            return InAppMessageAdapter.OK;
        }

        try {
            Logger.debug("InAppMessageManager - Preparing schedule: " + scheduleId);

            @InAppMessageAdapter.PrepareResult
            int result = adapter.onPrepare(UAirship.getApplicationContext());
            prepareCalled = true;

            if (result == InAppMessageAdapter.OK) {
                isReady = true;
            }
            return result;
        } catch (Exception e) {
            Logger.error("InAppMessageManager - Failed to prepare in-app message.", e);
            return InAppMessageAdapter.RETRY;
        }
    }

    boolean display(Activity activity) {
        Logger.debug("InAppMessageManager - Displaying schedule: " + scheduleId);
        try {
            DisplayHandler displayHandler = new DisplayHandler(scheduleId);
            if (adapter.onDisplay(activity, displayed, displayHandler)) {
                displayed = true;
                return true;
            }

            return false;
        } catch (Exception e) {
            Logger.error("InAppMessageManager - Failed to display in-app message.", e);
            return false;
        }
    }

    void finish() {
        Logger.debug("InAppMessageManager - Schedule finished: " + scheduleId);

        try {
            if (prepareCalled) {
                adapter.onFinish();
            }
        } catch (Exception e) {
            Logger.error("InAppMessageManager - Exception during onFinish().", e);
        }
    }
}
