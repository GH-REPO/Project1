package info.tinyapps.huges.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;

/**
 * local DB helper where records/fixes are stored before being reported
 * to the server
 */
public class LocalDB extends SQLiteOpenHelper {
//system required stuff to make a db
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "local_erecs.db";

    public static abstract class BeaconRec implements BaseColumns {
        public static final String TABLE_NAME = "tlb_beacon_recs";
        public static final String FLD_DTS = "dts";
        public static final String FLD_BEACON_UID = "beacon_uuid";
        public static final String FLD_LAT = "lat";
        public static final String FLD_LON = "lon";
    }

    private static final String SQL_CREATE_BEACONS_TABLE =
        "CREATE TABLE " + BeaconRec.TABLE_NAME + " (" +
        BeaconRec._ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        BeaconRec.FLD_DTS + " INTEGER NOT NULL , " +
        BeaconRec.FLD_BEACON_UID + " TEXT NOT NULL , " +
        BeaconRec.FLD_LAT + " TEXT , " +
        BeaconRec.FLD_LON + " TEXT  )";


    private static final String SQL_DELETE_BEACONS_TABLE = "DROP TABLE IF EXISTS " + BeaconRec.TABLE_NAME;

    public LocalDB(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try{
            db.execSQL(SQL_CREATE_BEACONS_TABLE);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_BEACONS_TABLE);
        onCreate(db);
    }
//end of system stuff

    //save new fix
    public static long addFix(Context ctx,String nearableUUID, String lat,String lon){
        long RecID = -1;

        SQLiteDatabase db = null;

        try{
            db = new LocalDB(ctx).getWritableDatabase();

            ContentValues values = new ContentValues();

            values.put(BeaconRec.FLD_DTS,System.currentTimeMillis());
            values.put(BeaconRec.FLD_BEACON_UID,nearableUUID);
            values.put(BeaconRec.FLD_LAT,lat);
            values.put(BeaconRec.FLD_LON,lon);

            RecID = db.insert(BeaconRec.TABLE_NAME,null,values);
            db.close();
            db = null;

            return RecID;
        }
        catch (Exception e){
            try{
                if(db != null)
                    db.close();
            }
            catch (Exception ee){
            }
        }

        return RecID;
    }

    //get all fixes
    public static ArrayList<FixRecord> getRecords(Context ctx) throws Exception{
        FixRecord rec;
        ArrayList<FixRecord> res = new ArrayList<>();

        SQLiteDatabase db = null;

        try{
            db = new LocalDB(ctx).getReadableDatabase();
            String [] projection = {
                    BeaconRec._ID,BeaconRec.FLD_DTS,BeaconRec.FLD_BEACON_UID,BeaconRec.FLD_LAT,BeaconRec.FLD_LON};

            Cursor cursor = db.query(BeaconRec.TABLE_NAME,projection,null,null,null,null,null);

            while(cursor.moveToNext()){
                rec = new FixRecord();
                rec.mRecID = cursor.getLong(0);
                rec.mDTS = cursor.getLong(1);
                rec.mUUID = cursor.getString(2);
                rec.mLat = cursor.getString(3);
                rec.mLon = cursor.getString(4);

                res.add(rec);
            }

            cursor.close();
            db.close();

            return res;
        }
        catch (Exception e){
            try{
                if(db != null)
                    db.close();
            }
            catch (Exception ee){
            }

            throw e;
        }
    }

    //delete a fix
    public static long deleteFix(Context ctx, long recID){
        long res = -1;
        SQLiteDatabase db = null;

        try{
            db = new LocalDB(ctx).getWritableDatabase();
            String selection = BeaconRec._ID + " = ?";
            String [] selectionArgs = {"" + recID};

            res = db.delete(BeaconRec.TABLE_NAME,selection,selectionArgs);
            db.close();

            return res;
        }
        catch (Exception e){
            return -1;
        }
    }
}

