package cis.pintu;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class GpioControlUtil {
    private final String TAG = GpioControlUtil.class.getSimpleName();
    private static GpioControlUtil sInstance;
    public static int nRet = -1;

    private GpioControlUtil() {}

    public static GpioControlUtil getInstance(){
        if (sInstance == null){
            synchronized (GpioControlUtil.class){
                if (sInstance == null){
                    sInstance = new GpioControlUtil();
                }
            }
        }
        return sInstance;
    }

    public boolean startGpio(int gpioNumber) {
        String openGpio = "echo " + gpioNumber + " > /sys/class/gpio/export\n";
        //String chmodGpioValue = "chmod 777 /sys/class/gpio/gpio" + gpioNumber + "/value\n";
        //String chmodGpioDirection = "chmod 777 /sys/class/gpio/gpio" + gpioNumber + "/direction\n";

        Runtime runtime = Runtime.getRuntime();
        Process process = null;

        if (runtime != null){
            try {
                process = runtime.exec("su");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }else {
            Log.d(TAG,"runtime is null");
            return false;
        }

        if(process != null) {
            OutputStream outputStream = process.getOutputStream();
            try {
                outputStream.write(openGpio.getBytes());
                //outputStream.write(chmodGpioValue.getBytes());
                //outputStream.write(chmodGpioDirection.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }finally {
                try {
                    if (outputStream != null){
                        outputStream.close();
                    }
                } catch (IOException e) {
                e.printStackTrace();
                    return false;
                }
            }
        }
        else {
            return false;
        }
        //setGpioDirection(gpioNumber,"out");
        return true;
    }

    public boolean setGpioDirection(int gpioNumber, String direction) {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        String cmd = "echo " + direction + "  > /sys/class/gpio/gpio" + gpioNumber + "/direction\n";

        if (runtime != null){
            try {
                process = runtime.exec("su");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }else {
            Log.d(TAG,"runtime is null");
            return false;
        }

        if(process != null) {
            OutputStream outputStream = process.getOutputStream();
            try {
                outputStream.write(cmd.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();

                return false;
            }finally {
                try {
                    if (outputStream != null){
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    return false;
                }
            }
        }
        else {
            return false;
        }
        return true;
    }

    public boolean setGpioOutputValue(int gpioNumber, int value) {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        String cmd = "echo " + value + "  > /sys/class/gpio/gpio" + gpioNumber + "/value\n";

        if (runtime != null){
            try {
                process = runtime.exec("su");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }else {
            Log.d(TAG,"runtime is null");
            return false;
        }
        if(process != null) {
            OutputStream outputStream = process.getOutputStream();
            try {
                outputStream.write(cmd.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();

                return false;
            }finally {
                try {
                    if (outputStream != null){
                        //Log.d(TAG,"close outputstream");
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    return  false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public String getGpioIntputValue(int gpioNumber) throws IOException {
        String gpioFilePath = "/sys/class/gpio/gpio" + gpioNumber + "/value";
        File file = new File(gpioFilePath);
        StringBuilder stringBuilder = null;
        FileInputStream is = null;
        BufferedReader reader = null;
        try {
            //if (file02.length() != 0) {
            if (file.exists()) {
                is = new FileInputStream(file);
                InputStreamReader streamReader = new InputStreamReader(is);
                reader = new BufferedReader(streamReader);
                String line;
                stringBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    // stringBuilder.append(line);
                    stringBuilder.append(line);
                }
            } else {
                nRet = -1;
                return "open file error!";
            }
        } catch (Exception e) {
            e.printStackTrace();
            nRet = -1;
        } finally {
            if (reader != null){
                reader.close();
            }
            if (is != null) {
                is.close();
            }
        }
        nRet = 1;
        return String.valueOf(stringBuilder);
    }
}
