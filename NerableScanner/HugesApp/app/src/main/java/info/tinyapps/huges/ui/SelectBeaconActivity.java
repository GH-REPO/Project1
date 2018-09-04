package info.tinyapps.huges.ui;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.UpdateAttributesHandler;
import com.estimote.coresdk.common.config.EstimoteSDK;
import com.estimote.coresdk.common.config.Flags;
import com.estimote.coresdk.recognition.packets.Nearable;
import com.estimote.coresdk.service.BeaconManager;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import info.tinyapps.huges.R;
import info.tinyapps.huges.data.NearableData;
import info.tinyapps.huges.services.StaticConfig;
import info.tinyapps.huges.utils.AppSettings;

public class SelectBeaconActivity extends BaseActivity{
    boolean mDoNotSave;
    String mUserBeaconID;
    BeaconAdapter mAdapter;
    BeaconManager mBeaconManager;
    Hashtable<String, NearableData> mBeacons;

    static final int MSG_DEVICE_FOUND = 1;
    static final int MSG_PURGE = 2;
    static final long PERGE_TIMEOUT = 15000;

    //do not save results, this is called form login screen
    public static final String TAG_DONOT_SAVE = "TAG_DONOT_SAVE";
    public static final String TAG_SELECTED_BEACON = "TAG_SELECTED_BEACON";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_beacon);

        mDoNotSave = getIntent().getBooleanExtra(TAG_DONOT_SAVE,false);

        mUserBeaconID = new AppSettings(getThis()).getBeaconID();
        mBeacons = new Hashtable<>();
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!btAdapter.isEnabled())
            btAdapter.enable();

        ListView list = (ListView)findViewById(R.id.beaconsList);
        mAdapter = new BeaconAdapter(getThis());
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try{
                    final String address = (String)view.getTag();
                    if(address != null){
                        AlertDialog.Builder builder = getAlertDialogBuilder();
                        builder.setTitle(R.string.app_name);
                        builder.setMessage("Ate you sure you wnat to asing " + address + "?");

                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setBeaconID(address);
                            }
                        });

                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });

                        builder.show();
                    }
                }
                catch (Exception e){
                    addLog("",e);
                }
            }
        });

        EstimoteSDK.initialize(getThis(), "testimote-80c", "fde35662eb5f8004e4792073b076df30");
        EstimoteSDK.enableDebugLogging(true);
        Flags.DISABLE_BATCH_SCANNING.set(true);
        Flags.DISABLE_HARDWARE_FILTERING.set(true);

        mBeaconManager = new BeaconManager(getThis());
        mBeaconManager.setNearableListener(new MyNearableListener());

        setSupportActionBar((Toolbar)findViewById(R.id.mainToolbar));
        setTitle2("Select Beacon");
    }

    class BeaconAdapter extends ArrayAdapter<NearableData> {
        public BeaconAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Nearable beaconData = getItem(position).mNearBale;

            if (convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_beacon_selection, parent, false);


            //beaconData.identifier;
            String btName = beaconData.beaconRegion.getProximityUUID().toString(); //beaconData.identifier;
            String btType = beaconData.type.toString();

            ((TextView)convertView.findViewById(R.id.text_name)).setText(btType);
            ((TextView)convertView.findViewById(R.id.text_address)).setText(btName);
            ((TextView)convertView.findViewById(R.id.text_rssi)).setText(String.format("%ddBm",beaconData.rssi));

            convertView.setBackground(null);
            if( btName.equals(mUserBeaconID))
                convertView.setBackgroundColor(Color.GREEN);

            convertView.setTag(btName);

            return convertView;
        }
    }

    class MyNearableListener implements BeaconManager.NearableListener{
        @Override
        public void onNearablesDiscovered(List<Nearable> nearables) {
            for(Nearable nearable : nearables){
                if(nearable.beaconRegion != null){
                    mHandler.obtainMessage(MSG_DEVICE_FOUND, nearable).sendToTarget();
                }
            }
        }
    }

    public void onResume(){
        super.onResume();
        try {
            mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
                @Override
                public void onServiceReady() {
                    mBeaconManager.startNearableDiscovery();
                    mHandler.sendEmptyMessageDelayed(MSG_PURGE,PERGE_TIMEOUT);
                }
            });
        }
        catch (Exception e){
            addLog("",e);
        }
    }

    public void onPause(){
        super.onPause();
        try {
            mBeaconManager.stopNearableDiscovery();
        }
        catch (Exception e){
            addLog("",e);
        }

        try {
            mBeaconManager.disconnect();
        }
        catch (Exception e){
            addLog("",e);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String beaconID;
            Nearable beacon;

            if(isFinishing())
                return;

            switch (msg.what){
                case MSG_DEVICE_FOUND:
                    beacon = (Nearable) msg.obj;
                    beaconID = beacon.beaconRegion.getProximityUUID().toString();

                    if(mBeacons.contains(beaconID))
                        mBeacons.get(beaconID).mDTS = System.currentTimeMillis();
                    else
                        mBeacons.put(beaconID,new NearableData(beacon));
                break;
                case MSG_PURGE:
                    for(Iterator<Map.Entry<String, NearableData>> it = mBeacons.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry<String, NearableData> entry = it.next();
                        if(System.currentTimeMillis() - entry.getValue().mDTS > PERGE_TIMEOUT) {
                            mBeacons.remove(entry.getKey());
                        }
                    }

                    mHandler.sendEmptyMessageDelayed(MSG_PURGE,PERGE_TIMEOUT);
                break;
            }

            mAdapter.setNotifyOnChange(false);
            mAdapter.clear();
            mAdapter.addAll(mBeacons.values());
            mAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_beacon, menu);

        menu.findItem(R.id.menu_refresh).setActionView(R.layout.view_prg);

        return true;
    }

    void setBeaconID(final String beaconID){
        if(mDoNotSave) {
            Intent data = new Intent();
            data.putExtra(TAG_SELECTED_BEACON, beaconID);
            setResult(RESULT_OK, data);
            finish();
            return;
        }

        UpdateAttributesHandler updatesHandler = new UpdateAttributesHandler() {
            @Override
            public void onSuccess(List<CognitoUserCodeDeliveryDetails> attributesVerificationList) {
                Intent data = new Intent();
                data.putExtra(TAG_SELECTED_BEACON,beaconID);

                new AppSettings(getThis()).setBeaconID(beaconID);

                setResult(RESULT_OK,data);
                finish();
            }

            @Override
            public void onFailure(Exception exception) {
                showErr(exception);
            }
        };

        CognitoUserAttributes attributes = new CognitoUserAttributes();
        attributes.addAttribute(StaticConfig.ATTR_BEACON_ID,beaconID);
        //attributes.addAttribute("address",beaconID);
        getUserPool().getUser(new AppSettings(getThis()).getUserName()).updateAttributesInBackground(attributes, updatesHandler);
    }
}

