package elenasid.com.gradientwatchface.config;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wear.widget.BoxInsetLayout;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

import elenasid.com.gradientwatchface.R;
import elenasid.com.gradientwatchface.adapter.ColorsListAdapter;
import elenasid.com.gradientwatchface.adapter.WatchFaceColors;
import elenasid.com.gradientwatchface.util.WatchFaceUtil;

/**
 * The watch-side config activity for {@link elenasid.com.gradientwatchface.GradientWatchFaceService},
 * which allows for setting the background color.
 */
public class WatchFaceWearableConfigActivity extends Activity implements
        WearableListView.ClickListener, WearableListView.OnScrollListener {
    private static final String TAG = "WatchFaceConfig";

    private GoogleApiClient mGoogleApiClient;
    private TextView mHeader;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_watch_face_config);

        mHeader = findViewById(R.id.header);
        WearableListView listView = findViewById(R.id.color_picker);
        BoxInsetLayout content = findViewById(R.id.content);
        content.setOnApplyWindowInsetsListener((v, insets) -> {
            if (!insets.isRound()) {
                v.setPaddingRelative(
                        getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                        v.getPaddingTop(),
                        v.getPaddingEnd(),
                        v.getPaddingBottom());
            }
            return v.onApplyWindowInsets(insets);
        });

        listView.setHasFixedSize(true);
        listView.setClickListener(this);
        listView.addOnScrollListener(this);

        ColorsListAdapter adapter = new ColorsListAdapter(this);
        listView.setAdapter(adapter);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(result -> Log.d(TAG, "onConnectionFailed: " + result))
                .addApi(Wearable.API)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        ColorsListAdapter.ColorItemViewHolder colorItemViewHolder
                = (ColorsListAdapter.ColorItemViewHolder) viewHolder;
        updateConfigDataItem(colorItemViewHolder.getColor());
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTopEmptyRegionClick() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScroll(int scroll) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAbsoluteScrollChange(int scroll) {
        float newTranslation = Math.min(-scroll, 0);
        mHeader.setTranslationY(newTranslation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScrollStateChanged(int scrollState) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCentralPositionChanged(int centralPosition) {
    }

    /**
     * Update configuration data such as background color for watch face
     *
     * @param backgroundColor watch face's background color
     */
    private void updateConfigDataItem(@NonNull @WatchFaceColors final String backgroundColor) {
        DataMap configKeysToOverwrite = new DataMap();
        configKeysToOverwrite.putString(WatchFaceUtil.KEY_BACKGROUND_COLOR,
                backgroundColor);
        WatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }
}
