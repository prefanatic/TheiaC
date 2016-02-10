#include <string.h>
#include <jni.h>
#include <math.h>

#define APPNAME "Theia"
#define MAX_PIXEL_COUNT_THRESHOLD 128000
#define BELT_SPLIT 16

JNIEXPORT void JNICALL
Java_com_example_theia_MainActivity_detectionFilter(JNIEnv *env, jobject instance, jint w_, jint h_,
                                                    jbyteArray current_, jbyteArray background_,
                                                    jbyteArray bitmapStorage_, jbooleanArray beltArray_) {
    jbyte *current = (*env)->GetByteArrayElements(env, current_, NULL);
    jbyte *background = (*env)->GetByteArrayElements(env, background_, NULL);
    jbyte *bitmapStorage = (*env)->GetByteArrayElements(env, bitmapStorage_, NULL);
    jboolean *beltArray = (*env)->GetBooleanArrayElements(env, beltArray_, NULL);

    int i, h, w, wShift, iShift;
    float sum;
    int pixelCountAboveThreshold = 0;
    int bitmapIndex = 0;
    jbyte val;
    for (h = 0; h < h_; h++) {
        i = h * w_;
        for (w = 0; w < w_; w++) {

            // Proceed to do frame subtraction on the image, only if we're within our normal bounds.
            if (w < w_ && h < h_) {
                if (fabs(current[i + w] - background[i + w]) >= 50) {
                    val = 255;
                    pixelCountAboveThreshold++;
                } else {
                    val = 0;
                }

                current[i + w] = val;
            }

            // If we've lagged behind enough to fit a 3x3 box in our frame subtraction, start doing that.
         /*   if (w > 3 && h > 3 && w < w_ + 3 && h < h_ + 3) {
                wShift = w - 3;
                iShift = (h - 3) * w_;
                sum = 1.0f / 9.0f * (
                        current[iShift + wShift - 1 - w_] + current[iShift + wShift - w_] + current[iShift + wShift + 1 - w_] +
                        current[iShift + wShift - 1] + current[iShift + wShift] + current[iShift + wShift + 1] +
                        current[iShift + wShift - 1 + w_] + current[iShift + wShift + w_] + current[iShift + wShift + 1 + w_]
                );

                current[iShift + wShift] = sum;
            }*/
        }
    }

    for (h = 0; h < h_; h++) {
        i = h * w_;
        for (w = 0; w < w_; w++) {
            if (w != 0 && h != 0 && w != w_ - 1 && h != h_ - 1) {
                sum = 1.0f / 9.0f * (
                        current[i + w - 1 - w_] + current[i + w - w_] + current[i + w + 1 - w_] +
                        current[i + w - 1] + current[i + w] + current[i + w + 1] +
                        current[i + w - 1 + w_] + current[i + w + w_] + current[i + w + 1 + w_]
                );
            } else {
                sum = current[i + w];
            }

            bitmapStorage[bitmapIndex++] = sum;
            bitmapStorage[bitmapIndex++] = sum;
            bitmapStorage[bitmapIndex++] = sum;
            bitmapStorage[bitmapIndex++] = (jbyte) 0xFF;
        }
    }

    /*// Collapse our 2D image into 1D for the belt.
    int collapsed = 0;
    for (w = 0; w < (w_ / BELT_SPLIT); w++) {
        for (h = 0; h < h_; h++) {
            if (current[h * w] == 255) collapsed++;
        }

        
    }*/

    (*env)->ReleaseByteArrayElements(env, current_, current, 0);
    (*env)->ReleaseByteArrayElements(env, background_, background, 0);
    (*env)->ReleaseByteArrayElements(env, bitmapStorage_, bitmapStorage, 0);
    (*env)->ReleaseBooleanArrayElements(env, beltArray_, beltArray, 0);
}

JNIEXPORT void JNICALL
Java_com_example_theia_OpenCVActivity_Test(JNIEnv *env, jobject instance) {

    // TODO

}