/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package elenasid.com.gradientwatchface.util;

import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

public final class WatchFaceUtil {
    /**
     * The {@link DataMap} key for {@link elenasid.com.gradientwatchface.GradientWatchFaceService} background color name.
     * The color name must be a {@link String} recognized by {@link Color#parseColor}.
     */
    public static final String KEY_BACKGROUND_COLOR = "BACKGROUND_COLOR";

    /**
     * The path for the {@link DataItem} containing {@link elenasid.com.gradientwatchface.GradientWatchFaceService} configuration.
     */
    public static final String PATH_WITH_FEATURE = "/watch_face_config/GradientWatchFaceAnalog";

    /**
     * Callback interface to perform an action with the current config {@link DataMap} for
     * {@link elenasid.com.gradientwatchface.GradientWatchFaceService}.
     */
    public interface FetchConfigDataMapCallback {
        /**
         * Callback invoked with the current config {@link DataMap} for
         * {@link elenasid.com.gradientwatchface.GradientWatchFaceService}.
         */
        void onConfigDataMapFetched(DataMap config);
    }

    /**
     * Asynchronously fetches the current config {@link DataMap} for {@link elenasid.com.gradientwatchface.GradientWatchFaceService}
     * and passes it to the given callback.
     * <p>
     * If the current config {@link DataItem} doesn't exist, it isn't created and the callback
     * receives an empty DataMap.
     */
    public static void fetchConfigDataMap(final GoogleApiClient client,
                                          final FetchConfigDataMapCallback callback) {
        Wearable.NodeApi.getLocalNode(client).setResultCallback(
                getLocalNodeResult -> {
                    String localNode = getLocalNodeResult.getNode().getId();
                    Uri uri = new Uri.Builder()
                            .scheme("wear")
                            .path(WatchFaceUtil.PATH_WITH_FEATURE)
                            .authority(localNode)
                            .build();
                    Wearable.DataApi.getDataItem(client, uri)
                            .setResultCallback(new DataItemResultCallback(callback));
                }
        );
    }

    /**
     * Overwrites (or sets, if not present) the keys in the current config {@link DataItem} with
     * the ones appearing in the given {@link DataMap}. If the config DataItem doesn't exist,
     * it's created.
     * <p>
     * It is allowed that only some of the keys used in the config DataItem appear in
     * {@code configKeysToOverwrite}. The rest of the keys remains unmodified in this case.
     */
    public static void overwriteKeysInConfigDataMap(final GoogleApiClient googleApiClient,
                                                    final DataMap configKeysToOverwrite) {

        WatchFaceUtil.fetchConfigDataMap(googleApiClient,
                currentConfig -> {
                    DataMap overwrittenConfig = new DataMap();
                    overwrittenConfig.putAll(currentConfig);
                    overwrittenConfig.putAll(configKeysToOverwrite);
                    WatchFaceUtil.putConfigDataItem(googleApiClient, overwrittenConfig);
                }
        );
    }

    /**
     * Overwrites the current config {@link DataItem}'s {@link DataMap} with {@code newConfig}.
     * If the config DataItem doesn't exist, it's created.
     */
    public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfig) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WITH_FEATURE);
        putDataMapRequest.setUrgent();
        DataMap configToPut = putDataMapRequest.getDataMap();
        configToPut.putAll(newConfig);
        Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest());
    }

    private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {

        private final FetchConfigDataMapCallback mCallback;

        DataItemResultCallback(FetchConfigDataMapCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
            if (dataItemResult.getStatus().isSuccess()) {
                if (dataItemResult.getDataItem() != null) {
                    DataItem configDataItem = dataItemResult.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();
                    mCallback.onConfigDataMapFetched(config);
                } else {
                    mCallback.onConfigDataMapFetched(new DataMap());
                }
            }
        }
    }

    private WatchFaceUtil() {
    }
}
