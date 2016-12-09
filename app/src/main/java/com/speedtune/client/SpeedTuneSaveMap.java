package com.speedtune.client;

import java.util.ArrayList;

/**
 * Created by yulexun on 12/4/2016.
 */

public class SpeedTuneSaveMap extends SpeedTuneSaveCmd {
    private static final String CMD_TYPE_SAVE_MAP = "M";
    static{
        SpeedTuneSaveCmdInfo info = new SpeedTuneSaveCmdInfo();
        ArrayList<String> saveTags = new ArrayList<>();
        saveTags.add("F");
        info.saveTags = saveTags;
        info.fieldSize = 2;
        infoMap.put(CMD_TYPE_SAVE_MAP, info);
    }

    public SpeedTuneSaveMap()
    {
        super(CMD_TYPE_SAVE_MAP);
    }
}
