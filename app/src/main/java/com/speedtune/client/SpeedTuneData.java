package com.speedtune.client;

/**
 * Created by yulexun on 12/2/2016.
 */

public class SpeedTuneData {
    protected String displayVal;
    protected String fieldName;
    protected String updatedVal;

    public SpeedTuneData(String displayVal, String fieldName) {
        this.displayVal = displayVal;
        this.fieldName = fieldName;
        this.updatedVal = null;
    }

    public String getActualDisplayVal()
    {
        return (updatedVal == null)? displayVal : updatedVal;
    }
}
