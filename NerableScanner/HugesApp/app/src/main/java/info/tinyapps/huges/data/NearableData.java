package info.tinyapps.huges.data;

import com.estimote.coresdk.recognition.packets.Nearable;

/**
 * data structure for beacon data
 * made of beacon data + dts
 */
public class NearableData {
    public long mDTS;
    public Nearable mNearBale;

    public NearableData(Nearable nearBale){
        mNearBale = nearBale;
        mDTS = System.currentTimeMillis();
    }
}

