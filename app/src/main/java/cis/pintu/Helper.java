package cis.pintu;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Base64;
import android.view.View;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class Helper {
    private static String dirPath = "/.pintu";
    public static String baseurl="http://pintu.biosentry.co.in/api/";

    public Helper(){
    }


    public static String PREF_FILE_NAME="pintu_config";
    public static void setSharedPref(SharedPreferences sharedPref, String key, String value){
        try {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(key, value);
            editor.apply();
        }catch (Exception er){
            //  er.printStackTrace();
        }
    }

    public static String getSharedPref(SharedPreferences sharedPreferences, String key){
        return sharedPreferences.getString(key,"");
    }

    public void takeScreenshot(Activity context) {
        try {
            // create bitmap screen capture
            View v1 = context.getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            FileOutputStream outputStream = new FileOutputStream(checkFile(getUUID(context.getApplicationContext())));
            int quality = 80;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

        } catch (Throwable e) { }
    }

    public static void sendSignal(String signal,Context context){
        try{

            JSONObject data = new JSONObject();
            data.put("uuid", getUUID(context));
            data.put("signal", signal);
            data.put("macid", Helper.getMacAddr());
            //Log.d("data",data.toString());
            new NR(Helper.baseurl + "log.php", 8000, "POST", data.toString(), context).networkRequest(new ResultCallback() {
                @Override
                public void onSuccess(String result) {

                }
                @Override
                public void onError(String result) {

                }
            });

        }catch (Exception er){
            // er.printStackTrace();
        }
    }

    public String createMd5(String key){
        StringBuffer sb = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            byte byteData[] = md.digest();
            //convert the byte to hex format method 1
            sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
        }catch (Exception er){}
        return sb.toString();
    }

    public String decodeBaseString(String encodedString){
        String decoded = new String(Base64.decode(encodedString, Base64.DEFAULT));
        return decoded;
    }

    public static String getUUID(Context context){
        return  android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    }


    public String getPintuConfig(File filePath){
        String line="";

        try{
            FileInputStream is;
            BufferedReader reader;
            final File file = new File(filePath.getAbsolutePath());
            if (file.exists()) {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                line += reader.readLine();
                /*while(line != null){
                    line = reader.readLine();
                }*/
                //reader.close();
               is.close();
                return line;
            }
        }catch (Exception er){
            //er.printStackTrace();
        }

        return  line;
    }


    public void saveConfigFile(String filepath,String data){
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filepath,false);
            OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
            writer.append(data+"\n");
            writer.close();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    public static File checkFile(String filepath){
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        File myDir = new File(root + dirPath);
        myDir.mkdirs();
        File file = new File (myDir, filepath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        }catch (Exception er){}
        return file;
    }

    private static String[] WEEKDAY = {"","SUN","MON","TUE","WED","THU","FRI","SAT"};
    public static String getCurrentDateTime(String type,long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);

        int second = cal.get(Calendar.SECOND);
        int minute = cal.get(Calendar.MINUTE);
        int hourofday = cal.get(Calendar.HOUR_OF_DAY);


        int year_new = cal.get(Calendar.YEAR);
        int dayofmonth = cal.get(Calendar.DAY_OF_MONTH);
        int month_new = cal.get(Calendar.MONTH) + 1;
        int weekDay = cal.get(Calendar.DAY_OF_WEEK);

        DecimalFormat mFormat = new DecimalFormat("00");
        mFormat.setRoundingMode(RoundingMode.DOWN);

        if (type.equals("date")) {
            return mFormat.format(Double.valueOf(dayofmonth)) + "/" + mFormat.format(Double.valueOf(month_new)) + "/" + mFormat.format(Double.valueOf(year_new));
        } else if (type.equals("time")) {
            return mFormat.format(Double.valueOf(hourofday)) + ":" + mFormat.format(Double.valueOf(minute)) + ":" + mFormat.format(Double.valueOf(second));
        }else if (type.equals("hh:mm")){
            return mFormat.format(Double.valueOf(hourofday)) + ":" + mFormat.format(Double.valueOf(minute));
        }else if (type.equals("W:D")){
            return WEEKDAY[weekDay]+" "+dayofmonth;
        }

        //cal=null;
        return mFormat.format(Double.valueOf(dayofmonth)) + "," + mFormat.format(Double.valueOf(month_new)) + "," + mFormat.format(Double.valueOf(year_new)) + "," + mFormat.format(Double.valueOf(hourofday)) + "," + mFormat.format(Double.valueOf(minute)) + "," + mFormat.format(Double.valueOf(second));
    }

    public static String isFilePresent(String filename){
        File file =new File(Environment.getExternalStorageDirectory().getAbsoluteFile()+File.separator+filename);
        String ispresent=file.exists()?"yes":"no";
        return  ispresent+ ", updated at " +getCurrentDateTime("date",file.lastModified())+" "+getCurrentDateTime("time",file.lastModified());
    }

    public void getFilesAndDel(String[] paths){
        try{
            //Log.d("pintu","files for del");
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            for (int i=0;i<paths.length;i++) {
                File myDir = new File(root +File.separator+paths[i]);
                //Log.d("pintu",myDir.getAbsolutePath());
                if (myDir.isDirectory()) {
                    File[] files = myDir.listFiles();
                    if (files.length > 0) {
                        for (File file : files) {
                            if (file.exists()) {
                                file.delete();
                            }
                        }
                    }
                }
            }
        }catch (Exception er){
            //er.printStackTrace();
        }
    }

    public  static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X-",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) { }
        return "02:00:00:00:00:00";
    }

}
