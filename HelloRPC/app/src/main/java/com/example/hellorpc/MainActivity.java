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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.abs;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2HSV;




public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    static{
        System.loadLibrary("carclient");
    }

    public native boolean CarConnect();
    public native void CarForward();
    public native void controllCar(float angle, float force);
    public native void GetFrameNative(long addrImg);
    private boolean isConnected = false;
    private boolean isImage = false;
    private CameraBridgeViewBase mOpenCVCameraView;
    final Handler handler = new Handler();
    final int delay = 20;
    int test = 0;
    public Mat img = new Mat(720,1280, CvType.CV_8UC4);


    //angolo della macchina
    float current_steering=0;

    //Definisco i limiti per le soglie colore
    Scalar lowerbScalar = new Scalar(30,0,0); // Set the lowest range
    Scalar highbScalar = new Scalar(80, 255, 255); // Set the highest range

    long last_time = System.nanoTime();

    int count=4;

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
            Mat img2 = line_detection(img);


            //Disegno sulla prima ImageView l'immaigne ottenuta da simulatore
            Bitmap bm = Bitmap.createBitmap(img.cols(), img.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bm);
            ImageView im = (ImageView) findViewById(R.id.imView1);
            im.setImageBitmap(bm);


            Bitmap bm2 = Bitmap.createBitmap(img2.cols(), img2.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img2, bm2);
            ImageView im2 = (ImageView) findViewById(R.id.imView2);
            im2.setImageBitmap(bm2);
        }
        return null;
    }

    float past_theta=0;
    Boolean sign= true;

    public Mat line_detection(Mat image) {
        Mat colorImg = img.clone();

        //converto l'immaigne da RBG in HSV
        Mat hsvImg=colorImg;
        Mat edge= new Mat(720,1280, CvType.CV_8UC4);

        Imgproc.cvtColor(colorImg, hsvImg, COLOR_BGR2HSV);

        //vado a prendere un range di colori nell'immagine HSV
        Core.inRange(hsvImg, lowerbScalar,highbScalar, edge);


        //Imgproc.Canny(image, edge, 50, 200, 3, false);


        Mat lines = new Mat(); // will hold the results of the detection

        Imgproc.Canny(edge, edge, 200,400);

        //Imgproc.HoughLinesP(edge, lines, 1, Math.PI/180, 10, 20, 10); // runs the actual detection

        Imgproc.HoughLines(edge, lines, 1, Math.PI/180, 100);

        double rho_s=0;
        double theta_s=100;

        List<Double> listTheta=  new ArrayList();



        // Draw the lines
        for (int x = 0; x < lines.rows(); x++) {
            double rho = lines.get(x, 0)[0],
                    theta = lines.get(x, 0)[1];
            double a = Math.cos(theta), b = Math.sin(theta);
            double x0 = a*rho, y0 = b*rho;
            Point pt1 = new Point(Math.round(x0 + 1000*(-b)), Math.round(y0 + 1000*(a)));
            Point pt2 = new Point(Math.round(x0 - 1000*(-b)), Math.round(y0 - 1000*(a)));


            Imgproc.line(image, pt1, pt2, new Scalar(0, 0, 255), 1, Imgproc.LINE_AA, 0);


            if (theta<theta_s)
            {
                rho_s=rho;
                theta_s=theta;
            }

            listTheta.add(theta);


            //Log.i("Linee Hought:", "rho:"+String.valueOf(rho)+"  theta:"+String.valueOf(theta));
        }



        /*
        for (int x = 0; x < lines.rows(); x++) {
            double[] l = lines.get(x, 0);
            Imgproc.line(image, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255), 10, Imgproc.LINE_AA, 0);
        }

         */


        if (lines.rows()>0)
        {
            //rho_s = lines.get(0, 0)[0];
            //theta_s = lines.get(0, 0)[1];


            double a = Math.cos(theta_s), b = Math.sin(theta_s);
            double x0 = a*rho_s, y0 = b*rho_s;
            Point pt1 = new Point(Math.round(x0 + 1000*(-b)), Math.round(y0 + 1000*(a)));
            Point pt2 = new Point(Math.round(x0 - 1000*(-b)), Math.round(y0 - 1000*(a)));



            /*
            double[] l1 = lines.get(0, 0);
            double[] l2 = lines.get(1, 0);
            double[] l_mean = {l1[0]+l2[0]/2, l1[1]+l2[1]/2, l1[2]+l2[2]/2, l1[3]+l2[3]/2};

             */


            Imgproc.line(image, pt1, pt2, new Scalar(0, 255, 0), 3, Imgproc.LINE_AA, 0);



            long time = System.nanoTime();
            int delta_time = (int) ((time - last_time) / 1000000);
            last_time = time;

            float current_theta=(float)(theta_s);
            float error_angles= current_theta-current_steering;


            float current_force= 0.5f;

            float delta_angle= 0.01f;

            double modulVector= Math.sqrt(Math.pow(x0, 2)+Math.pow(y0,2));
            current_theta=(float)Math.asin(y0/modulVector);

            Log.i("Current theta", String.valueOf(current_theta));
            Log.i("current_steering", String.valueOf(current_steering));
            Log.i("error_angles", String.valueOf(error_angles));

            double max_angle_deviation = 3/(2*Math.PI);

            double angle_deviation = current_theta - current_steering;

            double stabilized_steering_angle;

            if (abs(angle_deviation) > max_angle_deviation)
            {
                stabilized_steering_angle = (int)(current_steering
                    + max_angle_deviation * angle_deviation / abs(angle_deviation));
            }
            else
            {
                stabilized_steering_angle = current_theta;
            }


            current_steering= (float) stabilized_steering_angle;
            controllCar(current_steering, 0.2f);



        }
        else
        {
            controllCar(0,0.5f);
        }



        return edge;
    }

    public int countElement(List list)
    {
        int element_C1=1;

        for (int i =1; i <list.size();i++)
        {
            if((Double)list.get(i-1)-(Double)list.get(i)<0.5f)
            {
                element_C1++;
            }
        }

        return element_C1;
    }
}