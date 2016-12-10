package io.particle.cloudsdk.silver_v2;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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


public class SplashActivity extends AppCompatActivity {

    private static final String EMAIL = "EMAIL";
    private static final String PASSWORD = "PASSWORD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        final SharedPreferences sharedPref = SplashActivity.this.getSharedPreferences(SplashActivity.this.getString(R.string.app_name), Context.MODE_PRIVATE);
        final String username = sharedPref.getString(EMAIL, "");
        final String password = sharedPref.getString(PASSWORD, "");

        // Login to the particle cloud
        Async.executeAsync(ParticleCloud.get(SplashActivity.this), new Async.ApiWork<ParticleCloud, Object>() {

            private ParticleDevice mDevice;
            private List<ParticleDevice> mDevices = new ArrayList<ParticleDevice>();

            @Override
            public Object callApi(ParticleCloud sparkCloud) throws ParticleCloudException, IOException {
                sparkCloud.logIn(username, password);

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

                // do the subscription
                sparkCloud.subscribeToMyDevicesEvents(
                        null,  // the first argument, "eventNamePrefix", is optional
                        new ParticleEventHandler() {
                            public void onEvent(String eventName, ParticleEvent event) {
                                    NotificationCompat.Builder mBuilder =
                                            new NotificationCompat.Builder(SplashActivity.this)
                                                    .setSmallIcon(R.mipmap.ic_launcher)
                                                    .setContentTitle("Floe")
                                                    .setContentText(event.dataPayload);
                                    Intent resultIntent = new Intent(SplashActivity.this, ValueActivity.class);
                                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(SplashActivity.this);
                                    stackBuilder.addParentStack(ValueActivity.class);
                                    // Adds the Intent that starts the Activity to the top of the stack
                                    stackBuilder.addNextIntent(resultIntent);
                                    PendingIntent resultPendingIntent =
                                            stackBuilder.getPendingIntent(
                                                    0,
                                                    PendingIntent.FLAG_UPDATE_CURRENT
                                            );
                                    mBuilder.setContentIntent(resultPendingIntent);
                                    NotificationManager mNotificationManager =
                                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                    // mId allows you to update the notification later on.

                                    int mId = sharedPref.getInt("notification", 0);
                                    mId += 1;
                                    sharedPref.edit().putInt("notification", mId).commit();
                                    mNotificationManager.notify(mId, mBuilder.build());
                                    Log.i("some tag", "Received event with payload: " + event.dataPayload);

                            }

                            public void onEventError(Exception e) {
                                Log.e("some tag", "Event error: ", e);
                            }
                        });

                    Object value;
                    try{
                        value = mDevice.getVariable("testvar");
                    } catch (ParticleDevice.VariableDoesNotExistException e) {
                        value = 0;
                    }
                        //if (!validDevice){
                //    throw ParticleCloudException();
                //}

                return value;
            }

            @Override
            public void onSuccess(Object value) {

                Intent intent = ValueActivity.buildIntent(SplashActivity.this, value.toString(), mDevice.getID());
                startActivity(intent);
            }

            @Override
            public void onFailure(ParticleCloudException e) {
                Intent intent = LoginActivity.buildIntent(SplashActivity.this);
                startActivity(intent);
            }
        });

    }


    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_login) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
