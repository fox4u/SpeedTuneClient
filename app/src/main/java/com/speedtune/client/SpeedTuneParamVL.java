package com.speedtune.client;

import android.util.Log;

import static java.lang.Math.min;

/**
 * Created by yulexun on 12/2/2016.
 */

public class SpeedTuneParamVL extends SpeedTuneParam{
    private static final String CLASS_TAG ="SpeedTuneParamVL";
    protected String displayValue;
    public SpeedTuneParamVL(byte pTag, byte[] pRawValue) {
        super(pTag, pRawValue);
    }

    @Override
    public void updateValue(byte[] pRawValue) {
        super.updateValue(pRawValue);

        String strVal = new String(pRawValue);
        double dVal;
        switch(tag)
        {
            case '}':
            case '|':
            case 'z':
            case 'N':
            case 'B':
            case 'M':
            case ':':
            case ')':
            case '*':
            case '~':
            case 'I':
            case 'J':
            case 'O':
            case 'K':
            case 'y':
                dVal = Double.parseDouble(strVal) / 10;
                displayValue = String.format("%.1f", dVal);
                break;
            case 'P':
            case 'v':
            case 't':
            case 'x':
                dVal = Double.parseDouble(strVal) / getValueFactor();
                displayValue = String.format("%.1f", dVal);
                break;
            case 'L':
                dVal = Double.parseDouble(strVal) * 5 / 1024;
                displayValue = String.format("%.2f", dVal);
                break;
            case 'D':
                dVal = Double.parseDouble(strVal) * 0.3921568;
                displayValue = String.format("%d", (int)dVal);
                break;
            case 'H':
                dVal = Double.parseDouble(strVal);
                double lastDValue = 0;
                SpeedTuneParam lastParamUpdate = SpeedTuneReceiver.getInstance().mapLastParamUpdate.get(SpeedTuneReceiver.byteToString(tag));
                if(lastParamUpdate != null)
                {
                    String lastStrVal = new String(lastParamUpdate.rawValue);
                    lastDValue = Double.parseDouble(lastStrVal);
//                    Log.w(CLASS_TAG, "lastDValue:" + lastDValue);
                }
                if(dVal > lastDValue)
                {
                    displayValue = String.format("%d", (int)min(dVal - lastDValue, 500.0));
                }
                else
                {
                    displayValue = "0";
                }

                break;
            case '-':
                dVal = Double.parseDouble(strVal) / 2.55;
                displayValue = String.format("%d", (int)dVal);
                break;
            case '(':
                dVal = min(Double.parseDouble(strVal) / 10, 50.0);
                displayValue = String.format("%.1f", dVal);
                break;
            case 'e':
                dVal = Double.parseDouble(strVal) * 1.8 - 40;
                displayValue = String.format("%d", (int)dVal);
                break;
            default:
                displayValue = strVal;
        }
    }

    public String getDisplayValue()
    {
        return displayValue;
    }

    @Override
    public String toString() {
        return "SpeedTuneParam{" +
                "tag=" + (char)tag +
                ", rawValue=" + displayValue +
                '}';
    }
}
