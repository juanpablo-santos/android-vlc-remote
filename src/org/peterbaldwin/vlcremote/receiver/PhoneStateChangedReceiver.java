/*-
 *  Copyright (C) 2010 Peter Baldwin   
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

package org.peterbaldwin.vlcremote.receiver;

import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.work.StatusUpdateWorker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/**
 * Automatically pauses media when there is an incoming call.
 * <p>
 * {@code onReceive} runs on the main thread from a background context, so the
 * actual VLC request is handed off to {@link StatusUpdateWorker} rather than
 * performed inline or via a (now-restricted) background service start.
 */
public class PhoneStateChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String authority = Preferences.get(context).getAuthority();
            if (authority != null) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)
                        || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                    // Pause for both incoming and outgoing calls
                    StatusUpdateWorker.enqueue(context, StatusUpdateWorker.ACTION_PAUSE_FOR_CALL);
                } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                    StatusUpdateWorker.enqueue(context, StatusUpdateWorker.ACTION_RESUME_AFTER_CALL);
                }
            }
        }
    }
}
