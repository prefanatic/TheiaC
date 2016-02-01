package com.example.theia;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.github.prefanatic.theia.R;
import timber.log.Timber;

public class OpenCVActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    @Bind(R.id.camera_view) CameraView cameraView;

    private boolean captureBackground = false;
    private Mat background;

    private Mat mGray;
    private Mat mRgb;
    private Mat mFGMask;
    private List<MatOfPoint> contours;
    private double lRate = 0.5;

    static {
        //System.loadLibrary("opencv_java3");
        System.loadLibrary("native");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    cameraView.enableView();

                    cameraView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            captureBackground = true;
                        }
                    });
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_cv);
        ButterKnife.bind(this);

        cameraView.setVisibility(View.VISIBLE);
        cameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Timber.d("Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Timber.d("OpenCV library found in package.");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        cameraView.setResolution(cameraView.getResolutionList().get(10));

        //creates a new BackgroundSubtractorMOG class with the arguments

        //creates matrices to hold the different frames
        mRgb = new Mat();
        mFGMask = new Mat();
        mGray = new Mat();

        //arraylist to hold individual contours
        contours = new ArrayList<MatOfPoint>();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Imgproc.bilateralFilter(inputFrame.gray(), inputFrame.rgba(), 9, 75, 75);

       /* if (captureBackground) {
            captureBackground = false;
            background = new Mat(inputFrame.gray(), new Rect(0, 0, inputFrame.gray().width(), inputFrame.gray().height()));
        }

        if (background == null) {
            return inputFrame.rgba();
        }


        return background;*/

        background = inputFrame.gray();

        Imgproc.cvtColor(background, mRgb, Imgproc.COLOR_GRAY2RGB);

        /*Imgproc.erode(mFGMask, mFGMask, new Mat());
        Imgproc.dilate(mFGMask, mFGMask, new Mat());

        Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        Imgproc.drawContours(mRgb, contours, -1, new Scalar(255, 0, 0), 2);*/

        return mRgb;
    }

    public native void Test();
}
