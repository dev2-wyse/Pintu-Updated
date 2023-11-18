package cis.pintu;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {

    DevicePolicyManager mDevicePolicyManager;
    private ComponentName mAdminComponentName;


    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | WindowManager.LayoutParams.FLAG_SECURE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY  | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

    @Override
    protected void onCreate(Bundle savd){
            super.onCreate(savd);
            try {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(flags);
                decorView.requestLayout();

            }catch (Exception er){}
            setContentView(R.layout.activity_main);


        try {

                if (android.os.Build.VERSION.SDK_INT >= 23) {
                            //get Device Policy Manager
                            mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                            // get admin component
                            mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
                            if (mDevicePolicyManager.isDeviceOwnerApp(this.getPackageName())) {
                                setDefaultCosuPolicies(true);
                            }


                            // Start lock task mode if its not already active
                            if (mDevicePolicyManager.isLockTaskPermitted(this.getPackageName()) || mDevicePolicyManager.isDeviceOwnerApp(this.getPackageName())) {
                                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                                if (am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE) {
                                    if (Build.VERSION.SDK_INT>=28) {
                                        PintuManager.getInstance().getDpm().setLockTaskFeatures(PintuManager.getInstance().getAdmin(),
                                                DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO | DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS);
                                    }
                                    //startLockTask();
                                }
                            } else {
                                Toast.makeText(this, "Unable to lock", Toast.LENGTH_SHORT).show();
                            }
                }

        }catch (Exception er){
            //Log.d("MainActivity",er.getMessage());
            Toast.makeText(this,er.getMessage(),Toast.LENGTH_SHORT).show();
            //er.printStackTrace();
        }

          startDefaultApp();

    }

    public void releaseLock(View view){
                    //stopLockTask();
                    startDefaultApp();
    }

    public void startDefaultApp(){
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("wyse.android.biosentry");
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(launchIntent);//null pointer check in case package name was not found
            finish();
        }
    }

    public void stopTaskMode(View view){
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT>=23) {
            if (am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_LOCKED || am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_PINNED) {
                setDefaultCosuPolicies(false);
                stopLockTask();
            }
        }
    }

    public void startSetting(View view){
        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
    }

    private void setDefaultCosuPolicies(boolean active){

        // Set user restrictions
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
        //setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active);
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(UserManager.DISALLOW_UNINSTALL_APPS, active);

        //setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);
        // Disable keyguard and status bar

        if (Build.VERSION.SDK_INT>=23) {
            mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);
            mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, active);
        }

        getPackageManager().setComponentEnabledSetting(new ComponentName(getApplicationContext(), MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        // Enable STAY_ON_WHILE_PLUGGED_IN
        enableStayOnWhilePluggedIn(active);
        // Set system update policy
        if (active){
            //set when to update device or apps
            if (Build.VERSION.SDK_INT>=23) {
                mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName, SystemUpdatePolicy.createWindowedInstallPolicy(0, 0));
            }
        } else {
            if (Build.VERSION.SDK_INT>=23) {
                mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName, null);
            }
        }

              // set this Activity as a lock task package
                mDevicePolicyManager.setLockTaskPackages(mAdminComponentName, new String[]{"wyse.android.visimaster","wyse.android.biosentry"});

                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
                intentFilter.addCategory(Intent.CATEGORY_HOME);
                intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
                intentFilter.addCategory(Intent.CATEGORY_LAUNCHER);

        if (active) {
            // set  activity as home intent receiver so that it is started // on reboot
            mDevicePolicyManager.addPersistentPreferredActivity(mAdminComponentName, intentFilter, new ComponentName(getPackageName(), MainActivity.class.getName()));
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(mAdminComponentName, getPackageName());
        }

    }

    private void setUserRestriction(String restriction, boolean disallow){
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction);
        }
    }

    private void enableStayOnWhilePluggedIn(boolean enabled){
            if (enabled) {
                mDevicePolicyManager.setGlobalSetting( mAdminComponentName, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, Integer.toString(BatteryManager.BATTERY_PLUGGED_AC  |  BatteryManager.BATTERY_PLUGGED_USB  |  BatteryManager.BATTERY_PLUGGED_WIRELESS));
            } else {
                mDevicePolicyManager.setGlobalSetting(mAdminComponentName, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "0");
            }
    }


    @Override
    public void onBackPressed(){
        //DO NOTHING
    }

}
