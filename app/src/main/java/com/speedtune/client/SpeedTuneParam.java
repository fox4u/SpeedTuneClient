package com.speedtune.client;

import java.util.Arrays;

/**
 * Created by yulexun on 12/2/2016.
 */

public class SpeedTuneParam{
    public static final double VALUE_FACTOR_TMAP_DEFAULT = 10.0;
    public static final double VALUE_FACTOR_TMAP_1 = 7.045;
    public static final double VALUE_FACTOR_TMAP_2 = 5.909;
    public static int currentTMAPVal = 0;

    protected byte tag;
    protected byte[] rawValue;

    public SpeedTuneParam(byte pTag, byte[] pRawValue) {
        tag = pTag;
        updateValue(pRawValue);
    }

    public void updateValue(byte[] pRawValue)
    {
        rawValue = pRawValue;
    }

    //TODO: toString
    @Override
    public String toString() {
        return "SpeedTuneParam{" +
                "tag=" + (char)tag +
                ", rawValue=" + Arrays.toString(rawValue) +
                '}';
    }

    public static double getValueFactor()
    {
        return getValueFactor(currentTMAPVal);
    }

    public static double getValueFactor(int tmapVal)
    {
        double ret = VALUE_FACTOR_TMAP_DEFAULT;
        switch(tmapVal)
        {
            case 1:
                ret = VALUE_FACTOR_TMAP_1;
                break;
            case 2:
                ret = VALUE_FACTOR_TMAP_2;
                break;
            default:
                break;
        }

        return ret;
    }
}

