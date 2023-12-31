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

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import org.opencv.core.Range;
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
    private MatOfPoint2f src;
    private List<PointF> cordsDst = new ArrayList<PointF>();
    private MatOfPoint2f dst;


    private String imageDstPath;
    private String imageSrcPath;
    private Bitmap resultBitmap;

    private static final int REQUEST_CODE = 1;
    private String imageName = "";
    private Bitmap bitmapSrc;
    private Bitmap bitmapDst;
    private Point center_source;

    private double  max_x,max_y,min_x,min_y=0.0;

    double maxX = Integer.MIN_VALUE; // Initialize with a very small value
    double maxY = Integer.MIN_VALUE;

    private MatOfPoint2f dstPrueba = new MatOfPoint2f(new Point(254.944,958.4771),
            new Point(476.73565,673.9331),new Point(912.9345,1001.8),new Point(689.0405,1287.3));
    private MatOfPoint2f srcPrueba = new MatOfPoint2f(new Point(955.4076,321.9467),
            new Point(1637.5,837.4848),new Point(854.8274,1865.6),new Point(170.5218,1344.9));

    private MatOfPoint2f dstPrueba2 = new MatOfPoint2f(new Point(170.7500,997.2500),
            new Point(415.2500,679.2500),new Point(914.7500,1054.3),new Point(659.7500,1379.8));
    private MatOfPoint2f srcPrueba2= new MatOfPoint2f(new Point(1042.0,254.0),
            new Point(1656.0,718.00),new Point(951.0,1644),new Point(336.000,1183));

    private MatOfPoint2f dstPrueba3 = new MatOfPoint2f(new Point(101.8,1178.7),
            new Point(341.7,908.8),new Point(836.7,1309.3),new Point(607.2,1570.2));
    private MatOfPoint2f srcPrueba3= new MatOfPoint2f(new Point(2740.5,1513.5),
            new Point(3952.5,2203.5),new Point(96.5,2873.5),new Point(168.5,1357.5));

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
            imageSrcPath = extras.getString("imageSrc");
            imageDstPath = extras.getString("imageDst");

        ArrayList<Parcelable> parcelableDst = extras.getParcelableArrayList("dstPoints");
        ArrayList<Parcelable> parcelableSrc = extras.getParcelableArrayList("srcPoints");

            for (Parcelable parcelable : parcelableDst) {
                cordsDst.add((PointF) parcelable);
            }
            for (Parcelable parcelable : parcelableSrc) {
                cordsSrc.add((PointF) parcelable);
            }
        }

        bitmapSrc = BitmapFactory.decodeFile(imageSrcPath);
        bitmapDst = BitmapFactory.decodeFile(imageDstPath);
        btMapa = findViewById(R.id.bt_map);
        homoImg = findViewById(R.id.homographyImg);

        convertToMat(cordsSrc,cordsDst);



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

                DataHolder.getInstance().setData(imageName);

                Intent intent  = new Intent(ImageProjection.this, MapsActivity.class);
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
        //Mat homographyMatrix = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
        Mat homographyMatrix = Calib3d.findHomography(srcPoints,dstPoints);
        System.out.println(homographyMatrix.dump());

        getCenterSourceAverage(cordsSrc);
        Point dstCenterPoint = transformPoint(center_source,homographyMatrix);

        //System.out.println(center_source.x +","+ center_source.y);
        System.out.println(dstCenterPoint.x +","+ dstCenterPoint.y);

        List<PointF> image = new ArrayList<>();
        image.add( new PointF(0,0));
        image.add( new PointF(bitmapSrc.getWidth(),0));
        image.add( new PointF(bitmapSrc.getWidth(),bitmapSrc.getHeight()));
        image.add( new PointF(0,bitmapSrc.getHeight()));
        //System.out.println(image.stream());

        List<Point> transformArray =transformArray(image,homographyMatrix);
        //System.out.println(transformArray.stream().toString());
        shortestDistance(dstCenterPoint,transformArray);

        System.out.println("min x "+ min_x);
        System.out.println("min y "+ min_y);
        System.out.println("max x "+ max_x);
        System.out.println("max y "+ max_y);
        // Create a new Mat for the result image

        for (Point point : transformArray) {
            if (point != null) { // Check for null points if needed
                 maxX = Math.max(maxX, point.x);
                 maxY = Math.max(maxY, point.y);
            }
        }

        System.out.println(maxX);
        System.out.println(maxY);
        Mat resultImage = new Mat();
        // Apply perspective transformation to the source image
        Size targetSize;

        if ((int) maxX > bitmapDst.getWidth() && (int) maxY > bitmapDst.getHeight()) {
            targetSize = new Size(maxX + 1, maxY + 1);
        } else if ((int) maxX > bitmapDst.getWidth()) {
            targetSize = new Size(maxX + 1, bitmapDst.getHeight());
        } else if ((int) maxY > bitmapDst.getHeight()) {
            targetSize = new Size(bitmapDst.getWidth(), maxY + 1);
        } else {
            targetSize = new Size(bitmapDst.getWidth(), bitmapDst.getHeight());
        }
        Imgproc.warpPerspective(sourceImage, resultImage, homographyMatrix, targetSize);
        Bitmap result = Bitmap.createBitmap(resultImage.cols(),resultImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultImage, result);

        Range row = new Range((int) Math.abs(Math.round(dstCenterPoint.y - max_y)), (int) Math.abs(Math.round(dstCenterPoint.y+max_y)));
        Range column = new Range((int) Math.abs(Math.round(dstCenterPoint.x-max_x)), (int) Math.abs(Math.round(dstCenterPoint.x+max_x)));


        System.out.println(row.toString() + ","+ column.toString());

        Mat resultImage_cut= resultImage.submat(row,column);
