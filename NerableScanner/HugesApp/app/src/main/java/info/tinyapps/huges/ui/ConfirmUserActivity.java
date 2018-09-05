package info.tinyapps.huges.ui;

import android.os.Bundle;
import android.view.View;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;

import info.tinyapps.huges.R;

/**
 * activity to confirm new cognoto user user
 */
public class ConfirmUserActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_user);

        findViewById(R.id.btnConfirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = getFieldTxt(R.id.fldUser,4);
                if(userName == null)
                    return;

                String code = getFieldTxt(R.id.fldCode,4);
                if(code == null)
                    return;

                showWait("please wait");
                GenericHandler confirmationCallback = new GenericHandler() {
                    @Override
                    public void onSuccess() {
                        finish();
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        hideWait();
                        showErr(exception);
                    }
                };

                try{
                    CognitoUser user = getUserPool().getUser(userName);
                    if(user == null)
                        throw new Exception("No such user?");

                    user.confirmSignUpInBackground(code,false,confirmationCallback);
                }
                catch (Exception err){
                    hideWait();
                    showErr(err);
                }
            }
        });

        setTitle2(R.string.ConfirmUser);
    }
}

