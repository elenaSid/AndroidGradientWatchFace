package elenasid.com.gradientwatchface;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import elenasid.com.gradientwatchface.adapter.WatchFaceColors;
import elenasid.com.gradientwatchface.util.WatchFaceUtil;

/**
 * @author elena
 *         Date: 04.11.17
 *         Time: 12:33
 */

public class GradientWatchFaceService extends CanvasWatchFaceService {
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final String TAG = "GradientWatch";

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine
            implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
            DataApi.DataListener {
        static final int MSG_UPDATE_TIME = 0;

        Calendar mCalendar;

        private boolean mLowBitAmbient;
        private boolean mRegisteredTimeZoneReceiver;
        private boolean mBurnInProtection;
        private boolean mAmbient;

        private float mCenterX;
        private float mCenterY;
        private float mSecondHandLength;
        private float mMinuteHandLength;
        private float mHourHandLength;

        private Paint mBackgroundColorPaint;
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondAndHighlightPaint;
        private Paint mTickAndCirclePaint;

        private static final float HOUR_STROKE_WIDTH = 9f;
        private static final float MINUTE_STROKE_WIDTH = 5f;
        private static final float SECOND_TICK_STROKE_WIDTH = 3f;
        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 8f;
        private static final int SHADOW_RADIUS = 0;

        private int mWatchHandAndComplicationsColor;
        private int mWatchHandHighlightColor;
        private int mWatchHandShadowColor;

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(GradientWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        private @WatchFaceColors
        String mWatchFaceColor = WatchFaceColors.PURPLE_TEAL;

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(GradientWatchFaceService.this)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mWatchHandAndComplicationsColor = getColor(R.color.grey_watch_hand_primary);
            mWatchHandHighlightColor = getColor(R.color.grey_watch_hand_light);
            mWatchHandShadowColor = getColor(R.color.grey_watch_hand_dark);

            mCalendar = Calendar.getInstance();
            initializeWatchFace();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                mGoogleApiClient.connect();
                registerReceiver();
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceiver();
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
            updateTimer();
        }

        /**
         * Register receiver for {@link GradientWatchFaceService}
         */
        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            GradientWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        /**
         * Unregister receiver for {@link GradientWatchFaceService}
         */
        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            GradientWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawWatchFace(canvas);
        }

        /**
         * Draw watch face's background with color from configuration data
         *
         * @param canvas {@link Canvas}
         */
        private void drawBackground(@NonNull Canvas canvas) {
            mBackgroundColorPaint = getBackgroundPaint();
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                mBackgroundColorPaint.setColorFilter(new PorterDuffColorFilter(
                        getColor(R.color.grey_watch_hand_dark), PorterDuff.Mode.SRC));
            }

