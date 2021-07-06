extern "C" {

JNIEXPORT jboolean JNICALL Java_com_example_hellorpc_MainActivity_CarConnect(JNIEnv *env, jobject);
JNIEXPORT void JNICALL Java_com_example_hellorpc_MainActivity_CarForward(JNIEnv *env, jobject);
JNIEXPORT void JNICALL Java_com_example_hellorpc_MainActivity_controllCar(JNIEnv *env, jobject, float angle, float force);
JNIEXPORT void JNICALL Java_com_example_hellorpc_MainActivity_GetFrameNative(JNIEnv *env, jobject, jlong addrImg);
}