package info.tinyapps.huges.ui;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.DatePicker;
import android.widget.RadioButton;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import java.util.Calendar;
import info.tinyapps.huges.R;
import info.tinyapps.huges.services.StaticConfig;
import info.tinyapps.huges.utils.Utils;

public class RegisterActivityOne extends BaseActivity {
    String mBeaconID;

    public static final int REQ_REG2 = 11;
    public static final int RESULT_NOT_NOW = RESULT_FIRST_USER + 1;

    public static final String TAG_BEACON_ID = "TAG_BEACON_ID";
    public static final String TAG_GENDER = "TAG_GENDER";
    public static final String TAG_BIRTHDATE = "TAG_BIRTHDATE";
    public static final String TAG_NAME = "TAG_NAME";
    public static final String TAG_FAMILY = "TAG_FAMILY";
    public static final String TAG_ADDRESS1 = "TAG_ADDRESS1";
    public static final String TAG_ADDRESS2 = "TAG_ADDRESS2";
    public static final String TAG_ADDRESS3 = "TAG_ADDRESS3";
    public static final String TAG_COUNTRY = "TAG_COUNTRY";
    public static final String TAG_POSTCODE = "TAG_POSTCODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        mBeaconID = getIntent().getStringExtra(TAG_BEACON_ID);
        findViewById(R.id.fldDOB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Utils.getCalendar(getFieldTxt(R.id.fldDOB));
                new DatePickerDialog(getThis(), mDateListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        findViewById(R.id.btnSingUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    if(!validateInput())
                        return;

                    String geneder = "m";
                    if( !((RadioButton)findViewById(R.id.rbMale)).isChecked())
                        geneder = "f";

                    Intent intent = new Intent(getThis(),RegisterActivityTwo.class);
                    intent.putExtra(TAG_NAME,getFieldTxt(R.id.fldGivenName));
                    intent.putExtra(TAG_ADDRESS1,getFieldTxt(R.id.fldAddress1));
                    intent.putExtra(TAG_ADDRESS2,getFieldTxt(R.id.fldAddress2));
                    intent.putExtra(TAG_ADDRESS3,getFieldTxt(R.id.fldAddress3));
                    intent.putExtra(TAG_COUNTRY,getFieldTxt(R.id.fldCountry));
                    intent.putExtra(TAG_POSTCODE,getFieldTxt(R.id.fldPostCode));

                    intent.putExtra(TAG_BEACON_ID,mBeaconID);
                    intent.putExtra(TAG_GENDER,geneder);
                    intent.putExtra(TAG_BIRTHDATE,getFieldTxt(R.id.fldDOB));
                    intent.putExtra(TAG_FAMILY,getFieldTxt(R.id.fldFamiliyName));

                    startActivityForResult(intent,REQ_REG2);
                }
                catch (Exception e){
                    showErr("",e);
                }
            }
        });

        setTitle2(R.string.SignUp);
    }

    DatePickerDialog.OnDateSetListener mDateListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear,int dayOfMonth) {
            String dob = Utils.getDOB(year, monthOfYear,dayOfMonth);
            setFieldText(R.id.fldDOB,dob);
        }
    };

    boolean validateInput(){
        if(getFieldTxt(R.id.fldAddress1,2) == null)
            return false;

        if(getFieldTxt(R.id.fldAddress2,2) == null)
            return false;

        if(getFieldTxt(R.id.fldCountry,2) == null)
            return false;

        if(getFieldTxt(R.id.fldPostCode,2) == null)
            return false;

        if(getFieldTxt(R.id.fldDOB,4) == null)
            return false;

        if(getFieldTxt(R.id.fldGivenName,4) == null)
            return false;

        if(getFieldTxt(R.id.fldFamiliyName,4) == null)
            return false;

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQ_REG2:
                if(resultCode == RESULT_OK || resultCode == RESULT_NOT_NOW){
                    setResult(resultCode);
                    finish();
                }
            break;
        }
    }
}

