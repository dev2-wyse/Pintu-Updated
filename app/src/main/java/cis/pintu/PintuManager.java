package cis.pintu;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.HardwarePropertiesManager;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class PintuManager  {

  private static PintuManager pintuManager;
  private boolean isCharging;
  private long last_charging_connected;
  private long last_charging_disconnected;
  private long lastReboot;

  private PintuManager(){ }
  private DevicePolicyManager dpm;
  private ComponentName admin;
  private boolean isLockTask;

    private int gpioPort=121;



  public void setAdmin(ComponentName admin) {
    this.admin = admin;
  }

  public ComponentName getAdmin() {
    return admin;
  }

  public void setDpm(DevicePolicyManager dpm) {
    this.dpm = dpm;
  }

  public DevicePolicyManager getDpm() {
    return dpm;
  }

  public static PintuManager getInstance(){
      if (pintuManager==null){
          return pintuManager=new PintuManager();
      }else{
        return pintuManager;
      }
  }

    public void setLockTask(boolean lockTask) {
        isLockTask = lockTask;
    }

    public boolean isLockTask() {
        return isLockTask;
    }

    public void setLastReboot(long lastReboot) {
        this.lastReboot = lastReboot;
    }

    public long getLastReboot() {
        return lastReboot;
    }

    public void setCharging(boolean charging) {
        isCharging = charging;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public void setLast_charging_disconnected(long last_charging_disconnected) {
        this.last_charging_disconnected = last_charging_disconnected;
    }

    public void setLast_charging_connected(long last_charging_connected) {
        this.last_charging_connected = last_charging_connected;
    }

    public long getLast_charging_connected() {
        return last_charging_connected;
    }

    public long getLast_charging_disconnected() {
        return last_charging_disconnected;
    }

    public  String encodeImage(byte[] imageByteArray) {
        return Base64.encodeToString(imageByteArray, Base64.DEFAULT);
    }

    public void launchApp(Context context, String appname){
      Intent intent = context.getPackageManager().getLaunchIntentForPackage(appname);
      if(intent!=null) {
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
          context.startActivity(intent);
      }
    }

      private void installApp(File file,String appname,Context context){
          try {
              file.setReadable(true, false);
              if (isRooted()) {
                  bgUpdate(file);
                  //context.sendBroadcast(new Intent("ACTION_INSTALL_COMPLETE").putExtra("appname",appname));
              } else {
                  if(Build.VERSION.SDK_INT>=21) {
                      installPackage(context, new FileInputStream(file), appname);
                  }
              }

          }catch (Exception er){
             // er.printStackTrace();
          }
      }

    public boolean  installPackage(Context context, InputStream in, String packageName)
            throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(packageName);
        // set params
        int sessionId = packageInstaller.createSession(params);
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        OutputStream out = session.openWrite("COSU", 0, -1);
        byte[] buffer = new byte[65536];
        int c;
        while ((c = in.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
        session.fsync(out);
        in.close();
        out.close();

        session.commit(createIntentSender(context, sessionId,packageName));
        return true;
    }

    private  IntentSender createIntentSender(Context context, int sessionId,String packageName) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent("ACTION_INSTALL_COMPLETE").putExtra("appname",packageName),
                0);
        return pendingIntent.getIntentSender();
    }

    public boolean bgUpdate(File file){
        try {
            String command;
            command = "su 0 pm install -r " + file.getAbsolutePath();
            Log.d("COMMAND:",command);
            Process proc = Runtime.getRuntime().exec(command);
            if (proc.waitFor()==0){
                return true;
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return false;
    }

    public boolean uninstallApp(String pkgname,Context context){
        try {
            if (isRooted()) {
                String command;
                command = "pm uninstall  " + pkgname;
                //Log.d("COMMAND:",command);
                Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                proc.waitFor();
                return true;
            }else{
                Intent intent = new Intent(context, context.getClass());
                PendingIntent sender = PendingIntent.getActivity(context, 0, intent, 0);
                PackageInstaller mPackageInstaller = context.getPackageManager().getPackageInstaller();
                mPackageInstaller.uninstall(pkgname, sender.getIntentSender());
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRooted() {
        return findBinary("su");
    }

    private  boolean findBinary(String binaryName) {
        boolean found = false;
        if (!found) {
            String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/","/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
            for (String where : places) {
                if ( new File( where + binaryName ).exists() ) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    public void downloadFile(final Context context,final String endPoint,final File saveLocation){
        try {
            setupNotification(context);
            URL url = new URL(endPoint);
            URLConnection conection = url.openConnection();
            conection.connect();
            // getting file length
            int lenghtOfFile = conection.getContentLength();
            String ctype=conection.getContentType();
            // input stream to read file - with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);
            // Output stream to write file

            FileOutputStream output = new FileOutputStream(saveLocation);
            byte data[] = new byte[1024];
            int count = 0;
            int total=0;
            while ((count = input.read(data)) != -1) {
                total += count;
                // writing data to file
                output.write(data, 0, count);
                int p =  (int) ((total * 100) / lenghtOfFile);
                updateProgress(p,false);
            }

            // flushing output
            output.flush();
            // closing streams
            output.close();
            input.close();

            updateProgress(0,true);

        }catch (Exception er){ }

    }

    public void startDownload(final Context context,final String urlstring,final String fileName,final String appName){
        try {
            setupNotification(context);
            URL url = new URL(urlstring);
            URLConnection conection = url.openConnection();
            conection.connect();
            // getting file length
            int lenghtOfFile = conection.getContentLength();
            String ctype=conection.getContentType();
            // input stream to read file - with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);
            // Output stream to write file

            final File f=SaveToInternal(context,fileName);

            FileOutputStream output = new FileOutputStream(f);
            byte data[] = new byte[1024];
            int count = 0;
            int total=0;
            while ((count = input.read(data)) != -1) {
                total += count;
                // writing data to file
                output.write(data, 0, count);
                int p =  (int) ((total * 100) / lenghtOfFile);
                updateProgress(p,false);
            }

            // flushing output
            output.flush();
            // closing streams
            output.close();
            input.close();

            updateProgress(0,true);
            installApp(f,appName,context);

        }catch (Exception er){ }

    }

    private NotificationManager notificationManager;
    private NotificationCompat.Builder mBuilder;
    private int notificatonId=1235;
    private void setupNotification(Context context){

         notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "pintu_ch";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Pintu Notifications", NotificationManager.IMPORTANCE_HIGH);
            // Configure the notification channel.
            notificationChannel.setDescription("Download notification");
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.RED);
            //notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});
            notificationChannel.enableVibration(false);
            notificationManager.createNotificationChannel(notificationChannel);
            mBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
            mBuilder.setContentTitle("Pintu Download Manager");
            mBuilder.setContentText("Download in progress");
            mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);

        } else {
            mBuilder = new NotificationCompat.Builder(context);
            mBuilder.setContentTitle("Pintu Download Manager");
            mBuilder.setContentText("Download in progress");
            mBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        }

        notificationManager.notify(notificatonId, mBuilder.build());

        //mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
       // mBuilder = new NotificationCompat.Builder(context);
        //mBuilder.setContentTitle("Pintu Download Manager")
          //      .setContentText("Download in progress")
             //   .setSmallIcon(android.R.drawable.stat_sys_download);
    }

    private void updateProgress(final int p,final  boolean isComplete){
        try {
            if (mBuilder != null) {

                if (!isComplete) {
                    mBuilder.setProgress(100, p, false);
                } else {
                    mBuilder.setContentText("Download completed")
                            .setProgress(0, p, false);

                }

                if (notificationManager != null)
                    notificationManager.notify(notificatonId, mBuilder.build());

                if (isComplete) {
                    if (notificationManager != null)
                        notificationManager.cancel(notificatonId);
                }
            }
        }catch (Exception er){}
    }

    private  String internal_dir_name = "pintu_data";
    public  File SaveToInternal(final Context context, final String filename) {
        try {
            File directory = context.getDir(internal_dir_name, Context.MODE_PRIVATE);
            if (directory.exists()) {
                File file = new File(directory, filename);
                if (file.exists()) {
                    file.delete();
                } else {
                    file.createNewFile();
                }
                return file;
            }
        } catch (Exception er) { }
        return new File(filename);
    }

    public boolean disableCamera(boolean status){
        if (Build.VERSION.SDK_INT>=14) {
            //working disable camera
            getDpm().setCameraDisabled(getAdmin(), status);
            return true;
        }
        return false;
    }

    public void setUserRestriction(String restriction, boolean disallow){
        if (disallow) {
            if (getDpm()!=null)
            getDpm().addUserRestriction(getAdmin(), restriction);
        } else {
            if (getDpm()!=null)
            getDpm().clearUserRestriction(getAdmin(), restriction);
        }
    }

    public boolean refreshDevice(int type){
        try {
            switch (type){
                case 0:
                    if (isRooted()) {
                        Runtime.getRuntime().exec("reboot -p");
                        return true;
                    }
                    break;
                case 1:
                    if (isRooted()) {
                        Runtime.getRuntime().exec("reboot");
                        return true;
                    }else{
                        if (Build.VERSION.SDK_INT>=24) {
                            getDpm().reboot(getAdmin());
                            return true;
                        }
                    }
                    break;
                default:
                    break;
            }
        }catch (Exception er){ }
        return false;
    }

    public JSONObject getAllPermission(Context context){
        try {
            PackageManager pm=context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] permissions = info.requestedPermissions;//This array con
            JSONObject allpermissions=new JSONObject();
            for (int p=0;p<permissions.length;p++){
                int hasPerm = pm.checkPermission(permissions[p],
                        context.getPackageName());
                if (hasPerm == PackageManager.PERMISSION_GRANTED) {
                    allpermissions.put(permissions[p],true);
                }else {
                    allpermissions.put(permissions[p],false);
                }
            }
            return allpermissions;
        }catch (Exception er){
            try {
                return new JSONObject().put("status", "error in getting permission list");
            }catch (Exception err){}
        }
        return null;
    }


    public JSONObject getUsbDetails(Context context) {
        try {
            JSONObject data = new JSONObject();
            UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
            if (deviceList.size() == 0) {
                //Log.d("Usb", "No usb found");
                return data.put("usb_info","No usb found");
            }
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();

                String Model = device.getDeviceName();

                int DeviceID = device.getDeviceId();
                int Vendor = device.getVendorId();
                int Product = device.getProductId();
                int Class = device.getDeviceClass();
                int Subclass = device.getDeviceSubclass();
                String serialno=device.getSerialNumber();
                String maf=device.getManufacturerName();
                String pname=device.getProductName();
                String dname=device.getDeviceName();

                String info = Model + ", DeviceId " + DeviceID + ", Vendor:" + Vendor + "," +
                        "Product:" + Product + ",Class: " + Class + ",Subclass: " + Subclass+"," +
                        "Serial No: "+serialno+",Maf:"+maf+",Product Name:"+pname+",Device Name"+dname;
                Log.d("Usb", info);
                data.put(DeviceID + "", info);
            }
            return data;
        }catch (Exception er){}
        return new JSONObject();
    }

    public boolean isNetworkConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm.getActiveNetworkInfo()!=null) {
                return cm.getActiveNetworkInfo().isConnected();
            }
        }catch (Exception conn){ }
        return  false;
    }

    public String appInstalled(PackageManager packageManager){
        ArrayList<String> apps=new ArrayList<>();
        apps.add("wyse.android.visimaster");
        apps.add("wyse.android.biosentry");
        apps.add("wyse.cis.weaponman");
        apps.add("wyse.cis.visi");
        apps.add("wyse.cis.biosentryx");
        apps.add("android.wyse.face");

        for (String appname:apps){
                if(isPackageInstalled(appname,packageManager)){
                        apps.clear();
                        return appname;
                }
        }

        apps.clear();
        return "";
    }

    public String getPackageVersion(Context context,String pname){
        try {
            return context.getPackageManager().getPackageInfo(pname, 0).versionCode+"";
        }catch (Exception er){}
        return "";
    }

    public boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        boolean found = true;
        try {
            packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            found = false;
        }
        return found;
    }

    public String getCpuTemp(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            HardwarePropertiesManager hardwarePropertiesManager = (HardwarePropertiesManager) context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE);
            float[] temp = hardwarePropertiesManager.getDeviceTemperatures(HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU, HardwarePropertiesManager.TEMPERATURE_CURRENT);
            if (temp.length>0)
            return temp[0]+"";
        }
        return "";
    }

    public boolean isGPS(Context context){
        LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        try {
             return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            return lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}
        return false;
    }

    private String locationinfo="";

    public void setLocation(String location) {
        this.locationinfo = location;
    }

    public String getLocation() {
        return locationinfo;
    }

    public String getLocation(Context context)
    {
        // Get the location manager
        if (isGPS(context)) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 23) {
                PintuManager.getInstance().setLocation("");
                return getLocation();
            }
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String bestProvider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(bestProvider);
            try {
                if (location!=null){
                    Double lat = location.getLatitude();
                    Double lon = location.getLongitude();
                    if (lat!=null && lon!=null){
                        PintuManager.getInstance().setLocation(lat+","+lon+","+location.getAltitude()+","+location.getTime());
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
                return getLocation();
            }
        }
        return getLocation();
    }

    public String getAvailableRAM(Context context){
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return  (mi.availMem / 1048576L)+"";
    }

    public boolean isLowMemory(Context context){
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return  mi.lowMemory;
    }

    public void scheduleJob(Context context,Map<String,String> payload) {
        String command=payload.get("command");
        String txid=payload.get("txid");
        JSONObject  result=new JSONObject();
        JSONObject data;
        switch (command){
            case Constants.CMD_GET_PERMISSIONS:
                try{
                    result = PintuManager.getInstance().getAllPermission(context);
                }catch (Exception er){}
                break;
            case Constants.CMD_GET_USB:
                try{
                    result = PintuManager.getInstance().getUsbDetails(context.getApplicationContext()).put("status","success");
                }catch (Exception er){}
                break;
            case Constants.CMD_SECURE_WINDOW:
                try{
                    String extra=payload.get("extra");
                    context.sendBroadcast(new Intent("SECURE_WINDOW").putExtra("pkg",
                            context.getPackageName()).putExtra("isSecure",extra));
                    result = new JSONObject().put("status", "success");
                }catch (Exception er){}
                break;
            case Constants.CMD_REMOVE_OWNER:
                //do not expose this command only for testing
                if (PintuManager.getInstance().getDpm()==null){
                    PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(context));
                    PintuManager.getInstance().setDpm((DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE));
                }
                if (PintuManager.getInstance().getDpm().isDeviceOwnerApp(context.getPackageName())) {
                    PintuManager.getInstance().getDpm().clearDeviceOwnerApp(context.getPackageName());
                }
                break;
            case Constants.CMD_SHUTDOWN:
                try {
                    if (PintuManager.getInstance().getDpm()==null){
                        PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(context));
                        PintuManager.getInstance().setDpm((DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE));
                    }
                    boolean status=PintuManager.getInstance().refreshDevice(0);
                    result = new JSONObject().put("status", status);
                }catch (Exception er){}
                break;
            case Constants.CMD_REBOOT:
                //reboot services
                try {
                    if (PintuManager.getInstance().getDpm()==null){
                        PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(context));
                        PintuManager.getInstance().setDpm((DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE));
                    }
                    boolean status=PintuManager.getInstance().refreshDevice(1);
                    result = new JSONObject().put("status", status);
                }catch (Exception er){ }
                break;
            case Constants.CMD_APP_START:
                //start app
                try {
                    PintuManager.getInstance().launchApp(context.getApplicationContext(),payload.get("appName"));
                    result = new JSONObject().put("status","success").put("extra",payload.get("extra"));
                }catch (Exception er){}
                break;
            case Constants.CMD_BLOCK_ACCESS:
                //block app
                break;
            case Constants.CMD_BLUETOOTH_REBOOT:
                //reboot bluetooth
                BluetoothAdapter mBluetoothAdapter;
                boolean status=false;
                try {
                    final BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager
                    mBluetoothAdapter = bluetoothManager.getAdapter();
                    mBluetoothAdapter.disable();
                    status=mBluetoothAdapter.enable();
                    mBluetoothAdapter = bluetoothManager.getAdapter();
                    result=new JSONObject().put("status",status).put("bluetooth",mBluetoothAdapter.isEnabled());
                }catch (Exception er){}
                break;
            case Constants.CMD_BLUETOOTH_STOP:
                //disable bluetooth
                try {
                    final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager
                    result=new JSONObject().put("status",bluetoothManager.getAdapter().disable());
                }catch (Exception er){}
                break;
            case Constants.CMD_DEFAULT_APP:
                //set default app
                String appname=payload.get("extra");
                if (!appname.equals("")){
                    try {
                        Helper.setSharedPref(context.getSharedPreferences(Helper.PREF_FILE_NAME, Context.MODE_PRIVATE), "default_app", appname);
                        result = new JSONObject().put("status", "success");
                    }catch (Exception er){}
                }
                break;
            case Constants.CMD_CHECK_DATE_TIME:
                //reply date and time
                try {
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat simpledateformat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    result = new JSONObject().put("status", "success").put("date", simpledateformat.format(cal.getTime()));
                }catch (Exception er){}
                break;
            case Constants.HIDE_APP:
                if (PintuManager.getInstance().getDpm()==null){
                    PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(context));
                    PintuManager.getInstance().setDpm((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
                }

                if (PintuManager.getInstance().getDpm()!=null)
                    PintuManager.getInstance().getDpm().setApplicationHidden(PintuManager.getInstance().getAdmin(),payload.get("appName"),true);
                break;
            case Constants.CMD_DISABLE_FACTORY:
                //disable factory reset
                PintuManager.getInstance().setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, true);
                break;
            case Constants.CMD_DISABLE_HOME:
                //disable home button

                break;
            case Constants.CMD_DISABLE_VOL:
                //disable vol
                PintuManager.getInstance().setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, true);
                break;
            case Constants.CMD_DISALLOW_CALL:
                //disable call
                PintuManager.getInstance().setUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS, true);
                break;
            case Constants.CMD_ENABLE_CAMERA:
                //disable camera
                PintuManager.getInstance().disableCamera(payload.get("extra").equals("true"));

                break;
            case Constants.CMD_CAPTURE_PICTURE:
                try {
                    Intent intent = new Intent(context, CameraService.class);
                    intent.putExtra("txid",txid);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startService(intent);
                }catch (Exception er){}
                break;
            case Constants.CMD_GPS_LOCATION:
                try {
                    result = new JSONObject().put("location", PintuManager.getInstance().getLocation(context));
                }catch (Exception er){
                    //er.printStackTrace();
                }
                break;
            case Constants.CMD_ENABLE_GPS:
                //enable gps
                break;
            case Constants.CMD_WIFI_OFF:
                try {
                    result = new JSONObject().put("status", ManageWifi(command,context) + "");
                }catch (Exception er){}
                break;
            case Constants.CMD_DISABLE_WIFI:
                try {
                    result = new JSONObject().put("status", ManageWifi(command,context) + "");
                }catch (Exception er){}
                break;
            case Constants.CMD_ENABLE_WIFI:
                try {
                    result = new JSONObject().put("status", ManageWifi(command,context) + "");
                }catch (Exception er){}
                break;

            case Constants.CMD_RECONNECT_WIFI:
                try {
                    result = new JSONObject().put("status", ManageWifi(command,context) + "");
                }catch (Exception er){}
                break;

            case Constants.CMD_ENABLE_HOME:
                //enable home
                break;

            case Constants.CMD_FLUSH_DATA:
                //flush dir data
                flushLocalFiles(payload);
                break;
            case Constants.CMD_UPLOAD_PUNCHES:
                //upload local punches
                context.sendBroadcast(new Intent("ACTION_UPLOAD_DEVICE_DATA").putExtra("permission",getClass().getSimpleName()).putExtra("type","pending"));
                //sendBroadcast(new Intent(Constants.CMD_UPLOAD_PUNCHES));
                break;
            case Constants.CMD_GET_DEVICE_PUNCHES:
                //get device punches count
                context.sendBroadcast(new Intent(Constants.CMD_GET_DEVICE_PUNCHES));
                break;
            case Constants.CMD_SET_PROXY:
                //set dns proxy

                break;
            case Constants.CMD_INSTALL:
                PintuManager.getInstance().startDownload(context.getApplicationContext(),payload.get("url"),"tempapp.apk",payload.get("appName"));
                try {
                    result = new JSONObject().put("status", "ok");
                }catch (Exception er){}
                break;
            case Constants.CMD_UNINSTALL:
                //disable uninstall
                try {
                    result = new JSONObject().put("status", PintuManager.getInstance().uninstallApp(payload.get("appName"), context.getApplicationContext()) + "");
                }catch (Exception er){}
                break;
            case Constants.CMD_REBOOT_SENSOR:
                //reboot sensor
                try{
                    context.sendBroadcast(new Intent(Constants.CMD_REBOOT_SENSOR));
                }catch (Exception er){}
                break;
            case Constants.DISALLOW_SMS:
                //disallow sms
                PintuManager.getInstance().setUserRestriction(UserManager.DISALLOW_SMS, true);
                break;
            case Constants.CMD_REBOOT_LATCH:
                //reboot access control
                try{
                    context.sendBroadcast(new Intent(Constants.CMD_REBOOT_LATCH));
                }catch (Exception er){}
                break;
            case Constants.CMD_SIMULATE_APP:
                //simulate app function, sensor, latch and reply

                break;
            case Constants.CMD_GET_HEALTH:
                //get device info, memory, apps running, app installed
                data=new JSONObject();
                try{

                    data.put("time",System.currentTimeMillis()+"");
                    data.put("totalapps",getInstalledAppCount(context)+"");
                    data.put("totalservices",getRunningServicesCount(context)+"");
                    data.put("is_default_app_active","");
                    data.put("isDebugMode",PintuManager.getInstance().isUsbDebug(context));
                    data.put("memory",PintuManager.getInstance().getAvailableRAM(context));
                    data.put("isLowMem",PintuManager.getInstance().isLowMemory(context));
                    // data.put("cpu",PintuManager.getInstance().getCpuTemp(this));
                    data.put("wifi",getWifiInfo(context));
                    data.put("networkConnected",isWifi(context));
                    data.put("bluetooth",isBluetooth(context));
                    data.put("network",getNetworkInfo(context));
                    data.put("gps",PintuManager.getInstance().isGPS(context));
                    data.put("location",PintuManager.getInstance().getLocation());
                    data.put("battery",getBatteryinfo(context));
                    data.put("last_charge_connect",PintuManager.getInstance().getLast_charging_connected());
                    data.put("last_charge_disconnect",PintuManager.getInstance().getLast_charging_disconnected());
                    //data.put("finger_sensor",isMyServiceRunning("SensorService"));

                    String appInstalled=PintuManager.getInstance().appInstalled(context.getPackageManager());
                    data.put("current_app",appInstalled);
                    data.put("app_ver",PintuManager.getInstance().getPackageVersion(context.getApplicationContext(),appInstalled));
                    data.put("active_page",nameOfHomeApp(context));
                    data.put("app_permissions",getAllPermission(context));
                    data.put("last_boot",PintuManager.getInstance().getLastReboot());
                    data.put("last_shutdown",Helper.getSharedPref(context.getSharedPreferences(Helper.PREF_FILE_NAME, Context.MODE_PRIVATE),"last_shutdown"));

                }catch (Exception er){}
                result=data;
                break;
            case Constants.CMD_GET_SETTING:
                //get device settings
                break;
            case Constants.CMD_GET_KIOSK_INFO:
                //get device info
                data=new JSONObject();
                try{
                    data.put("pintuver",BuildConfig.VERSION_NAME);
                    data.put("os", Build.VERSION.SDK_INT+"");
                    data.put("maf", Build.MANUFACTURER);
                    data.put("buildtype", BuildConfig.BUILD_TYPE);
                    data.put("model", Build.MODEL);
                    data.put("brand", Build.BRAND);
                    data.put("device", Build.DEVICE);
                    data.put("display", Build.DISPLAY);
                    data.put("serial", Build.SERIAL);
                    data.put("radio", Build.getRadioVersion());
                    data.put("board", Build.BOARD);
                    data.put("lic", Helper.isFilePresent(".lic/face_sdk.lic"));
                    data.put("time",System.currentTimeMillis()+"");
                }catch (Exception er){}
                result=data;
                break;
            case Constants.CMD_LOCK_KIOSK:
                //lock kisok
                if (PintuManager.getInstance().getDpm()==null) {
                    PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(context));
                    PintuManager.getInstance().setDpm((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
                }

                if (PintuManager.getInstance().getDpm()!=null) {
                    PintuManager.getInstance().getDpm().lockNow();
                }
                break;
            case Constants.CMD_TASK_LOCK:
                //Log.d("Pintu",command+" received");
                try{
                    result = new JSONObject().put("status", kioskMode(context));
                }catch (Exception er){}
                break;
            case Constants.CMD_PLAY_SOUND:
                //play some sound
                try{
                    Ringtone ringtone = RingtoneManager.getRingtone(context.getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    ringtone.play();
                    result = new JSONObject().put("status", "success");
                }catch (Exception er){}
                break;
            case Constants.DISALLOW_ADD_USER:
                if (PintuManager.getInstance().getDpm()!=null)
                    PintuManager.getInstance().setUserRestriction(UserManager.DISALLOW_ADD_USER, true);
                break;
            case Constants.DISALLOW_SET_WALLPAPER:
                if (PintuManager.getInstance().getDpm()!=null)
                    PintuManager.getInstance().setUserRestriction(UserManager.DISALLOW_SET_WALLPAPER, true);
                break;
            case Constants.CMD_START_PAGE:
                //start specific activity
                try{
                    String appName=payload.get("appName");
                    Intent start = context.getPackageManager().getLaunchIntentForPackage(appName);
                    start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(start);
                }catch (Exception er){ }
                break;
            case Constants.CMD_GET_INSTALLED_APPS:
                //get all installed apps
                try {
                    result = new JSONObject().put("data", getInstalledApps(context));
                }catch (Exception er){}
                break;
            case Constants.CMD_KILL_APP:
                //kill app
                context.sendBroadcast(new Intent("CMD_KILL_ALL_APP").putExtra("txid",txid));
                break;
            case Constants.CMD_GET_RUNNING_SERVICES:
                //get list of running services
                JSONArray array = new JSONArray();
                try {
                    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                        data=new JSONObject();
                        data.put("name",service.service.getClassName());
                        data.put("pkg",service.service.getPackageName());
                        data.put("activesince",service.activeSince);
                        data.put("lastActivityTime",service.lastActivityTime);
                        data.put("pid",service.pid);
                        array.put(data);
                    }
                }catch (Exception er){}
                try {
                    result = new JSONObject().put("data", getInstalledApps(context));
                }catch (Exception er){}
                break;
            case Constants.CMD_CAPTURE_SCREEN :
                //new Helper().takeScreenshot(getApplicationContext());
                break;
            case Constants.CMD_SCHEDULE_EVENT:
                //schedule event
                break;
            //Wriiten by Nikhil Pawar for GPIO open door for Sunchip Dwin through pintu
            case Constants.CMD_DOOR_OPEN_SUNCHIP:
                if(GpioControlSunchip.getInstance().openGPIODoorSunchip())
                {
                    try {
                        result = new JSONObject().put("status", "door opened");
                    }
                    catch(Exception e){}
                }else {
                    try {
                        result = new JSONObject().put("status", "error in door opened");
                    }
                    catch(Exception e){}
                }
                break;
            case Constants.CMD_DOOR_CLOSE_SUNCHIP:
                if(GpioControlSunchip.getInstance().closeGPIODoorSunchip())
                {
                    try {
                        result = new JSONObject().put("status", "door closed");
                    }
                    catch(Exception e){}
                }else {
                    try {
                        result = new JSONObject().put("status", "error in door close");
                    }
                    catch(Exception e){}
                }
                break;
            case Constants.CMD_DOOR_OPEN_DWIN:
                if(!payload.get("type").equals("")) {
                    gpioPort = Integer.parseInt(payload.get("type"));
                }
                if(GpioControlUtil.getInstance().setGpioOutputValue(gpioPort,1))
                {
                    try {
                        result = new JSONObject().put("status", "door opened");
                    }
                    catch(Exception e){}
                }else {
                    try {
                        result = new JSONObject().put("status", "error in door opened");
                    }
                    catch(Exception e){}
                }
                break;
            case Constants.CMD_STARTGPIO_DWIN:
                if(!payload.get("type").equals("")) {
                    gpioPort = Integer.parseInt(payload.get("type"));
                }
                GpioControlUtil.getInstance().startGpio(gpioPort);
                try {
                    Thread.sleep(1000);
                    GpioControlUtil.getInstance().setGpioDirection(gpioPort,"out");
                }
                catch(Exception e)
                { }
                break;

            case Constants.CMD_DOOR_CLOSE_DWIN:
                if(!payload.get("type").equals("")) {
                    gpioPort = Integer.parseInt(payload.get("type"));
                }
                if(GpioControlUtil.getInstance().setGpioOutputValue(gpioPort,0))
                {
                    try {
                        result = new JSONObject().put("status", "door closed");
                    }
                    catch(Exception e){}
                }else {
                    try {
                        result = new JSONObject().put("status", "error in door close");
                    }
                    catch(Exception e){}
                }

                break;

            case Constants.CMD_APPLY_THEME:
                //apply theme
                PintuManager.getInstance().downloadFile(context.getApplicationContext(),payload.get("url"),
                        getThemeDir(payload.get("saveloc")));
                try {
                    result = new JSONObject().put("data",getThemeDir(payload.get("saveloc")).exists()+"");
                }catch (Exception er){}
                break;
            case Constants.DISALLOW_XXXX:
                //disallow specified type
                break;

            case Constants.CMD_NET_STATUS:
                try {
                    String ip=payload.get("ip");
                    int port=Integer.valueOf(payload.get("port"));
                    result = new JSONObject().put("status","success").put("extra",netTest(ip,port));
                }catch (Exception er){ }
                break;
            default:
                break;
        }

        sendResp(txid,command,result,context);
    }

    private String themeDir = ".themes";
    public File getThemeDir(String fileName) {
        File rootPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + themeDir);
        if (!rootPath.exists()) {
            rootPath.mkdirs();
        }

        return new File(rootPath,fileName);
    }

    private void flushLocalFiles(Map<String,String> payload){
        //Log.d("pintu",payload.get("extra"));
        String[] paths=payload.get("extra").split(",");
        new Helper().getFilesAndDel(paths);
    }

    private void sendResp(String txid,String command,JSONObject result,Context context){
        try {
            JSONObject data=new JSONObject();
            data.put("txid",txid);
            data.put("cmd",command);
            data.put("time",System.currentTimeMillis()+"");
            data.put("result",result.toString());
            new NR(Helper.baseurl+"action_update.php", 8000,
                    "POST", data.toString(), context).networkRequest(new ResultCallback() {
                @Override
                public void onSuccess(String result) {
                    // Log.d("result",result);
                }
                @Override
                public void onError(String result) {
                    //  Log.d("result",result);
                }
            });
        }catch (Exception er){}
    }

    private JSONObject getNetworkInfo(Context context){
        JSONObject data=new JSONObject();
        try{
            ConnectivityManager networkStatsManager=(ConnectivityManager) context.getApplicationContext().getSystemService(context.CONNECTIVITY_SERVICE);
            data.put("ip_address",getLocalIpAddress());
            data.put("active_network",networkStatsManager.getActiveNetworkInfo().getState());
            data.put("active_reason",networkStatsManager.getActiveNetworkInfo().getReason());
            data.put("is_available",networkStatsManager.getActiveNetworkInfo().isAvailable());
            data.put("is_connected",networkStatsManager.getActiveNetworkInfo().isConnected());
            data.put("is_roaming",networkStatsManager.getActiveNetworkInfo().isRoaming());
            data.put("extra_info",networkStatsManager.getActiveNetworkInfo().getExtraInfo().replaceAll("\"",""));
            data.put("is_metered",networkStatsManager.isActiveNetworkMetered());
        }   catch (Exception er){}
        return data;
    }

    public String getLocalIpAddress() {
        try {
            ArrayList<NetworkInterface>  nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni: nilist)
            {
                ArrayList<InetAddress>  ialist = Collections.list(ni.getInetAddresses());
                for (InetAddress address: ialist){
                    if (!address.isLoopbackAddress())
                    {
                        return address.getHostAddress();
                    }
                }

            }

        } catch (Exception ex) {
            //Log.e("local ip", ex.toString());
        }
        return "NA";
    }

    private boolean isMyServiceRunning(String serviceClass,Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean ManageWifi(String type,Context context){
        WifiManager wifiManager=(WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        switch (type){
            case  Constants.CMD_DISABLE_WIFI:
                wifiManager.disableNetwork(wifiManager.getConnectionInfo().getNetworkId());
                return wifiManager.disconnect();
            case Constants.CMD_ENABLE_WIFI:
                return  wifiManager.setWifiEnabled(true);
            case Constants.CMD_RECONNECT_WIFI:
                return  wifiManager.reconnect();
            case Constants.CMD_WIFI_OFF:
                return wifiManager.setWifiEnabled(false);
            default:

        }
        return false;
    }

    private JSONObject getWifiInfo(Context context){
        WifiManager wifiManager=(WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        JSONObject data=new JSONObject();
        try {
            data.put("iswifi", wifiManager.isWifiEnabled() + "");
            data.put("wifistate", wifiManager.getWifiState() + "");
            data.put("wifi_ip", getIpFromInt(wifiManager.getConnectionInfo().getIpAddress()));
            data.put("wifi_ssid", wifiManager.getConnectionInfo().getSSID().replaceAll("\"","")+ "");
            data.put("wifi_mac", wifiManager.getConnectionInfo().getMacAddress().toUpperCase()+ "");
            data.put("wifi_speed", wifiManager.getConnectionInfo().getLinkSpeed()+ "");
            data.put("wifi_rssi", wifiManager.getConnectionInfo().getRssi()+ "");
            data.put("wifi_freq", wifiManager.getConnectionInfo().getFrequency()+ "");
        }catch (Exception er){}
        //Log.d("data",data.toString());
        return data;
    }

    private String getIpFromInt(int ip){
        return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    private JSONObject  getBatteryinfo(Context context){
        try {
            JSONObject data = new JSONObject();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            if (Build.VERSION.SDK_INT>=21) {
                data.put("isinteractive",pm.isInteractive());
                data.put("ispowersave",pm.isPowerSaveMode());
                if (Build.VERSION.SDK_INT>=23) {
                    data.put("isidle", pm.isDeviceIdleMode());
                }
                data.put("isScreenOn",pm.isScreenOn());

                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.registerReceiver(null, ifilter);

                // Are we charging / charged?
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;

                // How are we charging?
                int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

                data.put("ischarging",isCharging);
                data.put("usbCharge",usbCharge);
                data.put("acCharge",acCharge);

                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                int batteryPct =(int)((level / (float)scale)*100);

                data.put("batt_voltage",batteryPct);

            }
            //Log.d("data",data.toString());
            return data;
        }catch (Exception er){}
        return new JSONObject();
    }

    private boolean isWifi(Context context){
        try {
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Activity.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            } else {
                return false;
            }
        }catch (Exception e){
            return false;
        }
    }

    private boolean isBluetooth(Context context){
        BluetoothAdapter mBluetoothAdapter;
        try {
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager
            mBluetoothAdapter = bluetoothManager.getAdapter();
            return  mBluetoothAdapter.isEnabled();
        }catch (Exception er){}
        return false;
    }

    public boolean isUsbDebug(Context context){
        int adb = Settings.Secure.getInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED , 0);
        return adb>0;
    }

    public int getRunningServicesCount(Context context){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return  manager.getRunningServices(Integer.MAX_VALUE).size();
    }

    public int getInstalledAppCount(Context context){
        return context.getPackageManager().getInstalledPackages(0).size();
    }

    public JSONArray getInstalledApps(Context context)
    {
        JSONArray array=new JSONArray();
        JSONObject data;
        List<PackageInfo> packList = context.getPackageManager().getInstalledPackages(0);
        try {
            for (int i = 0; i < packList.size(); i++) {
                PackageInfo packInfo = packList.get(i);
                //if ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = packInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
                //System.out.println("App name: "+ appName+" "+packInfo.packageName);
                data=new JSONObject();
                data.put("name",appName);
                data.put("pkg",packInfo.packageName);
                data.put("lastUpdateTime",packInfo.lastUpdateTime);
                data.put("firstInstallTime",packInfo.firstInstallTime);
                data.put("ver",packInfo.versionName);
                array.put(data);
                // }
            }
        }catch (Exception er){ }
        return  array;
    }

    private String nameOfHomeApp(Context context)
    {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = context.getPackageManager();
            final ResolveInfo mInfo = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            return mInfo.activityInfo.packageName;
        } catch(Exception e) {
            return "";
        }
    }

    private JSONObject netTest(String ip,int port){
        try{
                JSONObject data=new JSONObject();
                data.put("visiapp.in",urlTest("https://visiapp.in"));
                data.put("dash.visiapp.in",urlTest("https://dash.visiapp.in"));
                data.put("pintu",urlTest("http://pintu.biosentry.co.in"));
                data.put("call_port",checkPort(ip,port));
                return data;
        }catch(Exception e){
            return new JSONObject();
        }
    }

    private String checkPort(String ip,int port){
        try{
            Socket clientSocket = new Socket(ip, port);
            clientSocket.setSoTimeout(500);
            if(clientSocket.isConnected()) {
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                outToServer.write("HEL:\r\n".getBytes());
                outToServer.flush();
                clientSocket.close();
                return "OPEN FOR COMMUNICATION";
            }else{
                return "UNABLE TO CONNECT";
            }
        }catch (Exception er){
            return er.getMessage();
        }
    }

    private String kioskMode(Context context){
        try {
           /* if (PintuManager.getInstance().getDpm()==null) {
                PintuManager.getInstance().setAdmin(DeviceAdminReceiver.getComponentName(context));
                PintuManager.getInstance().setDpm((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
            }
            String[] apps = new String[]{"wyse.android.visimaster", "wyse.android.biosentry",
                    "wyse.cis.weaponman", "wyse.cis.visi", "wyse.cis.biosentryx", "android.wyse.face","cis.pintu"};
            PintuManager.getInstance().getDpm().setLockTaskPackages(PintuManager.getInstance().getAdmin(), apps);*/
           Intent kisokmode=new Intent(context,PintuLocker.class);
           kisokmode.setAction(Constants.CMD_TASK_LOCK);
           kisokmode.putExtra("access",context.getPackageName());
           kisokmode.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(kisokmode);
            return "success";
        }catch (Exception er){
            //er.printStackTrace();
            return er.getMessage();
        }
    }

    private String urlTest(String link){
        try{
            URL url = new URL(link);
            int responseCode=-1;
            if (url.getProtocol().equalsIgnoreCase("https")) {
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setReadTimeout(4000);
                urlConnection.setConnectTimeout(4000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                 responseCode = urlConnection.getResponseCode();
            }else{
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setReadTimeout(4000);
                urlConnection.setConnectTimeout(4000);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                 responseCode = urlConnection.getResponseCode();
            }

            return responseCode+"";
        }catch (Exception er){
           return er.getMessage();
        }
        //return "Error";
    }

}
