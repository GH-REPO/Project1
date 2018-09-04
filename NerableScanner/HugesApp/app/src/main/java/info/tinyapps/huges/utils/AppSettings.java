package info.tinyapps.huges.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

/**
 * persistent storage for app settins
 */
public class AppSettings {
    Context mCtx;
    SharedPreferences mPrefs;

    /** when set service is disabled**/
    public static final String TAG_DISABLED = "TAG_DISABLED";

    /** last AWS token **/
    public static final String TAG_JWTTOKEN_VAL = "TAG_JWTTOKEN_VAL";
    /** last AWS token exp date **/
    public static final String TAG_JWTTOKEN_EXP = "TAG_JWTTOKEN_EXP";

    /** last AWS user **/
    public static final String TAG_USER_NAME = "TAG_USER_NAME";
    /** last AWS password **/
    public static final String TAG_USER_PASS = "TAG_USER_PASS";

    /** user beacon to search for **/
    public static final String TAG_BEACON_ID = "TAG_BEACON_ID";
    /** DTS of the last upload **/
    public static final String TAG_LAST_UPLOAD = "TAG_LAST_UPLOAD";

    public AppSettings(Context ctx){
        mCtx = ctx;
        mPrefs = ctx.getSharedPreferences("HUGES_SETTINGS", Activity.MODE_PRIVATE);
    }

    public String getStrValue(String tag){
        return mPrefs.getString(tag, null);
    }

    public int getIntValue(String tag){
        return mPrefs.getInt(tag, -1);
    }

    public void setValue(String tag,int value){
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(tag, value);
        editor.commit();
    }

    public void setValue(String tag,long value){
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong(tag, value);
        editor.commit();
    }

    public long getLongValue(String tag){
        return mPrefs.getLong(tag, -1);
    }

    public void setValue(String tag,String value){
        SharedPreferences.Editor editor = mPrefs.edit();

        if(value == null)
            editor.remove(tag);
        else
            editor.putString(tag, value);

        editor.commit();
    }

    public void setMute(boolean mute){
        setValue(TAG_DISABLED,mute?1:0);
    }

    public boolean isMuted(){
        //default is is muted
        int result = getIntValue(TAG_DISABLED);
        if(result == -1)
            return true;
        return getIntValue(TAG_DISABLED) > 0;
    }

    public void setJWTToken(String token, long expDate){
        setValue(TAG_JWTTOKEN_VAL,token);
        setValue(TAG_JWTTOKEN_EXP,expDate);
    }

    public String getJWTToken(){
        String res = getStrValue(TAG_JWTTOKEN_VAL);
        if(res == null)
            return null;

        //it is expired
        if(System.currentTimeMillis() > getLongValue(TAG_JWTTOKEN_EXP))
            return null;

        return res;
    }

    public long getTokenExpDate(){
        return getLongValue(TAG_JWTTOKEN_EXP);
    }


    public void setUserName(String userName){
        setValue(TAG_USER_NAME,userName);
    }

    public void setUserPass(String userPass){
        setValue(TAG_USER_PASS,userPass);
    }

    public String getUserName(){
        return getStrValue(TAG_USER_NAME);
    }

    public String getUserPass(){
        return getStrValue(TAG_USER_PASS);
    }

    public void setBeaconID(String beaconID){
        setValue(TAG_BEACON_ID,beaconID);
    }

    public String getBeaconID(){
        return getStrValue(TAG_BEACON_ID);
    }

    public void setLastUpload(long dts){
        setValue(TAG_LAST_UPLOAD,dts);
    }

    public long getLastUpload(){
        return getLongValue(TAG_LAST_UPLOAD);
    }

    public void clear(){
        setJWTToken(null,-1);
        setUserName(null);
        setUserPass(null);
        setBeaconID(null);
    }
}

