package wf.com;

import android.app.Application;
import android.content.Context;

public class AgencyContext extends Application {
        private static Context context;

        public void onCreate() {
                AgencyContext.context = getApplicationContext();
        }

        public static Context getAppContext() {
                return context;
        }

}