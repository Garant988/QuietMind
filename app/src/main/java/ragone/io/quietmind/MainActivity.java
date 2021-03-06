package ragone.io.quietmind;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.lantouzi.wheelview.WheelView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String INTERVAL_PREF = "interval";
    private static final String MY_PREF = "my_prefs";
    private static final String VIPASSANA = "vipassana";
    private static final String FIRST_TIME = "first_time";
    private static final String SESSION_NUM = "session_num";
    private final String LONGEST_STREAK = "longeststreak";
    private final String TOTAL_TIME = "totaltime";
    private final String AVERAGE_TIME = "averagetime";
    private final String STREAK = "streak";
    private final String TIME = "time";
    private final String LAST_DAY = "lastday";
    private WheelView wheelView;
    private CountDownTimer timer;
    private int selectedIndex;
    private List<SmoothCheckBox> days;
    private String lastDay;
    private int count = 0;
    private CoordinatorLayout coordinatorLayout;
    private MediaPlayer bell1Player;
    private MediaPlayer bell2Player;
    private MediaPlayer vipassanaStartPlayer;
    private MediaPlayer vipassanaEndPlayer;
    private int streak;
    private PlayPauseView playPauseView;
    private LinearLayout dayLayout;
    private SwitchCompat vipassanaMode;
    private TextView bigText;
    private ShowcaseView scv;
    private int counter = 0;
    private MyDrawer myDrawer;
    private boolean firstTime;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager notificationManager;
    private LinearLayout mainLayout;
    private ImageView statsBtn;
    private ImageView exitBtn;
    private int ringer = 0;
    private AudioManager audio;
    private float brightness;
    private ImageView helpBtn;
    private boolean intervalOn = false;
    private Button intervalBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        bigText = (TextView) findViewById(R.id.bigText);
        bigText.setVisibility(View.INVISIBLE);
        vipassanaMode = (SwitchCompat) findViewById(R.id.vipassanaMode);
        if(getVipassanaSelected()) {
            vipassanaMode.setChecked(true);
        }
        vipassanaMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    wheelView.smoothSelectIndex(59);
