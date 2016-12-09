package com.speedtune.client;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.speedtune.client.SpeedTuneParamFL.TAG_A_TMAP_BYTE;

/**
 * Created by yulexun on 12/4/2016.
 */

class SpeedTuneSaveCmdInfo{
    public ArrayList<String> saveTags;
    public Byte endByte;
    public Byte fieldSize;
}

public class SpeedTuneSaveCmd {
    private static final String TAG = "SpeedTuneSaveCmd";
    protected static HashMap<String, SpeedTuneSaveCmdInfo> infoMap;

    static{
        infoMap = new HashMap<>();
    }

    protected byte[] cmdData;
    protected String strCmdType;

    protected SpeedTuneSaveCmd(String cmdType)
    {
        strCmdType = cmdType;
        int cmdLength = getCmdLength();
        cmdData = new byte[cmdLength];
        if(cmdType.length() > 0)
        {
            cmdData[0] = (byte)cmdType.charAt(0);
            Byte endByte = getEndByte();
            if(endByte != null)
            {
                cmdData[cmdLength - 1] = endByte;
            }
        }
    }

    private Byte getEndByte()
    {
        Byte endByte = null;
        SpeedTuneSaveCmdInfo info = infoMap.get(strCmdType);
        if(info != null)
        {
            endByte = info.endByte;
        }
        return endByte;
    }

    private Byte getFieldSize()
    {
        Byte fieldSize = 1;
        SpeedTuneSaveCmdInfo info = infoMap.get(strCmdType);
        if(info != null && info.fieldSize != null && info.fieldSize <= 4)
        {
            fieldSize = info.fieldSize;
        }
        return fieldSize;
    }

    public ArrayList<String> getSaveTags()
    {
        ArrayList<String> saveTags = null;
        SpeedTuneSaveCmdInfo info = infoMap.get(strCmdType);
        if(info != null)
        {
            saveTags = info.saveTags;
        }
        return saveTags;
    }

    protected byte getByteCmdType()
    {
        return cmdData[0];
    }

    protected int getBodyLength()
    {
        int ret = 0;
        ArrayList<String> saveTags = getSaveTags();
        if(saveTags != null)
        {
            ret = saveTags.size() * getFieldSize();
        }
        return ret;
    }

    protected int getCmdLength()
    {
        int ret = 1 + getBodyLength();
        Byte endByte = getEndByte();
        if(endByte != null)
        {
            ret++;
        }
        return ret;
    }

    public boolean generateData(HashMap<String, SpeedTuneData> deviceSetting)
    {
        boolean ret = false;
        ArrayList<String> saveTags = getSaveTags();
        if(saveTags != null && deviceSetting != null)
        {
            SpeedTuneData tmapValData = deviceSetting.get("a_" + TAG_A_TMAP_BYTE);
            int tmapVal = (tmapValData != null) ? Integer.parseInt(tmapValData.getActualDisplayVal()) : SpeedTuneParam.currentTMAPVal;

            int i = 1;
            for(String tag : saveTags)
            {
                SpeedTuneData displayData = deviceSetting.get(tag);
                int data = 0;
                if(displayData != null)
                {
                    double dVal = Double.parseDouble(displayData.getActualDisplayVal());
                    switch(tag)
                    {
                        case "}":
                        case "|":
                        case "z":
                            dVal = dVal * 10;
                            break;
                        case "a_0":
                        case "a_1":
                        case "a_2":
                        case "a_3":
                        case "a_4":
                        case "a_5":
                        case "a_6":
                        case "a_7":
                        case "a_8":
                        case "a_9":
                        case "a_10":
                        case "a_11":
                        case "a_13":
                        case "P":
                        case "v":
                        case "t":
                        case "x":
                            dVal = dVal * SpeedTuneParam.getValueFactor(tmapVal);
                            break;
                        default:
                            break;
                    }
                    data = (int)(dVal + 0.5);
                }

                int lastByte = i + getFieldSize() - 1;

                if(lastByte < getBodyLength() + 1)
                {
                    //big-endian
                    for(int j = lastByte;i <= lastByte; i++, j--)
                    {
                        cmdData[j] = (byte)(data & 0xff);
                        data = data >> 8;
                    }
                }
                else
                {
                    Log.w(TAG, "saveTags.size > body length");
                    break;
                }
            }

            if(i == getBodyLength() + 1)
            {
                ret = true;
            }
        }

        return ret;
    }

    @Override
    public String toString() {
        return "SpeedTuneSaveCmd{" +
                "cmdData=" + Arrays.toString(cmdData) +
                ", strCmdType='" + strCmdType + '\'' +
                '}';
    }
}
