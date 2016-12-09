package com.speedtune.client;

import java.util.ArrayList;

/**
 * Created by yulexun on 12/4/2016.
 */

public class SpeedTuneSaveMeth extends SpeedTuneSaveCmd {
    private static final String CMD_TYPE_SAVE_METH = "T";
    static{
        SpeedTuneSaveCmdInfo info = new SpeedTuneSaveCmdInfo();
        ArrayList<String> saveTags = new ArrayList<>();
        saveTags.add("P");
        saveTags.add("Q");
        saveTags.add("R");
        saveTags.add("S");
        saveTags.add("T");
        saveTags.add("U");
        saveTags.add("V");
        saveTags.add("w");
        saveTags.add("@");
        saveTags.add("{");
        saveTags.add("}");
        saveTags.add("|");
        info.saveTags = saveTags;
        info.endByte = (byte)'$';
        info.fieldSize = 2;
        infoMap.put(CMD_TYPE_SAVE_METH, info);
    }

    public SpeedTuneSaveMeth()
    {
        super(CMD_TYPE_SAVE_METH);
    }
}
