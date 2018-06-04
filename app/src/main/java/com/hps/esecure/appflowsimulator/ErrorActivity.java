package com.hps.esecure.appflowsimulator;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.gson.Gson;
import com.hps.esecure.model.emv.dto.network.ARes;
import com.hps.esecure.model.emv.dto.network.CReq;
import com.hps.esecure.model.emv.dto.network.EMVmessage;

public class ErrorActivity extends AppCompatActivity {

    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);
    }
}
