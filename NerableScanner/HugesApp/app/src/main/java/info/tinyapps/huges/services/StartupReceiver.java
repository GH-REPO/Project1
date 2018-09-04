package info.tinyapps.huges.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import info.tinyapps.huges.utils.AppSettings;
import info.tinyapps.huges.utils.BaseLogging;

public class StartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AppSettings settings = new AppSettings(context);
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            String beaconID = settings.getBeaconID();

            BaseLogging.addLog("checking if service needs to start");
            if(beaconID == null) {
                BaseLogging.addLog("beaconID is null");
                return;
            }
            if(settings.isMuted()) {
                BaseLogging.addLog("service muted");
                return;
            }
            if(settings.getUserName() == null) {
                BaseLogging.addLog("no user");
                return;
            }

            //we have all conditions to start the service
            BaseLogging.addLog("we have all conditions to start the service");
            context.startService(new Intent(context, ScanService.class));
        }
    }
}

