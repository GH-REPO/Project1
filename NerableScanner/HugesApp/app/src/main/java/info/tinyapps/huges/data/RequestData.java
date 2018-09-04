package info.tinyapps.huges.data;

import android.location.Location;

import com.estimote.coresdk.recognition.packets.Nearable;

public class RequestData {
    String UUID;
    String latitude;
    String longtitude;
    long dts;

    public RequestData(Nearable nearable, Location loc) {
        UUID = nearable.beaconRegion.getProximityUUID().toString();
        if(loc != null){
            latitude = "" + loc.getLatitude();
            longtitude = "" + loc.getLongitude();
        }
    }

    public RequestData(FixRecord rec) {
        dts = rec.mDTS;
        UUID = rec.mUUID;
        latitude = rec.mLat;
        longtitude = rec.mLon;
    }

    public String getUUID(){
        return UUID;
    }

    public String getLatitude(){
        return latitude;
    }

    public String getLongtitude(){
        return longtitude;
    }

    public long getDts(){
        return dts;
    }
}

