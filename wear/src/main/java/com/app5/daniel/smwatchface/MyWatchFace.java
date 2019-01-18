package com.app5.daniel.smwatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class MyWatchFace extends CanvasWatchFaceService {

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
   // private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private static final long INTERACTIVE_UPDATE_RATE_MS = 150;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_TICK_STROKE_WIDTH = 2f;

        private int animacion;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;

        private static final int SHADOW_RADIUS = 6;

        private AssetManager assetManager;
        private String[] imgPath;
        private int contadorPath;


        private  boolean isDigital;

        float seconds2;
        int cambioI;

        private Typeface CAPTAIN_TYPEFACE =Typeface.createFromAsset (getAssets(), "sailor.ttf");




        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
        private float mSecondHandLength;
        private float sMinuteHandLength;
        private float sHourHandLength;
        /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
        private int mWatchHandColor;
        private int mWatchHandHighlightColor;
        private int mWatchHandShadowColor;
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mTickAndCirclePaint;
        private Paint mBackgroundPaint;
        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;
        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        private Paint mTextPaint,mTextPaintAmbient;

        private Bitmap bmDigital, bmAnalog, bmCambio;
        private Rect rectBotonDigital, rectBotonAnalog, rectBotonCambio;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();
            animacion = 1;

            contadorPath = 0;

            rectBotonDigital = new Rect();
            rectBotonAnalog = new Rect();
            rectBotonCambio = new Rect ();


            mTextPaint = new Paint();
            mTextPaint.setTypeface(CAPTAIN_TYPEFACE);
            mTextPaint.setTextSize(R.dimen.sailorTamano);
            //mTextPaint.setAntiAlias(true);
            mTextPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.ppSailor));
            //mTextPaint.setAlpha(100);

            mTextPaintAmbient = new Paint();
            mTextPaintAmbient.setTypeface(CAPTAIN_TYPEFACE);
            mTextPaintAmbient.setTextSize(R.dimen.sailorTamano);
            //mTextPaint.setAntiAlias(true);
            mTextPaintAmbient.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.ppSailor
                    ));
            //mTextPaint.setAlpha(100);
            mTextPaintAmbient.setAntiAlias(true);

            initializeBackground();
            initializeWatchFace();


            bmDigital = BitmapFactory.decodeResource(getResources(), R.mipmap.digital1);
            //bmAnalog = BitmapFactory.decodeResource(getResources(), R.mipmap.analog);
            bmCambio = BitmapFactory.decodeResource(getResources(), R.mipmap.face);


            AssetManager assetManager = getAssets();

            InputStream is = null;
            try {
                is = assetManager.open("analog.png");
            } catch (IOException e) {
                e.printStackTrace();
            }
          bmAnalog = BitmapFactory.decodeStream(is);












            seconds2=61f;
            cambioI=1;

        }

        private void initializeBackground() {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);
            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);

            /* Extracts colors from background image to improve watchface style. */
            Palette.from(mBackgroundBitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    if (palette != null) {
                        mWatchHandHighlightColor = palette.getVibrantColor(Color.MAGENTA);
                        mWatchHandColor = palette.getLightVibrantColor(Color.CYAN);
                        mWatchHandShadowColor = palette.getDarkMutedColor(Color.BLACK);
                        updateWatchHandStyle();
                    }
                }
            });
        }

        private void initializeWatchFace() {
            /* Set defaults for colors */
            mWatchHandColor = Color.WHITE;
            mWatchHandHighlightColor = Color.RED;
            mWatchHandShadowColor = Color.BLACK;

            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchHandHighlightColor);
            mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchHandColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;

            updateWatchHandStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                mSecondPaint.setColor(Color.WHITE);
                mTickAndCirclePaint.setColor(Color.WHITE);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondPaint.setAntiAlias(false);
                mTickAndCirclePaint.setAntiAlias(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondPaint.clearShadowLayer();
                mTickAndCirclePaint.clearShadowLayer();

            } else {
                mHourPaint.setColor(mWatchHandColor);
                mMinutePaint.setColor(mWatchHandColor);
                mSecondPaint.setColor(mWatchHandHighlightColor);
                mTickAndCirclePaint.setColor(Color.YELLOW);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondPaint.setAntiAlias(true);
                mTickAndCirclePaint.setAntiAlias(true);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            isDigital = !insets.isRound();


        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            int tamanoIcono= width/7;






            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (float) (mCenterX * 0.875);
            sMinuteHandLength = (float) (mCenterX * 0.75);
            sHourHandLength = (float) (mCenterX * 0.5);


            /* Scale loaded background image (more efficient) if surface dimensions change. */
            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);




            rectBotonDigital.set(width/20*4, height/20*2, width/20*4+tamanoIcono, height/20*2+tamanoIcono);
            rectBotonAnalog.set(width/20*16-tamanoIcono, height/20*2, width/20*16, height/20*2+tamanoIcono);
            rectBotonCambio.set(width/20*16-tamanoIcono, height/20*2, width/20*16, height/20*2+tamanoIcono);






            bmDigital = Bitmap.createScaledBitmap(bmDigital, tamanoIcono,
                    tamanoIcono, true);
            bmAnalog = Bitmap.createScaledBitmap(bmAnalog, tamanoIcono,
                    tamanoIcono, true);
            bmCambio = Bitmap.createScaledBitmap(bmCambio, tamanoIcono, tamanoIcono, true
            );




            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don't want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             *
             * Also, if your watch face will know about all images ahead of time (users aren't
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            if (!mBurnInProtection && !mLowBitAmbient) {
                initGrayBackgroundBitmap();
            }


            mTextPaint.setTextSize(getResources().getDimension(R.dimen.sailorTamanoLetraDigital));
            mTextPaintAmbient.setTextSize(getResources().getDimension(R.dimen.sailorTamanoLetraDigitalAmbient));
        }

        private void initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                    mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.

                    if(x>= rectBotonDigital.left&&x<= rectBotonDigital.right&&y>= rectBotonDigital.top&&y<= rectBotonDigital.bottom) {

                        if (isDigital) {
                            isDigital = false;
                        } else {
                            isDigital = true;
                        }


                    }


                        if(x>= rectBotonCambio.left&&x<= rectBotonCambio.right&&y>= rectBotonCambio.top&&y<= rectBotonCambio.bottom){

                        contadorPath = 0;


                        animacion++;
                            if (animacion == 5){
                               animacion = 1;
                            }



                    }


                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackgroundGif(canvas);
             if(isDigital) {
                drawWatchFaceDigital(canvas, bounds);
            }else{
                drawWatchFace(canvas);
            }

            //drawWatchFaceAnimado(canvas, bounds);
        }

        private void drawBackgroundGif(Canvas canvas) {

            String nameAnimacion = "";
             switch (animacion ) {

                 case 1:
                     nameAnimacion = "sailor";
                     break;
                 case 2:
                     nameAnimacion = "sailor2";
                     break;
                 case 3:
                     nameAnimacion = "sailor3";
                     break;
                 case 4:
                     nameAnimacion = "sailor4";
                     break;
            }


            assetManager = getAssets();
            try {
                imgPath = assetManager.list(nameAnimacion);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            } else {

                InputStream is = null;
                try {
                    is = assetManager.open(nameAnimacion+"/"+imgPath[contadorPath]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("rutaImagen", imgPath[contadorPath]);
                Bitmap  bitmap = BitmapFactory.decodeStream(is);
                bitmap = Bitmap.createScaledBitmap(bitmap,
                        (canvas.getWidth()), (canvas.getWidth()), true);

                canvas.drawBitmap(bitmap,0,0,mBackgroundPaint);

                contadorPath++;
                if (contadorPath == imgPath.length){
                    contadorPath = 0;
                }
            }
        }

        private void drawWatchFace(Canvas canvas) {

             float seconds = mCalendar.get(Calendar.SECOND);

            canvas.drawBitmap(bmDigital,rectBotonDigital.left, rectBotonDigital.top, mBackgroundPaint);
            canvas.drawBitmap(bmCambio,rectBotonCambio.left, rectBotonCambio.top, mBackgroundPaint);



            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint);
            }

            final float secondsRotation = seconds * 6f;
            Log.i("seconds rotatio",secondsRotation+"");

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sMinuteHandLength,
                    mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint);

            }
            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    CENTER_GAP_AND_CIRCLE_RADIUS,
                    mTickAndCirclePaint);

            /* Restore the canvas' original orientation. */
            canvas.restore();



        }

        private void drawWatchFaceDigital(Canvas canvas, Rect bounds) {

            SimpleDateFormat dfHM = new SimpleDateFormat("h:mm a");
            SimpleDateFormat dfHMS = new SimpleDateFormat("h:mm:ss");



            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaintAmbient.setTextAlign(Paint.Align.CENTER);



            String tiempo = dfHM.format(mCalendar.getTime());
            String tiempoS = dfHMS.format(mCalendar.getTime());




            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);

                canvas.drawText(tiempo, bounds.right/2, bounds.bottom/18*11, mTextPaintAmbient);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);


                canvas.drawText(tiempo, bounds.right/2, bounds.bottom/18*11, mTextPaintAmbient);
            } else {


                mTextPaint.setTextAlign(Paint.Align.CENTER);

                canvas.drawText(tiempoS, bounds.right/2, bounds.bottom/10*8, mTextPaint);

            }

            canvas.drawBitmap(bmAnalog,rectBotonDigital.left, rectBotonDigital.top, mBackgroundPaint);
            canvas.drawBitmap(bmCambio,rectBotonCambio.left, rectBotonCambio.top, mBackgroundPaint);


        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
