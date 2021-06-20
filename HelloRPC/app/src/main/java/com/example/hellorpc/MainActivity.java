package com.example.hellorpc;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;


public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    static{
        System.loadLibrary("carclient");
    }
    public native boolean CarConnect();
    public native void CarForward();
    public native void GetFrameNative(long addrImg);
    private boolean isConnected = false;
    private boolean isImage = false;
    private CameraBridgeViewBase mOpenCVCameraView;
    final Handler handler = new Handler();
    final int delay = 20;
    int test = 0;
    public Mat img = new Mat(720,1280, CvType.CV_8UC4);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status){
            switch(status) {
                case LoaderCallbackInterface.SUCCESS:
                {

                    mOpenCVCameraView.enableView();
                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCVCameraView = findViewById(R.id.java_surface_view);
        mOpenCVCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCVCameraView.setCvCameraViewListener(this);
        handler.postDelayed(new Runnable() {
            public void run() {
                handler.postDelayed(this, delay);
                manageFrame(test);
                test++;
            }
        }, delay);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        }
        else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void OnButtonForward(View view) {
        TaskRunner runner = new TaskRunner();
        runner.executeAsync(new CustomCallable<Void>() {
            @Override
            public Void call() throws Exception {
                CarForward();
                return null;
            }

            @Override
            public void postExecute(Void result) {
                Toast.makeText(getApplicationContext(),"connection result " + result,Toast.LENGTH_LONG).show();
            }

            @Override
            public void preExecute() {
            }
        });
    }

    public void OnButtonConnect(View view) {
        TaskRunner runner = new TaskRunner();
        runner.executeAsync(new CustomCallable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                isConnected = true;
                return CarConnect();
            }

            @Override
            public void postExecute(Boolean result) {
                Toast.makeText(getApplicationContext(),"connection result " + result,Toast.LENGTH_LONG).show();
            }

            @Override
            public void preExecute() {
            }
        });
    }

    public void OnButtonImage(View view) {
        isImage = true;
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCVCameraView);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Mat img = new Mat(144,256, CvType.CV_8UC4);
        //if(isConnected)
        //{
        //    GetFrameNative(img.getNativeObjAddr());
        //}
        if(!isConnected)
        {
            img = inputFrame.rgba();
        }
        return null;

    }


    public Void manageFrame(int test)
    {
        System.out.println("myHandler: here!" + test); // Do your work here
        if(isImage)
        {
            GetFrameNative(img.getNativeObjAddr());

            //rilevo le linee nell'immaigne
            lineDetection(img);


            Bitmap bm = Bitmap.createBitmap(img.cols(), img.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bm);
            ImageView im = (ImageView) findViewById(R.id.imView);
            im.setImageBitmap(bm);
        }
        return null;
    }


    public void lineDetection(Mat inputImg)
    {
        
    }
}