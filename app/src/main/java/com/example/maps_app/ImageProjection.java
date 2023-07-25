package com.example.maps_app;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import org.opencv.core.Size;

import org.opencv.imgproc.Imgproc;


import java.io.OutputStream;

import java.util.ArrayList;

import java.util.List;
import java.util.Objects;

public class ImageProjection extends AppCompatActivity {

    private Button btMapa;
    private ImageView homoImg;

    private List<PointF> cordsSrc = new ArrayList<PointF>();
    private List<PointF> cordsDst = new ArrayList<PointF>();


    private String imageDstPath;
    private String imageSrcPath;
    private Bitmap resultBitmap;

    private static final int REQUEST_CODE = 1;
    private String imageName = "";
    Bitmap bitmapSrc;
    Bitmap bitmapDst;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_projection);
        getSupportActionBar().hide();

        //gets the points from previous activity into a list
        Bundle extras  = getIntent().getExtras();
        if(extras !=null){
        ArrayList<Parcelable> parcelableDst = extras.getParcelableArrayList("dstPoints");
        ArrayList<Parcelable> parcelableSrc = extras.getParcelableArrayList("srcPoints");

            for (Parcelable parcelable : parcelableDst) {
                cordsDst.add((PointF) parcelable);
            }
            for (Parcelable parcelable : parcelableSrc) {
                cordsSrc.add((PointF) parcelable);
            }
        }

        btMapa = findViewById(R.id.bt_map);
        homoImg = findViewById(R.id.homographyImg);

        imageDstPath = extras.getString("imageDst");
        imageSrcPath = extras.getString("imageSrc");
        System.out.println(imageDstPath);
        System.out.println(imageSrcPath);


        bitmapSrc = BitmapFactory.decodeFile(imageSrcPath);
        bitmapDst = BitmapFactory.decodeFile(imageDstPath);

        calculateDisplacements(cordsSrc,cordsDst);


        btMapa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(ImageProjection.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    saveImage();
                }
                else{
                    ActivityCompat.requestPermissions(ImageProjection.this,new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },REQUEST_CODE);
                }


                Bundle bundle = new Bundle();
                bundle.putString("image",imageName);
                Intent intent  = new Intent(ImageProjection.this, MapsActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
                System.out.println(imageName);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE){
            if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                saveImage();
            }
            else{
                Toast.makeText(ImageProjection.this,"Please provide required permission",Toast.LENGTH_SHORT).show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void warpedImage(MatOfPoint2f srcPoints, MatOfPoint2f dstPoints){
        Mat sourceImage = new Mat();
        // Convert the Bitmap to Mat
        Utils.bitmapToMat(bitmapSrc, sourceImage);

        // Compute the homography matrix
        Mat homographyMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
        // Create a new Mat for the result image
        Mat resultImage = new Mat();

        // Apply perspective transformation to the source image
        Imgproc.warpPerspective(sourceImage, resultImage, homographyMatrix, new Size( sourceImage.cols(), sourceImage.rows()));

        // Convert the result image to bitmap
        resultBitmap = Bitmap.createBitmap(resultImage.cols(), resultImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultImage, resultBitmap);
        homoImg.setImageBitmap(resultBitmap);
    }


    private void saveImage(){
        Uri images;
        ContentResolver contentResolver = getContentResolver();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }
        else{
            images =  MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        ContentValues contentValues =  new ContentValues();
        imageName = System.currentTimeMillis() + ".jpg";
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME,imageName);
        contentValues.put(MediaStore.Images.Media.MIME_TYPE,"images/*");
        Uri uri = contentResolver.insert(images,contentValues);

        try{

            BitmapDrawable bitmapDrawable = (BitmapDrawable) homoImg.getDrawable();
            Bitmap bitmap = bitmapDrawable.getBitmap();

            OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(uri));
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
            Objects.requireNonNull(outputStream);

            Toast.makeText(ImageProjection.this,"Image Saved Succesfully",Toast.LENGTH_SHORT).show();

        }catch (Exception e){

            Toast.makeText(ImageProjection.this,"Image not Saved",Toast.LENGTH_SHORT).show();
            e.printStackTrace();

        }

    }

    public void calculateDisplacements(List<PointF>cordSrc,List<PointF>cordDst) {


        int height_image_src = bitmapSrc.getHeight();
        int width_image_src = bitmapSrc.getWidth();


        int height_image_dst = bitmapDst.getHeight();
        int width_image_dst = bitmapDst.getWidth();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height_screen = displayMetrics.heightPixels;
        int width_screen = displayMetrics.widthPixels;


        int a_src = Math.abs((height_screen-226)-height_image_src)/2;
        int b_src = Math.abs(width_screen-width_image_src)/2;

        int a_dst = Math.abs((height_screen-226)-height_image_dst)/2;
        int b_dst = Math.abs(width_screen-width_image_dst)/2;

        MatOfPoint2f src = new MatOfPoint2f(new Point( cordSrc.get(0).x-b_src,cordSrc.get(0).y-a_src),
                new Point( cordSrc.get(1).x-b_src,cordSrc.get(1).y-a_src),
                new Point( cordSrc.get(2).x-b_src,cordSrc.get(2).y-a_src),
                new Point( cordSrc.get(3).x-b_src,cordSrc.get(3).y-a_src));

        System.out.println(src.dump());

        MatOfPoint2f dst = new MatOfPoint2f(new Point( cordDst.get(0).x-b_dst,cordDst.get(0).y-a_dst),
                new Point( cordDst.get(1).x-b_dst,cordDst.get(1).y-a_dst),
                new Point( cordDst.get(2).x-b_dst,cordDst.get(2).y-a_dst),
                new Point( cordDst.get(3).x-b_dst,cordDst.get(3).y-a_dst));

        System.out.println(dst.dump());

        warpedImage(src,dst);

    }
}
