package io.particle.cloudsdk.silver_v2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.common.base.Function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

public class ValueActivity extends AppCompatActivity {

    private static final String ARG_VALUE = "ARG_VALUE";
    private static final String ARG_DEVICEID = "ARG_DEVICEID";

    private static final Handler handler = new Handler();

    private TextView tv;

    UIUpdater mUIUpdater = new UIUpdater(new Runnable() {
        @Override
        public void run() {
            Async.executeAsync(ParticleCloud.get(ValueActivity.this), new Async.ApiWork<ParticleCloud, Object>() {
                @Override
                public Object callApi(ParticleCloud ParticleCloud) throws ParticleCloudException, IOException {
                    ParticleDevice device = ParticleCloud.getDevice(getIntent().getStringExtra(ARG_DEVICEID));
                    Object variable;

                    // get variables this way
                    try {
                        variable = device.getVariable("testvar");
                    } catch (ParticleDevice.VariableDoesNotExistException e) {
                        Toaster.l(ValueActivity.this, e.getMessage());
                        variable = -1;
                    }
                    // is Object i that is returned to onSuccess.
                    // to return multiple variables, either return a list, or initialize variables in the ValueActivity class and have the callApi method
                    // set the values for those variables
                    return variable;
                }

                @Override
                public void onSuccess(Object i) { // this goes on the main thread
                    tv.setText(i.toString());
                }

                @Override
                public void onFailure(ParticleCloudException e) {
                    e.printStackTrace();
                }
            });
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_value);
        tv = (TextView) findViewById(R.id.value);
        tv.setText(String.valueOf(getIntent().getIntExtra(ARG_VALUE, 0)));

        findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //override and do your own update
                Async.executeAsync(ParticleCloud.get(ValueActivity.this), new Async.ApiWork<ParticleCloud, Object>() {
                    @Override
                    public Object callApi(ParticleCloud ParticleCloud) throws ParticleCloudException, IOException {
                        ParticleDevice device = ParticleCloud.getDevice(getIntent().getStringExtra(ARG_DEVICEID));
                        Object variable;
                        List<String> ledCommand = new ArrayList<String>();
                        ledCommand.add("on");
                        // get variables this way
                        try {
                            device.callFunction("led", ledCommand);
                        }
                        catch(ParticleDevice.FunctionDoesNotExistException e){

                        }
                        // is Object i that is returned to onSuccess.
                        // to return multiple variables, either return a list, or initialize variables in the ValueActivity class and have the callApi method
                        // set the values for those variables
                        return -1;
                    }

                    @Override
                    public void onSuccess(Object i) { // this goes on the main thread
                        tv.setText("Switching");
                    }

                    @Override
                    public void onFailure(ParticleCloudException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        findViewById(R.id.off_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //override and do your own update
                Async.executeAsync(ParticleCloud.get(ValueActivity.this), new Async.ApiWork<ParticleCloud, Object>() {
                    @Override
                    public Object callApi(ParticleCloud ParticleCloud) throws ParticleCloudException, IOException {
                        ParticleDevice device = ParticleCloud.getDevice(getIntent().getStringExtra(ARG_DEVICEID));
                        Object variable;
                        List<String> ledCommand = new ArrayList<String>();
                        ledCommand.add("off");
                        // get variables this way
                        try {
                            device.callFunction("led", ledCommand);
                        }
                        catch(ParticleDevice.FunctionDoesNotExistException e){

                        }
                            // is Object i that is returned to onSuccess.
                        // to return multiple variables, either return a list, or initialize variables in the ValueActivity class and have the callApi method
                        // set the values for those variables
                        return -1;
                    }

                    @Override
                    public void onSuccess(Object i) { // this goes on the main thread
                        tv.setText("Switching");
                    }

                    @Override
                    public void onFailure(ParticleCloudException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        //do the running loop
        mUIUpdater.startUpdates();
    }

    @Override
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


            final SharedPreferences sharedPref = ValueActivity.this.getSharedPreferences(ValueActivity.this.getString(R.string.app_name), Context.MODE_PRIVATE);
            sharedPref.edit().clear().commit();

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static Intent buildIntent(Context ctx, String value, String deviceid) {
        Intent intent = new Intent(ctx, ValueActivity.class);
        intent.putExtra(ARG_VALUE, value);
        intent.putExtra(ARG_DEVICEID, deviceid);

        return intent;
    }

    /**
     * A class used to perform periodical updates,
     * specified inside a runnable object. An update interval
     * may be specified (otherwise, the class will perform the
     * update every 2 seconds).
     *
     * @author Carlos Simões
     */
    public class UIUpdater {
        // Create a Handler that uses the Main Looper to run in
        private Handler mHandler = new Handler(Looper.getMainLooper());
        public Runnable initializer;
        private Runnable mStatusChecker;
        private int UPDATE_INTERVAL = 2000;

        /**
         * Creates an UIUpdater object, that can be used to
         * perform UIUpdates on a specified time interval.
         *
         * @param uiUpdater A runnable containing the update routine.
         */
        public UIUpdater(final Runnable uiUpdater) {
            initializer = uiUpdater;
            mStatusChecker = new Runnable() {
                @Override
                public void run() {
                    // Run the passed runnable
                    uiUpdater.run();
                    // Re-run it after the update interval
                    mHandler.postDelayed(this, UPDATE_INTERVAL);
                }
            };
        }

        /**
         * The same as the default constructor, but specifying the
         * intended update interval.
         *
         * @param uiUpdater A runnable containing the update routine.
         * @param interval  The interval over which the routine
         *                  should run (milliseconds).
         */
        public UIUpdater(Runnable uiUpdater, int interval){
            this(uiUpdater);
            UPDATE_INTERVAL = interval;
        }

        /**
         * Starts the periodical update routine (mStatusChecker
         * adds the callback to the handler).
         */
        public synchronized void startUpdates(){
            mStatusChecker.run();
        }


        /**
         * Stops the periodical update routine from running,
         * by removing the callback.
         */
        public synchronized void stopUpdates(){
            mHandler.removeCallbacks(mStatusChecker);
        }
    }



}
