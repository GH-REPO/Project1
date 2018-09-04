package info.tinyapps.huges.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProvider;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import info.tinyapps.huges.R;
import info.tinyapps.huges.utils.BaseLogging;

public class BaseActivity extends AppCompatActivity {
    public static final String USER_POOL_ID = "eu-west-1_8U1atIXea";
    public static final String ID_POOL_ID = "eu-west-1:5af1e331-fb34-4772-a49a-6f2e30e1699d";
    public static final String CLIENT_ID = "1ou5ftrsbocath9v2t9uq1dcf9";
    public static final String CLIENT_SECRET = "plvagd268khqijtt0f5bero6c81225orseaf1ro4fe69jj7shqq";
    public static final Regions CLIENT_REGION = Regions.EU_WEST_1;
    public static final String POOL_ARN = "cognito-idp.eu-west-1.amazonaws.com/eu-west-1_8U1atIXea";
/*
IdentityPool 'eu-west-1:19920b7d-dfc7-4d1e-b1ea-d99a23f711f6' not found.
    public static final String USER_POOL_ID = "eu-west-1_cE32SzhDi";
    public static final String ID_POOL_ID = "eu-west-1:47203615-c0c9-4ff1-8838-e9e3eb8718ab";
    public static final String CLIENT_ID = "3dddjosojiji9vr16r7tfh47ec";
    public static final String CLIENT_SECRET = "1s4riqh4uoco9faa2g0d4ucqh57tdhkctor4sp1utd5j0oh5rlbn";
    public static final Regions CLIENT_REGION = Regions.EU_WEST_1;
    public static final String POOL_ARN = "cognito-idp.eu-west-1.amazonaws.com/eu-west-1_cE32SzhDi";
*/
    protected ProgressDialog mWaitDlg;

    public static String getErrDetails(Exception err) {
        return err.getMessage();
    }

    public void showErr(Exception err) {
        showErr("Error occurred", err);
    }

    public void showErrToast(Exception err) {
        String err_txt = "";

        if (err != null) {
            err_txt = getErrDetails(err);
            addLog(err_txt, err);
        }

        showErrToast(err_txt);
    }

    public void showErrToast(String err) {
        Toast.makeText(getThis(), err, Toast.LENGTH_LONG).show();
    }

    public void showErr(String txt, Exception err) {
        String err_txt = "";

        if (err != null) {
            err_txt = getErrDetails(err);
            addLog(err_txt, err);
        }

        AlertDialog.Builder builder = getAlertDialogBuilder();
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(txt + "\n" + err_txt);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.cancel();
            }
        });

        builder.create().show();
    }

    public BaseActivity getThis() {
        return this;
    }

    protected AlertDialog.Builder getAlertDialogBuilder(){
        return new AlertDialog.Builder(getThis(), R.style.MyAlertDialogStyle);
    }

    public String getFieldTxt(int field_id, int min){
        String txt = getFieldTxt(field_id);
        if(txt == null || txt.length() < min) {
            setError(field_id);
            return null;
        }

        return txt;
    }

    public String getFieldTxt(int field_id){
        View view = findViewById(field_id);
        if(view == null)
            return null;

        if(view instanceof Button)
            return ((Button)view).getText().toString();
        else if(view instanceof EditText)
            return ((EditText)view).getText().toString();
        else if(view instanceof TextView)
            return ((TextView)view).getText().toString();

        return null;
    }

    public void setFieldText(int field_id, int res_id){
        String txt = getString(res_id);
        setFieldText(field_id, txt);
    }

    public void setFieldText(int field_id, String txt){
        View view = findViewById(field_id);
        if(view == null)
            return;

        if(view instanceof Button)
            ((Button)view).setText(txt);
        else if(view instanceof EditText)
            ((EditText)view).setText(txt);
        else if(view instanceof TextView)
            ((TextView)view).setText(txt);

        return;
    }

    public void setError(int field_id, String msg) {
        View view = findViewById(field_id);
        if (view == null)
            return;

        if (view instanceof EditText) {
            ((EditText) view).setError(msg);
        }
        else if (view instanceof Spinner){
            ((TextView)((Spinner)view).getSelectedView()).setError(msg);
        }

        view.requestFocus();
    }

    public void setError(int field_id){
        setError(field_id,"Please provide");
    }

    public static CognitoUserPool getUserPool(Context ctx){
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        AmazonCognitoIdentityProvider cipClient = new AmazonCognitoIdentityProviderClient(new AnonymousAWSCredentials(),clientConfiguration);

        cipClient.setRegion(Region.getRegion(CLIENT_REGION));
        CognitoUserPool pool = new CognitoUserPool(ctx, USER_POOL_ID,CLIENT_ID,CLIENT_SECRET,new ClientConfiguration(),CLIENT_REGION);

        return pool;
    }

    public CognitoUserPool getUserPool(){
        return getUserPool(getThis());
    }

    public void addLog(String txt, Exception e) {
        BaseLogging.addLog(txt, e);
    }

    public void addLog(String txt) {
        BaseLogging.addLog(txt);
    }

    protected void showWait(String message){
        try {
            mWaitDlg = ProgressDialog.show(getThis(), null, message, true, false);
            mWaitDlg.setCanceledOnTouchOutside(false);
        }
        catch (Exception e) {
        }
    }

    protected void hideWait(){
        try {
            mWaitDlg.dismiss();
        }
        catch (Exception e) {
        }
    }

    protected void setTitle2(String title){
        setFieldText(R.id.toolBarTitle,title);
    }

    protected void setTitle2(int title){
        setFieldText(R.id.toolBarTitle,title);
    }
}

