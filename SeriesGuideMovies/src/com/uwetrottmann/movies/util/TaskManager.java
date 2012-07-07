/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.uwetrottmann.movies.util;

import com.uwetrottmann.movies.R;

import android.content.Context;
import android.widget.Toast;

public class TaskManager {

    private static TaskManager _instance;

    private MoviesUpdateTask mUpdateTask;

    private Context mContext;

    private TaskManager(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized TaskManager getInstance(Context context) {
        if (_instance == null) {
            _instance = new TaskManager(context.getApplicationContext());
        }
        return _instance;
    }

    public synchronized void tryUpdateTask(MoviesUpdateTask task) {
        if (!isUpdateTaskRunning(true)) {
            mUpdateTask = task;
            task.execute();
        }
    }

    public synchronized void onTaskCompleted() {
        mUpdateTask = null;
    }

    public boolean isUpdateTaskRunning(boolean displayWarning) {
        if (mUpdateTask != null) {
            if (displayWarning) {
                Toast.makeText(mContext, R.string.update_inprogress, Toast.LENGTH_LONG).show();
            }
            return true;
        } else {
            return false;
        }
    }

}
