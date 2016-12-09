package com.speedtune.client;

import android.util.Log;

import java.util.HashMap;
import java.util.Set;

import static java.lang.Math.max;

/**
 * Created by yulexun on 12/2/2016.
 */

public class SpeedTuneParamFL extends SpeedTuneParam{
    private static final String CLASS_TAG ="SpeedTuneParamFL";

    private static final int TAG_A_MAX_USER_BOOST_BYTE = 11;
    public static final int TAG_A_TMAP_BYTE = 12;
    private static final int TAG_A_USER_MAXBOOST_3RD_BYTE = 13;

    protected HashMap<Integer, String> mapDisplayValues; //field to display map


    public SpeedTuneParamFL(byte pTag, byte[] pRawValue) {
        super(pTag, pRawValue);
    }

    public Set<HashMap.Entry<Integer, String>> getMapEntrySet()
    {
        return mapDisplayValues.entrySet();
    }


    @Override
    public void updateValue(byte[] pRawValue) {
        super.updateValue(pRawValue);
        if(mapDisplayValues == null) mapDisplayValues = new HashMap<Integer, String>();
        switch(tag)
        {
            case 'a':
                if(pRawValue.length > TAG_A_TMAP_BYTE)
                {
                    String strVal;
                    int i = TAG_A_TMAP_BYTE;
                    int tmapValue = pRawValue[i];
                    strVal = String.format("%d", tmapValue);
                    mapDisplayValues.put(i, strVal);
                    currentTMAPVal = tmapValue;
                    double factor = getValueFactor();

                    for(i = 0; i < pRawValue.length; i++)
                    {
                        if(i == TAG_A_TMAP_BYTE) continue;
                        double dVal = pRawValue[i];
                        if(i <= TAG_A_MAX_USER_BOOST_BYTE || i == TAG_A_USER_MAXBOOST_3RD_BYTE)
                        {
                            dVal = dVal / factor;
                            strVal = String.format("%.1f", dVal);
                        }
                        else
                        {
                            strVal = String.format("%d", (int)dVal);
                        }
                        mapDisplayValues.put(i, strVal);
                    }
                }
                break;
            case 'Z':
                String strVal = new String(pRawValue);
                mapDisplayValues.put(0, strVal);
                break;
            default:
                Log.w(CLASS_TAG, "wrong tag:" + tag);
        }
    }
}
