package com.example.maps_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.maps_app.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    float zoomLevelg = 16.0f;



    private ActivityMapsBinding binding;
    private final ArrayList<Marker> markers = new ArrayList<>();
    private String data = "";
    private String[] separated;
    private ArrayList<UTM> utA;



    private ImageButton screen_shot;
    private Button btn_draw_State;
    private ImageButton btn_pin_state;
    private ImageButton btn_send,btnCornerSelection,btnOverlay ;
    private FrameLayout fram_map ;



    private Polyline line;
    ArrayList<LatLng> arraylistoflatlng;
    List<Polyline> polylineList;
    boolean Is_MAP_Moveable = false;
    boolean Is_Pin_Activated = false;
    boolean available = false;
    Projection projection;
    public double latitude;
    public double longitude;
    private PolylineOptions rectOptions;




    private ArrayList<String> position = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        askPermissions();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        screen_shot =findViewById(R.id.btnTakeScreenshot);
        screen_shot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    mMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
                        @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
                        @Override
                        public void onSnapshotReady(@Nullable Bitmap bitmap) {

                            saveScreenshot(bitmap);

                            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                            builder.setTitle("Corner Selection ");
                            final TextView input = new TextView(MapsActivity.this);
                            input.setText("      " +
                                    " " +
                                    "Do you want to proceed to the corner selection section?");
                            input.setGravity(Gravity.HORIZONTAL_GRAVITY_MASK);
                            builder.setView(input);
                            builder.setPositiveButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
                                Intent  intent = new Intent(MapsActivity.this,CornerSelection.class);
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
        });

        btn_draw_State = findViewById(R.id.btn_draw_State);
        btn_draw_State.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Is_MAP_Moveable = !Is_MAP_Moveable;
                System.out.println(Is_MAP_Moveable);
            }
        });

        btn_pin_state = findViewById(R.id.pinBtn);
        btn_pin_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Is_Pin_Activated = !Is_Pin_Activated;
            }
        });
        btn_send = findViewById(R.id.sendBtn);
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCoordsfromLine(arraylistoflatlng);
                convertToUTM(position);
                sendData(utA);
            }
        });

        btnCornerSelection = findViewById(R.id.cornerBtn);
        btnCornerSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent  intent = new Intent(MapsActivity.this,CornerSelection.class);
                startActivity(intent);
            }
        });

        btnOverlay = findViewById(R.id.overlayBtn);
        btnOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(markers.isEmpty()){
                    Toast.makeText(MapsActivity.this,"Please input the 4 markers",Toast.LENGTH_SHORT).show();
                }

            }
        });
        arraylistoflatlng = new ArrayList<>();
        polylineList = new ArrayList<>();

        fram_map = findViewById(R.id.path_draw);
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;


        fram_map.setOnTouchListener(new View.OnTouchListener() {
            @Override

            public boolean onTouch(View v, MotionEvent event) {

                float x = event.getX();
                float y = event.getY();

                int x_co = Math.round(x);
                int y_co = Math.round(y);

                projection = mMap.getProjection();
                Point x_y_points = new Point(x_co, y_co);

                LatLng latLng = mMap.getProjection().fromScreenLocation(x_y_points);
                latitude = latLng.latitude;
                longitude = latLng.longitude;
                int event_action = event.getAction();


                if(available){
                    //clear the previous polygon first. Write code here
                    available = false;
                }

                switch (event_action) {
                    case MotionEvent.ACTION_DOWN:
                        // finger touches the screen
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // finger moves on the screen
                        arraylistoflatlng.add(new LatLng(latitude, longitude));
                        rectOptions = new PolylineOptions()
                                .addAll(arraylistoflatlng)
                                .geodesic(true)
                                .color(Color.RED);
                        line = mMap.addPolyline(rectOptions);
                        polylineList.add(line);
                        break;
                    case MotionEvent.ACTION_UP:

                        available = true;
                        break;
                }

                return Is_MAP_Moveable;
            }


        });
        //MOVE THE CAMERA TO SUNY
        double sLat =37.373513;
        double sLng =126.666975;
        LatLng sunyK = new LatLng(sLat, sLng);

        mMap.setOnMapClickListener(this);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sunyK, zoomLevelg));

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);


    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {

        if (Is_Pin_Activated) {
            // Create a new marker at the touched location
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng);
            Marker marker = mMap.addMarker(markerOptions);
            markers.add(marker);
            for (Marker m : markers) {
                System.out.println("Latitud :" + m.getPosition().latitude + " Longitud :" + m.getPosition().longitude);
            }


        }

    }


    private void saveScreenshot(Bitmap bitmap) {

        String displayName = "screenshot_" + System.currentTimeMillis() + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        ContentResolver resolver = getContentResolver();
        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // Write the bitmap to the output stream obtained from the MediaStore
        try {
            OutputStream outStream = resolver.openOutputStream(imageUri);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();

            // Show a message to the user or trigger any further actions as needed
            Toast.makeText(this, "Screenshot saved successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving screenshot", Toast.LENGTH_SHORT).show();
        }
    }

    public void askPermissions(){
        int permission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        int permission2 = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission == PackageManager.PERMISSION_DENIED || permission2 == PackageManager.PERMISSION_DENIED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                requestPermissions(
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        1000);
            }
            return;
        }
    }

    private void getCoordsfromLine(ArrayList<LatLng>list){
        try{
            for (LatLng cord:list) {
                String coord = Double.toString(cord.latitude) + ","+Double.toString(cord.longitude);
                position.add(coord);
            }
            Toast toast = Toast.makeText(MapsActivity.this, "Markers location saved", Toast.LENGTH_SHORT);
            toast.show();
            System.out.println(position.size());
            System.out.println(position);
        }catch(Exception e){
            Toast msg = Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
            msg.show();
        }

    }
    private ArrayList<UTM> convertToUTM(ArrayList<String> list) {
        try {
            utA = new ArrayList<>();
            for (String position : list) {
                separated = position.split(",");
                double lat2 = Double.parseDouble(separated[0]);
                double lng2 = Double.parseDouble(separated[1]);
                WGS84 loc = new WGS84(lat2, lng2);
                UTM ut = new UTM(loc);
                utA.add(ut);
            }
        } catch (Exception e) {
            Toast msg = Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
            msg.show();
        }
        return utA;
    }

    private String sendData(ArrayList<UTM> u) {

        try {
            data = "";
            for (UTM utm : u) {
                data += utm.toString() + ",";
            }
            System.out.println(data);
        } catch (Exception e) {
            Toast msg = Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
            msg.show();
        }
        return data;
    }

}