            canvas.drawPaint(mBackgroundColorPaint);
        }

        /**
         * Update watch face's background if it's interactive
         */
        private void updatePaintIfInteractive() {
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                mBackgroundColorPaint = getBackgroundPaint();
            }
        }

        /**
         * Create a {@link Paint} with current background color
         *
         * @return {@link} Paint - watch face's background
         */
        @NonNull
        private Paint getBackgroundPaint() {
            Paint background = new Paint();
            @ColorRes int colorIn;
            @ColorRes int colorOut;
            switch (mWatchFaceColor) {
                case WatchFaceColors.PURPLE_TEAL:
                    colorIn = R.color.teal_background;
                    colorOut = R.color.deep_purple_background;
                    break;
                case WatchFaceColors.ORANGE_PINK:
                default:
                    colorIn = R.color.orange_background;
                    colorOut = R.color.pink_background;
                    break;
                case WatchFaceColors.BLACK_GREY:
                    colorIn = R.color.light_grey_watch_hand_background;
                    colorOut = R.color.grey_watch_hand_background;
                    break;
                case WatchFaceColors.CYAN_INDIGO:
                    colorIn = R.color.indigo_watch_hand_background;
                    colorOut = R.color.cyan_watch_hand_background;
                    break;
            }
            background.setShader(new RadialGradient(mCenterX, mCenterY, 250, getColor(colorIn),
                    getColor(colorOut), Shader.TileMode.CLAMP));
            return background;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            mSecondHandLength = (float) (mCenterX * 0.875);
            mMinuteHandLength = (float) (mCenterX * 0.75);
            mHourHandLength = (float) (mCenterX * 0.5);

            Rect screenForBackgroundBound = new Rect(0, 0, width, height);

            ComplicationDrawable backgroundComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());
            backgroundComplicationDrawable.setBounds(screenForBackgroundBound);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,
                    false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            invalidate();
            updateTimer();
        }

        @SuppressLint("HandlerLeak")
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /**
         * Update timer. For example after invisibility
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Check if timer can be running
         *
         * @return true - it can be, false - cannot be
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        /**
         * Initialize a clock hands (minute, second and hour)
         */
        private void initializeWatchFace() {
            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHandAndComplicationsColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHandAndComplicationsColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondAndHighlightPaint = new Paint();
            mSecondAndHighlightPaint.setColor(mWatchHandHighlightColor);
            mSecondAndHighlightPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mSecondAndHighlightPaint.setAntiAlias(true);
            mSecondAndHighlightPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondAndHighlightPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchHandAndComplicationsColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.FILL);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
        }

        /**
         * Redraw a clock hands on the watch face's canvas
         *
         * @param canvas {@link Canvas}
         */
        private void drawWatchFace(@NonNull Canvas canvas) {
            float innerTickRadius = mCenterX - 10;
            float outerTickRadius = mCenterX;
            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(
                        mCenterX + innerX,
                        mCenterY + innerY,
                        mCenterX + outerX,
                        mCenterY + outerY,
                        mTickAndCirclePaint);
            }

            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mMinuteHandLength,
                    mMinutePaint);

            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondAndHighlightPaint);
            }
            canvas.drawCircle(
                    mCenterX, mCenterY, CENTER_GAP_AND_CIRCLE_RADIUS, mTickAndCirclePaint);

            canvas.restore();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            updateConfigDataOnStart();
        }

        /**
         * Update configuration data on the first connection
         */
        private void updateConfigDataOnStart() {
            WatchFaceUtil.fetchConfigDataMap(mGoogleApiClient,
                    startupConfig -> {
                        setDefaultValuesForMissingConfigKeys(startupConfig);
                        WatchFaceUtil.putConfigDataItem(mGoogleApiClient, startupConfig);
                        updateUiForConfigDataMap(startupConfig);
                    }
            );
        }

        /**
         * Set default configuration data (background color) for watch face
         *
         * @param config {@link DataMap} configuration data
         */
        private void setDefaultValuesForMissingConfigKeys(@NonNull DataMap config) {
            if (!config.containsKey(WatchFaceUtil.KEY_BACKGROUND_COLOR)) {
                config.putString(WatchFaceUtil.KEY_BACKGROUND_COLOR, WatchFaceColors.PURPLE_TEAL);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConnectionSuspended(int i) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent dataEvent : dataEventBuffer) {
                if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                    continue;
                }
                DataItem dataItem = dataEvent.getDataItem();
                if (!dataItem.getUri().getPath().equals(
                        WatchFaceUtil.PATH_WITH_FEATURE)) {
                    continue;
                }

                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                DataMap config = dataMapItem.getDataMap();

                updateUiForConfigDataMap(config);
            }
        }

        /**
         * Update UI according to configuration data (change background color)
         *
         * @param config {@link DataMap} configuration data
         */
        private void updateUiForConfigDataMap(@NonNull final DataMap config) {
            boolean uiUpdated = false;
            for (String configKey : config.keySet()) {
                if (!config.containsKey(configKey)) {
                    continue;
                }
                @WatchFaceColors String color = config.getString(configKey);
                Log.d(TAG, "Found watch face config key: " + configKey + " -> " + color);
                if (configKey.equals(WatchFaceUtil.KEY_BACKGROUND_COLOR)) {
                    uiUpdated = true;
                    mWatchFaceColor = color;
                    updatePaintIfInteractive();
                }
            }
            if (uiUpdated) {
                invalidate();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, "Connected failed: " + connectionResult.getErrorCode());
        }
    }
}