//                    wheelView.setEnabled(false);

                } else {
//                    wheelView.smoothSelectIndex(selectedIndex);
//                    wheelView.setEnabled(true);
                }
                saveData();
            }
        });
        wheelView = (WheelView) findViewById(R.id.wheel);
        playPauseView = (PlayPauseView) findViewById(R.id.play_pause_view);

        setupDays();

        setupPlayPauseButton();
        setupWheel();


        if (isFirstTime()) {
            showShowcase();
        }

        statsBtn = (ImageView) findViewById(R.id.stats_button);
        statsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatsActivity.class);
                startActivity(intent);
            }
        });

        helpBtn = (ImageView) findViewById(R.id.help_button);
        helpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShowcase();
            }
        });

        intervalBtn = (Button) findViewById(R.id.interval);
        intervalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final NumberPicker picker = new NumberPicker(MainActivity.this);
                String[] list = new String[21];
                for(int i = 0; i <= 20; i++) {
                    if(i == 0) {
                        list[i] = "Disabled";
                    } else {
                        list[i] = i + " min.";
                    }
                }
                picker.setMaxValue(20);
                picker.setMinValue(0);

                picker.setDisplayedValues(list);
                int interval = getInterval();
                picker.setValue(interval);

                picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Set Interval of Bells")
                        .setView(picker)
                        .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences.Editor editor = getSharedPreferences(MY_PREF, MODE_PRIVATE).edit();
                                editor.putInt(INTERVAL_PREF, picker.getValue());
                                editor.commit();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })

                .show();
            }
        });
    }

    private int getInterval() {
        SharedPreferences prefs = getSharedPreferences(MY_PREF, MODE_PRIVATE);
        return prefs.getInt(INTERVAL_PREF, 0);
    }


    private boolean isFirstTime() {
        SharedPreferences prefs = getSharedPreferences(MY_PREF, MODE_PRIVATE);
        return prefs.getBoolean(FIRST_TIME, true);
    }


    private void showShowcase() {


        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Handler handler = new Handler();
                for (int i = 1; i <= days.size(); i++) {
                    final int finalI = i;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(counter == 0 && isFirstTime()) {
                                days.get(finalI - 1).setChecked(true, true);
                            }
                        }
                    }, 1000 * i);
                }
                Looper.loop();
            }
        }).start();

        ViewTarget target = new ViewTarget(R.id.dayLayout, this);
        myDrawer = new MyDrawer(getResources(), MainActivity.this, days);

        scv = new ShowcaseView.Builder(this)
                .setTarget(target)
                .setStyle(R.style.MyTheme)
                .setContentTitle("Streaks!")
                .setContentText("Keep track of how many days in a row you have meditated.")
                .setOnClickListener(this)
                .blockAllTouches()
                .setShowcaseDrawer(myDrawer)
                .build();
    }

    @Override
    public void onClick(View v) {
        switch (counter) {
            case 0:
                ViewTarget target2 = new ViewTarget(R.id.vipassanaMode, this);
                scv.setTarget(target2);
                scv.setContentTitle("Vipassanā Mode!");
                scv.setContentText("Fixed 60 minutes meditation by S. N. Goenka. Sadhu! Sadhu! Sadhu!");

                Handler handler = new Handler();
                for (int i = 1; i<=days.size() ;i++) {
                    final int finalI = i;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            SmoothCheckBox checkBox = days.get(finalI - 1);
                            if (checkBox.isChecked() && isFirstTime()) {
                                checkBox.setChecked(false, true);
                            }
                        }
                    }, 100 * i);
                }
                updateDays();
                break;
            case 1:
                ViewTarget target = new ViewTarget(R.id.stats_button, this);
                scv.setTarget(target);
                scv.setContentTitle("Stages of Meditation!");
                scv.setContentText("Ten stages to help you figure out where you are and how best to continue.");
                break;
            case 2:
                ViewTarget target4 = new ViewTarget(R.id.interval, this);
                scv.setTarget(target4);
                scv.setContentTitle("Set an Interval!");
                scv.setContentText("Be reminded to focus on your breathing by playing bells during your session.");
                break;
            case 3:
                scv.setTarget(Target.NONE);
                scv.setContentTitle("How to Meditate?");
                scv.setStyle(R.style.MyTheme2);
                scv.setShouldCentreText(true);
                scv.setContentText("1. Set the timer.\n2. Press play.\n3. Take a deep breath.\n4. Relax.\n5. Focus on your breathing.");
                break;
            case 4:
                scv.hide();
                firstTime = false;
                saveData();
                break;
        }
        counter++;
        if(counter == 5) {
            counter = 0;
        }
    }

    private void setScreenDim(float value) {
        WindowManager.LayoutParams WMLP = getWindow().getAttributes();
        WMLP.screenBrightness = value;
        getWindow().setAttributes(WMLP);
    }

    private float getScreenDim() {
        WindowManager.LayoutParams WMLP = getWindow().getAttributes();
        return WMLP.screenBrightness;
    }


    private void setupPlayPauseButton() {
        playPauseView.toggle();
        playPauseView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPauseView.toggle();
                audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                if (playPauseView.getDrawable().isPlay()) {
                    brightness = getScreenDim();
                    setScreenDim(0.2f);
                    ringer = audio.getRingerMode();
                    audio.setRingerMode(0);
                    setInputFieldEnabled(false);
                    showSnackBar();
                    if (vipassanaMode.isChecked()) {
                        vipassanaStartPlayer = MediaPlayer.create(MainActivity.this, R.raw.vipassanastart);
                        vipassanaStartPlayer.start();
                    } else {
                        bell2Player = MediaPlayer.create(MainActivity.this, R.raw.bell2);
                        bell2Player.start();
                    }
                    selectedIndex = wheelView.getSelectedPosition();
                    timer = new myCountDownTimer((selectedIndex + 1) * 60000, 1000).start();
                    setupNotification();
                } else {
                    setScreenDim(brightness);
                    audio.setRingerMode(ringer);
                    timer.cancel();
                    stopPlayers();
                    setInputFieldEnabled(true);
                    wheelView.smoothSelectIndex(selectedIndex);
                    removeNotification();
                }
            }
        });
    }

    private void stopPlayers() {
        if(vipassanaStartPlayer != null) {
            vipassanaStartPlayer.stop();
            vipassanaStartPlayer.release();
            vipassanaStartPlayer = null;
        }

        if(vipassanaEndPlayer != null) {
            vipassanaEndPlayer.stop();
            vipassanaEndPlayer.release();
            vipassanaEndPlayer = null;
        }

        if(bell1Player != null) {
            bell1Player.stop();
            bell1Player.release();
            bell1Player = null;
        }

        if(bell2Player != null) {
            bell2Player.stop();
            bell2Player.release();
            bell2Player = null;
        }
    }

    private void removeNotification() {
        if(notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    private void setupNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        1,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("Meditation in progress")
                        .setContentText("Time left: " + wheelView.getSelectedPosition() + 1)
                        .setColor(getResources().getColor(R.color.float_color))
                        .setOngoing(true)
                        .setShowWhen(false)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(resultPendingIntent);
        Notification notification = mBuilder.build();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(001, notification);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeNotification();
        stopPlayers();
        if(timer != null) {
            timer.cancel();
        }
        saveData();
    }

    private void setupWheel() {
        List<String> data = new LinkedList<>();
        for (int i = 1; i <= 90; i++) {
            data.add(String.valueOf(i));
        }
        wheelView.setItems(data);
        wheelView.selectIndex(getTime());
//        if (vipassanaMode.isChecked()) {
//            wheelView.setEnabled(true);
//        }
        wheelView.setOnWheelItemSelectedListener(new WheelView.OnWheelItemSelectedListener() {
            @Override
            public void onWheelItemChanged(WheelView wheelView, int position) {
                saveData();
            }

            @Override
            public void onWheelItemSelected(WheelView wheelView, int position) {
                selectedIndex = position;
                if (vipassanaMode.isChecked()) {
                    vipassanaMode.setChecked(false);
                }
                saveData();
            }
        });
    }

    private void setupDays() {
        days = new ArrayList<>();
        dayLayout = (LinearLayout) findViewById(R.id.dayLayout);

        streak = getStreak();
        Log.v("streak", ""+streak);
        Log.v("lastday", ""+getLastDay());
        Log.v("yesterday", "" + getYesterday());
        Log.v("currentday", "" + getCurrentDay());

        int streakRemain = streak % 7;
        int dayStart = streak - streakRemain;

        for (int i = dayStart; i < dayStart + 7; i++) {
            SmoothCheckBox checkBox = new SmoothCheckBox(MainActivity.this);
            checkBox.setText("" + (i + 1));
            checkBox.setEnabled(false);
            int a = CompatUtils.dp2px(MainActivity.this, 30);
            int b = CompatUtils.dp2px(MainActivity.this, 6);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(a, a);
            params.setMargins(b, b, b, b);
            checkBox.setLayoutParams(params);

            if(i < streak) {
                checkBox.setChecked(true);
            }
            dayLayout.addView(checkBox);
            days.add(checkBox);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v("Last day ", getLastDay());
        Log.v("Yester day ", getYesterday());
        Log.v("current day ", getCurrentDay());

        if(!getLastDay().equals(getYesterday()) && !getLastDay().equals(getCurrentDay()) && streak != 0) {
            if(streak > 1) {
                SweetAlertDialog pDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE);
                pDialog.setTitleText("Oh no!");
                pDialog.setContentText("Your " + streak + " day streak is over!");
                pDialog.setConfirmText("Ok");
                pDialog.setCancelable(false);
                pDialog.show();
            }
            streak = 0;
            Log.v("Streak ", "Streak reset to 0");
            updateDays();
            saveData();
        }
    }

    private void setInputFieldEnabled(boolean isEnabled) {
        intervalBtn.setEnabled(isEnabled);
        wheelView.setEnabled(isEnabled);
        vipassanaMode.setEnabled(isEnabled);
        statsBtn.setEnabled(isEnabled);
        helpBtn.setEnabled(isEnabled);
    }

    private void saveData() {
        Log.v("Streak: ", "" + streak);
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREF, MODE_PRIVATE).edit();
        editor.putInt(TIME, wheelView.getSelectedPosition());
        editor.putBoolean(VIPASSANA, vipassanaMode.isChecked());
        editor.putBoolean(FIRST_TIME, firstTime);
        editor.commit();
    }

    private void saveProgress() {
        Log.v("Streak: ", "" + streak);
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREF, MODE_PRIVATE).edit();
        SharedPreferences prefs = getSharedPreferences(MY_PREF, MODE_PRIVATE);
        editor.putInt(STREAK, streak);
        editor.putString(LAST_DAY, lastDay);
        int longestStreak = prefs.getInt(LONGEST_STREAK, 0);
        int totalTime = prefs.getInt(TOTAL_TIME, 0);
        int sessionNum = prefs.getInt(SESSION_NUM, 1);

        if(streak > longestStreak) {
            editor.putInt(LONGEST_STREAK, streak);
        }
        editor.putInt(TOTAL_TIME, totalTime + selectedIndex + 1);
        editor.putInt(AVERAGE_TIME, (totalTime + selectedIndex + 1) / sessionNum);
        editor.putInt(SESSION_NUM, sessionNum + 1);
        editor.commit();
    }

    private boolean getVipassanaSelected() {
        SharedPreferences prefs = getSharedPreferences(MY_PREF, MODE_PRIVATE);
        return prefs.getBoolean(VIPASSANA, false);
    }

    private int getTime() {
        SharedPreferences prefs = getSharedPreferences(MY_PREF, MODE_PRIVATE);
        return prefs.getInt(TIME, 14);
    }

    private int getStreak() {
        SharedPreferences prefs = getSharedPreferences(MY_PREF, MODE_PRIVATE);
//        return 6;
        return prefs.getInt(STREAK, 0);
    }

    private String getLastDay() {
        SharedPreferences prefs = getSharedPreferences(MY_PREF, MODE_PRIVATE);
//        return "11/11/2016";
        Log.v("LAST DAY", prefs.getString(LAST_DAY, "nothing"));
        Log.v("STREAK", prefs.getInt(STREAK, 0) + "");
        return prefs.getString(LAST_DAY, "");
    }

    private void showSnackBar() {
        bigText.setVisibility(View.VISIBLE);
        AnimationSet set1 = new AnimationSet(true);
        final AnimationSet set2 = new AnimationSet(true);
        final AlphaAnimation ani1 = new AlphaAnimation(0.0f, 1.0f);
        final AlphaAnimation ani2 = new AlphaAnimation(1.0f, 0.0f);
        final ScaleAnimation anis1 = new ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        final ScaleAnimation anis2 = new ScaleAnimation(1.0f, 0.5f, 1.0f, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ani1.setDuration(5000);
        ani2.setDuration(5000);
        anis1.setDuration(5000);
        anis2.setDuration(5000);

        set1.addAnimation(ani1);
        set1.addAnimation(anis1);

        set2.addAnimation(ani2);
        set2.addAnimation(anis2);

        set1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.v("", "Animation start");
                bigText.startAnimation(set2);
                bigText.setVisibility(View.INVISIBLE);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        bigText.startAnimation(set1);
    }

    private String getCurrentDay() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
//        return "12/11/2016";
        return dateFormat.format(cal.getTime());
    }

    private String getYesterday() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        cal.add(Calendar.DATE, -1);
