package wf.utils;

import wf.com.AgencyService;
import wf.com.R;

import wf.com.AgencyContext;

import wf.db.Profile;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class Utils {

	public static final String SERVICE_NAME = "org.sshtunnel.SSHTunnelService";
	public static final String TAG = "AgencyUtils";

	public static String getProfileName(Profile profile) {
		if (profile.getName() == null || profile.getName().equals("")) {
			return AgencyContext.getAppContext().getString(R.string.profile_base) + " "
					+ profile.getId();
		}
		return profile.getName();
	}

	public static boolean isWorked() {
		// TODO Auto-generated method stub
		return AgencyService.isServiceStarted();
	}
	
   
	public static Drawable getAppIcon(Context c, int uid) {
        PackageManager pm = c.getPackageManager();
        Drawable appIcon = c.getResources().getDrawable(R.drawable.sym_def_app_icon);
        String[] packages = pm.getPackagesForUid(uid);

        if (packages != null) {
            if (packages.length == 1) {
                try {
                    ApplicationInfo appInfo = pm.getApplicationInfo(packages[0], 0);
                    appIcon = pm.getApplicationIcon(appInfo);
                } catch (NameNotFoundException e) {
                	Log.e(TAG, "No package found matching with the uid " + uid);
                }
            }
        } else {
            Log.e(TAG, "Package not found for uid " + uid);
        }

        return appIcon;
    }
}