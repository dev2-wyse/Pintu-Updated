package cis.pintu;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by cis on 22/04/18.
 */
public class PintuReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
         Helper.sendSignal(intent.getAction(),context);
         if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)){
             PintuManager.getInstance().setCharging(true);
             PintuManager.getInstance().setLast_charging_connected(System.currentTimeMillis());
         }
         if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)){

             PintuManager.getInstance().setLast_charging_disconnected(System.currentTimeMillis());
             PintuManager.getInstance().setCharging(false);
         }

         if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
             PintuManager.getInstance().setLastReboot(System.currentTimeMillis());
         }

        if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)){
            SharedPreferences sharedPreferences = context.getSharedPreferences(Helper.PREF_FILE_NAME, Context.MODE_PRIVATE);
            if (sharedPreferences!=null)
            Helper.setSharedPref(sharedPreferences,"last_shutdown",System.currentTimeMillis()+"");
        }

        if (intent.getAction().equals("ACTION_INSTALL_COMPLETE")){
             PintuManager.getInstance().launchApp(context,intent.getStringExtra("appname"));
         }

        if (intent.getAction().equals(Constants.CMD_REBOOT)){
            try {
                if (intent.getStringExtra("access").equals(PintuManager.getInstance().appInstalled(context.getPackageManager()))) {
                    if (PintuManager.getInstance().getDpm() == null) {
                        PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(context));
                        PintuManager.getInstance().setDpm((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
                    }
                    boolean status = PintuManager.getInstance().refreshDevice(1);
                    //result = new JSONObject().put("status", "success").put("status",status);
                }
            }catch (Exception er){ }
        }else if (intent.getAction().equals(Constants.CMD_INSTALL)){
            PintuManager.getInstance().startDownload(context,intent.getStringExtra("url"),
                    "tempapp.apk",intent.getStringExtra("appName"));
        }

        if (intent.getAction().equals(Constants.CMD_TASK_LOCK)){
            try {
                if (intent.getStringExtra("access")!=null) {
                    Intent start = context.getPackageManager().getLaunchIntentForPackage(context.getApplicationContext().getPackageName());
                    start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(start);
                }
            }catch (Exception er){}
        }

    }

}
