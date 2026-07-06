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

package org.peterbaldwin.vlcremote.util;

import android.app.Activity;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.OnApplyWindowInsetsListener;

/**
 * Handles the edge-to-edge display that Android forces on apps targeting
 * API 35+. The app uses the framework Holo theme (not AppCompat/Material),
 * so nothing pads for the system bars automatically; without this the action
 * bar and bottom controls would render underneath the status and navigation
 * bars.
 */
public final class WindowInsetsUtil {

    private WindowInsetsUtil() {
    }

    /**
     * Pads the activity's content view by the system-bar and display-cutout
     * insets so no content is drawn under the status or navigation bars.
     * Applied to {@code android.R.id.content} so it works regardless of the
     * activity's specific layout.
     */
    public static void applySystemBarInsets(Activity activity) {
        final View content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(content, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets bars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars()
                                | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            }
        });
        ViewCompat.requestApplyInsets(content);
    }
}
