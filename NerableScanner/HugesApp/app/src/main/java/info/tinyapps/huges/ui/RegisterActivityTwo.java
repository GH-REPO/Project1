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

public class RegisterActivityTwo extends BaseActivity {
    String mBeaconID,mName,mFamily,mGender,mDOB;
    String mAdr1,mAdr2,mAdr3,mCountry,mPostCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register_two);

        Intent intent = getIntent();

        mBeaconID = intent.getStringExtra(RegisterActivityOne.TAG_BEACON_ID);
        mName = intent.getStringExtra(RegisterActivityOne.TAG_NAME);
        mAdr1 = intent.getStringExtra(RegisterActivityOne.TAG_ADDRESS1);
        mAdr2 = intent.getStringExtra(RegisterActivityOne.TAG_ADDRESS2);
        mAdr3 = intent.getStringExtra(RegisterActivityOne.TAG_ADDRESS3);
        mCountry = intent.getStringExtra(RegisterActivityOne.TAG_COUNTRY);
        mPostCode = intent.getStringExtra(RegisterActivityOne.TAG_POSTCODE);

        mDOB = intent.getStringExtra(RegisterActivityOne.TAG_BIRTHDATE);
        mFamily = intent.getStringExtra(RegisterActivityOne.TAG_FAMILY);
        mGender = intent.getStringExtra(RegisterActivityOne.TAG_GENDER);

        findViewById(R.id.btnSingUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    if(!validateInput())
                        return;

                    showWait("please wait");
                    doRegister();
                }
                catch (Exception e){
                    showErr("",e);
                }
            }
        });

        setTitle2(R.string.SignUp);
    }

    boolean validateInput(){
        if(getFieldTxt(R.id.fldUser,4) == null)
            return false;

        String pass = getFieldTxt(R.id.fldPass,8);
        String pass2 = getFieldTxt(R.id.fldPass2,8);

        if(pass == null || pass2 == null)
            return false;

        if(!pass.equals(pass2)){
            setError(R.id.fldPass2,"Password does't match");
            return false;
        }

        return true;
    }

    String formatAddress(){
        //Address 1, Address 2, Address 3, Country, Postcode
        StringBuilder res = new StringBuilder();

        res.append(mAdr1);
        res.append(",");
        res.append(mAdr2);
        res.append(",");
        res.append(mAdr3);
        res.append(",");
        res.append(mCountry);
        res.append(",");
        res.append(mPostCode);

        return res.toString();
    }

    void doRegister(){
        CognitoUserPool userPool = getUserPool();
        CognitoUserAttributes userAttributes = new CognitoUserAttributes();

        userAttributes.addAttribute("email", getFieldTxt(R.id.fldEmail));
        userAttributes.addAttribute("address",formatAddress());
        userAttributes.addAttribute("birthdate",mDOB);
        userAttributes.addAttribute("family_name",mFamily);
        userAttributes.addAttribute("gender",mGender);
        userAttributes.addAttribute("given_name",mName);
        userAttributes.addAttribute(StaticConfig.ATTR_BEACON_ID,mBeaconID);

        SignUpHandler signupCallback = new SignUpHandler() {
            @Override
            public void onSuccess(CognitoUser cognitoUser, boolean userConfirmed, CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
                hideWait();

                try {
                    AlertDialog.Builder builder = getAlertDialogBuilder();

                    builder.setTitle("Registration");
                    // Check if this user (cognitoUser) needs to be confirmed
                    if (!userConfirmed) {
                        StringBuilder msg = new StringBuilder();
                        msg.append("Confirmation code sent view\n");
                        msg.append(cognitoUserCodeDeliveryDetails.getDeliveryMedium());
                        msg.append("\nto ");
                        msg.append(cognitoUserCodeDeliveryDetails.getDestination());

                        builder.setMessage(msg.toString());
                        builder.setPositiveButton("Confirm User", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setResult(RESULT_OK);
                                finish();
                            }
                        });

                        builder.setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setResult(RegisterActivityOne.RESULT_NOT_NOW);
                                finish();
                            }
                        });
                    }
                    else {
                        builder.setMessage("Your registration is confirmed!You can now go to login screen");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        });
                    }

                    builder.show();
                }
                catch (Exception e){
                    showErr(e);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                addLog("",exception);
                hideWait();

                showErr(exception);
            }
        };

        userPool.signUpInBackground(getFieldTxt(R.id.fldUser), getFieldTxt(R.id.fldPass), userAttributes, null, signupCallback);
    }
}


