package elenasid.com.gradientwatchface.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import elenasid.com.gradientwatchface.R;

/**
 * @author elena
 *         Date: 03.01.2018
 *         Time: 11:31
 */

public class ColorsListAdapter extends WearableListView.Adapter {
    private Context context;

    public ColorsListAdapter(@NonNull Context context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColorItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ColorItemViewHolder(new ColorItem(parent.getContext()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) holder;
        List<String> colors = WatchFaceColors.watchFaceColors;
        colorItemViewHolder.mColorItem.setColor(colors.get(position));

        RecyclerView.LayoutParams layoutParams =
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        int colorPickerItemMargin = (int) context.getResources()
                .getDimension(R.dimen.digital_config_color_picker_item_margin);

        if (position == 0) {
            layoutParams.setMargins(0, colorPickerItemMargin, 0, 0);
        } else if (position == colors.size() - 1) {
            layoutParams.setMargins(0, 0, 0, colorPickerItemMargin);
        } else {
            layoutParams.setMargins(0, 0, 0, 0);
        }
        colorItemViewHolder.itemView.setLayoutParams(layoutParams);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getItemCount() {
        return WatchFaceColors.watchFaceColors.size();
    }

    /**
     * The layout of a color item including image and label in {@link TextView}.
     */
    private static class ColorItem extends LinearLayout implements
            WearableListView.OnCenterProximityListener {

        private static final int ANIMATION_DURATION_MS = 150;
        private static final float SHRINK_LABEL_ALPHA = .5f;
        private static final float EXPAND_LABEL_ALPHA = 1f;

        private final TextView tvColor;

        private final ObjectAnimator expandLabelAnimator;
        private final AnimatorSet expandAnimator;

        private final ObjectAnimator shrinkLabelAnimator;
        private final AnimatorSet shrinkAnimator;

        public ColorItem(@NonNull Context context) {
            super(context);
            View.inflate(context, R.layout.watch_face_color_item, this);

            tvColor = findViewById(R.id.tvColor);

            shrinkLabelAnimator = ObjectAnimator.ofFloat(tvColor, "alpha",
                    EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);
            shrinkAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            shrinkAnimator.play(shrinkLabelAnimator);

            expandLabelAnimator = ObjectAnimator.ofFloat(tvColor, "alpha",
                    SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);
            expandAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            expandAnimator.play(expandLabelAnimator);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCenterPosition(boolean animate) {
            if (animate) {
                shrinkAnimator.cancel();
                if (!expandAnimator.isRunning()) {
                    expandLabelAnimator.setFloatValues(tvColor.getAlpha(), EXPAND_LABEL_ALPHA);
                    expandAnimator.start();
                }
            } else {
                expandAnimator.cancel();
                tvColor.setAlpha(EXPAND_LABEL_ALPHA);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onNonCenterPosition(boolean animate) {
            if (animate) {
                expandAnimator.cancel();
                if (!shrinkAnimator.isRunning()) {
                    shrinkLabelAnimator.setFloatValues(tvColor.getAlpha(), SHRINK_LABEL_ALPHA);
                    shrinkAnimator.start();
                }
            } else {
                shrinkAnimator.cancel();
                tvColor.setAlpha(SHRINK_LABEL_ALPHA);
            }
        }

        /**
         * Set background and color name to adapter item
         *
         * @param colorName {@link WatchFaceColors} background color name
         */
        private void setColor(@NonNull @WatchFaceColors String colorName) {
            @StringRes int colorNameRes;
            @DrawableRes int colorDrawableRes;

            switch (colorName) {
                case WatchFaceColors.PURPLE_TEAL:
                default:
                    colorNameRes = R.string.color_purple_blue;
                    colorDrawableRes = R.drawable.bg_deep_purple_teal;
                    break;
                case WatchFaceColors.ORANGE_PINK:
                    colorNameRes = R.string.color_orange_pink;
                    colorDrawableRes = R.drawable.bg_orange_pink;
                    break;
                case WatchFaceColors.BLACK_GREY:
                    colorNameRes = R.string.color_grey_black;
                    colorDrawableRes = R.drawable.bg_grey_black;
                    break;
                case WatchFaceColors.CYAN_INDIGO:
                    colorNameRes = R.string.color_cyan_indigo;
                    colorDrawableRes = R.drawable.bg_cyan_indigo;
                    break;
            }
            tvColor.setText(colorNameRes);
            Drawable drawable = ContextCompat.getDrawable(getContext(), colorDrawableRes);
            tvColor.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }
    }

    public class ColorItemViewHolder extends WearableListView.ViewHolder {
        private final ColorItem mColorItem;

        ColorItemViewHolder(@NonNull ColorItem mColorItem) {
            super(mColorItem);
            this.mColorItem = mColorItem;
        }

        @WatchFaceColors
        @NonNull
        public String getColor() {
            return WatchFaceColors.watchFaceColors.get(getAdapterPosition());
        }
    }
}

