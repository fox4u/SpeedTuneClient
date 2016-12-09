package com.speedtune.client;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by yulexun on 11/29/2016.
 */

public class CodesFragment extends Fragment
{
    private static final String TAG = "CodesFragment";
    protected MainActivity act;
    private Button btReadCodes;
    private Button btDelCodes;
    private TextView tvCodes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_codes, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        act = (MainActivity) getActivity();
        tvCodes = (TextView) view.findViewById(R.id.codes_box);
        btReadCodes = (Button) view.findViewById(R.id.bt_codes_read);
        btReadCodes.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                act.sendReadCodesCmd(false);
            }
        });

        btDelCodes = (Button) view.findViewById(R.id.bt_codes_delete);
        btDelCodes.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                act.sendReadCodesCmd(true);
            }
        });

        initPlatformSpinner();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState)
    {
        super.onViewStateRestored(savedInstanceState);
        showAllCodes(act.deviceCodes);
    }

    private void initPlatformSpinner()
    {
        View fragmentView = getView();
        if (fragmentView != null)
        {
            final Spinner sp = (Spinner) fragmentView.findViewById(R.id.plat_spinner);
            if (sp != null)
            {
                sp.setSelection(act.getPlatformType(), false);
                sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
                    {
                        act.setPlatformType(i);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView)
                    {

                    }
                });
            }
        }
    }

    public void showAllCodes(ArrayList<String> deviceCodes)
    {
        String strDisplay = "";
        if (deviceCodes != null)
        {
            for (String code : deviceCodes)
            {
                strDisplay = strDisplay + code + "\r\n";
            }
        } else
        {
            strDisplay = getResources().getString(R.string.codes_read_none);
        }

        tvCodes.setText(strDisplay);
    }

    public void setButtonsEnabled(boolean enabled)
    {
        if (btReadCodes != null)
        {
            btReadCodes.setEnabled(enabled);
        }

        if (btDelCodes != null)
        {
            btDelCodes.setEnabled(enabled);
        }
    }

    public void updateUIStatusReading()
    {
        tvCodes.setText(getResources().getString(R.string.codes_read_in_progress));
        setButtonsEnabled(false);
    }

    public void updateUIStatusDeleting()
    {
        tvCodes.setText(getResources().getString(R.string.codes_delete_in_progress));
        setButtonsEnabled(false);
    }
}
