package io.particle.cloudsdk.silver_v2;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.ParticleEvent;
import io.particle.android.sdk.cloud.ParticleEventHandler;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class LoginActivity extends AppCompatActivity {


    private static final String EMAIL = "EMAIL";
    private static final String PASSWORD = "PASSWORD";

    final SharedPreferences sharedPref = LoginActivity.this.getSharedPreferences(LoginActivity.this.getString(R.string.app_name), Context.MODE_PRIVATE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Make it so when login is clicked, it does stuff
        findViewById(R.id.login_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {

                        // read the email and password from the view ids
                        final String email = ((EditText) findViewById(R.id.email)).getText().toString();
                        final String password = ((EditText) findViewById(R.id.password)).getText().toString();

                        // save them in shared preferences for our app (private location)
                        final SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(EMAIL,email);
                        editor.putString(PASSWORD,password);
                        editor.commit();

                        // do the login
                        login(v.getContext(), email, password);
                    }
                }
        );

    }

    // Create intent to go from the Context ctx to the Context LoginActivity
    public static Intent buildIntent(Context ctx) {
        Intent intent = new Intent(ctx, LoginActivity.class);
        return intent;
    }

    void login(final Context ctx, final String email, final String password){

        // Start an Async message to interface with the network
        Async.executeAsync(ParticleCloud.get(ctx), new Async.ApiWork<ParticleCloud, Object>() {

            //Initialize variables
            private ParticleDevice mDevice;
            private List<ParticleDevice> mDevices = new ArrayList<ParticleDevice>();

            // Call the API to do particle functions
            @Override
            public Object callApi(ParticleCloud sparkCloud) throws ParticleCloudException, IOException {
                // login
                sparkCloud.logIn(email, password);

                // get the device for floe. Doesn't currently work if you have multiple boards
                mDevices = sparkCloud.getDevices();
                boolean validDevice = false;
                for (int i=0; i < mDevices.size(); i++){
                    mDevice = mDevices.get(i);
                    try{
                        String strVariable = mDevice.getStringVariable("deviceID");
                        if (strVariable == "Floe"){
                            validDevice = true;
                            break;
                        }
                    }
                    catch(ParticleDevice.VariableDoesNotExistException e) {

                    }
                }
                //if (!validDevice){
                //    throw ParticleCloudException();
                //}

                return -1;
            }

            @Override
            public void onSuccess(Object value) {
                //make a screen notification letting you know you are logged in
                Toaster.l(LoginActivity.this, "Logged in");
                //Go to value activity
                Intent intent = ValueActivity.buildIntent(LoginActivity.this, "Waiting on data", mDevice.getID());
                startActivity(intent);
            }

            @Override
            public void onFailure(ParticleCloudException e) {
                //Lets you know login failure info
                Toaster.l(LoginActivity.this, e.getBestMessage());
                e.printStackTrace();
                Log.d("info", e.getBestMessage());
            }
        });
    }
}
