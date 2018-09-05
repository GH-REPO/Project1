package info.tinyapps.huges.ui;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import info.tinyapps.huges.R;
import info.tinyapps.huges.services.ScanService;
import info.tinyapps.huges.utils.AppSettings;

/**
 * this is main (and currently mainly for testing) activity
 * to start stop service and change settings
 */
public class MainActivity extends BaseActivity {
    TextView mLogField;
    static final int MSG_ADD_LOG = 1;
    static final SimpleDateFormat DTS_FORMATER = new SimpleDateFormat("HH:mm:ss.SSS");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLogField = (TextView)findViewById(R.id.fldLog);

        ((CheckBox)findViewById(R.id.cbMuted)).setChecked(new AppSettings(getThis()).isMuted());

        ((CheckBox)findViewById(R.id.cbMuted)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!b){
                    new AppSettings(getThis()).setMute(false);
                }
                else{
                    new AppSettings(getThis()).setMute(true);
                }

                invalidateOptionsMenu();
            }
        });

        String beaconID = new AppSettings(getThis()).getBeaconID();
        if(beaconID != null)
            setFieldText(R.id.fldBeaconID,beaconID);
        else
            doSelectBeacon();

        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(getThis(),SelectBeaconActivity.class),1);
            }
        });

        findViewById(R.id.btnLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    new AppSettings(getThis()).setMute(true);
                    new AppSettings(getThis()).clear();
                }
                catch (Exception e){
                }
                try{
                    stopService(new Intent(getThis(), ScanService.class));
                }
                catch (Exception e){
                }
                try{
                    getUserPool().getCurrentUser().signOut();
                    getUserPool().getUser().signOut();
                }
                catch (Exception e){
                }

                finish();
            }
        });

        setSupportActionBar((Toolbar)findViewById(R.id.mainToolbar));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onResume(){
        super.onResume();
        registerReceiver(mReceiver,makeFilter());
    }

    public void onPause(){
        super.onPause();
        try{
            unregisterReceiver(mReceiver);
        }
        catch (Exception e){
            addLog("",e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            addUiLog(intent.getStringExtra(ScanService.TAG_DATA));
        }
    };

    protected static IntentFilter makeFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ScanService.ACTION);

        return intentFilter;
    }

    void addUiLog(String txt){
        if(txt == null)
            return;

        String msg = DTS_FORMATER.format(new Date()) + "\n" + txt;
        mHandler.obtainMessage(MSG_ADD_LOG,msg).sendToTarget();
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isFinishing())
                return;

            switch (msg.what){
                case MSG_ADD_LOG:
                    String txt = msg.obj + "\n" + mLogField.getText().toString();
                    mLogField.setText(txt);
                break;
            }
        }
    };

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName()))
                return true;

        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if(new AppSettings(getThis()).isMuted()){
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setVisible(false);
            return true;
        }

        if (!isMyServiceRunning(ScanService.class)) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setVisible(false);
        }
        else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.view_prg);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                String beaconID = new AppSettings(getThis()).getBeaconID();
                if(beaconID == null){
                    doSelectBeacon();
                }
                else {
                    startService(new Intent(getThis(), ScanService.class));
                }
            break;
            case R.id.menu_stop:
                stopService(new Intent(getThis(), ScanService.class));
            break;
        }

        invalidateOptionsMenu();

        return true;
    }

    void doSelectBeacon(){
        AlertDialog.Builder builder = getAlertDialogBuilder();

        builder.setTitle(R.string.app_name);
        builder.setMessage("You do not have beacon ID set. Would you liek to set it now?");

        builder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivityForResult(new Intent(getThis(),SelectBeaconActivity.class),1);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String beaconID = new AppSettings(getThis()).getBeaconID();
        if(beaconID != null)
            setFieldText(R.id.fldBeaconID,beaconID);
    }
}

