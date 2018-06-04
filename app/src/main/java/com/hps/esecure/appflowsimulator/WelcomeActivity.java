package com.hps.esecure.appflowsimulator;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.hps.esecure.appflowsimulator.settings.SettingsActivity;
import com.hps.esecure.model.emv.dto.network.AReq;

public class WelcomeActivity extends AppCompatActivity implements View.OnClickListener {

    private Button buttonPA;
    private Button buttonNPA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        buttonPA = (Button) findViewById(R.id.payment_authentication);
        buttonNPA = (Button) findViewById(R.id.nonpayment_authentication);

        buttonPA.setOnClickListener(this);
        buttonNPA.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        Intent intent = new Intent(this, AuthenticationActivity.class);

        switch (viewId) {
            case R.id.payment_authentication:
                intent.putExtra(AReq.MESSAGECATEGORY_FIELD, AReq.MESSAGECATEGORY_PA);
                break;
            case R.id.nonpayment_authentication:
                intent.putExtra(AReq.MESSAGECATEGORY_FIELD, AReq.MESSAGECATEGORY_NPA);
                break;
        }

        startActivity(intent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.welcome_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
