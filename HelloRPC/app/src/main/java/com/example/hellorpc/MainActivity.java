package com.example.hellorpc;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.Bundle;
import org.opencv.android.*;
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
    float current_acceleration=0;

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



    public List<double[]> average_slope_intercept(Mat frame, Mat line_segments)
    {
        /*
        This function combines line segments into one or two lane lines
        If all line slopes are < 0: then we only have detected left lane
        If all line slopes are > 0: then we only have detected right lane
        */

        List<double[]> lane_lines = new ArrayList<>();
        List<double[]> mean_value= new ArrayList<>();

        int lane_ind= 0;

        if (line_segments.rows()==0)
        {
            Log.i("average_slope", "'No line_segment segments detected'");
        }

        double height = frame.rows();
        double width = frame.cols();


        int lef_indx=0;
        int right_indx=0;

        double boundary = 1/3;
        double left_region_boundary = width * (1 - boundary) ; // left lane line segment should be on left 2/3 of the screen
        double right_region_boundary = width * boundary ;// right lane line segment should be on left 2/3 of the screen

        //coeffienti m e p medi per entrambe le sctrisc e
        double m_mean_left= 0;
        double m_mean_right= 0;
        double q_mean_left= 0;
        double q_mean_right= 0;

        for (int x= 0; x<line_segments.rows(); x++)
        {
            double[] l = line_segments.get(x, 0);
            double x1= l[0];
            double y1= l[1];
            double x2= l[2];
            double y2= l[3];

            Log.i("average_slope", "x1"+String.valueOf(x1)+"x2"+String.valueOf(x2)+"y1"+String.valueOf(y1)+"y2"+String.valueOf(y2));

            if(x1==x2)
            {
                Log.i("average_slope", "skipping vertical line segment (slope=inf): %s"+String.valueOf(l));
                continue;
            }

            double m=(y2-y1)/(x2-x1);
            double q= (x2*y1-x1*y2)/(x2-x1);

            //se l'angolo è minore di zero vuol dire che è della striscia sinistra
            if (m <0)
            {
                if (x1 < left_region_boundary && x2 < left_region_boundary)
                {
                    lef_indx++;

                    m_mean_left+= m;
                    q_mean_left+= q;

                    Imgproc.line(frame, new Point(x1,y1), new Point(x2,y2), new Scalar(255, 0, 255), 1, Imgproc.LINE_AA, 0);

                }
            }
            else
            {
                if (x1 > right_region_boundary && x2 > right_region_boundary)
                {
                    right_indx++;

                    m_mean_right+= m;
                    q_mean_right+= q;
                    Imgproc.line(frame, new Point(x1,y1), new Point(x2,y2), new Scalar(255, 255, 0), 1, Imgproc.LINE_AA, 0);


                }
            }
        }

        //faccio a media dei parametri ottenuti per entrambe le strisce
        m_mean_left= m_mean_left/(lef_indx);
        m_mean_right= m_mean_right/(right_indx);
        q_mean_left= q_mean_left/(lef_indx);
        q_mean_right= q_mean_right/(right_indx);


        if( lef_indx > 0){
            lane_lines.add(new double[]{ m_mean_left, q_mean_left});
            lane_ind++;
        }

        if(right_indx> 0){
            lane_lines.add(new double[]{ m_mean_right, q_mean_right});
            lane_ind++;
        }
        Log.i("lane line", "number line "+String.valueOf(lane_lines.size()));


        return lane_lines;

    }


    public Point[] make_points(Mat frame, double[] line)
    {
        double height = frame.rows();
        double width = frame.cols();
        double m= line[0];
        double q= line[1];

        double y1 = height;  // bottom of the frame
        double y2 = y1 * 1 / 2 ; // make points from middle of the frame down

        //double x1 = Math.max(-width, Math.min(2 * width, ((y1 - q) / m)));
        //double x2 = Math.max(-width, Math.min(2 * width, ((y2 - q) / m)));
        double x1 = (y1 - q) / m;
        double x2 = (y2 - q) / m;

        Point p1= new Point(x1,y1);
        Point p2= new Point(x2,y2);

        Point[] points= {p1,p2};
        return  points;
    }

    public Point defineCentralLine(Mat image , List<double[]> lane_lines)
    {
        if(lane_lines.size()==0)
            return null;

        double height = image.rows();
        double width = image.cols();

        double x_offset;
        double y_offset;


        if(lane_lines.size()==2)
        {
            Point[] pointsLeft= make_points(image, lane_lines.get(0));
            Point[] pointsRight= make_points(image, lane_lines.get(1));

            double left_x2= pointsLeft[1].x;
            double right_x2= pointsRight[1].x;


            int mid = (int)(width / 2);
            x_offset = (left_x2 + right_x2) / 2 ;
            y_offset = (height / 2);

            //per disegnare la curva uso anche x1
            double left_x1= pointsLeft[0].x;
            double right_x1= pointsRight[0].x;

            double xmean= (left_x1 + right_x1) / 2 ;

            Imgproc.line(image,new Point(width/2, pointsLeft[0].y), new Point(x_offset, y_offset), new Scalar(255, 0, 0), 10, Imgproc.LINE_AA, 0);
        }
        else
        {
            Point[] points= make_points(image, lane_lines.get(0));

            double x1= points[0].x;
            double x2= points[1].x;

            
            x_offset = x2 - x1;
            y_offset = (height / 2);

            Imgproc.line(image,new Point(width/2, points[0].y), new Point(x_offset, y_offset), new Scalar(255, 0, 0), 10, Imgproc.LINE_AA, 0);
        }

        Point[] points = make_points(image, lane_lines.get(0));

        return new Point(x_offset, y_offset);


    }


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

        Imgproc.HoughLinesP(edge, lines, 1, Math.PI/180, 20, 50, 4); // runs the actual detection
        Log.i("line Hought", "number line with HOugh "+String.valueOf(lines.rows()));



        // disegno tutte le piccole curve di hogh
        /*
        for (int x = 0; x < lines.rows(); x++) {
            double[] l = lines.get(x, 0);
            Imgproc.line(image, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(255, 0, 255), 1, Imgproc.LINE_AA, 0);
        }

         */


        //individuo e disegno le due linee della corsia
        List<double[]> lane_line= average_slope_intercept(image, lines);

        // disegno le curve delle corsia
        for (int x = 0; x < lane_line.size(); x++) {
            double[] line = lane_line.get(x);
            Point[] points= make_points(image, line);
            Imgproc.line(image,points[0], points[1], new Scalar(127, 255, 212), 10, Imgproc.LINE_AA, 0);
        }


        Point p_offset= defineCentralLine(image , lane_line);


        if (p_offset!=null)
        {
            Point[] points= make_points(image, lane_line.get(0));
            Point p2= new Point(image.cols()/2, points[0].y);

            //double angle_to_mid_radian = Math.atan((p_offset.x / p_offset.y));  // angle (in radian) to center vertical line
            //double angle_to_mid_deg = (angle_to_mid_radian * 180.0 / Math.PI);  // angle (in degrees) to center vertical line
            //double steering_angle = angle_to_mid_deg + 90 ; // this is the steering angle needed by picar front wheel

            double angle_to_mid_radian = Math.atan((p_offset.x-p2.x)/(p_offset.y-p2.y));
            angle_to_mid_radian= -angle_to_mid_radian;
            //angle_to_mid_radian= angle_to_mid_radian-Math.PI/2;

            //double modulVector= Math.sqrt(Math.pow(p_offset.x, 2)+Math.pow(p_offset.y,2));
            //double angle_to_mid_radian=Math.asin(p_offset.y/modulVector);

            Log.i("Steering angle", "current steering calculate"+String.valueOf(angle_to_mid_radian));

            //definisco alcuni parametri per la stabilizzazione dell'angolo
            double max_angle_deviation_two_lines=1;
            double max_angle_deviation_one_lane=0.9;
            double max_angle_deviation;

            double stabilized_steering_angle=0;


            if(lane_line.size()==2)
            {
                max_angle_deviation= max_angle_deviation_two_lines;

                current_acceleration=0.2f;
            }
            else
            {
                max_angle_deviation= max_angle_deviation_one_lane;

                current_acceleration=0.2f;

            }
            double angle_deviation = +current_steering - angle_to_mid_radian;

            if(Math.abs(angle_deviation)>max_angle_deviation)
            {
                stabilized_steering_angle = current_steering
                    + max_angle_deviation * (angle_deviation / Math.abs(angle_deviation));
            }
            else
            {
                stabilized_steering_angle = angle_to_mid_radian;
            }

            current_steering= (float) angle_to_mid_radian;
            controllCar(current_steering, current_acceleration);

        }

        /*
        if (lines.rows()>0)
        {
            //rho_s = lines.get(0, 0)[0];
            //theta_s = lines.get(0, 0)[1];

            /*
            double a = Math.cos(theta_s), b = Math.sin(theta_s);
            double x0 = a*rho_s, y0 = b*rho_s;
            Point pt1 = new Point(Math.round(x0 + 1000*(-b)), Math.round(y0 + 1000*(a)));
            Point pt2 = new Point(Math.round(x0 - 1000*(-b)), Math.round(y0 - 1000*(a)));



            double[] l1 = lines.get(0, 0);
            double[] l2 = lines.get(lines.rows()-1, 0);
            double[] l_mean = {l1[0]+l2[0]/2, l1[1]+l2[1]/2, l1[2]+l2[2]/2, l1[3]+l2[3]/2};






            long time = System.nanoTime();
            int delta_time = (int) ((time - last_time) / 1000000);
            last_time = time;

            float current_theta=0;
            float error_angles= current_theta-current_steering;


            float current_force= 0.5f;

            float delta_angle= 0.01f;

            //double modulVector= Math.sqrt(Math.pow(x0, 2)+Math.pow(y0,2));
            //current_theta=(float)Math.asin(y0/modulVector);

            Log.i("Current theta", String.valueOf(current_theta));
            Log.i("current_steering", String.valueOf(current_steering));
            Log.i("error_angles", String.valueOf(error_angles));

            double max_angle_deviation = (5/180)*(Math.PI);

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
            //controllCar(current_steering, 0);



        }
        else
        {
            controllCar(0,0.5f);
        }

         */



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