/*
 * Copyright (C) 2014 Sam Malone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.peterbaldwin.vlcremote.widget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.widget.RemoteViews;
import org.peterbaldwin.client.android.vlcremote.PlaybackActivity;
import org.peterbaldwin.client.android.vlcremote.R;
import org.peterbaldwin.vlcremote.model.Preferences;
import org.peterbaldwin.vlcremote.model.Status;

/**
 *
 * @author Sam Malone
 */
public class NotificationControls {

    /**
     * Notification channel for the ongoing playback-control notification.
     * Required on API 26+ or {@code notify()} is silently dropped.
     */
    public static final String CHANNEL_ID = "playback_controls";

    public static void cancel(Context context) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(ID);
    }

    private static final int ID = 1;

    /**
     * Creates the playback-controls notification channel. Safe to call
     * repeatedly (creating an existing channel is a no-op) and a no-op below
     * API 26 where channels do not exist.
     */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.service_label),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
    }

    public static void showLoading(Context context) {
        show(context, null);
    }
    
    public static void show(Context context, Status status) {
        show(context, status, BitmapFactory.decodeResource(context.getResources(), R.drawable.albumart_mp_unknown));
    }
    
    public static void show(Context context, Status status, Bitmap art) {
        RemoteViewsFactory views = new RemoteViewsFactory(context);
        show(context, views.getNotifiation(status, art), views.getNotifiationExpanded(status, art));
    }       
    
    public static void show(Context context, RemoteViews normal, RemoteViews expanded) {
        createChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, PlaybackActivity.class);

        // The stack builder will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(PlaybackActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(resultPendingIntent);
        
        boolean isTransparent = Preferences.get(context).isNotificationIconTransparent();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = builder.setContent(normal)
                                .setWhen(0)
                                .setOngoing(true)
                                .setSmallIcon(isTransparent ? R.drawable.ic_transparent : R.drawable.ic_vlc_server)
                                .build();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            n.bigContentView = expanded;
        }
        notificationManager.notify(ID, n);
    }
    
    public static void showError(Context context, Throwable tr) {
        RemoteViewsFactory views = new RemoteViewsFactory(context);
        show(context, views.getNotification(tr), views.getNotificationExpanded(tr));
    }
    
    
}
