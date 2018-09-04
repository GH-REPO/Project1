package info.tinyapps.huges.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoIdToken;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;
import com.estimote.coresdk.common.config.EstimoteSDK;
import com.estimote.coresdk.common.config.Flags;
import com.estimote.coresdk.recognition.packets.Nearable;
import com.estimote.coresdk.service.BeaconManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.tinyapps.huges.R;
import info.tinyapps.huges.data.FixRecord;
import info.tinyapps.huges.data.LocalDB;
import info.tinyapps.huges.data.RequestData;
import info.tinyapps.huges.data.ResponseData;
import info.tinyapps.huges.data.TestInterface;
import info.tinyapps.huges.ui.BaseActivity;
import info.tinyapps.huges.ui.LoginActivity;
import info.tinyapps.huges.utils.AppSettings;
import info.tinyapps.huges.utils.BaseLogging;

/**
 * the service that does scanning
 * 1) it starts new scan every every 6000 ms (StaticConfig.NEXT_RUN)
 * 2) service runs as foreground service so Android 8.0 is not killing us (that's why we need the notification on the top)
 * 3) every minute the service is running a task made of:
 *  getting gps location
 *  scanniing for beacons
 * 4) once the required beacon (registered to user) is detected
 * it is stored locally
 * 5) once an hour records are reported to the backend
 */
public class ScanService extends Service{
    private static int FOREGROUND_ID = 1335;
    private static final String TAG_SERVICE = "TAG_SERVICE";
    static final String NAME = "info.tinyapps.huges.services.ScanService";
    private static volatile PowerManager.WakeLock mLockStatic = null;
    public static String ACTION = "MainService.Action";
    public static String TAG_DATA = "TAG_DATA";
    static final SimpleDateFormat DTS_FORMATER = new SimpleDateFormat("HH:mm:ss.SSS");

    ScanService getThis(){
        return this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        startForeground(FOREGROUND_ID,buildForegroundNotification("Service Started"));

        getLock();
        if(!new AppSettings(getThis()).isMuted())
            new CheckTask().execute();

        return START_STICKY;
    }

    private Notification buildForegroundNotification(String text) {
        NotificationCompat.Builder buidler = new NotificationCompat.Builder(this);

        buidler.setOngoing(true);
        buidler.setContentTitle(getString(R.string.app_name));
        buidler.setContentText(text);
        buidler.setSmallIcon(R.drawable.logo);
        buidler.setTicker("tracking");
        buidler.setContentIntent(getNotificationIntent());

        return buidler.build();
    }

    private void updateNotification(String text) {
        Notification notification = buildForegroundNotification(text);
        NotificationManager mNotificationManager =(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(FOREGROUND_ID, notification);
    }

    void addLog(String txt){
        BaseLogging.addLog(txt);
        Log.d(TAG_SERVICE,txt);
    }

    void addLog(String txt,Exception e){
        BaseLogging.addLog(txt,e);
        Log.e(TAG_SERVICE,"",e);
    }

    AuthenticationHandler mAuthHandler = new AuthenticationHandler() {
        public void authenticationChallenge(ChallengeContinuation continuation){
            addLog("failed with authenticationChallenge");
            showError("Please login again and restart service",true);
        }

        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            addLog("new login done");
            CognitoIdToken token = userSession.getIdToken();

            try {
                new AppSettings(getThis()).setJWTToken(token.getJWTToken(), token.getExpiration().getTime());
            }
            catch (Exception e){
                showError("Please login again and restart service",true);
            }
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            addLog("doing new login");
            AppSettings settings = new AppSettings(getThis());
            AuthenticationDetails authenticationDetails = new AuthenticationDetails(userId, settings.getUserPass(), null);
            authenticationContinuation.setAuthenticationDetails(authenticationDetails);
            authenticationContinuation.continueTask();
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
            addLog("failed with getMFACode");
            showError("User session has expired, please login again and restart service",true);
        }

        @Override
        public void onFailure(Exception exception) {
            addLog("failed with onFailure",exception);
            showError("User session has expired, please login again and restart service",false);
        }
    };

