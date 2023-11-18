package cis.pintu;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PintuLocker extends Activity {

    private PackageManager mPm;
    private ArrayList<AppModel> Appitems;
    private ListView app_list;
    private AppListAdapter appListAdapter;
    private ArrayList<String> whiteApps;
    private Button activatebtn;

    private TextView textinfo;
    private SharedPreferences sharedPreferences;
    private CountDownTimer activtyTimer;

    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          Helper.sendSignal(intent.getAction(),context);
        }
    };

    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | WindowManager.LayoutParams.FLAG_SECURE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(flags);
            decorView.requestLayout();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception er){}

        setContentView(R.layout.pintu_main);

        whiteApps=new ArrayList<>();
        whiteApps.add("wyse.android.visimaster");
        whiteApps.add("wyse.android.biosentry");
        whiteApps.add("wyse.cis.weaponman");
        whiteApps.add("wyse.cis.visi");
        whiteApps.add("wyse.cis.biosentryx");
        whiteApps.add("android.wyse.face");

        //checkForKioskMode();

        app_list=  findViewById(R.id.app_list);
        textinfo=  findViewById(R.id.textinfo);
        activatebtn=findViewById(R.id.activatebtn);
        activatebtn.setVisibility(View.INVISIBLE);
        TextView version=findViewById(R.id.version);
        version.setText(BuildConfig.VERSION_NAME+"("+BuildConfig.BUILD_TYPE+")"+" "+BuildConfig.BUILD_TIME);

            if (sharedPreferences==null) {
                sharedPreferences = getSharedPreferences(Helper.PREF_FILE_NAME, Context.MODE_PRIVATE);
            }

            if (sharedPreferences.getString("isActivated","").equals("false") || sharedPreferences.getString("isActivated","").equals("")){
                activatebtn.setVisibility(View.VISIBLE);
                textinfo.setText("Not activated, tap button to activate");
            }else{
                textinfo.setText("Activated");
            }

            activatebtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setFadeAnimation(view);
                    //Intent sr = new Intent("android.intent.action.dwin_reboot"); sendBroadcast(sr);
                    //sendBroadcast(sr);
                        activateKiosk();
                }
            });

            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(broadcastReceiver,intentFilter);


            PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(this));
            PintuManager.getInstance().setDpm((DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE));
            enableAdminApp();


            if (PintuManager.getInstance().getDpm().isDeviceOwnerApp(getPackageName())) {
                //PintuManager.getInstance().getDpm().clearDeviceOwnerApp(getPackageName());
            }


            if ( ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT>=23) {
                    makeRequest(4444);
            }

            getAllApps();

            if (Appitems.size()>0) {
                textinfo.setText("");
                textinfo.setVisibility(View.INVISIBLE);
                appListAdapter = new AppListAdapter(PintuLocker.this, Appitems);
                app_list.setAdapter(appListAdapter);
                appListAdapter.notifyDataSetChanged();
            }else {
                textinfo.setText("No apps found to display !");
            }
        activateKiosk();

        checkForKioskMode();

    }

    private void checkForKioskMode(){
        if (getIntent()!=null){
            if (getIntent().getAction().equals(Constants.CMD_REBOOT)){
                if (whiteApps.contains(getIntent().getStringExtra("access"))){
                    if (PintuManager.getInstance().getDpm() == null) {
                        PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(this.getApplicationContext()));
                        PintuManager.getInstance().setDpm((DevicePolicyManager) getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE));
                    }
                    PintuManager.getInstance().refreshDevice(1);
                    return;
                }
            }else if (getIntent().getAction().equals(Constants.CMD_TASK_LOCK)){
                try {
                    Log.d("pintu_logs","into "+Constants.CMD_TASK_LOCK);
                    if (getIntent().getStringExtra("access")!=null) {
                        if (Build.VERSION.SDK_INT >= 24) {
                            if (PintuManager.getInstance().getDpm() == null) {
                                PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(this.getApplicationContext()));
                                PintuManager.getInstance().setDpm((DevicePolicyManager) getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE));
                            }

                            if (getIntent().getStringExtra("action") != null && getIntent().getStringExtra("action").equals("stop_task")) {
                                PintuManager.getInstance().getDpm().setLockTaskPackages(PintuManager.getInstance().getAdmin(), new String[]{});
                                stopLockTask();
                                Toast.makeText(getApplicationContext(), "Kiosk mode disabled !", Toast.LENGTH_SHORT).show();
                            } else {
                                startKioskMode();
                            }
                        }
                    }else{
                        Log.d("pintu_logs","no access");
                    }
                }catch (Exception er){
                    //er.printStackTrace();
                    //finish();
                }
            }else{
                Log.d("pintu_logs","no action received");
            }
        }
    }


    private void startKioskMode(){
       // Log.d("pintu_logs","startKioskMode");
        String[] apps = new String[]{"wyse.android.visimaster",
                "wyse.android.biosentry",
                "wyse.cis.weaponman", "wyse.cis.visi",
                "wyse.cis.biosentryx", "android.wyse.face",
                "cis.pintu", Settings.ACTION_SETTINGS,
                Settings.ACTION_WIRELESS_SETTINGS,
                "android.Settings"};

        PintuManager.getInstance().getDpm().setLockTaskPackages(PintuManager.getInstance().getAdmin(), apps);

        if (Build.VERSION.SDK_INT >= 28) {
            //this feature is available in android 8+
            PintuManager.getInstance().getDpm().setLockTaskFeatures(PintuManager.getInstance().getAdmin(),
                    DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO | DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS);
        }

        startLockTask();

        PintuManager.getInstance().setLockTask(isLockTaskMode());
        Toast.makeText(getApplicationContext(), "Kiosk mode started !", Toast.LENGTH_SHORT).show();

        String appname = getIntent().getStringExtra("appname");
        //Log.d("Pintu",appname+"");
        if (appname != null) {
            if (Build.VERSION.SDK_INT >= 28) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLockTaskEnabled(true);
                //Toast.makeText(getApplicationContext(),appname,Toast.LENGTH_SHORT).show();
                Intent intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(appname);
                if (intent != null) {
                    startActivity(intent, options.toBundle());
                }
            } else {
                Intent intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(appname);
                if (intent != null) {
                    startActivity(intent);
                }
            }
        }
    }

    private boolean isLockTaskMode(){
        Log.d("pintu_logs","into is locktaskmode");
        ActivityManager activityManager = (ActivityManager)
                this.getSystemService(Context.ACTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For SDK version 23 and above.
            return activityManager.getLockTaskModeState()
                    != ActivityManager.LOCK_TASK_MODE_NONE;
        }else{
            // When SDK version >= 21. This API is deprecated in 23.
            return activityManager.isInLockTaskMode();
        }

    }

    public void setFadeAnimation(View view) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(300);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(300);
        fadeOut.setDuration(300);

        view.startAnimation(fadeOut);
        view.startAnimation(fadeIn);
    }


    private void activateKiosk(){
        try {
            if (sharedPreferences != null) {
                Helper.setSharedPref(sharedPreferences, "macid", Helper.getMacAddr());
                Helper.setSharedPref(sharedPreferences, "uuid", Helper.getUUID(getApplicationContext()));
                Helper.setSharedPref(sharedPreferences, "isActivated", "false");

                JSONObject data = new JSONObject();
                data.put("uuid", Helper.getUUID(getApplicationContext()));
                data.put("token", FirebaseInstanceId.getInstance().getToken());
                data.put("macid", Helper.getMacAddr());

                //Log.d("Tag","play id"+FirebaseInstanceId.getInstance().getToken());
                //Log.d("Tag",data.toString());

                new NR(Helper.baseurl + "token.php", 8000, "POST", data.toString(), getApplicationContext()).networkRequest(new ResultCallback() {
                    @Override
                    public void onSuccess(String result) {
                        if (result != null) {
                            try {
                                JSONObject data = new JSONObject(result);
                                if (data.getString("result").equals("success") && data.getString("type").equals("token")) {
                                    Helper.setSharedPref(sharedPreferences, "isActivated", "true");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            activatebtn.setVisibility(View.INVISIBLE);
                                            textinfo.setText("Activated");
                                        }
                                    });
                                }
                            } catch (Exception er) {
                            }
                        }
                    }

                    @Override
                    public void onError(String result) {
                    }
                });
            }
        }catch(Exception er){
        }
    }

    public void onVersionTap(View view){
        setFadeAnimation(view);
        //stopLockTask();
    }

    private void enableAdminApp() {
        if (!PintuManager.getInstance().getDpm().isAdminActive(PintuManager.getInstance().getAdmin())) {
            Intent activateDeviceAdmin = new Intent(
                    DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdmin.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    PintuManager.getInstance().getAdmin());
            activateDeviceAdmin
                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "After activating admin, you will be able to block application uninstallation.");
            startActivityForResult(activateDeviceAdmin,
                    4444);
        }
    }

    protected void makeRequest(int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.READ_SMS,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_CONTACTS,Manifest.permission.RECORD_AUDIO}, requestCode);
    }

    private void getAllApps(){
        mPm=this.getPackageManager();
        List<ApplicationInfo> apps = mPm.getInstalledApplications(0);

        if (apps == null) {
            apps = new ArrayList<ApplicationInfo>();
        }

        final Context context = getApplicationContext();
        // create corresponding apps and load their labels
        Appitems = new ArrayList<AppModel>();
        //Appitems.clear();
        for (int i = 0; i < apps.size(); i++) {
            String pkg = apps.get(i).packageName;
            // only apps which are launchable
            //Log.d("TAG",pkg);
           for (int a=0;a<whiteApps.size();a++){
               if (whiteApps.get(a).equals(pkg)) {
                  // Log.d("TAG",pkg +", "+whiteApps.size());
                   AppModel app = new AppModel(context, apps.get(i));
                   app.loadLabel(context);
                   Appitems.add(app);
               }
            }
        }
    }

    public void startDefaultApp(String appName){
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(appName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent); //null pointer check in case package name was not found
            //finish();
        }
    }

    public void onRefreshList(View view){
        getAllApps();
        if (Appitems.size()>0) {
            textinfo.setText("");
            textinfo.setVisibility(View.INVISIBLE);
            appListAdapter = new AppListAdapter(PintuLocker.this, Appitems);
            //app_list.setAdapter(appListAdapter);
            appListAdapter.notifyDataSetChanged();
        }else {
            textinfo.setText("No apps found to display !");
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        getAllApps();
        appListAdapter = new AppListAdapter(PintuLocker.this, Appitems);
        appListAdapter.notifyDataSetChanged();

        if (activtyTimer!=null){
            activtyTimer.cancel();
        }
        activtyTimer=new CountDownTimer(30000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (Appitems.size()>0) {
                    //Log.d("auto_start","app called from timer");
                    Intent intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(
                            Appitems.get(0).getApplicationPackageName());
                    if (intent != null) {
                        startKioskMode();
                        startActivity(intent);
                    }
                }
            }
        };
        activtyTimer.start();

    }

    @Override
    public void onStart(){
        super.onStart();
        /*try {
            if (sharedPreferences==null)
            sharedPreferences  = getSharedPreferences(Helper.PREF_FILE_NAME, Context.MODE_PRIVATE);

            String default_app=Helper.getSharedPref(sharedPreferences,"default_app");
            if (!default_app.equals("")){
               // startDefaultApp(default_app);
            }else{
                Helper.setSharedPref(sharedPreferences,"default_app","");
            }
        }catch (Exception er){
            // er.printStackTrace();
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activtyTimer!=null){
            activtyTimer.cancel();
        }
    }

    private void setDefaultCosuPolicies(boolean active){

        // Set user restrictions
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
     //   setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active);
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(UserManager.DISALLOW_UNINSTALL_APPS, active);
        setUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS, active);
        setUserRestriction(UserManager.DISALLOW_SMS, active);
        //setUserRestriction(UserManager.DISALLOW_SET_WALLPAPER, active);

        //setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);
        // Disable keyguard and status bar
        if (Build.VERSION.SDK_INT>=23) {
            PintuManager.getInstance().getDpm().setKeyguardDisabled(PintuManager.getInstance().getAdmin(), active);
            PintuManager.getInstance().getDpm().setStatusBarDisabled(PintuManager.getInstance().getAdmin(), active);
        }

        PintuManager.getInstance().getDpm().setScreenCaptureDisabled(PintuManager.getInstance().getAdmin(),active);
        //mDevicePolicyManager.setDeviceOwnerLockScreenInfo(mAdminComponentName,"");
        PintuManager.getInstance().getDpm().setCameraDisabled(PintuManager.getInstance().getAdmin(),true);
        PintuManager.getInstance().getDpm().setAutoTimeRequired(PintuManager.getInstance().getAdmin(),true);


        getPackageManager().setComponentEnabledSetting(new ComponentName(getApplicationContext(), PintuLocker.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        // Enable STAY_ON_WHILE_PLUGGED_IN
        enableStayOnWhilePluggedIn(active);
        // Set system update policy

        if (Build.VERSION.SDK_INT>=23) {
            if (active) {
                //set when to update device or apps
                PintuManager.getInstance().getDpm().setSystemUpdatePolicy(PintuManager.getInstance().getAdmin(), SystemUpdatePolicy.createWindowedInstallPolicy(0, 0));
            } else {
                PintuManager.getInstance().getDpm().setSystemUpdatePolicy(PintuManager.getInstance().getAdmin(), null);
            }
        }

        // set this Activity as a lock task package
        //mDevicePolicyManager.setLockTaskPackages(mAdminComponentName, new String[]{"wyse.android.visimaster","wyse.android.biosentry","wyse.cis.weaponman"});

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addCategory(Intent.CATEGORY_LAUNCHER);

        if (active) {
            // set  activity as home intent receiver so that it is started // on reboot
            PintuManager.getInstance().getDpm().addPersistentPreferredActivity(PintuManager.getInstance().getAdmin(), intentFilter, new ComponentName(getPackageName(), PintuLocker.class.getName()));
        } else {
            PintuManager.getInstance().getDpm().clearPackagePersistentPreferredActivities(PintuManager.getInstance().getAdmin(), getPackageName());
        }

    }

    private void setUserRestriction(String restriction, boolean disallow){
        if (disallow) {
            PintuManager.getInstance().getDpm().addUserRestriction(PintuManager.getInstance().getAdmin(), restriction);
        } else {
            PintuManager.getInstance().getDpm().clearUserRestriction(PintuManager.getInstance().getAdmin(), restriction);
        }
    }

    public void startSetting(View view){
        //stopLockTask();
        startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
    }

    private void enableStayOnWhilePluggedIn(boolean enabled){
        if (enabled) {
            PintuManager.getInstance().getDpm().setGlobalSetting( PintuManager.getInstance().getAdmin(), Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Integer.toString(BatteryManager.BATTERY_PLUGGED_AC  |
                            BatteryManager.BATTERY_PLUGGED_USB  |
                            BatteryManager.BATTERY_PLUGGED_WIRELESS));
        } else {
            PintuManager.getInstance().getDpm().setGlobalSetting(PintuManager.getInstance().getAdmin(), Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "0");
        }
    }

    @Override
    public void onBackPressed(){
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        try {
            if (broadcastReceiver != null) {
                unregisterReceiver(broadcastReceiver);
            }
        }catch (Exception er){}
    }

}
