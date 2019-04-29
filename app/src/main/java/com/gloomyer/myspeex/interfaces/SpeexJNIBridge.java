package com.gloomyer.myspeex.interfaces;

public class SpeexJNIBridge {

    static {
        System.loadLibrary("speexdsp");
        System.loadLibrary("myspeex");
    }

    public static native void init(int frame_size, int sampling_rate);

    public static native void destory();

    public static native int denoise(byte[] buffer);

    /**
     * 回声消除处理
     * 这个方法包含降噪处理，无需在单独调用
     *
     * @param inBuffer   本地录制的声音(准备发出去的)
     * @param playBuffer 本地准备播放的声音(接受到的)
     * @return 处理好的数据
     */
    public static native byte[] cancellation(byte[] inBuffer, byte[] playBuffer);
}
