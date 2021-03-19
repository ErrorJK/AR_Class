package com.crash.ar_class;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;

import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;

import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompatSideChannelService;
import androidx.core.content.ContextCompat;

import butterknife.BindView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crash.ar_class.GraphicUtils.GraphicOverlay;
import com.crash.ar_class.GraphicUtils.TextGraphic;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

        private Button button;
        private TextureView textureView;
        public TextView textView;
        private Boolean isCameraSet = false;
        public String currentCabinet = "";
        public boolean isDetect = false;
        @SuppressLint("NonConstantResourceId")
        GraphicOverlay mGraphicOverlay;
        final DatabaseHelper helper = new DatabaseHelper(this);
        ArrayList array_list;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            textureView = findViewById(R.id.textureView);
            textView = (TextView)findViewById(R.id.textView);
            SurfaceTexture surfaceTexture = new SurfaceTexture(1);
            textureView.setSurfaceTexture(surfaceTexture);
            setupCamerax();

        }

        protected void setupCamerax() {
            if (isCameraSet) return;
            isCameraSet = true;
            CameraX.unbindAll();

            Preview preview = new Preview(
                    new PreviewConfig.Builder().build()
            );

            preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
                @Override
                public void onUpdated(@NonNull Preview.PreviewOutput output) {
                    textureView.setSurfaceTexture(output.getSurfaceTexture());
                }
            });

            Size size;
            size = new Size(900  , 1900);

            ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                    .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                    .setTargetResolution(size)
                    .build();
            ImageAnalysis imageAnalyzer = new ImageAnalysis(imageAnalysisConfig);


            ExecutorService executor = Executors.newFixedThreadPool(1);
            imageAnalyzer.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(ImageProxy image, int rotationDegrees) {

                    runTextRecognition(image, rotationDegrees);
                }
            });
            CameraX.bindToLifecycle(this, preview, imageAnalyzer);

        /*
        //TODO: Implement for debugging
        previewBuilder.setTargetName();
        */
        }


    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }

    private void runTextRecognition(ImageProxy imageProxy, int degrees) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            return;
        }

        Image mediaImage = imageProxy.getImage();
        int rotation = degreesToFirebaseRotation(degrees);
        FirebaseVisionImage image =  FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        detector.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                processTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });
    }


    private void processTextRecognitionResult(FirebaseVisionText texts) {
        List<FirebaseVisionText.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            textView.setText("Наведите на кабинет");
            if(mGraphicOverlay!= null)
                mGraphicOverlay.clear();

            return;
        }
        mGraphicOverlay = (GraphicOverlay)findViewById(R.id.graphic_overlay) ;

        if(mGraphicOverlay!= null)
            mGraphicOverlay.clear();

        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {

                   GraphicOverlay.Graphic textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
                   mGraphicOverlay.add(textGraphic);
                    String elementText = elements.get(k).getText();

                //    textView.setText(elementText);
                    if(!elementText.equals(""))
                        createText(elementText);

                }
            }
        }
    }

    private void createText(String text)    {

            String txt = "";
            // Текущее время
        Date currentDate = new Date();
            // Форматирование времени как "день.месяц.год"
        // DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        // String dateText = dateFormat.format(currentDate);
        // Форматирование времени как "часы:минуты:секунды"
         DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
         String timeText = timeFormat.format(currentDate);

       //проверка дня недели
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

      // array_list = helper.getSchoolClass(text); //получение из базы по номеру кабинета
      //  array_list.clear();

       // для наглядной проверки работы

        if(dayOfWeek != 0 && dayOfWeek != 7 )  // СБ И ВСКР
        {
        switch (text) {
            case "283": { if(!currentCabinet.equals("283")){
                txt =   "Кабинет: 283" + "\r\n" +
                        "Урок: Информатика" + "\r\n" +
                        "Класс: 8" + "\r\n" +
                        "Учитель: Петров И.И." + "\r\n";
            }
                break;
            }
            case "241": { if(!currentCabinet.equals("241")) {
                txt =   "Кабинет: 241" + "\r\n" +
                        "Урок: Классный час" + "\r\n" +
                        "Класс: 10" + "\r\n" +
                        "Учитель: Иванов И.И." + "\r\n";
            }
                break;
            }
        }
        }
        else
            {
                txt =   "Сегодня выходной";
        }

        if(!txt.equals("")) {
            currentCabinet = text;
            textView.setText(txt);
                }
    }
}