package uk.co.borconi.emil.obd2aa.services;


import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.UI_MODE_SERVICE;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.prowl.torque.remote.ITorqueService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import uk.co.borconi.emil.obd2aa.R;
import uk.co.borconi.emil.obd2aa.androidauto.OBD2AA;
import uk.co.borconi.emil.obd2aa.gauge.GaugeUpdate;
import uk.co.borconi.emil.obd2aa.helpers.PreferencesHelper;
import uk.co.borconi.emil.obd2aa.helpers.UnitConvertHelper;
import uk.co.borconi.emil.obd2aa.pid.PIDToFetch;


/**
 * Created by Emil on 31/08/2017.
 */


public class OBD2Background
{
    public static boolean isDebugging;
    static volatile boolean isRunning;
    private final Context context;
    private final List<PIDToFetch> pidToFetch = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    public boolean ecuConnected = true;
    NotificationManager mNotifyMgr;
    private ITorqueService torqueService;
    private String[] pids;
    private PreferencesHelper prefs;
    private SharedPreferences.Editor editor;
    private String[] units;
    private boolean alternativePulling;
    private boolean firstFetch = true;
    private boolean mBind = false;
    private UiModeManager mUimodemanager = null;
    private boolean isDemoMode;
    private int audio_1, audio_2, audio_3, visual_display;
    private boolean useImperial;

