package com.example.maps_app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CornerSelection extends AppCompatActivity {
    private ImageButton add;
    private ImageView imageView;
    private Button homographybt;
    private Button send;
    private TextView imagen1;
    private TextView imagen2;
    private final int GALLERY_REQ_CODE = 1000;

    ArrayList<Parcelable> parcelableSrcPoints = new ArrayList<>();
    ArrayList<Parcelable> parcelableDstPoints = new ArrayList<>();
    public FrameLayout layout;
    private static final int REQUEST_CODE = 1337;

    List<PointF> dstPoints = new ArrayList<>();
    List<PointF> sourcePoints = new ArrayList<>();
    private Bitmap bitmap;
    private Bitmap resized;
    public String imagePath;
    public String imageDstPath;
    public String imageSrcPath;
    public int counter =0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corner_selection);

        OpenCVLoader.initDebug();
        getSupportActionBar().hide();
        imagen1 = findViewById(R.id.tv1);
        imagen2 = findViewById(R.id.tv2);

        add = findViewById(R.id.imageButton);
        imageView = findViewById(R.id.imageView);

        send = findViewById(R.id.homographybt2);
        homographybt = findViewById(R.id.homographybt);

        PolygonView pv = new PolygonView(CornerSelection.this);

        layout = findViewById(R.id.layout);
        layout.addView(pv);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent iGallery = new Intent(Intent.ACTION_PICK);
                iGallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(iGallery, GALLERY_REQ_CODE);


            }
        });
        homographybt.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (counter >0){
                    homographybt.setVisibility(View.INVISIBLE);
                }
                counter+=1;

                dstPoints = pv.getPoints();
                calculateDisplacements(dstPoints);

                for (PointF point : dstPoints) {
                    System.out.println(point);
                    parcelableDstPoints.add((Parcelable) point);
                }


                AlertDialog.Builder builder = new AlertDialog.Builder(CornerSelection.this);
                builder.setTitle("Points Confirmation");

                final TextView input = new TextView(CornerSelection.this);
                input.setText("      Do you want to send the previously selected points?");
                input.setGravity(Gravity.CENTER);

                builder.setView(input);


                builder.setPositiveButton("Send", (DialogInterface.OnClickListener) (dialog, which) -> {
                    imageDstPath = imagePath;
                    System.out.println(imageDstPath);
                    imagen1.setVisibility(View.INVISIBLE);
                    imagen2.setVisibility(View.VISIBLE);
                    imageView.setImageDrawable(null);
                    add.setVisibility(View.VISIBLE);
                    homographybt.setVisibility(View.INVISIBLE);
                    send.setVisibility(View.VISIBLE);
                    layout.setVisibility(View.INVISIBLE);

                });
                builder.setNegativeButton("Back", (DialogInterface.OnClickListener) (dialog, which) -> {
                    // If user click no then dialog box is canceled.
                    dialog.cancel();
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                alertDialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.rounder_rectangle));
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View view) {
                sourcePoints = pv.getPoints();
                calculateDisplacements(sourcePoints);
                for (PointF point : sourcePoints) {
                    System.out.println(point);
                    parcelableSrcPoints.add((Parcelable) point);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(CornerSelection.this);
                builder.setTitle("Points Confirmation");
                final TextView input = new TextView(CornerSelection.this);
                input.setText("      Do you want to send the previously selected points?");
                input.setGravity(Gravity.CENTER);

                builder.setView(input);
                builder.setPositiveButton("Send", (DialogInterface.OnClickListener) (dialog, which) -> {
                    imageSrcPath = imagePath;
                    System.out.println(imageSrcPath);
                    Intent intent = new Intent(CornerSelection.this, ImageProjection.class);
                    Bundle extras = new Bundle();
                    extras.putParcelableArrayList("dstPoints", parcelableDstPoints);
                    extras.putParcelableArrayList("srcPoints", parcelableSrcPoints);
                    extras.putString("imageDst", imageDstPath);
                    extras.putString("imageSrc", imageSrcPath);

                    intent.putExtras(extras);
                    startActivity(intent);

                });
                builder.setNegativeButton("Back", (DialogInterface.OnClickListener) (dialog, which) -> {
                    // If user click no then dialog box is canceled.
                    dialog.cancel();
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                alertDialog.getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.rounder_rectangle));
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == GALLERY_REQ_CODE) {
            try {
                // Get the URI of the selected image
                Uri imageUri = data.getData();

                // Get the path of the selected image
                imagePath = getPathFromURI(imageUri);

                // Load the image into a bitmap
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                bitmap = BitmapFactory.decodeStream(inputStream);

                // Display the bitmap in the ImageView


                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int height_screen = displayMetrics.heightPixels;
                System.out.println(height_screen);
                int width_screen = displayMetrics.widthPixels;
                System.out.println(width_screen);

                if(bitmap.getHeight() < height_screen-200 | bitmap.getWidth()< width_screen-200){
                    resized = bitmap;
                    imageView.setImageBitmap(resized);
                    System.out.println(imageView.getWidth() + " "+ imageView.getHeight());
                    System.out.println(bitmap.getWidth() + " "+ bitmap.getHeight());;
                }
                 resized = Bitmap.createScaledBitmap(bitmap,(int)Math.round(bitmap.getWidth()*0.6),(int)Math.round(bitmap.getHeight()*0.6),true);
                imageView.setImageBitmap(resized);
                System.out.println(imageView.getWidth() + " "+ imageView.getHeight());
                System.out.println(resized.getWidth() + " "+ resized.getHeight());;
                // Hide the "Add" button and show the "Homography" button and layout
                add.setVisibility(View.INVISIBLE);
                homographybt.setVisibility(View.VISIBLE);
                layout.setVisibility(View.VISIBLE);


            } catch (Exception e) {
                // Handle exceptions
            }
        }
    }

    private String getPathFromURI(Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        } else {
            return contentUri.getPath();
        }
    }


    public void calculateDisplacements(List<PointF> points) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height_screen = displayMetrics.heightPixels;
        int width_screen = displayMetrics.widthPixels;
        int height_img = resized.getHeight();
        int width_img = resized.getWidth();

        float a = (height_screen - height_img)/2.0f;
        float b = (width_screen - width_img)/2.0f;

        for (PointF point : points){
             point.x -=b;
             point.y-=a;
        }

        for (PointF point : points){
            point.x = point.x/0.6f;
            point.y = point.y/0.6f;
        }
    }
}