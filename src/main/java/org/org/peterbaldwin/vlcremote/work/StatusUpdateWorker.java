/*-
 *  Copyright (C) 2026
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.work;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.peterbaldwin.client.android.vlcremote.MediaAppWidgetProvider;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Status;
import org.peterbaldwin.vlcremote.net.MediaServer;

/**
 * Performs VLC status refreshes and the pause-for-call action off the main
 * thread without starting {@link org.peterbaldwin.vlcremote.service.StatusService}.
 * <p>
 * On modern Android a {@code Service} cannot be started from a background
 * context (widget update, alarm callback, phone-state broadcast). Those
 * triggers now enqueue this worker instead. The interactive path (while the
 * activity is in the foreground) still uses the service.
 */
public class StatusUpdateWorker extends Worker {

    /** Work type: refresh the widget (and notification) with current status. */
    public static final String ACTION_REFRESH = "refresh";
    /** Work type: pause playback for an incoming/outgoing call. */
    public static final String ACTION_PAUSE_FOR_CALL = "pause_for_call";
    /** Work type: resume playback after a call ends, if we paused it. */
    public static final String ACTION_RESUME_AFTER_CALL = "resume_after_call";

    static final String KEY_ACTION = "action";

    public StatusUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String action = getInputData().getString(KEY_ACTION);
        Preferences pref = Preferences.get(context);
        String authority = pref.getAuthority();
        if (authority == null) {
            return Result.success();
        }
        MediaServer server = new MediaServer(context, authority);
        try {
            if (ACTION_PAUSE_FOR_CALL.equals(action)) {
                Status status = server.status().read();
                if (status.isPlaying()) {
                    server.status().readCommand("command=pl_pause");
                    pref.setResumeOnIdle();
                }
            } else if (ACTION_RESUME_AFTER_CALL.equals(action)) {
                if (pref.isResumeOnIdleSet()) {
                    Status status = server.status().read();
                    if (status.isPaused()) {
                        server.status().readCommand("command=pl_pause");
                    }
                    pref.clearResumeOnIdle();
                }
            } else {
                // ACTION_REFRESH (default): update the widget and notification.
                Status status = server.status().read();
                MediaAppWidgetProvider.scheduleUpdate(context, status);
                android.graphics.Bitmap art;
                try {
                    art = server.art().read();
                } catch (Exception e) {
                    art = null;
                }
                if (MediaAppWidgetProvider.getWidgetIds(context).length != 0) {
                    if (art != null) {
                        MediaAppWidgetProvider.update(context, status, art);
                    }
                }
                if (art != null && pref.isNotificationSet()) {
                    org.peterbaldwin.vlcremote.widget.NotificationControls.show(context, status, art);
                }
            }
            return Result.success();
        } catch (Throwable tr) {
            if (ACTION_REFRESH.equals(action) || action == null) {
                MediaAppWidgetProvider.cancelPendingUpdate(context);
                if (MediaAppWidgetProvider.getWidgetIds(context).length != 0) {
                    MediaAppWidgetProvider.update(context, tr);
                }
            }
            // Transient network errors are common on a LAN remote; don't retry
            // aggressively — the next trigger will refresh again.
            return Result.success();
        }
    }

    /**
     * Enqueues an expedited one-shot job for the given action.
     */
    public static void enqueue(Context context, String action) {
        androidx.work.Data data = new androidx.work.Data.Builder()
                .putString(KEY_ACTION, action)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(StatusUpdateWorker.class)
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
        androidx.work.WorkManager.getInstance(context).enqueue(request);
    }
}
