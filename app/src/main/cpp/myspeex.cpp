#include <jni.h>
#include <string>
#include <speex/speex_preprocess.h>
#include <speex/speex_echo.h>
#include <android/log.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "gloomy", __VA_ARGS__)

SpeexPreprocessState *state = nullptr;
SpeexEchoState *echoState = nullptr;

extern "C"
JNIEXPORT void JNICALL
Java_com_gloomyer_myspeex_interfaces_SpeexJNIBridge_init(JNIEnv *env, jclass type,
                                                         jint frame_size, jint sampling_rate) {

    frame_size /= 2;
    int filterLength = frame_size * 8;

    //降噪初始化
    state = speex_preprocess_state_init(frame_size, sampling_rate);
    int i = 1;
    speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_DENOISE, &i); //降噪 建议1
    i = -25;
    speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &i);////设置噪声的dB
    i = 1;
    speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_AGC, &i);////增益
    i = 24000;
    speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_AGC_LEVEL, &i);
    i = 0;
    speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_DEREVERB, &i);
    float f = 0;
    speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_DEREVERB_DECAY, &f);
    f = 0;
    speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_DEREVERB_LEVEL, &f);

    //回音消除初始化
    echoState = speex_echo_state_init(frame_size, filterLength);
    speex_echo_ctl(echoState, SPEEX_ECHO_SET_SAMPLING_RATE, &sampling_rate);

    //关联
    speex_preprocess_ctl(state, SPEEX_PREPROCESS_SET_ECHO_STATE, echoState);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_gloomyer_myspeex_interfaces_SpeexJNIBridge_destory(JNIEnv *env, jclass type) {
    if (state) {
        speex_preprocess_state_destroy(state);
        state = nullptr;
    }

    if (echoState) {
        speex_echo_state_destroy(echoState);
        echoState = nullptr;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_gloomyer_myspeex_interfaces_SpeexJNIBridge_denoise(JNIEnv *env, jclass jcls,
                                                            jbyteArray buffer_) {
    jbyte *in_buffer = env->GetByteArrayElements(buffer_, nullptr);

    auto *in = (short *) in_buffer;
    int rcode = speex_preprocess_run(state, in);

    env->ReleaseByteArrayElements(buffer_, in_buffer, 0);
    return rcode;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_gloomyer_myspeex_interfaces_SpeexJNIBridge_cancellation(JNIEnv *env, jclass jcls,
                                                                 jbyteArray inBuffer_,
                                                                 jbyteArray playBuffer_) {
    jbyte *inBuffer = env->GetByteArrayElements(inBuffer_, nullptr);
    jbyte *playBuffer = env->GetByteArrayElements(playBuffer_, nullptr);

    jsize length = env->GetArrayLength(playBuffer_);
    jbyteArray retBuffer_ = env->NewByteArray(length);
    jbyte *retBuffer = env->GetByteArrayElements(retBuffer_, nullptr);

    auto *in = (short *) inBuffer;
    auto *play = (short *) playBuffer;
    auto *ret = (short *) retBuffer;
    speex_echo_cancellation(echoState, in, play, ret); //消除回声
    speex_preprocess_run(state, ret);

    env->ReleaseByteArrayElements(inBuffer_, inBuffer, 0);
    env->ReleaseByteArrayElements(playBuffer_, playBuffer, 0);
    env->ReleaseByteArrayElements(retBuffer_, retBuffer, 0);
    return retBuffer_;
}