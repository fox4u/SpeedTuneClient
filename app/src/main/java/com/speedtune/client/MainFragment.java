package com.speedtune.client;

import android.app.Fragment;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by yulexun on 11/28/2016.
 */

public class MainFragment extends Fragment {
    private static final String TAG = "MainFragment";
    private static final String TAG_PREFIX_PARAM_VALUE_TEXTVIEW = "TextviewTag_";
    private static final String TAG_PREFIX_PARAM_VALUE_EDITTEXT = "EdittextTag_";

    public final static int CONN_STATUS_NONE = 0;
    public final static int CONN_STATUS_CONNECTING = 1;
    public final static int CONN_STATUS_CONNECTED = 2;

    protected MainActivity act;
    private ProgressBar bar;
    private Button btConnect;
    private Button btSaveSettings;
    private Button btSaveMeth;
    private EditText focusedET;
    protected int connectionStatus = CONN_STATUS_NONE;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        act = (MainActivity) getActivity();

        btConnect = (Button) view.findViewById(R.id.bt_connect);

        btConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(connectionStatus == CONN_STATUS_NONE)
                {
                    connectionStatus = CONN_STATUS_CONNECTING;
                    updateConnStatus();
                    act.handleConnect();
                }
                else if(connectionStatus == CONN_STATUS_CONNECTED)
                {

                }
            }
        });


        Button btDiscard = (Button) view.findViewById(R.id.bt_discard_changes);
        final View fragmentView = view;
        btDiscard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                resetSettingsToDeviceValue();
            }
        });

        btSaveSettings = (Button) view.findViewById(R.id.bt_save_settings);
        btSaveSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connectionStatus == CONN_STATUS_CONNECTED)
                {
                    act.sendSaveSettingCmd();
                }
            }
        });

        btSaveMeth = (Button) view.findViewById(R.id.bt_save_meth);
        btSaveMeth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connectionStatus == CONN_STATUS_CONNECTED)
                {
                    act.sendSaveMethCmd();
                }
            }
        });

        LinearLayout[] layouts = {(LinearLayout)view.findViewById(R.id.main_param_list_left),(LinearLayout)view.findViewById(R.id.main_param_list_right)};
        int i = 0;
        for (HashMap.Entry<String, SpeedTuneData> entry : act.deviceParams.entrySet()) {
            LinearLayout container_layout =  layouts[i % 2];

            if(container_layout != null)
            {
                String key = entry.getKey();
                SpeedTuneData data = entry.getValue();
                LinearLayout layout = new LinearLayout(getActivity());
                layout.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams params0 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                container_layout.addView(layout, params0);

                TextView label = new TextView(getActivity());
                label.setText(data.fieldName);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
                LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f);
                layout.addView(label, params1);

                TextView value = new TextView(getActivity());
                value.setTag(TAG_PREFIX_PARAM_VALUE_TEXTVIEW + key);
                value.setText(data.displayVal);
                value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18.0f);
                LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.5f);
                layout.addView(value, params2);
            }

            i++;
        }

        bar = (ProgressBar) view.findViewById(R.id.bar);
        bar.setVisibility(View.INVISIBLE);

        updateConnStatus();

        prepareSaveCmds();

        ScrollView scroll = (ScrollView) view.findViewById(R.id.main_scroll);
        if(scroll != null)
        {
            configViewTouch(scroll);
        }

        initMapSpinner();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        restoreAllValues();
    }

    public void setConnStatus(int status)
    {
        connectionStatus = status;
        updateConnStatus();
    }

    private void updateConnStatus()
    {
        switch (connectionStatus)
        {
            case CONN_STATUS_NONE:
                btConnect.setText(R.string.main_connect);
                btConnect.setEnabled(true);
                bar.setVisibility(View.INVISIBLE);
                break;
            case CONN_STATUS_CONNECTING:
                btConnect.setEnabled(false);
                bar.setVisibility(View.VISIBLE);
                break;
            case CONN_STATUS_CONNECTED:
                btConnect.setEnabled(true);
                btConnect.setText(R.string.main_connected);
                bar.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
    }

    public void updateParam(SpeedTuneParam param)
    {
        View view = getView();

        if(view != null) {
            if (param instanceof SpeedTuneParamVL) {
                String dispVal = ((SpeedTuneParamVL) param).getDisplayValue();
                String etTag = TAG_PREFIX_PARAM_VALUE_EDITTEXT + (char) param.tag;
                String tvTag = TAG_PREFIX_PARAM_VALUE_TEXTVIEW + (char) param.tag;
                EditText et = (EditText) view.findViewWithTag(etTag);
                if (et != null) {
                    et.setText(dispVal);
                    et.getBackground().clearColorFilter();
                }
                TextView tv = (TextView) view.findViewWithTag(tvTag);
                if (tv != null) {
                    tv.setText(dispVal);
                }
            } else if (param instanceof SpeedTuneParamFL) {
                SpeedTuneParamFL paramFL = (SpeedTuneParamFL) param;
                for (HashMap.Entry<Integer, String> entry : paramFL.getMapEntrySet()) {
                    String dispVal = entry.getValue();
                    String strTag = SpeedTuneReceiver.byteToString(param.tag) + "_" + entry.getKey();
                    String etTag = TAG_PREFIX_PARAM_VALUE_EDITTEXT + strTag;
                    EditText et = (EditText) view.findViewWithTag(etTag);
                    if (et != null) {
                        et.setText(dispVal);
                        et.getBackground().clearColorFilter();
                    }
                }
            }
        }
    }

    private void prepareSaveCmds()
    {
        prepareSaveCmd(new SpeedTuneSaveMap().getSaveTags());
        prepareSaveCmd(new SpeedTuneSaveSetting().getSaveTags());
        prepareSaveCmd(new SpeedTuneSaveMeth().getSaveTags());
    }

    private void prepareSaveCmd(ArrayList<String> listTags)
    {
        View fragView = getView();

        for(String strTag : listTags)
        {
            final SpeedTuneData data = act.deviceSetting.get(strTag);

            if(fragView != null && data != null)
            {
                final String etTag = TAG_PREFIX_PARAM_VALUE_EDITTEXT + strTag;
                final EditText et = (EditText) fragView.findViewWithTag(etTag);
                if (et != null) {
                    et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View view, boolean b) {
                            if(!b)
                            {
                                String curDisplayVal = et.getText().toString();
                                curDisplayVal.trim();
                                if(!curDisplayVal.equals(data.displayVal))
                                {
                                    data.updatedVal = curDisplayVal;
                                    et.getBackground().setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.OVERLAY);
                                }
                                else
                                {
                                    data.updatedVal = null;
                                    et.getBackground().clearColorFilter();
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private void configViewTouch(final View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent motionEvent) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    if(focusedET != null)
                    {
                        focusedET.setFocusable(false);
                    }
                    focusedET = null;
                    return false;
                }
            });
        }
        else {
            view.setFocusable(false);
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent motionEvent) {
                    if(focusedET != null)
                    {
                        focusedET.setFocusable(false);
                    }
                    view.setFocusableInTouchMode(true);
                    focusedET = (EditText) view;
                    return false;
                }
            });
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup)view).getChildCount(); ++i) {
                configViewTouch(((ViewGroup)view).getChildAt(i));
            }
        }
    }

    private void resetSettingsToDeviceValue()
    {
        View fragmentView = getView();
        if(fragmentView != null)
        {
            for (HashMap.Entry<String, SpeedTuneData> entry : act.deviceSetting.entrySet()) {
                String strTag = entry.getKey();
                SpeedTuneData data = entry.getValue();
                String dispVal = data.displayVal;
                data.updatedVal = null;
                String etTag = TAG_PREFIX_PARAM_VALUE_EDITTEXT + strTag;
                EditText et = (EditText) fragmentView.findViewWithTag(etTag);
                if (et != null) {
                    et.setText(dispVal);
                    et.getBackground().clearColorFilter();
                }
            }
        }
    }

    private void restoreAllValues()
    {
        View fragmentView = getView();
        if(fragmentView != null)
        {
            for (HashMap.Entry<String, SpeedTuneData> entry : act.deviceSetting.entrySet()) {
                String strTag = entry.getKey();
                SpeedTuneData data = entry.getValue();
                String dispVal = data.getActualDisplayVal();
                String etTag = TAG_PREFIX_PARAM_VALUE_EDITTEXT + strTag;
                String tvTag = TAG_PREFIX_PARAM_VALUE_TEXTVIEW + strTag;
                EditText et = (EditText) fragmentView.findViewWithTag(etTag);
                if (et != null) {
                    et.setText(dispVal);
                    if (data.updatedVal == null)
                    {
                        et.getBackground().clearColorFilter();
                    }
                    else
                    {
                        et.getBackground().setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.OVERLAY);
                    }
                }
                TextView tv = (TextView) fragmentView.findViewWithTag(tvTag);
                if (tv != null) {
                    tv.setText(dispVal);
                }
            }
        }
    }

    private void initMapSpinner()
    {
        View fragmentView = getView();
        if(fragmentView != null)
        {
            final Spinner sp = (Spinner) fragmentView.findViewById(R.id.map_spinner);
            if (sp != null) {
                sp.setSelection(0, false);
                sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if(i > 0)
                        {
                            if(connectionStatus == CONN_STATUS_CONNECTED)
                            {
                                SpeedTuneData data = act.deviceSetting.get("F");
                                if(data != null)
                                {
                                    data.updatedVal = String.format(Locale.US, "%d", i - 1);
                                }
                                act.sendSaveMapCmd();
                            }
                            else
                            {
                                resetSpinner();
                            }
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        Log.w(TAG, "onNothingSelected");
                    }
                });
            }
        }
    }


    public void resetSpinner()
    {
        View fragmentView = getView();
        if(fragmentView != null)
        {
            final Spinner sp = (Spinner) fragmentView.findViewById(R.id.map_spinner);
            if (sp != null)
            {
                sp.setSelection(0, false);
            }
        }
    }

    public void setMapSpinnerEnabled(boolean enabled)
    {
        View fragmentView = getView();
        Spinner sp = (Spinner) fragmentView.findViewById(R.id.map_spinner);
        ProgressBar barMap = (ProgressBar) fragmentView.findViewById(R.id.map_bar);

        if(enabled)
        {
            if(sp != null)
            {
                sp.setEnabled(enabled);
                sp.setSelection(0, false);
            }

            if(barMap != null)
            {
                barMap.setVisibility(View.INVISIBLE);
            }
        }
        else
        {
            if(sp != null)
            {
                sp.setEnabled(enabled);
            }

            if(barMap != null)
            {
                barMap.setVisibility(View.VISIBLE);
            }
        }
    }

    public void setSaveSettingsEnabled(boolean enabled)
    {
        View fragmentView = getView();
        ProgressBar barSetting = (ProgressBar) fragmentView.findViewById(R.id.setting_bar);

        if(btSaveSettings != null)
        {
            btSaveSettings.setEnabled(enabled);
        }

        if(barSetting != null)
        {
            int v = enabled ? View.INVISIBLE : View.VISIBLE;
            barSetting.setVisibility(v);
        }
    }

    public void setSaveMethEnabled(boolean enabled)
    {
        View fragmentView = getView();
        ProgressBar barMeth = (ProgressBar) fragmentView.findViewById(R.id.meth_bar);

        if(btSaveMeth != null)
        {
            btSaveMeth.setEnabled(enabled);
        }

        if(barMeth != null)
        {
            int v = enabled ? View.INVISIBLE : View.VISIBLE;
            barMeth.setVisibility(v);
        }
    }
}
