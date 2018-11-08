package de.leo.smartTrigger.datacollector.ui.GeofencesWithPlayServices;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import de.leo.smartTrigger.datacollector.R;

public class RadarOverlayView extends LinearLayout {
    Bitmap centerBitmap;
    private Bitmap windowFrame;
    private float radius = 0f;
    private int centerX = 0;
    private int centerY = 0;
    private float MIN_RADIUS = 3f;

    public RadarOverlayView(Context context) {
        super(context);
    }

    public RadarOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.RadarOverlayView, 0, 0);

        try {
            radius = a.getDimension(R.styleable.RadarOverlayView_radius, 0f);
        } finally {
            a.recycle();
        }
        centerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable
                .radar_view_center_marker);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        createWindowFrame();
        canvas.drawBitmap(windowFrame, 0, 0, null);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isClickable() {
        return false;
    }

    protected void createWindowFrame() {
        windowFrame = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas osCanvas = new Canvas(windowFrame);

        centerX = getWidth() / 2;
        centerY = getHeight() / 2;

        if (radius > 0) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            // Draw the circunference
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.BLUE);
            paint.setAlpha(200);
            paint.setStrokeWidth(5);
            osCanvas.drawCircle(centerX, centerY, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLUE);
            //center point
            osCanvas.drawCircle(centerX, centerY, MIN_RADIUS, paint);
            //10%
            paint.setAlpha(25);
            osCanvas.drawCircle(centerX, centerY, radius, paint);

            // Draw the center icon
            paint.setAlpha(255);

            osCanvas.drawBitmap(centerBitmap, centerX - centerBitmap.getWidth() / 2,
                    centerY - centerBitmap.getHeight(),
                    paint);
        }
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        windowFrame = null;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float value) {
        if (value < MIN_RADIUS)
            value = MIN_RADIUS;
        if (radius != value) {
            radius = value;
            invalidate();
        }
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }
}