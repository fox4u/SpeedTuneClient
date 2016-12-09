package com.speedtune.client;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by fox on 11/30/2016.
 */


class SpeedTuneCode{
    public String codes;
    public SpeedTuneCode(byte[] rawData) {
        if(rawData != null)
        {
            codes = new String(rawData);
        }
    }

    //TODO: details
}

public class SpeedTuneReceiver {
    private static final String CLASS_TAG = "SpeedTuneReceiver";

    private static final int CHECK_TAG_RESULT_NONE = 0;
    private static final int CHECK_TAG_RESULT_VARIABLE = 1;
    private static final int CHECK_TAG_RESULT_FIXED = 2;
    private static final int CHECK_TAG_RESULT_DELIMITER = 3;
    private static final String strTagVariableLength = "DNXB%$>MACGHE&!^-:)(*<e=W~IJOKyYPQRSTUw@V{}|Lqo+prvnstuxz#`mF";
    protected static final HashMap<String, Integer> mapTagFixedLength;
    protected static final String strTagLastUpdateRequired = "H";
    private static final byte tagDelimiter = ';';
    private static final int CODE_LENGTH_F_SERIES = 6;
    private static final int CODE_LENGTH_OTHER = 4;

    static{
        mapTagFixedLength = new HashMap<String, Integer>();
        mapTagFixedLength.put("a", 36);
        mapTagFixedLength.put("Z", 12);
    }

    private byte[] workingByteArray = null;
    private static SpeedTuneReceiver mInst = null;
    public HashMap<String, SpeedTuneParam> mapLastParamUpdate;
    public ConcurrentLinkedQueue<SpeedTuneParam> paramQueue;
    public ConcurrentLinkedQueue<SpeedTuneCode> codesQueue;
    public boolean isAutoParseParam = false;
    public boolean isAutoParseCodes = false;
    public boolean isCodesReadDone = false;
    public static int nPlatformType = MainActivity.PLATFORM_TYPE_E_SERIES_N54;

    public static SpeedTuneReceiver getInstance(){
        if(mInst == null)
        {
            mInst = new SpeedTuneReceiver();
        }

        return mInst;
    }

    private SpeedTuneReceiver()
    {
        paramQueue = new ConcurrentLinkedQueue<>();
        codesQueue = new ConcurrentLinkedQueue<>();
        mapLastParamUpdate = new HashMap<>();
    }

    public void receive(byte[] rawData)
    {
        int prevBuffLength = 0;
        int rawDataLength = rawData.length;

        if(workingByteArray == null)
        {
            workingByteArray = rawData;
        }
        else
        {
            prevBuffLength = workingByteArray.length;
            byte[] merged = new byte[workingByteArray.length + rawDataLength];
            System.arraycopy(workingByteArray, 0, merged, 0, workingByteArray.length);
            System.arraycopy(rawData, 0, merged, workingByteArray.length, rawDataLength);
            workingByteArray = merged;
        }

        if(isAutoParseParam)
        {
            parseParam();
        }
        else if(isAutoParseCodes)
        {
            parseCodes(prevBuffLength);
        }

    }

    public static String byteToString(byte data)
    {
        StringBuilder strBld = new StringBuilder(1);
        strBld.append((char)data);
        return strBld.toString();
    }

    private int checkTag(byte data)
    {
        int ret = CHECK_TAG_RESULT_NONE;

        String strTag = byteToString(data);

        if(data == tagDelimiter)
        {
            ret = CHECK_TAG_RESULT_DELIMITER;
        }
        if(strTagVariableLength.contains(strTag))
        {
            ret = CHECK_TAG_RESULT_VARIABLE;
        }
        else if(mapTagFixedLength.containsKey(strTag))
        {
            ret = CHECK_TAG_RESULT_FIXED;
        }
//        Log.w(CLASS_TAG, "checkTag " + strTag + ", ret " + ret);
        return ret;
    }

