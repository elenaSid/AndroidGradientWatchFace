package elenasid.com.gradientwatchface;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.companion.WatchFaceCompanion;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import elenasid.com.gradientwatchface.adapter.ColorsAdapter;
import elenasid.com.gradientwatchface.adapter.WatchFaceColors;

/**
 * @author elena
 *         Date: 05.11.17
 *         Time: 16:31
 */

public class WatchFaceCompanionConfigActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DataItemResult> {

    private static final String TAG_BACKGROUND_COLOR = "BACKGROUND_COLOR";
    public static final String PATH_WITH_FEATURE = "/watch_face_config/GradientWatchFaceAnalog";

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setBackgroundResource(android.R.color.white);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setContentView(recyclerView);

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        recyclerView.setAdapter(new ColorsAdapter(mListener));
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
    public void onConnected(@Nullable Bundle bundle) {
        if (mPeerId != null) {
            Uri.Builder builder = new Uri.Builder();
            Uri uri = builder.scheme("wear").path(PATH_WITH_FEATURE).authority(mPeerId).build();
            Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);
        } else {
            displayNoConnectedDeviceDialog();
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            DataMap config = dataMapItem.getDataMap();
        }
    }

    private void sendConfigUpdateMessage(@NonNull @WatchFaceColors String color) {
        if (mPeerId != null) {
            DataMap config = new DataMap();
            config.putString(TAG_BACKGROUND_COLOR, color);
            byte[] rawData = config.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, PATH_WITH_FEATURE, rawData);
        }
    }

    private void displayNoConnectedDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String messageText = getResources().getString(R.string.mobile_color_list_no_device);
        String okText = getResources().getString(android.R.string.ok);
        builder.setMessage(messageText)
                .setCancelable(false)
                .setPositiveButton(okText, (dialog, id) -> {
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private ColorsAdapter.WatchFaceListener mListener = this::sendConfigUpdateMessage;
}