    private ServiceConnection connection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName arg0, IBinder service)
        {
            Log.d("HU", "SERVICE CONNECTED!");
            torqueService = ITorqueService.Stub.asInterface(service);

            try
            {
                if (torqueService.getVersion() < 19)
                {
                    Log.d("OBD2-APP", "Incorrect version. You are using an old version of Torque with this plugin.\n\nThe plugin needs the latest version of Torque to run correctly.\n\nPlease upgrade to the latest version of Torque from Google Play");
                    return;
                }
            }
            catch (RemoteException e)
            {
                throw new RuntimeException(e);
            }
            Log.d("HU", "Have Torque service connection, starting fetching");
        }

        public void onServiceDisconnected(ComponentName name) {
            torqueService = null;
        }
    };
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle torqueAlarm = intent.getExtras();
            Log.d("OBD2AA", "Receiver : " + intent.getAction());

            //Intent localIntent = new Intent(getApplicationContext(), MyOdbService.class);
            String text;
            if (torqueAlarm.getString("ALARM_TYPE").equalsIgnoreCase("MIN"))
            {
                text = "Current value " + String.format("%.2f", torqueAlarm.getDouble("CURRENT_VALUE")) + " " + torqueAlarm.getString("UNIT") + " is lower than: " + String.format("%.2f", torqueAlarm.getDouble("TRIGGER_VALUE")) + " " + torqueAlarm.getString("UNIT");
            }
            else
            {
                text = "Current value " + String.format("%.2f", torqueAlarm.getDouble("CURRENT_VALUE")) + " " + torqueAlarm.getString("UNIT") + " is over than: " + String.format("%.2f", torqueAlarm.getDouble("TRIGGER_VALUE")) + " " + torqueAlarm.getString("UNIT");
            }

            showNotification(torqueAlarm.getString("ALARM_NAME"), text, R.drawable.ic_category_engine, R.drawable.ic_danger_r);
        }
    };
    private Location lastlocation = null;
    private double accumulated_distance = 999;
    private long lastcardupdate;


    public OBD2Background(ITorqueService torqueService, Context context)
    {
        this.torqueService = torqueService;
        this.context = context;

        onCreate();
    }

    public void onCreate()
    {
        prefs = PreferencesHelper.getPreferences(context);
        mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        editor = prefs.edit();
        isDemoMode = prefs.isInDemoMode();
        useImperial = prefs.shouldUseImperial();
        int gaugeNumber = prefs.getNumberOfGauges();
        audio_1 = prefs.getAudio1();
        audio_2 = prefs.getAudio2();
        audio_3 = prefs.getAudio3();
        visual_display = prefs.getVisualDisplay();

        if (useImperial)
        {
            audio_1 = (int) Math.round(audio_1 / 1.09361);
            audio_2 = (int) Math.round(audio_2 / 1.09361);
            audio_3 = (int) Math.round(audio_3 / 1.09361);
            visual_display = (int) Math.round(visual_display / 1.09361);
        }

        pids = new String[gaugeNumber];
        units = new String[gaugeNumber];
        isDebugging = prefs.isDebugging();
        alternativePulling = prefs.hasAlternativePulling();
        for (int i = 1; i <= gaugeNumber; i++)
        {
            pids[i - 1] = prefs.getPidForGauge(i);
            units[i - 1] = prefs.getUnitForGauge(i);
            Log.d("OBD2AA", "Gounde number: " + i + " pid: " + prefs.getPidForGauge(i).split(",")[0] + " Unit: " + prefs.getUnitForGauge(i));
        }

        Log.d("OBD2AA", "OBD2 Background Service Created!");

        IntentFilter filter = new IntentFilter();
        filter.addAction("org.prowl.torque.ALARM_TRIGGERED");
        context.registerReceiver(receiver, filter);

        /* Register Torque Service only if autostart is enabled */
      /*  if (prefs.getBoolean("autostart", false)) {
            startTorque();
            data_fecther();
        }*/

        dataFetcher();
        mUimodemanager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
    }

    public void onDestroy()
    {
        Log.d("OBD2AA", "OBD2 Background Service on Destroy");
        isRunning = false;
        if (mBind)
        {
            context.unbindService(connection);
        }
        if (receiver != null)
        {
            context.unregisterReceiver(receiver);
        }
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(1983);
        notificationManager.cancel(1984);
        notificationManager.cancel(1985);
        notificationManager.cancel(1986);
        Intent sendIntent = new Intent();
        sendIntent.setAction("org.prowl.torque.REQUEST_TORQUE_QUIT"); //Stor torque
        context.sendBroadcast(sendIntent);
        android.os.Process.killProcess(android.os.Process.myPid()); //Do a kill.
    }

    private void dataFetcher()
    {
        final String[] fuelPid = {prefs.watchFuel(), prefs.coolantPid()};

        isRunning = true;
        Log.d("OBD2AA", "Data fetcher started....");
        Thread thread = new Thread() {
            /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
            @SuppressLint({"WrongConstant"})
            public void run() {
                int i;
                char c;
                double d;
                try
                {
                    while (OBD2Background.isRunning)
                    {
                        if (torqueService != null)
                        {
                            if (!ecuConnected)
                            {
                                try
                                {
                                    if (torqueService.isConnectedToECU())
                                    {
                                        ecuConnected = true;
                                    }
                                }
                                catch (RemoteException unused) { }
                            }
                            else if (firstFetch)
                            {
                                firstFetch = false;
                                sortPids();
                            }
                            else if (!alternativePulling)
                            {
                                List pidsAsList = Arrays.asList(pids);
                                float[] pIDValues = torqueService.getPIDValues(pids);
                                if (OBD2Background.isDebugging)
                                {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Pids requested: ");
                                    sb.append(Arrays.toString(pids));
                                    Log.d("OBD2-APP", sb.toString());
                                    StringBuilder sb2 = new StringBuilder();
                                    sb2.append("Float values: ");
                                    sb2.append(Arrays.toString(pIDValues));
                                    Log.d("OBD2-APP", sb2.toString());
                                }
                                long[] pIDUpdateTime = torqueService.getPIDUpdateTime(pids);
                                for (PIDToFetch pIDToFetch : pidToFetch)
                                {
                                    int indexOf = pidsAsList.indexOf(pIDToFetch.getSinglePid());
                                    if (indexOf == 0 || pIDValues[indexOf] != pIDValues[indexOf - 1])
                                    {
                                        if (isDemoMode)
                                        {
                                            if (pIDToFetch.getLastvalue() == 0.0d)
                                            {
                                                d = pIDToFetch.getMaxValue();
                                            }
                                            else
                                            {
                                                d = pIDToFetch.getLastvalue() * 1.2d;
                                            }
                                            double nextDouble = ThreadLocalRandom.current().nextDouble(Math.max(pIDToFetch.getMinValue(), pIDToFetch.getLastvalue() / 1.2d), Math.min(pIDToFetch.getMaxValue(), d));
                                            pIDToFetch.setLastvalue(nextDouble);
                                            if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class))
                                                EventBus.getDefault().post(new GaugeUpdate(pIDToFetch.getGaugeNumber(), (float) nextDouble));

                                        }
                                        else if (!(pIDUpdateTime[indexOf] == pIDToFetch.getLastFetch() || pIDUpdateTime[indexOf] == 0))
                                        {
                                            pIDToFetch.putLastFetch(pIDUpdateTime[indexOf]);

                                            if (pIDToFetch.getNeedsConversion())
                                            {
                                                if (OBD2Background.isDebugging)
                                                {
                                                    StringBuilder sb3 = new StringBuilder();
                                                    sb3.append("PID BEFORE CONVERSION");
                                                    sb3.append(pIDToFetch.getPID()[0]);
                                                    sb3.append(" unit: ");
                                                    sb3.append(pIDToFetch.getUnit());
                                                    sb3.append(" value= ");
                                                    sb3.append(pIDValues[indexOf]);
                                                    sb3.append("last updated at: ");
                                                    sb3.append(pIDUpdateTime[indexOf]);
                                                    Log.d("OBD2-APP", sb3.toString());
                                                }
                                                pIDValues[indexOf] = UnitConvertHelper.ConvertValue(pIDValues[indexOf], pIDToFetch.getUnit());
                                            }
                                            if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class))
                                            {
                                                GaugeUpdate update = new GaugeUpdate(
                                                        pIDToFetch.getGaugeNumber(),
                                                        Math.max(
                                                                Math.max(pIDValues[indexOf], pIDToFetch.getMinValue()),
                                                                Math.min(pIDValues[indexOf], pIDToFetch.getMaxValue())
                                                        ));
                                                EventBus.getDefault().post(update);
                                            }

                                            if (OBD2Background.isDebugging)
                                            {
                                                StringBuilder sb5 = new StringBuilder();
                                                sb5.append("PID   ");
                                                sb5.append(pIDToFetch.getPID()[0]);
                                                sb5.append(" unit: ");
                                                sb5.append(pIDToFetch.getUnit());
                                                sb5.append(" value= ");
                                                sb5.append(pIDValues[indexOf]);
                                                sb5.append("last updated at: ");
                                                sb5.append(pIDUpdateTime[indexOf]);
                                                Log.d("OBD2-APP", sb5.toString());
                                            }
                                        }
                                    }
                                }
                                i = 100;
                                Thread.sleep(i);
                            }
                            else
                            {
                                for (PIDToFetch pIDToFetch2 : pidToFetch)
                                {
                                    float[] fArr = {0.0f};
                                    fArr[0] = torqueService.getValueForPid(Integer.parseInt(pIDToFetch2.getPID()[0].split(",")[0], 16), true);
                                    long[] pIDUpdateTime2 = torqueService.getPIDUpdateTime(pIDToFetch2.getPID());
                                    if (!(pIDUpdateTime2[0] == pIDToFetch2.getLastFetch() || pIDUpdateTime2[0] == 0))
                                    {
                                        pIDToFetch2.putLastFetch(pIDUpdateTime2[0]);
                                        if (OBD2Background.isDebugging)
                                        {
                                            StringBuilder sb7 = new StringBuilder();
                                            sb7.append("PID   ");
                                            sb7.append(pIDToFetch2.getPID()[0]);
                                            sb7.append(" unit: ");
                                            sb7.append(pIDToFetch2.getUnit());
                                            sb7.append(" value= ");
                                            sb7.append(fArr[0]);
                                            sb7.append("last updated at: ");
                                            sb7.append(pIDUpdateTime2[0]);
                                            Log.d("OBD2-APP", sb7.toString());
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            try
                            {
                                Thread.sleep(250);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                    Log.d("OBS2AA", "Running StopSelf...");
                    OBD2Background.isRunning = false;
                    if (mBind)
                    {
                        mBind = false;
                    }
                    connection = null;
                    if (receiver != null)
                    {
//                        unregisterReceiver(receiver);
                    }
                    receiver = null;
                    Intent intent = new Intent();
                    intent.setAction("org.prowl.torque.REQUEST_TORQUE_QUIT");
//                    sendBroadcast(intent);
//                    stopSelf();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        //Second Thread for monitoring fuel, this can pull much slower, once every 30 seconds for example.
        Thread fuelwatcher = new Thread() {
            @Override
            public void run() {
                int lastwarningvalue = 0;
                boolean miles = false;
                boolean celsius = true;
                boolean first = true;
                boolean warm_engine = false;
                int warm_engine_degree = prefs.getCoolantWarningValue();
                while (isRunning)
                {
                    if (torqueService != null)
                    {
                        if (first)
                        {
                            first = false;
                            try
                            {
                                miles = !torqueService.getPreferredUnit("km").equalsIgnoreCase("km");
                                celsius = torqueService.getPreferredUnit("°C").equalsIgnoreCase("°C");
                            }
                            catch (RemoteException e)
                            {
                                e.printStackTrace();
                            }
                        }

                        try
                        {
                            if (prefs.shouldMonitorFuel())
                            {  //If we should monitor fuel
                                int fuelVal = Math.round(torqueService.getPIDValues(fuelPid)[0]);
                                if ((fuelVal < 80) && (fuelPid[0].equalsIgnoreCase("ff126a")) || fuelVal < 5)
                                {
                                    if (lastwarningvalue != fuelVal)
                                    {
                                        String warrning = "";
                                        if (fuelPid[0].equalsIgnoreCase("ff126a"))
                                        {
                                            if (miles)
                                                // warrning = "Estimated fuel range is only: " + Math.round(fuelVal / 1.60) + " miles.";
                                                warrning = context.getString(R.string.est_range, Math.round(fuelVal / 1.60), " miles");
                                            else
                                                warrning = context.getString(R.string.est_range, fuelVal, " km");
                                        }
                                        else
                                            warrning = context.getString(R.string.rem_fuel, fuelVal, " %");
                                        lastwarningvalue = fuelVal;
                                        showNotification(context.getResources().getString(R.string.low_fuel_tit), warrning, R.drawable.ic_danger_r, R.drawable.fuel);
                                    }
                                }
                            }
                            if (prefs.shouldMonitorCoolant()) // If we should monitor coolant
                            {
                                float coolantval = torqueService.getPIDValues(fuelPid)[1];
                                if (!celsius)
                                    coolantval = UnitConvertHelper.ConvertValue(coolantval, "°C");

                                if (!warm_engine && coolantval >= warm_engine_degree)
                                {
                                    Log.d("OBD2AA", "Should show the engine temp warning.");
                                    warm_engine = true;
                                    showNotification(context.getResources().getString(R.string.coolant_ok), context.getResources().getString(R.string.engine_warm), R.drawable.ic_danger_green, R.drawable.ic_coolant);

                                    Thread.sleep(10000);
                                    Log.d("OBD2AA", "Should clear warm engine temp");
                                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                                    notificationManager.cancel(1984);
                                }
                            }

                        }
                        catch (RemoteException e)
                        {
                            e.printStackTrace();
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    try
                    {
                        Thread.sleep(30000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
        if (prefs.shouldMonitorFuel() || prefs.shouldMonitorCoolant())
        {
            fuelwatcher.start();
        }
    }

    public String getUnit(String unit)
    {
        try
        {
            return torqueService.getPreferredUnit(unit);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        return unit;
    }

    private void sortPids()
    {
        Log.d("OBD2AA", "PIDS to string....");
        try
        {
            String[] pidsdesc = torqueService.getPIDInformation(pids);

            for (int i = 0; i < pids.length; i++)
            {
                //Build the pidstofetch object with correct data
                boolean needsconversion = !(torqueService.getPreferredUnit(units[i]).equalsIgnoreCase(units[i]));
                // Log.d("OBD2AA","Pid "+pids[i] + "Status: " +pidMap.get(pids[i]));

                String[] info = pidsdesc[i].split(",");
                // Log.d("OBD2AA"," Max val stored for pid (" + pids[i]+"): "+prefs.getFloat("maxval_" + (i+1), 0) +" Reported from Torque: " + parseInt(info[3]) + "units: " +units[i] + ",Locked: " + prefs.getBoolean("locked_"+(i+1),false) +"needconversion: "+needsconversion);

                if (!prefs.isLockedForGauge(i + 1) && prefs.getMaxValueForGauge(i + 1) == Float.POSITIVE_INFINITY)
                {
                    if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class))
                        EventBus.getDefault().post(new GaugeUpdate(i, Float.parseFloat(info[3]), true, false));

                    editor.putString("maxval_" + (i + 1), info[3]);
                    editor.apply();

                }
                if (needsconversion)
                {
                    Float maxFromTorque = UnitConvertHelper.ConvertValue(Float.parseFloat(info[3]), units[i]);
                    if (!prefs.isLockedForGauge(i + 1) && prefs.getMaxValueForGauge(i + 1).equals(Float.POSITIVE_INFINITY))
                    {
                        if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class))
                            EventBus.getDefault().post(new GaugeUpdate(i, maxFromTorque, true, false));

                        Log.d("OBD2AA", "Need to update Gauge_" + (i + 1) + "Max val: " + maxFromTorque);
                        editor.putString("maxval_" + (i + 1), maxFromTorque.toString());
                        editor.apply();
                    }

                    Float minFromTorque = UnitConvertHelper.ConvertValue(Float.parseFloat(info[4]), units[i]);
                    if (!prefs.isLockedForGauge(i + 1) && !prefs.getMinValueForGauge(i + 1).equals(Float.NEGATIVE_INFINITY))
                    {
                        if (EventBus.getDefault().hasSubscriberForEvent(GaugeUpdate.class))
                            EventBus.getDefault().post(new GaugeUpdate(i, minFromTorque, false, true));

                        Log.d("OBD2AA", "Need to update Gauge_" + (i + 1) + "Min val: " + minFromTorque);
                        editor.putString("minval_" + (i + 1), minFromTorque.toString());
                        editor.apply();
                    }
                }
                pidToFetch.add(new PIDToFetch(pids[i], true, 0, i, units[i], needsconversion, prefs.getMaxValueForGauge(i + 1), prefs.getMinValueForGauge(i + 1)));
            }
        }
        catch (RemoteException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void startTorque()
    {
        Intent intent = new Intent();
        intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.startForegroundService(intent);
        else
            context.startService(intent);

        boolean successfulBind = context.bindService(intent, connection, 0);
        if (successfulBind)
        {
            mBind = true;
            Log.d("HU", "Connected to torque service!");
        }
        else
        {
            mBind = false;
            Log.e("HU", "Unable to connect to Torque plugin service");
        }
    }

    protected void showNotification(String Title, String Subtitle, int actionicon, int thumbnail)
    {
    /*
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "torque_not_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel used for Torque notifications");
            mNotifyMgr.createNotificationChannel(notificationChannel);
        }

        CarNotificationExtender paramString2 = new CarNotificationExtender.Builder()
                .setTitle(Title)
                .setSubtitle(Subtitle)
                .setShouldShowAsHeadsUp(true)
                .setActionIconResId(actionicon)
                .setBackgroundColor(Color.WHITE)
                .setNightBackgroundColor(Color.DKGRAY)
                .setThumbnail(BitmapFactory.decodeResource(getResources(), thumbnail))
                .build();

        NotificationCompat.Builder mynot = new NotificationCompat.Builder(getApplicationContext(),NOTIFICATION_CHANNEL_ID)
                .setContentTitle(Title)
                .setContentText(Subtitle)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), thumbnail))
                .setSmallIcon(actionicon)
                .extend(paramString2);


        mNotifyMgr.notify(1984,mynot.build());
    */
    }
}
