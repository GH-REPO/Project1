package info.tinyapps.huges.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.NewPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GetDetailsHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.tokens.CognitoIdToken;
import info.tinyapps.huges.BuildConfig;
import info.tinyapps.huges.R;
import info.tinyapps.huges.services.StaticConfig;
import info.tinyapps.huges.utils.AppSettings;

public class LoginActivity extends BaseActivity {
    private static final int REQ_REQGISTARTION = 101;
    private static final int REQ_GETBEACON = 103;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if(BuildConfig.DEBUG){
            //setFieldText(R.id.fldUser,"user_test2");
            //setFieldText(R.id.fldPass,"User_Test2");
            setFieldText(R.id.fldUser,"ssmmailinator");
            setFieldText(R.id.fldPass,"SSss11,,");
        }

        findViewById(R.id.btnLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = getFieldTxt(R.id.fldUser,4);
                if(user == null)
                    return;
                String password = getFieldTxt(R.id.fldPass,4);
                if(password == null)
                    return;

                showWait("please wait");
                getUserPool().getUser(user).getSessionInBackground(mAuthHandler);
            }
        });

        findViewById(R.id.btnRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setBeacon();
            }
        });

        findViewById(R.id.btnForgotPass).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doPasswordReset();
            }
        });

        findViewById(R.id.btnConfirmUser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getThis(),ConfirmUserActivity.class));
            }
        });

        if(new AppSettings(getThis()).getJWTToken() != null){
            startActivity(new Intent(getThis(), MainActivity.class));
            finish();
        }

        setTitle2(R.string.LogIn);
    }

    AuthenticationHandler mAuthHandler = new AuthenticationHandler() {
        public void authenticationChallenge(ChallengeContinuation continuation){
            if ("NEW_PASSWORD_REQUIRED".equals(continuation.getChallengeName())) {
                hideWait();
                setNewPassword(continuation,null,null);
            }
            else {
                hideWait();
                showErr("App requires extra auth!", null);
            }
        }

        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {
            onUserLogged(userSession);
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            String password = getFieldTxt(R.id.fldPass);
            AuthenticationDetails authenticationDetails = new AuthenticationDetails(userId, password, null);
            authenticationContinuation.setAuthenticationDetails(authenticationDetails);
            authenticationContinuation.continueTask();
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
            hideWait();
            showErr("Multi-factor authentication is required!",null);
        }

        @Override
        public void onFailure(Exception exception) {
            hideWait();
            showErr(exception);
        }
    };

    boolean hasPermissions(){
        final String [] permissions = new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for(String permission : permissions){
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions, PERMISSION_REQUEST_COARSE_LOCATION);
                    return false;
                }
            }
        }

        return true;
    }

    public void onResume(){
        super.onResume();
        hasPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                boolean deniedPermission = false;

                for(int i = 0; i < permissions.length;i++){
                    if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                        deniedPermission = true;
                        break;
                    }
                }

                if (!deniedPermission) {

                }
                else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    void setNewPassword(final ChallengeContinuation continuation, String err1,String err2){
        AlertDialog.Builder builder = getAlertDialogBuilder();
            View dialogView = getLayoutInflater().inflate(R.layout.dlg_new_password, null);

        builder.setView(dialogView);

        builder.setTitle("New password is required");
        final EditText fldPass = (EditText) dialogView.findViewById(R.id.fldPass);
        final EditText fldPass2 = (EditText) dialogView.findViewById(R.id.fldPass2);

        if(err1 != null)
            fldPass.setError(err1);

        if(err2 != null)
            fldPass2.setError(err2);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try{
                    String pass = fldPass.getText().toString();
                    String pass2 = fldPass2.getText().toString();

                    if(pass.length() < 8){
                        setNewPassword(continuation, "Please provide",null);
                        return;
                    }
                    if(pass2.length() < 8){
                        setNewPassword(continuation, null,"Please provide");
                        return;
                    }

                    if(pass.equals(pass2)){
                        setNewPassword(continuation, null,"doesnt match");
                        return;
                    }

                    NewPasswordContinuation newPasswordContinuation = (NewPasswordContinuation) continuation;
                    newPasswordContinuation.setPassword(pass);

                    showWait("Please wait");
                    continuation.continueTask();
                }
                catch (Exception err){
                    showErr(err);
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showWait("Please wait");
                continuation.continueTask();
            }
        });

        builder.show();
    }

    void doPasswordReset(){
        String userName = getFieldTxt(R.id.fldUser,4);
        if(userName == null)
            return;

        ForgotPasswordHandler newPassHandler = new ForgotPasswordHandler() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void getResetCode(ForgotPasswordContinuation continuation) {
                resetPassword(continuation, null,null,null);
            }

            /**
             * This is called for all fatal errors encountered during the password reset process
             * Probe {@exception} for cause of this failure.
             * @param exception
             */
            public void onFailure(Exception exception) {
                showErr(exception);
            }
        };

        CognitoUser user = getUserPool().getUser(userName);
        if(user != null) {
            showWait("Please wait");
            user.forgotPasswordInBackground(newPassHandler);
        }
    }

    void resetPassword(final ForgotPasswordContinuation continuation, String err1,String err2,String err3){
        AlertDialog.Builder builder = getAlertDialogBuilder();
        View dialogView = getLayoutInflater().inflate(R.layout.dlg_password_reset, null);

        builder.setView(dialogView);
        builder.setTitle("Password reset");

        final EditText fldCode = (EditText) dialogView.findViewById(R.id.fldCode);
        final EditText fldPass = (EditText) dialogView.findViewById(R.id.fldPass);
        final EditText fldPass2 = (EditText) dialogView.findViewById(R.id.fldPass2);

        if(err1 != null)
            fldCode.setError(err1);

        if(err2 != null)
            fldPass.setError(err2);

        if(err3 != null)
            fldPass2.setError(err3);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try{
                    String code = fldCode.getText().toString();
                    String pass = fldPass.getText().toString();
                    String pass2 = fldPass2.getText().toString();

                    if(code.length() < 6){
                        resetPassword(continuation,"Please provide",null,null);
                        return;
                    }

                    if(pass.length() < 8){
                        resetPassword(continuation, null,"Please provide",null);
                        return;
                    }

                    if(pass2.length() < 8){
                        resetPassword(continuation, null,null,"Please provide");
                        return;
                    }

                    if(!pass.equals(pass2)){
                        resetPassword(continuation, null,null,"doesnt match");
                        return;
                    }


                    CognitoUserCodeDeliveryDetails details =  continuation.getParameters();
                    continuation.setPassword(pass);
                    continuation.setVerificationCode(code);
                    showWait("Please wait");
                    continuation.continueTask();
                }
                catch (Exception err){
                    showErr(err);
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        builder.show();
    }

    void onUserLogged(final CognitoUserSession userSession){
        GetDetailsHandler detailsHandler =new GetDetailsHandler() {
            @Override
            public void onSuccess(CognitoUserDetails cognitoUserDetails) {
                try {
                    hideWait();
                    CognitoIdToken token = userSession.getIdToken();

                    String beaconID = cognitoUserDetails.getAttributes().getAttributes().get(StaticConfig.ATTR_BEACON_ID);
                    AppSettings settings = new AppSettings(getThis());
                    settings.setJWTToken(token.getJWTToken(), token.getExpiration().getTime());
                    settings.setUserName(getFieldTxt(R.id.fldUser));
                    settings.setUserPass(getFieldTxt(R.id.fldPass));
                    settings.setBeaconID(beaconID);
                    startActivity(new Intent(getThis(), MainActivity.class));

                    finish();
                }
                catch (Exception e){
                    showErr(e);
                }
            }

            @Override
            public void onFailure(Exception exception) {
                hideWait();
                showErr(exception);
            }
        };

        getUserPool().getUser(userSession.getUsername()).getDetailsInBackground(detailsHandler);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQ_REQGISTARTION:
                if(resultCode != RESULT_OK) {
                    return;
                }
                else {
                    startActivity(new Intent(getThis(),ConfirmUserActivity.class));
                }
            break;
            case REQ_GETBEACON:
                if(resultCode == RESULT_OK && data != null){
                    String beacon = data.getStringExtra(SelectBeaconActivity.TAG_SELECTED_BEACON);
                    if(beacon != null){
                        Intent intent = new Intent(getThis(),RegisterActivityOne.class);
                        intent.putExtra(RegisterActivityOne.TAG_BEACON_ID,beacon);
                        startActivityForResult(intent,REQ_REQGISTARTION);
                    }
                }
            break;
        }
    }

    //add new beacon pick up here
    void setBeacon(){
        Intent intent = new Intent(getThis(),SelectBeaconActivity.class);
        intent.putExtra(SelectBeaconActivity.TAG_DONOT_SAVE,true);
        startActivityForResult(intent,REQ_GETBEACON);
    }
}

