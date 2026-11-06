package ru.inversion.fx.form.controls.renderer;

import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;
import ru.inversion.db.entity.ContentTypeEnum;
import ru.inversion.utils.U;

/**
 *
 * @author antonovdi
 */
public class ContentTypeManager {

    public static final String MASK_DATE_DD_MM_YYYY = "dd.MM.yyyy";
    public static final String MASK_TIME_MM_SS = "HH:mm:ss";
    public static final String MASK_TIME_NO_SECONDS = "HH:mm";

    public static final String MASK_DATE_DD_MM_YYYY_HH_MIN = "dd.MM.yyyy HH:mm";
    public static final String MASK_DATE_HH_MIN_DD_MM_YYYY = "HH:mm dd.MM.yyyy";

    public static final String MASK_DATE_DD_MM_YYYY_HH_MIN_SEC = "dd.MM.yyyy HH:mm:ss";
    public static final String MASK_DATE_HH_MIN_SEC_DD_MM_YYYY = "HH:mm:ss dd.MM.yyyy";

    public static final String MASK_MONEY_DEFAULT = "### ### ### ### ### #0.00";
    public static final String MASK_PERCENT_DEFAULT = "##0.0000";

    public static final String MASK_DATE_DEFAULT = MASK_DATE_DD_MM_YYYY;
    public static final Pos ALLGN_DEFAULT = Pos.CENTER_LEFT;

    /** */
    public static String getFormatMask(ContentTypeEnum type) {

        if (type != null) {
            switch (type) {
                case DATE:
                    return MASK_DATE_DD_MM_YYYY;
                case TIME:
                    return MASK_TIME_MM_SS;
                case TIME_NO_SECONDS:
                    return MASK_TIME_NO_SECONDS;
                case DATE_TIME:
                    return MASK_DATE_DD_MM_YYYY_HH_MIN;
                case TIME_DATE:
                    return MASK_DATE_HH_MIN_DD_MM_YYYY;
                case TIME_DATE_DETAIL:
                    return MASK_DATE_HH_MIN_SEC_DD_MM_YYYY;
                case DATE_TIME_DETAIL:
                    return MASK_DATE_DD_MM_YYYY_HH_MIN_SEC;
                case MONEY:
                    return MASK_MONEY_DEFAULT;
                case PERCENT:
                    return MASK_PERCENT_DEFAULT;
            }
        }

        return null;
    }

    /**
     *      */
    public static TextAlignment getAlignment(ContentTypeEnum type) {

        if (type != null) {
            switch (type) {
                case DATE:
                case TIME:
                case TIME_NO_SECONDS:
                case DATE_TIME:
                case TIME_DATE:
                    return TextAlignment.CENTER;
                case MONEY:
                case PERCENT:
                    return TextAlignment.RIGHT;
                case CURRENCY:
                    return TextAlignment.CENTER;
            }
        }
        return TextAlignment.LEFT;
    }

    /**
     *      */
    public static int getDefaultLengthSyb(ContentTypeEnum type) {

        if (type != null) {
            switch (type) {
                case DATE:
                case TIME:
                case DATE_TIME:
                case TIME_DATE:
                    return U.callIfNotNull(getFormatMask(type), String::length);
                case MONEY:
                case PERCENT:
                    return 10;
                case CURRENCY:
                    return 15;
            }
        }
        return -1;
    }

}
