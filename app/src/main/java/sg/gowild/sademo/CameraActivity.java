package sg.gowild.sademo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import FacialRecognition.FacialRecognitionConfiguration;

public class CameraActivity extends Activity implements PictureCallback, SurfaceHolder.Callback {

    public static final String EXTRA_CAMERA_DATA = "camera_data";

    private static final String KEY_IS_CAPTURING = "is_capturing";

    private Camera mCamera;
    private ImageView mCameraImage;
    private SurfaceView mCameraPreview;
    private Button mCaptureImageButton;
    private byte[] mCameraData;
    private boolean mIsCapturing;

    private ProgressBar progressBar;

     static String patientFaceID;

    private OnClickListener mCaptureImageButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            captureImage();
        }
    };

    private OnClickListener mRecaptureImageButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setupImageCapture();
        }
    };




    private OnClickListener mDoneButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCameraData != null) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_CAMERA_DATA, mCameraData);
                setResult(RESULT_OK, intent);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);


        progressBar = findViewById(R.id.progressbar);
        progressBar.setVisibility(View.INVISIBLE);

        mCameraImage = (ImageView) findViewById(R.id.camera_image_view);
        mCameraImage.setVisibility(View.INVISIBLE);

        mCameraPreview = (SurfaceView) findViewById(R.id.preview_view);
        final SurfaceHolder surfaceHolder = mCameraPreview.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mCaptureImageButton = (Button) findViewById(R.id.capture_image_button);
        mCaptureImageButton.setOnClickListener(mCaptureImageButtonClickListener);

        Button doneButton = (Button) findViewById(R.id.done_button);
        doneButton.setOnClickListener(mDoneButtonClickListener);


        mCaptureImageButton.setVisibility(View.INVISIBLE);
        doneButton.setVisibility(View.INVISIBLE);

        mIsCapturing = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putBoolean(KEY_IS_CAPTURING, mIsCapturing);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mIsCapturing = savedInstanceState.getBoolean(KEY_IS_CAPTURING, mCameraData == null);
        if (mCameraData != null) {
            //setupImageDisplay();
        } else {
            //setupImageCapture();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCamera == null) {
            try {


                int cameraIndex = -1;
                int cameraCount = Camera.getNumberOfCameras();
                for (int i = 0; i < cameraCount && cameraIndex == -1; i++) {
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        cameraIndex = i;
                    }
                }
                if (cameraIndex != -1) {}

                mCamera = Camera.open(cameraIndex);
                //set camera to continually auto-focus
                Camera.Parameters params = mCamera.getParameters();
//*EDIT*//params.setFocusMode("continuous-picture");
//It is better to use defined constraints as opposed to String, thanks to AbdelHady
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(params);
                mCamera.setPreviewDisplay(mCameraPreview.getHolder());
                if (mIsCapturing) {
                    mCamera.startPreview();
                }
            } catch (Exception e) {
                Toast.makeText(CameraActivity.this, "Unable to open camera.", Toast.LENGTH_LONG)
                        .show();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }




    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.setDisplayOrientation(90);
                //holder.setFixedSize(100,100);
                mCamera.setPreviewDisplay(holder);
                if (mIsCapturing) {
                    mCamera.startPreview();
                    mCamera.startFaceDetection();
                    mCamera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
                        @Override
                        public void onFaceDetection(Camera.Face[] faces, Camera camera) {

                            if(faces.length > 0){
                                if(faces[0].score == 100){
                                    mCamera.stopFaceDetection();
                                    MainActivity.textToSpeech.speak(MainActivity.lang.getVERIFYING_FACE_RESPONSE(), TextToSpeech.QUEUE_FLUSH,null);
                                    System.out.println("fafjafakfjakjfakjf " + faces[0].score);
                                    captureImage();

                                    //mCamera.stopPreview();
                                    progressBar.setVisibility(View.VISIBLE);
                                    //mCameraPreview.setVisibility(View.INVISIBLE);
                                }
                            }






                        }
                    });

                }
            } catch (IOException e) {
                Toast.makeText(CameraActivity.this, "Unable to start camera preview.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }



    private void captureImage() {
        mCamera.takePicture(null, null, this);
    }

    public static final String FACIAL_RECOGNITION_DATA = "fr_DATA";
    public static final int FACIAL_RECOGNITON_RESULT = 888;


    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        //textToSpeech.speak("Verifying Face",TextToSpeech.QUEUE_FLUSH,null);


        mCameraData = data;


        //convert byte to bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(mCameraData, 0, mCameraData.length);
        bitmap = RotateBitmap(bitmap,-90);

        //bitmap to byte
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        bitmap.recycle();



        //ONCE PICTURE TAKEN CALL FACIAL RECONGNITION
        //AND PASS DATA TO MAIN ACTIVITY
        try {
            System.out.println("gagagagagagaa" + patientFaceID);
            boolean isValid = new FacialRecognitionConfiguration().execute(byteArray,patientFaceID).get();
            Intent intent = new Intent();
            intent.putExtra(FACIAL_RECOGNITION_DATA,isValid);
            setResult(FACIAL_RECOGNITON_RESULT,intent);
            finish();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }




        //setupImageDisplay();
    }


    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void setupImageCapture() {
        mCameraImage.setVisibility(View.INVISIBLE);
        mCameraPreview.setVisibility(View.VISIBLE);
        mCamera.startPreview();
        mCaptureImageButton.setText("SNAP IT");
        mCaptureImageButton.setOnClickListener(mCaptureImageButtonClickListener);
    }
//
    private void setupImageDisplay() {
        Bitmap bitmap = BitmapFactory.decodeByteArray(mCameraData, 0, mCameraData.length);
        bitmap = RotateBitmap(bitmap,270);
        mCameraImage.setImageBitmap(bitmap);
        mCamera.stopPreview();
        mCameraPreview.setVisibility(View.INVISIBLE);
        mCameraImage.setVisibility(View.VISIBLE);
        mCaptureImageButton.setText("TAKE AGAIN");
        //mCaptureImageButton.setOnClickListener(mRecaptureImageButtonClickListener);
    }
}