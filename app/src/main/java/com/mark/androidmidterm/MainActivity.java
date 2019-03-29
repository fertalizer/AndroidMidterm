package com.mark.androidmidterm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.SyncStateContract;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnVideoSizeChangedListener, SeekBar.OnSeekBarChangeListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener {
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MediaPlayer mMediaPlayer;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private SeekBar mSeekBar;
    private TextView mTextCurrentTime;
    private TextView mTextTotalTime;
    private boolean isFullScreen = false;
    private boolean isMute = false;

    private String mVideoUrl = "https://s3-ap-northeast-1.amazonaws.com/mid-exam/Video/taeyeon.mp4";
//    private String mVideoUrl = "https://s3-ap-northeast-1.amazonaws.com/mid-exam/Video/protraitVideo.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextCurrentTime = findViewById(R.id.tv_current_time);
        mTextTotalTime = findViewById(R.id.tv_total_time);

        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);

        mSeekBar = findViewById(R.id.seekBar);

    }

    public void setVideo() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.reset();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        try {
            mMediaPlayer.setDataSource(mVideoUrl);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_mute:
                if (!isMute) {
                    mute();
                    findViewById(R.id.iv_mute).setBackgroundResource(R.drawable.ic_volume_off_black_24dp);
                    isMute = true;
                } else {
                    unmute();
                    findViewById(R.id.iv_mute).setBackgroundResource(R.drawable.ic_volume_mute_black_24dp);
                    isMute = false;
                }
                break;
            case R.id.iv_backward:
                break;
            case R.id.iv_play:
                if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.start();
                    findViewById(R.id.iv_play).setBackgroundResource(R.drawable.ic_pause_black_24dp);
                    Log.i("Mark", "Start");
                } else {
                    mMediaPlayer.pause();
                    findViewById(R.id.iv_play).setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                    Log.i("Mark", "Pause");
                }
                break;
            case R.id.iv_forward:
                break;
            case R.id.iv_full_screen:
                if (!isFullScreen) {
                    fullScreen();
                    isFullScreen = true;
                } else {
                    changeVideoSize();
                    isFullScreen = false;
                }
                break;
            default:
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i("Mark", "surfaceCreated");
        setVideo();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setDisplay(holder);
        mMediaPlayer.prepareAsync();
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            mSurfaceWidth = mSurfaceView.getWidth();
            mSurfaceHeight = mSurfaceView.getHeight();
        } else if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            mSurfaceWidth = mSurfaceView.getHeight();
            mSurfaceHeight = mSurfaceView.getWidth();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i("Mark", "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("Mark", "surfaceDestroyed");
        if (null != mMediaPlayer) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onPrepared(final MediaPlayer mp) {
        findViewById(R.id.iv_mute).setOnClickListener(this);
        findViewById(R.id.iv_backward).setOnClickListener(this);
        findViewById(R.id.iv_play).setOnClickListener(this);
        findViewById(R.id.iv_forward).setOnClickListener(this);
        findViewById(R.id.iv_full_screen).setOnClickListener(this);

        mSeekBar.setProgress(0);
        mSeekBar.setSecondaryProgress(0);
        mSeekBar.setMax(mp.getDuration());
        Log.i("Mark", "Total Time = " + mp.getDuration());
        mSeekBar.setOnSeekBarChangeListener(this);


        String videoTimeString = getShowTime(mp.getDuration());
        mTextTotalTime.setText(videoTimeString);

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String currentTimeString = getShowTime(mp.getCurrentPosition());
                mTextCurrentTime.setText(currentTimeString);
                mSeekBar.setProgress(mp.getCurrentPosition());
            }
        };

        Thread thread = new Thread() {
            public void run() {
                super.run();
                try {
                    while (true) {
                        if (mp.isPlaying()) {
                            handler.sendEmptyMessage(0);
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Mark", "Exception = " + e);
                }
            }
        };
        thread.start();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        percent = (mp.getDuration() * percent) / 100;
        mSeekBar.setSecondaryProgress(percent);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (progress >= 0) {
            if (fromUser) {
                mMediaPlayer.seekTo(progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        changeVideoSize();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        changeVideoSize();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (null != mMediaPlayer) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        findViewById(R.id.iv_play).setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
    }

    public void changeVideoSize() {
        int videoWidth = mMediaPlayer.getVideoWidth();
        int videoHeight = mMediaPlayer.getVideoHeight();

        float max;
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            max = Math.max((float) videoWidth / (float) mSurfaceWidth, (float) videoHeight / (float) mSurfaceHeight);
        } else if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            max = Math.max(((float) videoWidth / (float) mSurfaceHeight), (float) videoHeight / (float) mSurfaceWidth);
        } else {
            max = Math.max(((float) videoWidth / (float) mSurfaceHeight), (float) videoHeight / (float) mSurfaceWidth);
        }
        videoWidth = (int) Math.ceil((float) videoWidth / max);
        videoHeight = (int) Math.ceil((float) videoHeight / max);

        mSurfaceView.setLayoutParams(new ConstraintLayout.LayoutParams(videoWidth, videoHeight));
    }

    private void fullScreen() {
        mSurfaceView.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public String getShowTime(long milliseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        SimpleDateFormat dateFormat;
        if (milliseconds / 60000 > 60) {
            dateFormat = new SimpleDateFormat("hh:mm:ss");
        } else {
            dateFormat = new SimpleDateFormat("mm:ss");
        }
        return dateFormat.format(calendar.getTime());
    }

    public void mute() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC,true);
    }

    public void unmute() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC,false);
    }
}
