/*
 * Copyright (C) 2019 AquariOS
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
 * Allow settings fragments to use ActionPreference and associated
 * action binding UI/UX to bind actions to things beyond SmartNav.
 * As such, we have to manually provide some information that is provided
 * to SmartNav in the superclass because we don't use a defaults mapping
 * configuration
 */

package com.android.settings.smartnav;

import java.util.HashMap;
import java.util.Map;

import com.android.internal.utils.ActionHandler;
import com.android.internal.utils.Config;
import com.android.internal.utils.ActionConstants.Defaults;
import com.android.internal.utils.Config.ActionConfig;
import com.android.internal.utils.Config.ButtonConfig;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

public abstract class SimpleActionFragment extends ActionFragment {
    // Map the ActionPreference keys to their info
    protected Map<String, ActionPreferenceInfo> mInfoMap = new HashMap<String, ActionPreferenceInfo>();

    public static class ActionPreferenceInfo {
        public static String TYPE_SYSTEM = "system";
        public static String TYPE_SECURE = "secure";
        public static String TYPE_GLOBAL = "global";

        public final String type;
        public final String defaultAction;
        public final String settingsUri;
        public ActionConfig defaultConfig;
        public ActionConfig currentConfig;

        public ActionPreferenceInfo(Context ctx, String type, String defaultAction,
                String settingsUri) {
            this.type = type;
            this.defaultAction = defaultAction;
            this.settingsUri = settingsUri;
            defaultConfig = new ActionConfig(ctx, defaultAction);
            String configString = getConfigStringFromProvider(ctx);
            if (configString == null) {
                currentConfig = defaultConfig;
            } else {
                currentConfig = new ActionConfig(ctx);
                currentConfig.fromDelimitedString(configString);
            }
        }

        private String getConfigStringFromProvider(Context ctx) {
            if (type.equals(TYPE_SYSTEM)) {
                String config = Settings.System.getStringForUser(ctx.getContentResolver(),
                        settingsUri, UserHandle.USER_CURRENT);
                return config;
            } else if (type.equals(TYPE_SECURE)) {
                String config = Settings.Secure.getStringForUser(ctx.getContentResolver(),
                        settingsUri, UserHandle.USER_CURRENT);
                return config;
            } else if (type.equals(TYPE_GLOBAL)) {
                String config = Settings.Global.getStringForUser(ctx.getContentResolver(),
                        settingsUri, UserHandle.USER_CURRENT);
                return config;
            } else {
                return null;
            }
        }

        public void commitConfigToProvider(Context ctx, ActionConfig newConfig) {
            currentConfig = newConfig;
            String toCommit = newConfig.toDelimitedString();
            if (type.equals(TYPE_SYSTEM)) {
                Settings.System.putStringForUser(ctx.getContentResolver(),
                        settingsUri, toCommit, UserHandle.USER_CURRENT);
            } else if (type.equals(TYPE_SECURE)) {
                Settings.Secure.putStringForUser(ctx.getContentResolver(),
                        settingsUri, toCommit, UserHandle.USER_CURRENT);
            } else if (type.equals(TYPE_GLOBAL)) {
                Settings.Global.putStringForUser(ctx.getContentResolver(),
                        settingsUri, toCommit, UserHandle.USER_CURRENT);
            }
        }
    }

    // subclass must provide this information about ActionPreferences
    protected abstract ActionPreferenceInfo getActionPreferenceInfoForKey(String key);

    @Override
    public void onStart() {
        super.onStart();
        loadAndSetConfigs();
        onActionPolicyEnforced(mPrefHolder);
    }

    @Override
    protected void loadAndSetConfigs() {
        for (ActionPreference pref : mPrefHolder) {
            String key = pref.getKey();
            ActionPreferenceInfo info = getActionPreferenceInfoForKey(key);
            mInfoMap.put(key, info);
            pref.setActionConfig(info.currentConfig);
            pref.setDefaultActionConfig(info.defaultConfig);
        }
    }

    @Override
    protected void findAndUpdatePreference(ActionConfig action, String tag) {
        for (ActionPreference pref : mPrefHolder) {
            if (pref.getTag().equals(mHolderTag)) {
                if (action == null) {
                    action = pref.getDefaultActionConfig();
                }
                pref.setActionConfig(action);
                ActionPreferenceInfo info = mInfoMap.get(mHolderTag);
                info.commitConfigToProvider(getActivity(), action);
                mInfoMap.replace(mHolderTag, info);
                onActionPolicyEnforced(mPrefHolder);
                break;
            }
        }
    }
}
