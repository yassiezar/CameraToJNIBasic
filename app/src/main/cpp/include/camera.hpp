#ifndef CAMERATOJNIBASIC_CAMERA_HPP
#define CAMERATOJNIBASIC_CAMERA_HPP

#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>

#include <string>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL
Java_com_upwork_jaycee_cameratojnibasic_JNIWrapper_YUV2Greyscale(JNIEnv*, jobject, jint, jint, jobject, jobject);

#ifdef __cplusplus
}
#endif

#endif //CAMERATOJNIBASIC_CAMERA_HPP
