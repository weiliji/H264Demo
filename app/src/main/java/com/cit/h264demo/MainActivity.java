package com.cit.h264demo;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.cit.uvccamera.aidl.CoreClient;
import com.cit.uvccamera.aidl.ICore;
import com.cit.uvccamera.aidl.ICoreCallback;
import com.cit.uvccamera.aidl.ISharedCallback;

import java.io.File;

public class MainActivity extends AppCompatActivity
{
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 30;

    private H264Decoder mH264Decoder;
    private H264Encoder mH264Encoder;

    private boolean mIsH264;
    private ICore mCore;
    private ICoreCallback mCoreCallback = new ICoreCallback()
    {
        @Override
        public void onServiceConnected()
        {
        }

        @Override
        public void onServiceDisconnected()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    exit();
                }
            });
        }

        @Override
        public void onSelected()
        {
        }

        @Override
        public void onUnselected()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    exit();
                }
            });
        }

        @Override
        public void onConnected()
        {
        }

        @Override
        public void onDisConnected()
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    exit();
                }
            });
        }

        @Override
        public void onFormat(final int pixelformat, final int fps, final int width, final int height)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mIsH264 = (pixelformat == 1);
                    String text = "[" + pixelformat + ", " + fps + ", " + width + ", " + height + "]";
                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private ISharedCallback mCaptrueCallback = new ISharedCallback()
    {
        @Override
        public void onUpdated(final byte[] data, final int available)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (mH264Encoder != null)
                        mH264Encoder.onFrame(data, 0, available);
                    if (mH264Decoder != null)
                        mH264Decoder.onFrame(data, 0, available);
                }
            });
        }
    };

    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        initView();
        mCore = new CoreClient(getApplicationContext(), mCoreCallback);
    }

    @Override
    protected void onDestroy()
    {
        exit();
        if (mCore != null)
        {
            mCore.release();
            mCore = null;
        }
        super.onDestroy();
    }

    private void initView()
    {
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback()
        {
            @Override
            public void surfaceCreated(SurfaceHolder holder)
            {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
            {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder)
            {
                stopDecoder();
            }
        });
    }

    public void onClickOpen(View view)
    {
        if (mCore != null)
        {
            // 打开/dev/video0, 权限666
            mCore.select("/dev/video0".hashCode());
            mCore.format(1, FPS, WIDTH, HEIGHT);
        }
    }

    public void onClickClose(View view)
    {
        if (mCore != null)
        {
            mCore.unselect();
        }
    }

    public void onClickStartDecoder(View view)
    {
        startDecoder();
    }

    public void onClickStopDecoder(View view)
    {
        stopDecoder();
    }

    public void onClickStartEncoder(View view)
    {
        startEncoder();
    }

    public void onClickStopEncoder(View view)
    {
        stopEncoder();
    }

    private void startDecoder()
    {
        if (mIsH264)
        {
            if (mH264Decoder == null)
            {
                mCore.setCaptureCallback(mCaptrueCallback);
                //
                Surface surface = mSurfaceView.getHolder().getSurface();
                mH264Decoder = new H264Decoder(WIDTH, HEIGHT, FPS, surface);
                mH264Decoder.start();
                Toast.makeText(this, "startDecoder", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopDecoder()
    {
        if (mH264Decoder != null)
        {
            Toast.makeText(this, "stopDecoder", Toast.LENGTH_SHORT).show();
            mH264Decoder.stop();
            mH264Decoder = null;
        }
        if (mH264Encoder == null)
            mCore.setCaptureCallback(null);
    }

    private void startEncoder()
    {
        if (mIsH264)
        {
            if (mH264Encoder == null)
            {
                mCore.setCaptureCallback(mCaptrueCallback);
                //
                File dir = new File(Environment.getExternalStorageDirectory(), "mp4s");
                if (!dir.exists())
                    dir.mkdirs();
                File file = new File(dir, System.currentTimeMillis() + ".mp4");
                mH264Encoder = new H264Encoder(WIDTH, HEIGHT, FPS, file.getAbsolutePath());
                mH264Encoder.start();
                Toast.makeText(this, "startEncoder: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopEncoder()
    {
        if (mH264Encoder != null)
        {
            Toast.makeText(this, "stopEncoder", Toast.LENGTH_SHORT).show();
            mH264Encoder.stop();
            mH264Encoder = null;
        }
        if (mH264Decoder == null)
            mCore.setCaptureCallback(null);
    }

    private void exit()
    {
        stopEncoder();
        stopDecoder();
        mIsH264 = false;
    }
}