// Convert the result image to bitmap
        Bitmap resultBitmap = Bitmap.createBitmap(resultImage_cut.cols(),resultImage_cut.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultImage_cut, resultBitmap);
// Set the resulting bitmap to your ImageView or perform any other operation as needed
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

            Toast.makeText(ImageProjection.this,"Image Saved Successfully",Toast.LENGTH_SHORT).show();

        }catch (Exception e){

            Toast.makeText(ImageProjection.this,"Image not Saved",Toast.LENGTH_SHORT).show();
            e.printStackTrace();

        }

    }


    private void getCenterSourceAverage(List<PointF>list){
        double centerX = 0.0;
        double centerY = 0.0;

        for (PointF point : list){
            centerX += point.x;
            centerY += point.y;
        }

        center_source =new Point(centerX/4.0,centerY/4.0);

    }
    private void convertToMat(List<PointF> source, List<PointF> destiny){

         src = new MatOfPoint2f(new Point( source.get(0).x,source.get(0).y),
                new Point( source.get(1).x,source.get(1).y),
                new Point( source.get(2).x,source.get(2).y),
                new Point( source.get(3).x,source.get(3).y));

         dst = new MatOfPoint2f(new Point( destiny.get(0).x,destiny.get(0).y),
                 new Point( destiny.get(1).x,destiny.get(1).y),
                 new Point( destiny.get(2).x,destiny.get(2).y),
                 new Point( destiny.get(3).x,destiny.get(3).y));

         warpedImage(src,dst);

    }

    private Point transformPoint(Point point, Mat homographyMatrix) {
        Mat srcPointMat = new Mat(3, 1, CvType.CV_64FC1);
        srcPointMat.put(0, 0, point.x);
        srcPointMat.put(1, 0, point.y);
        srcPointMat.put(2, 0, 1.0);

        Mat dstPointMat = new Mat(3, 1, CvType.CV_64FC1);

        Core.gemm(homographyMatrix,srcPointMat,1,new Mat(),0,dstPointMat);
        double w = dstPointMat.get(2, 0)[0];
        Point pointT= new Point(dstPointMat.get(0, 0)[0] / w, dstPointMat.get(1, 0)[0] / w);

        return pointT;
    }
    private List<Point> transformArray(List<PointF> points , Mat homography){

        List<Point> array = new ArrayList<>();
        for (PointF point: points){
            Mat srcPointMat = new Mat(3, 1, CvType.CV_64FC1);
            srcPointMat.put(0, 0, point.x);
            srcPointMat.put(1, 0, point.y);
            srcPointMat.put(2, 0, 1.0);

            Mat dstPointMat = new Mat(3, 1, CvType.CV_64FC1);

            Core.gemm(homography,srcPointMat,1,new Mat(),0,dstPointMat);
            double w = dstPointMat.get(2, 0)[0];
            Point pointT= new Point(dstPointMat.get(0, 0)[0] / w, dstPointMat.get(1, 0)[0] / w);
            array.add(pointT);
        }

       return array;

    }
    private void shortestDistance(Point center, List<Point>points){

        List<Double> distanceX = new ArrayList<>();
        List<Double> distanceY = new ArrayList<>();

        for(Point point: points){
            double d_x = Math.abs((point.x - center.x));
            double d_y = Math.abs((point.y - center.y));

            distanceX.add(d_x);
            distanceY.add(d_y);
        }
         min_x =  distanceX.stream().mapToDouble(Double ::doubleValue).min().getAsDouble();
         min_y =  distanceY.stream().mapToDouble(Double ::doubleValue).min().getAsDouble();

         max_x =  distanceX.stream().mapToDouble(Double ::doubleValue).max().getAsDouble();
         max_y =  distanceY.stream().mapToDouble(Double ::doubleValue).max().getAsDouble();
    }
 }