//        return "11/11/2016";
        return dateFormat.format(cal.getTime());
    }

    private String getMinutesAndSeconds(long millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    private void updateDays() {
        int streakRemain = streak % 7;
        int dayStart = streak - streakRemain;

        for (int i = dayStart; i < dayStart + 7; i++) {
            SmoothCheckBox checkBox = days.get(i%7);
            checkBox.setText("" + (i + 1));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveData();
    }

    private class myCountDownTimer extends CountDownTimer {

        public myCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            vipassanaEndPlayer = null;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            int timeLeftInMinutes = (int) Math.floor(millisUntilFinished / 60000);
            wheelView.smoothSelectIndex(timeLeftInMinutes);
            if(vipassanaEndPlayer == null && millisUntilFinished < 809400 && vipassanaMode.isChecked()) {
                vipassanaEndPlayer = MediaPlayer.create(MainActivity.this, R.raw.vipassanaend);
                vipassanaEndPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        vipassanaEndPlayer.release();
                        vipassanaEndPlayer = null;
                    }
                });
                vipassanaEndPlayer.start();
            }


            int interval = getInterval() * 60000;
            if(interval != 0 && millisUntilFinished > interval && millisUntilFinished % interval < 1000) {
                bell1Player = MediaPlayer.create(MainActivity.this, R.raw.bell1);
                bell1Player.start();
                Log.v("BELL", "INTERVAL BELL PLAYED at " + interval);
            }

            mBuilder.setContentText("Time left: " + getMinutesAndSeconds(millisUntilFinished));
            int timeInMillis = (selectedIndex + 1) * 60000;
            mBuilder.setProgress(timeInMillis, timeInMillis - (int) millisUntilFinished, false);
            Notification notification = mBuilder.build();
            notificationManager.notify(001, notification);
        }

        @Override
        public void onFinish() {
            if(!vipassanaMode.isChecked()) {
                bell1Player = MediaPlayer.create(MainActivity.this, R.raw.bell1);
                bell1Player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    int maxCount = 2;

                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        if (count < maxCount) {
                            count++;
                            mediaPlayer.seekTo(0);
                            mediaPlayer.start();
                        } else {
                            mediaPlayer.release();
                            mediaPlayer = null;
                            count = 0;
                        }
                    }
                });
                bell1Player.start();
            }
            playPauseView.toggle();
            setScreenDim(brightness);
            audio.setRingerMode(ringer);
            setInputFieldEnabled(true);
            wheelView.smoothSelectIndex(selectedIndex);
            if(!getLastDay().equals(getCurrentDay()) && getLastDay().equals(getYesterday()) || streak == 0) {
                streak++;
                if(streak % 7 == 0) {
                    days.get(6).setChecked(true, true);
                    final SweetAlertDialog pDialog = new SweetAlertDialog(MainActivity.this, SweetAlertDialog.SUCCESS_TYPE);
                    pDialog.setTitleText("You're on a " + streak + " day streak!");
                    pDialog.setConfirmText("I'm awesome");
                    pDialog.setCancelable(false);
                    pDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            pDialog.dismissWithAnimation();
                            Handler handler = new Handler();
                            for (int i = 1; i <= days.size(); i++) {
                                final int finalI = i;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        days.get(finalI - 1).setChecked(false, true);
                                    }
                                }, 100 * i);
                            }
                        }
                    });
                    pDialog.show();
                    updateDays();
                } else {
                    days.get(streak % 7 - 1).setChecked(true, true);
                }
            }
            lastDay = getCurrentDay();
            removeNotification();
            saveData();
            saveProgress();
        }
    }
}
