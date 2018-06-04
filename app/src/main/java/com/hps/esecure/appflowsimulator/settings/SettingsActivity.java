package com.hps.esecure.appflowsimulator.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.hps.esecure.appflowsimulator.R;
import com.hps.esecure.model.emv.dto.network.AReq;

import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private CheckBox checkBoxNative;
    private CheckBox checkBoxhtml;
    private CheckBox checkBoxText;
    private CheckBox checkBoxSingleSelect;
    private CheckBox checkBoxMultiSelect;
    private CheckBox checkBoxOOB;
    private CheckBox checkBoxHTMLOther;
    private EditText acsurlEditText;
    private EditText dsurlEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        acsurlEditText = (EditText) findViewById(R.id.acsurl);
        dsurlEditText = (EditText) findViewById(R.id.dsurl);

        checkBoxNative = (CheckBox) findViewById(R.id.interface_native);
        checkBoxhtml = (CheckBox) findViewById(R.id.interface_html);
        checkBoxText = (CheckBox) findViewById(R.id.uitype_text);
        checkBoxSingleSelect = (CheckBox) findViewById(R.id.uitype_single_select);
        checkBoxMultiSelect = (CheckBox) findViewById(R.id.uitype_multi_select);
        checkBoxOOB = (CheckBox) findViewById(R.id.uitype_oob);
        checkBoxHTMLOther = (CheckBox) findViewById(R.id.uitype_html_other);

        initializeUI();

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setPreferences();

            }
        });
    }

    private void setPreferences() {
        SettingsPreferenceHelper.setAcsUrl(this, acsurlEditText.getText().toString());
        SettingsPreferenceHelper.setDsUrl(this, dsurlEditText.getText().toString());

        if (checkBoxNative.isChecked() && checkBoxhtml.isChecked()){
            SettingsPreferenceHelper.setUiInterface(this, AReq.DeviceRenderOptions.UIINTERFACE_BOTH);
        } else if (checkBoxNative.isChecked()){
            SettingsPreferenceHelper.setUiInterface(this, AReq.DeviceRenderOptions.UIINTERFACE_NATIVE);
        } else if (checkBoxhtml.isChecked()){
            SettingsPreferenceHelper.setUiInterface(this, AReq.DeviceRenderOptions.UIINTERFACE_HTML);
        }

        Set<String> uiTypes = new HashSet<>();
        if (checkBoxText.isChecked()){
            uiTypes.add(AReq.DeviceRenderOptions.UITYPE_TEXT);
        }
        if (checkBoxOOB.isChecked()){
            uiTypes.add(AReq.DeviceRenderOptions.UITYPE_OOB);
        }
        if (checkBoxMultiSelect.isChecked()){
            uiTypes.add(AReq.DeviceRenderOptions.UITYPE_MULTISELECT);
        }
        if (checkBoxSingleSelect.isChecked()){
            uiTypes.add(AReq.DeviceRenderOptions.UITYPE_SINGLESELECT);
        }
        if (checkBoxHTMLOther.isChecked()){
            uiTypes.add(AReq.DeviceRenderOptions.UITYPE_HTML);
        }

        SettingsPreferenceHelper.setUiType(this, uiTypes);

        finish();
    }

    private void initializeUI() {
        acsurlEditText.setText(SettingsPreferenceHelper.getAcsUrl(this));
        dsurlEditText.setText(SettingsPreferenceHelper.getDsUrl(this));

        String uiInterface = SettingsPreferenceHelper.getUiInterface(this);
        if (AReq.DeviceRenderOptions.UIINTERFACE_NATIVE.equals(uiInterface)){
            checkBoxNative.setChecked(true);
        } else if (AReq.DeviceRenderOptions.UIINTERFACE_HTML.equals(uiInterface)){
            checkBoxhtml.setChecked(true);
        } else if (AReq.DeviceRenderOptions.UIINTERFACE_BOTH.equals(uiInterface)){
            checkBoxhtml.setChecked(true);
            checkBoxNative.setChecked(true);
        }

        Set<String> uiTypes = SettingsPreferenceHelper.getUiType(this);

        for (String uiType:uiTypes){
            switch (uiType){
                case AReq.DeviceRenderOptions.UITYPE_TEXT:
                    checkBoxText.setChecked(true);
                    break;
                case AReq.DeviceRenderOptions.UITYPE_OOB:
                    checkBoxOOB.setChecked(true);
                    break;
                case AReq.DeviceRenderOptions.UITYPE_MULTISELECT:
                    checkBoxMultiSelect.setChecked(true);
                    break;
                case AReq.DeviceRenderOptions.UITYPE_SINGLESELECT:
                    checkBoxSingleSelect.setChecked(true);
                    break;
                case AReq.DeviceRenderOptions.UITYPE_HTML:
                    checkBoxHTMLOther.setChecked(true);
                    break;
            }
        }

    }


}