    public void parseParam()
    {
//        Log.w(CLASS_TAG, "parseParam called");
        if(workingByteArray != null)
        {
            int parsePtr;
            int curTagPtr = -1;
            for( parsePtr = 0; parsePtr < workingByteArray.length; )
            {
                byte curByte = workingByteArray[parsePtr];
                int checkTagRet = checkTag(curByte);

                int lastTagPtr = curTagPtr;
                if((lastTagPtr != -1) && (checkTagRet == CHECK_TAG_RESULT_FIXED || checkTagRet == CHECK_TAG_RESULT_VARIABLE || checkTagRet == CHECK_TAG_RESULT_DELIMITER))
                {
                    byte lastTag = workingByteArray[lastTagPtr];
                    int dataLength = parsePtr - lastTagPtr - 1;
                    if(dataLength > 0) {
                        byte[] rawData = new byte[dataLength];
                        System.arraycopy(workingByteArray, lastTagPtr + 1, rawData, 0, dataLength);
                        SpeedTuneParam param = new SpeedTuneParamVL(lastTag, rawData);
                        paramQueue.offer(param);
                        String strTag = byteToString(lastTag);
                        if(strTagLastUpdateRequired.contains(strTag))
                        {
                            mapLastParamUpdate.put(strTag, param);
                        }
                    }
                    curTagPtr = -1;
                }

                if(checkTagRet == CHECK_TAG_RESULT_VARIABLE)
                {
                    curTagPtr = parsePtr;
                    parsePtr++;
                }
                else if(checkTagRet == CHECK_TAG_RESULT_FIXED)
                {
                    String strTag = byteToString(curByte);
                    Integer dataLengthObj = mapTagFixedLength.get(strTag);
                    if(dataLengthObj !=  null && dataLengthObj > 0)
                    {
                        int dataLength = dataLengthObj;
                        if(parsePtr + 1 + dataLength <= workingByteArray.length)
                        {
                            byte[] rawData = new byte[dataLength];
                            System.arraycopy(workingByteArray, parsePtr + 1, rawData, 0, dataLength);
                            SpeedTuneParam param = new SpeedTuneParamFL(curByte, rawData);
                            paramQueue.offer(param);
                            if(strTagLastUpdateRequired.contains(strTag))
                            {
                                mapLastParamUpdate.put(strTag, param);
                            }
                            parsePtr = parsePtr + dataLength + 1;
                        }
                        else
                        {
                            curTagPtr = parsePtr;
                            parsePtr = workingByteArray.length;
                        }
                    }
                    else
                    {
                        parsePtr++;
                        Log.e(CLASS_TAG, "Fixed length TAG without data length:" + curByte);
                    }
                }
                else
                {
                    parsePtr++;
                }

            }

            if(curTagPtr != -1)
            {
                int newWorkingLength = workingByteArray.length - curTagPtr;
                byte[] newWorkingByteArray = new byte[newWorkingLength];
                System.arraycopy(workingByteArray, curTagPtr, newWorkingByteArray, 0, newWorkingLength);
                workingByteArray = newWorkingByteArray;
            }
            else
            {
                workingByteArray = null;
            }

        }
    }

    public boolean parseCodes(int prevBuffLength)
    {
        boolean ret = false;
        if(workingByteArray != null)
        {
            boolean delimiterFound = false;
            int delimiterPtr;
            for(delimiterPtr = workingByteArray.length - 1; delimiterPtr >= prevBuffLength; delimiterPtr--)
            {
                if(workingByteArray[delimiterPtr] == tagDelimiter)
                {
                    delimiterFound = true;
                    break;
                }
            }

            if(delimiterFound)
            {
                int dataLength = delimiterPtr;
                if(dataLength <= 0)
                {
                    Log.w(CLASS_TAG, "No code found.");
                }
                else if(nPlatformType == MainActivity.PLATFORM_TYPE_F_SERIES)
                {
                    getCodesFromBuffer(CODE_LENGTH_F_SERIES, delimiterPtr);
                }
                else
                {
                    getCodesFromBuffer(CODE_LENGTH_OTHER, delimiterPtr);
                }
                ret = true;
            }
        }

        return ret;
    }

    private void getCodesFromBuffer(int codeLength, int dataLength)
    {
        int i;
        for(i = 0; i < dataLength; i = i + codeLength)
        {
            byte[] rawData = new byte[codeLength];
            System.arraycopy(workingByteArray, i, rawData, 0, codeLength);
            SpeedTuneCode code = new SpeedTuneCode(rawData);
            codesQueue.offer(code);
        }

        if(i > dataLength)
        {
            Log.w(CLASS_TAG, "wrong data received: " + Arrays.toString(workingByteArray));
        }
    }

    public void dumpParamQueue()
    {
        Log.w(CLASS_TAG, "paramQueue size:" + paramQueue.size());
//        SpeedTuneParam param;
//        while((param = paramQueue.poll()) != null)
        for(SpeedTuneParam param : paramQueue)
        {
            Log.w(CLASS_TAG, "param:" + param.toString());
        }
    }

    public void clearParamQueue()
    {
        workingByteArray = null;
        paramQueue.clear();
    }

    public void clearCodesQueue()
    {
        workingByteArray = null;
        codesQueue.clear();
        isCodesReadDone = false;
    }
}