    void checkBt(){
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!btAdapter.isEnabled())
            btAdapter.enable();
    }

    PendingIntent getNotificationIntent(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    void showError(String message, boolean withMute) {
        if(withMute)
            new AppSettings(getThis()).setMute(true);
        updateNotification(message);
    }

    class MyLocationCallback extends LocationCallback {
        Location mLastGoodLoc;

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location loc = locationResult.getLastLocation();
            if (loc != null) {
                addLog("location accuracy is " + loc.getAccuracy());
                mLastGoodLoc = getBetterLoc(mLastGoodLoc,loc);
            }
        }
    }

    public static Location getBetterLoc(Location l1, Location l2){
        if(l1 == null)
            return l2;
        if(l2 == null)
            return l2;

        //seconds
        long timeDelta = (l1.getTime() - l2.getTime())/1000;

        //one is too old
        if(StaticConfig.LOC_TOO_OLD < Math.abs(timeDelta)){
            if(timeDelta < 1)
                return l1;
            else
                return l2;
        }

        float accur_diff = l1.getAccuracy() - l2.getAccuracy();

        //l1 is more accure
        if(accur_diff < 0)
            return l1;
        else
            return l2;
    }

    class CheckTask extends AsyncTask<Void, Void, Void> {
        MyLocationCallback mLocCallback;
        MyNearableListener mNearableListener;
        FusedLocationProviderClient mFusedLocationClient;
        BeaconManager mBeaconManager;

        public CheckTask(){
        }

        protected LocationRequest getLocationrequest(){
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(StaticConfig.LOC_UPDATE_INTERVAL);
            locationRequest.setFastestInterval(StaticConfig.LOC_FASTEST_UPDATE);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            return locationRequest;
        }

        protected void onPreExecute(){
            try {
                addLog("onPreExecute");
                checkBt();
                EstimoteSDK.initialize(getThis(), "testimote-80c", "fde35662eb5f8004e4792073b076df30");
                EstimoteSDK.enableDebugLogging(true);
                Flags.DISABLE_BATCH_SCANNING.set(true);
                Flags.DISABLE_HARDWARE_FILTERING.set(true);

                broadcastInfo("check started");

                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getThis());
                mLocCallback = new MyLocationCallback();
                mFusedLocationClient.requestLocationUpdates(getLocationrequest(), mLocCallback, null);
                mNearableListener = new MyNearableListener();

                mBeaconManager = new BeaconManager(getThis());
                mBeaconManager.setNearableListener(mNearableListener);


                //mBeaconManager.setBackgroundScanPeriod(7000,0);
                addLog("connecting beacon manager");
                mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
                    @Override
                    public void onServiceReady() {
                        addLog("starting beacon discovery");
                        broadcastInfo("check ping");
                        mBeaconManager.startNearableDiscovery();
                    }
                });
            }
            catch (Exception e){
                broadcastInfo("error occured " + e.getClass().getName() + "\n" + e.getMessage());
                addLog("onPreExecute failed",e);
            }
        }

        protected Void doInBackground(Void... var1){
            try{
                addLog("checking");

                for(int i = 0; i < StaticConfig.CHECK_MAX_RETRIES;i++) {
                    if (mLocCallback.mLastGoodLoc != null){
                        if(mNearableListener.mMyBeacon != null){
                            broadcastInfo("my beacon detected!");
                            break;
                        }
                    }

                    if(new AppSettings(getThis()).isMuted())
                        break;
                    Thread.sleep(1000);
                }

                try {
                    mFusedLocationClient.removeLocationUpdates(mLocCallback);
                }
                catch (Exception e){
                    addLog("removeLocationUpdates failed",e);
                }

                try {
                    mBeaconManager.stopNearableDiscovery();
                }
                catch (Exception e){
                    addLog("stopNearableDiscovery failed",e);
                }

                if(mNearableListener.mMyBeacon != null) {
                    saveBeacons(mNearableListener.mMyBeacon, mLocCallback.mLastGoodLoc);
                }
                else{
                    addLog("nothing to report now");
                }
            }
            catch (Exception e){
                addLog("doInBackground failed",e);
            }

            return null;
        }

        protected void onPostExecute(Void result){

            addLog("scheduling making next run");
            if(new AppSettings(getThis()).isMuted()) {
                stopForeground(true);
                stopSelf();
            }
            else{
                getLock();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        new CheckTask().execute();
                    }
                }, StaticConfig.NEXT_RUN);
            }
        }
    }

    void broadcastInfo(String info){
        addLog("broadcasting " + info);
        Intent intent = new Intent();
        intent.setAction(ACTION);

        intent.putExtra(TAG_DATA,info);

        sendBroadcast(intent);
    }

    void broadcastResult(ResponseData result){
        StringBuilder data = new StringBuilder();

        data.append("Server Responses:\n");

        Intent intent = new Intent();
        intent.setAction(ACTION);

        if(result != null) {
            data.append(result.getResult());
            data.append("\n");
        }

        intent.putExtra(TAG_DATA,data.toString());

        sendBroadcast(intent);
    }

    class MyNearableListener implements BeaconManager.NearableListener{
        Nearable mMyBeacon;

        MyNearableListener(){
        }

        @Override
        public void onNearablesDiscovered(List<Nearable> nearables) {
            String nearableID;
            for(Nearable nearable : nearables){
                Log.d(TAG_SERVICE,"nearable found");
                if(nearable.beaconRegion != null){
                    nearableID = nearable.beaconRegion.getProximityUUID().toString();

                    Log.d(TAG_SERVICE,"\tunique key = " + nearableID);
                    Log.d(TAG_SERVICE,"\tmin = " + nearable.beaconRegion.getMinor());
                    Log.d(TAG_SERVICE,"\tmaj = " + nearable.beaconRegion.getMajor());
                    Log.d(TAG_SERVICE,"\tuuid = " + nearable.beaconRegion.getProximityUUID());
                    //we have beacon found so lets report it

                    if(nearable.beaconRegion.getProximityUUID().toString().equals(new AppSettings(getThis()).getBeaconID())) {
                        mMyBeacon = nearable;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void saveBeacons(Nearable beacon, Location loc){
        String lat,lon,uuid;
        addLog("saving beacons");

        if(beacon == null){
            addLog("no beacon to store");
            return;
        }

        uuid = beacon.beaconRegion.getProximityUUID().toString();

        if(loc != null){
            lat = "" + loc.getLatitude();
            lon = "" + loc.getLongitude();
        }
        else{
            lat = null;
            lon = null;
        }

        //store to DB
        LocalDB.addFix(getThis(),uuid,lat,lon);

        //check if time to do upload
        if(new AppSettings(getThis()).getLastUpload() + StaticConfig.UPLOAD_INTERVAL < System.currentTimeMillis()){
            broadcastInfo("doing batch upload");
            doBatchUpload();
        }
    }

    void doLogin() throws Exception{
        CognitoUserPool userPool = BaseActivity.getUserPool(getThis());
        CognitoUser user = userPool.getUser(new AppSettings(getThis()).getUserName());

        if(user == null){
            showError("Cannot login!",true);
            throw new Exception("Failed to login");
        }

        user.getSession(mAuthHandler);
    }

    TestInterface getLambdaMethod(){
        CognitoCredentialsProvider provider = new CognitoCredentialsProvider(BaseActivity.ID_POOL_ID, BaseActivity.CLIENT_REGION);

        Map<String, String> logins = new HashMap<>();
        logins.put(BaseActivity.POOL_ARN, new AppSettings(getThis()).getJWTToken());
        provider.setLogins(logins);

        LambdaInvokerFactory factory = LambdaInvokerFactory.builder().
                context(getThis()).
                region(BaseActivity.CLIENT_REGION).
                credentialsProvider(provider).
                build();

        return factory.build(TestInterface.class);
    }

    void doBatchUpload(){
        try {
            java.util.logging.Logger.getLogger("com.amazonaws").setLevel(java.util.logging.Level.FINEST);
            java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.FINEST);

            addLog("starting batch uploads");
            ArrayList<FixRecord> recs = LocalDB.getRecords(getThis());
            addLog("" + recs.size() + " records found");

            doLogin();


            TestInterface invoker = getLambdaMethod();

            for(FixRecord rec : recs){
                ResponseData response = invoker.writeFactsToDB(new RequestData(rec));
                addLog("data sent with " + response.getResult());
                LocalDB.deleteFix(getThis(),rec.mRecID);
            }

            addLog("last upload updated");
        }
        catch (Exception e){
            addLog("",e);
        }

        new AppSettings(getThis()).setLastUpload(System.currentTimeMillis());
    }

    synchronized private void getLock() {
        try {
            if (mLockStatic == null) {
                PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
                mLockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);
                mLockStatic.acquire();
            }

            if (mLockStatic.isHeld())
                return;

            mLockStatic.acquire();
        }
        catch (Exception e){
            addLog("",e);
        }
    }

    synchronized private void freeLock() {
        if (mLockStatic != null) {
            mLockStatic.release();
            mLockStatic = null;
        }
    }

    public void onDestroy(){
        try{
            freeLock();
        }
        catch (Exception e){
        }
    }
}

