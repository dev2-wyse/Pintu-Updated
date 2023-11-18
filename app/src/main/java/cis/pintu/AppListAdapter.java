package cis.pintu;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class AppListAdapter extends BaseAdapter {

    Context context;
    private static LayoutInflater inflater=null;
    ArrayList<AppModel> appModels;
    private String DEFAULT_CONFIG_FILE="pintu.conf";
    public AppListAdapter(Context context,ArrayList<AppModel> appModels){
        this.appModels=appModels;
        this.context=context;
        inflater = ( LayoutInflater )context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount(){
        return appModels.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public class Holder
    {
        TextView appName;
     //   TextView appVer;
        Button default_btn;
        ImageView appIcon;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        Holder holder = new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.app_list_layout, null);

        holder.appName=(TextView)rowView.findViewById(R.id.appname);
        holder.appIcon=(ImageView) rowView.findViewById(R.id.appIcon);
        holder.default_btn = (Button)rowView.findViewById(R.id.default_btn);
//        holder.appVer=(TextView)rowView.findViewById(R.id.appver);

        holder.appName.setText(appModels.get(position).getLabel());
        holder.appIcon.setImageDrawable(appModels.get(position).getIcon());
        //holder.appVer.setText(appModels.get(position).getApplicationPackageName());

        holder.default_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        Helper helper=new Helper();
                        helper.saveConfigFile(helper.checkFile(DEFAULT_CONFIG_FILE).getAbsolutePath(),appModels.get(position).getApplicationPackageName());
                        if (appModels.get(position) != null) {
                            Intent intent = context.getPackageManager().getLaunchIntentForPackage(appModels.get(position).getApplicationPackageName());
                            if (intent != null) {
                                SharedPreferences sharedPreferences=context.getSharedPreferences(Helper.PREF_FILE_NAME, Context.MODE_PRIVATE);
                                Helper.setSharedPref(sharedPreferences,"default_app",appModels.get(position).getApplicationPackageName());
                                context.startActivity(intent);
                            }
                        }
            }
        });

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppModel app=appModels.get(position);
                if (app != null) {
                    Intent intent = context.getPackageManager().getLaunchIntentForPackage(app.getApplicationPackageName());
                    if (intent != null) {  context.startActivity(intent);  }
                }
            }
        });

        return rowView;
    }



}
