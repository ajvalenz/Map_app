package com.example.maps_app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PolygonView extends View {
    private Paint paint;
    private Path path;
    private float circleRadius =20.0f;
    private List <PointF> points = new ArrayList<>();
    private boolean isMoving = false;
    private int movingPointIndex =0;
    private static final float TOUCH_THRESHOLD = 20.0f;



    public PolygonView(Context context) {
        super(context);
        drawPolygon();
    }

    public PolygonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        drawPolygon();
    }

    public PolygonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        drawPolygon();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
        for (PointF point : points) {
            canvas.drawCircle(point.x, point.y, circleRadius, paint);
        }
    }

    public void drawPolygon() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        path = new Path();
        path.moveTo(200 , 700);
        path.lineTo(900, 700);
        path.lineTo(900, 1200);
        path.lineTo(200 , 1200);

        path.close();

        PointF point1 = new PointF(200, 700);
        PointF point2 = new PointF(900, 700);
        PointF point3 = new PointF(900, 1200);
        PointF point4 = new PointF(200, 1200);
        points = Arrays.asList(point1,point2,point3,point4);


        invalidate(); // Redraw the view
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check if the touch point is inside any circle
                for (int i = 0; i < points.size(); i++) {
                    PointF point = points.get(i);
                    float dx = x - point.x;
                    float dy = y - point.y;
                    if (dx * dx + dy * dy < circleRadius * circleRadius) {
                        // Start moving the polygon
                        isMoving = true;
                        movingPointIndex = i;
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // Move the polygon
                if (isMoving && movingPointIndex >= 0 && movingPointIndex < points.size()) {
                    // Calculate the distance between the current touch point and the original touch point
                    float dx = x - points.get(movingPointIndex).x;
                    float dy = y - points.get(movingPointIndex).y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance > TOUCH_THRESHOLD) { // Only move the point if the distance is greater than the threshold
                        points.set(movingPointIndex, new PointF(x, y));
                        updatePath();

                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // Stop moving the polygon
                isMoving = false;
                movingPointIndex = -1;
                break;
        }

        return true;
    }

    private void updatePath() {
        // Construct the path from the points
        path.reset();
        path.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i).x, points.get(i).y);

        }
        path.close();
    }

    public List<PointF> getPoints() {
        List<PointF> currentPoints = new ArrayList<>();
        for (PointF point : points) {
            currentPoints.add(new PointF(point.x, point.y));
        }
        return currentPoints;
    }
}