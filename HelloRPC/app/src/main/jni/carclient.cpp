#include <jni.h>
#include "carclient.h"
#include <vehicles/car/api/CarRpcLibClient.hpp>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/features2d.hpp>
using namespace msr::airlib;
CarRpcLibClient * m_client;

JNIEXPORT jboolean JNICALL Java_com_example_hellorpc_MainActivity_CarConnect(JNIEnv *env, jobject)
{
    m_client = new CarRpcLibClient("10.0.2.2");
    m_client->confirmConnection();
    m_client->enableApiControl(true);
    bool isEnabled = m_client->isApiControlEnabled();
    return isEnabled;
}

JNIEXPORT void JNICALL Java_com_example_hellorpc_MainActivity_CarForward(JNIEnv *env, jobject)
{
if(!m_client)
    return;
    CarApiBase::CarControls controls;
    controls.throttle = 0.5f;
    controls.steering = 0.0f;
    m_client->setCarControls(controls);
}

JNIEXPORT void JNICALL Java_com_example_hellorpc_MainActivity_GetFrameNative(JNIEnv *env, jobject, jlong addrImg)
{
    using std::cin;
    using std::cout;
    using std::endl;
    using std::vector;
    using cv::Mat;
    using cv::imdecode;
    using namespace msr::airlib;
    typedef ImageCaptureBase::ImageRequest ImageRequest;
    typedef ImageCaptureBase::ImageResponse ImageResponse;
    typedef ImageCaptureBase::ImageType ImageType;
    typedef common_utils::FileSystem FileSystem;

        Mat& Img = *(Mat*)addrImg;

        m_client->confirmConnection();

        std::cout << "Press Enter to get FPV image" << std::endl; std::cin.get();
        vector<ImageRequest> request = { ImageRequest("0", ImageType::Scene,false)/*, ImageRequest("1", ImageType::DepthPlanar, true) */};
        const vector<ImageResponse>& response = m_client->simGetImages(request);
        std::cout << "# of images received: " << response.size() << std::endl;
        //TODO: only print response size
        if (response.size() > 0)
                {


                    int camera_0_img_width = response.at(0).width;
                    int camera_0_img_height = response.at(0).height;
                    //std::cout << "Image uint8 size: " << camera_0_img_width <<std::endl;
                    // create Mat to save img
                    //cv::Mat camera_0_img = cv::Mat(144, 256, CV_8UC4, cv::Scalar(0, 0, 0, 0));

                    cv::Mat camera_0_img = cv::imdecode(response.at(0).image_data_uint8, cv::ImreadModes::IMREAD_GRAYSCALE);
                    //  vector<uint8_t> to Mat
                    /*for (int row = 0; row < response.at(0).height; row++)
                    {
                        for (int col = 0; col < response.at(0).width; col++)
                        {
                            camera_0_img.at<cv::Vec4b>(row, col) = cv::Vec4b(
                                    response.at(0).image_data_uint8[row*camera_0_img_width*4 + 4*col + 2],
                                    response.at(0).image_data_uint8[row*camera_0_img_width*4 + 4*col + 1],
                                    response.at(0).image_data_uint8[row*camera_0_img_width*4 + 4*col + 0],
                                    response.at(0).image_data_uint8[row*camera_0_img_width*4 + 4*col + 3]);
                        }
                    }*/
                    Img = camera_0_img;

                }
}