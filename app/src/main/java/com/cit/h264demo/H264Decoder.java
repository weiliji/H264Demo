package com.cit.h264demo;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

public class H264Decoder
{
    private static final String TAG = "decoder";
    private static final boolean DEBUG = false;

    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced
    // Video
    private final static int TIME_INTERNAL = 5;
    private MediaCodec mMediaCodec;
    private int mCount;
    // params
    private int mWidth;
    private int mHeight;
    private int mFps;
    private Surface mSurface;

    public H264Decoder(int width, int height, int fps, Surface surface)
    {
        this.mWidth = width;
        this.mHeight = height;
        this.mFps = fps;
        this.mSurface = surface;
    }

    public void start()
    {
        if (mMediaCodec == null)
        {
            try
            {
                // 初始化MediaFormat
                MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);
                // 配置MediaFormat
                mMediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
                mMediaCodec.configure(mediaFormat, mSurface, null, 0);
                mMediaCodec.start();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void stop()
    {
        if (mMediaCodec != null)
        {
            try
            {
                mMediaCodec.stop();
                mMediaCodec.release();
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                mMediaCodec = null;
            }
        }
    }

    public boolean onFrame(byte[] buffer, int offset, int length)
    {
        if (mMediaCodec != null)
        {
            // 获取输入buffer index
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            // -1表示一直等待；0表示不等待；其他大于0的参数表示等待毫秒数
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (DEBUG) Log.v(TAG, "inputBufferIndex = " + inputBufferIndex);
            if (inputBufferIndex >= 0)
            {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                // 清空buffer
                inputBuffer.clear();
                // put需要解码的数据
                inputBuffer.put(buffer, offset, length);
                // 解码
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount * TIME_INTERNAL, 0);
                mCount++;
            } else
            {
                return false;
            }
            // 获取输出buffer index
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 100);
            // 循环解码，直到数据全部解码完成
            while (outputBufferIndex >= 0)
            {
                if (DEBUG) Log.d(TAG, "outputBufferIndex = " + outputBufferIndex);
                // true : 将解码的数据显示到surface上
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
            if (outputBufferIndex < 0)
            {
                if (DEBUG) Log.w(TAG, "outputBufferIndex = " + outputBufferIndex);
            }
            return true;
        }
        return false;
    }
}
