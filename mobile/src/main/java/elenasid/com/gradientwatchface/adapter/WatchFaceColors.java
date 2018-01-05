package elenasid.com.gradientwatchface.adapter;

/**
 * @author elena
 * Date: 03.01.2018
 * Time: 14:31
 */

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

@Retention(RetentionPolicy.SOURCE)
@StringDef()
public @interface WatchFaceColors {
    String PURPLE_TEAL = "purple_teal";
    String ORANGE_PINK = "orange_pink";
    String BLACK_GREY = "black_grey";
    String CYAN_INDIGO = "cyan_indigo";

    List<String> watchFaceColors = Arrays.asList(PURPLE_TEAL, ORANGE_PINK, BLACK_GREY, CYAN_INDIGO);
}

