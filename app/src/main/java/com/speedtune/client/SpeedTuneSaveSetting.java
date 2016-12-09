package com.speedtune.client;

import java.util.ArrayList;

/**
 * Created by yulexun on 12/4/2016.
 */

public class SpeedTuneSaveSetting extends SpeedTuneSaveCmd {
    private static final String CMD_TYPE_SAVE_SETTING = "R";
    static{
        SpeedTuneSaveCmdInfo info = new SpeedTuneSaveCmdInfo();
        ArrayList<String> saveTags = new ArrayList<>();
        saveTags.add("a_0");
        saveTags.add("a_1");
        saveTags.add("a_2");
        saveTags.add("a_3");
        saveTags.add("a_4");
        saveTags.add("a_5");
        saveTags.add("a_6");
        saveTags.add("a_7");
        saveTags.add("a_8");
        saveTags.add("a_9");
        saveTags.add("a_10");
        saveTags.add("a_11");
        saveTags.add("t");
        saveTags.add("u");
        saveTags.add("m");
        saveTags.add("v");
        saveTags.add("n");
        saveTags.add("s");
        saveTags.add("o");
        saveTags.add("p");
        saveTags.add("+");
        saveTags.add("r");
        saveTags.add("x");
        saveTags.add("`");
        saveTags.add("a_12");
        saveTags.add("a_13");
        saveTags.add("a_14");
        saveTags.add("a_15");
        saveTags.add("a_16");
        saveTags.add("a_17");
        saveTags.add("a_18");
        saveTags.add("a_19");
        saveTags.add("a_20");
        saveTags.add("a_21");
        saveTags.add("a_22");
        saveTags.add("a_23");
        saveTags.add("a_24");
        saveTags.add("a_25");
        saveTags.add("a_26");
        saveTags.add("a_27");
        saveTags.add("a_28");
        saveTags.add("a_29");
        saveTags.add("a_30");
        saveTags.add("a_31");
        saveTags.add("a_32");
        saveTags.add("a_33");
        saveTags.add("a_34");
        saveTags.add("a_35");
        info.saveTags = saveTags;
        info.endByte = (byte)'#';
        infoMap.put(CMD_TYPE_SAVE_SETTING, info);
    }

    public SpeedTuneSaveSetting()
    {
        super(CMD_TYPE_SAVE_SETTING);
    }
}
