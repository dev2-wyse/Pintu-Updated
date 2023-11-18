package cis.pintu;

// Created by Nikhil on 07/10/23. for GPIO based access control

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

public class GpioControlSunchip {

    private static GpioControlSunchip gpioControlSunchip;

    private GpioControlSunchip(){}

    public static synchronized GpioControlSunchip getInstance() {
        if (gpioControlSunchip==null)
            gpioControlSunchip=new GpioControlSunchip();

        return gpioControlSunchip;
    }

    public boolean initGpio(String lowHigh) {

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
            return false;
        }

        if(process != null) {
            OutputStream outputStream = process.getOutputStream();
            try {
                String command="echo "+lowHigh+" > /sys/devices/virtual/adw/adwdev/adwgpio2\n"; //pull up gpio port
                outputStream.write(command.getBytes());
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

        return true;
    }

    public boolean setGpioValue(String value) {
        Runtime runtime = Runtime.getRuntime();
        Process process = null;
        String cmd = "echo " + value + "  > /sys/devices/virtual/adw/adwdev/adwgpio2\n";

        if (runtime != null){
            try {
                process = runtime.exec("su");
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }else {
            //Log.d(TAG,"runtime is null");
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
        }
        else {
            return false;
        }
        return true;
    }


    public synchronized boolean openGPIODoorSunchip() {
        Log.d("door","into toggle gpio");

            if (setGpioValue("c")) {
                Log.d("door", "door open successfully !");
                return true;
            } else {
                setGpioValue("o");
            }

            return false;
    }

    public synchronized boolean closeGPIODoorSunchip() {
        Log.d("door","into toggle gpio");

            if (setGpioValue("o")) {
                Log.d("door", "door open successfully !");
                return true;
            } else {
                setGpioValue("o");
            }

            return false;
    }


}
