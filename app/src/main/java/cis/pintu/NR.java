package cis.pintu;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

interface ResultCallback{
     void onSuccess(String result);
     void onError(String result);
}

public class NR {
    private String method="POST";
    private int connectionTime=10000;
    private String url="";
    private Context context;
    private String data;

    public NR(String url, int connectionTime, String method, String data, Context context){
        this.method=method;
        this.connectionTime=connectionTime;
        this.url=url;
        this.context=context;
        this.data=data;
       // Log.d("data",data);
    }

    public int getConnectionTime() {
        return connectionTime;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Context getContext() {
        return context;
    }

    public String getData() {
        return data;
    }

    public boolean isConnected(){
        try {
            ConnectivityManager connMgr = (ConnectivityManager) this.context.getSystemService(Activity.CONNECTIVITY_SERVICE);
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

    public void networkRequest(final ResultCallback resultCallback){
        Thread networkth=new Thread(new Runnable() {
                    @Override
                    public void run() {
                            if (isConnected()){

                                //Log.d("Pintu",getUrl());
                                //Log.d("Pintu",getData());
                                try {
                                    URL url = new URL(getUrl());
                                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                                    urlConnection.setReadTimeout(getConnectionTime());
                                    urlConnection.setConnectTimeout(getConnectionTime());
                                    urlConnection.setRequestProperty("Content-Type", "application/json");
                                    urlConnection.setRequestProperty("Accept", "application/json");
                                    urlConnection.setRequestMethod(getMethod());
                                    urlConnection.setDoInput(true);
                                    urlConnection.setDoOutput(true);

                                    OutputStream os = urlConnection.getOutputStream();
                                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                                    writer.write(getData());
                                    writer.close();
                                    os.close();

                                    int responseCode = urlConnection.getResponseCode();
                                    //Log.d("Pintu",responseCode+"");

                                    if (responseCode == HttpsURLConnection.HTTP_OK) {

                                        InputStreamReader inputStream = new InputStreamReader(urlConnection.getInputStream());
                                        BufferedReader br = new BufferedReader(inputStream);

                                        StringBuilder builder = new StringBuilder();
                                        String line;
                                        while ((line = br.readLine()) != null) {
                                            builder.append(line);
                                        }
                                        br.close();
                                        final String response = builder.toString();
                                       // Log.d("Pintu",response);
                                        if (response!=null){
                                            resultCallback.onSuccess(response);
                                        }else{
                                            resultCallback.onError("Error in getting data");
                                        }

                                    }else {
                                        resultCallback.onError("Error in request "+responseCode);
                                    }
                                } catch (Exception er){
                                    //er.printStackTrace();
                                        resultCallback.onError(er.getMessage());
                                }
                            }else{
                                resultCallback.onError("no network");
                            }
                    }
                });
                networkth.setPriority(Thread.NORM_PRIORITY);
                networkth.start();

    }

}
