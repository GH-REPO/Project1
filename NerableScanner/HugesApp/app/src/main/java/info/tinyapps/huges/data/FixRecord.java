package info.tinyapps.huges.data;

/**
 * data structure for the fix/record taking in background service
 */
public class FixRecord {
    //local db record id so it is used to delete once reported
    public long mRecID;
    //dts of the fix (time it was taken
    public long mDTS;
    //beacon id
    public String mUUID;
    //geo lat
    public String mLat;
    //geo lon
    public String mLon;
}

