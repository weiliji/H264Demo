package com.cit.h264demo;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class H264Encoder
{
    private static final String TAG = "encoder";
    private static final boolean DEBUG = true;

    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced
    // Video
    private final static int TIME_INTERNAL = 5;
    private MediaMuxer mMediaMuxer;
    private int mIndex;
    private long mTime;
    // params
    private int mWidth;
    private int mHeight;
    private int mFps;
    private String mFilename;

    public H264Encoder(int width, int height, int fps, String filename)
    {
        this.mWidth = width;
        this.mHeight = height;
        this.mFps = fps;
        this.mFilename = filename;
    }

    public void start()
    {
//        if (mMediaMuxer == null)
//        {
//            try
//            {
//                // 初始化MediaFormat
//                MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
//                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);
////                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
////                mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
//                // 配置MediaFormat
//                mMediaMuxer = new MediaMuxer(mFilename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//                mIndex = mMediaMuxer.addTrack(mediaFormat);
//                mMediaMuxer.start();
//            } catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//        }
    }

    public void stop()
    {
        if (mMediaMuxer != null)
        {
            try
            {
                mMediaMuxer.stop();
                mMediaMuxer.release();
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                mMediaMuxer = null;
            }
        }
    }

    private void initSpsAndPps(byte[] buffer, int offset, int length)
    {
        // 判断I帧，然后根据具体情况获取sps,pps
        if ((buffer[4] & 0x1f) == 7)
        {
            int save_x = 0;
            Map<Integer, Integer> map = new HashMap<>();
            byte[] save = new byte[length];
            System.arraycopy(buffer, 0, save, 0, length);
            //
            for (int i = 0; i < save.length - 3; i++)
            {
                if (save[i] == 0 && save[i + 1] == 0 && save[i + 2] == 0 && save[i + 3] == 1)
                {
                    map.put(save_x, i);
                    save_x++;
                }
            }
            int length_sps = map.get(1) - map.get(0) - 4;
            int offset_sps = map.get(0) + 3;
            byte[] sps = new byte[length_sps];
            System.arraycopy(save, offset_sps, sps, 0, length_sps);

            int length_pps = map.get(2) - map.get(1) - 4;
            int offset_pps = map.get(1) + 3;
            byte[] pps = new byte[length_pps];
            System.arraycopy(save, offset_pps, pps, 0, length_pps);

            if (DEBUG)
            {
                Log.d(TAG, "map.size: " + map.size());
                Log.d(TAG, "sps: " + Arrays.toString(sps));
                Log.d(TAG, "pps: " + Arrays.toString(pps));
                Log.d(TAG, "mFilename: " + mFilename);
            }

            try
            {
                // 初始化MediaFormat
                MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFps);
                mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
                mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
                // 配置MediaFormat
                mMediaMuxer = new MediaMuxer(mFilename, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                mIndex = mMediaMuxer.addTrack(mediaFormat);
                mMediaMuxer.start();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void onFrame(byte[] buffer, int offset, int length)
    {
        if (mMediaMuxer != null)
        {
            long timeUs = 0L;
            if (mTime == 0L)
                mTime = System.nanoTime() / 1000;
            else
                timeUs = System.nanoTime() / 1000 - mTime;
//            if (DEBUG) Log.v(TAG, "timeUs = " + timeUs);
            //
            ByteBuffer byteBuffer = ByteBuffer.allocate(length);
            byteBuffer.put(buffer, 0, length);
            //
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            bufferInfo.offset = 0;
            bufferInfo.size = length;
            bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
            bufferInfo.presentationTimeUs = timeUs;
            //
            mMediaMuxer.writeSampleData(mIndex, byteBuffer, bufferInfo);
        } else
        {
            initSpsAndPps(buffer, offset, length);
        }
    }
}
