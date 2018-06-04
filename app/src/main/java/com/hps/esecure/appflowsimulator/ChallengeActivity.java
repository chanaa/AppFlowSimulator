package com.hps.esecure.appflowsimulator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hps.esecure.appflowsimulator.settings.SettingsActivity;
import com.hps.esecure.common.security.emv.crypto.impl.AppFlowCryptoService;
import com.hps.esecure.common.utils.JSONSenderUtilsSim;
import com.hps.esecure.model.emv.dto.network.ARes;
import com.hps.esecure.model.emv.dto.network.CReq;
import com.hps.esecure.model.emv.dto.network.CRes;
import com.hps.esecure.model.emv.dto.network.EMVmessage;
import com.hps.esecure.model.emv.dto.network.EmvError;
import com.hps.esecure.model.emv.dto.validation.JsonEmvDeserializer;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.X509CertUtils;

import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import components.SDKWebView;

public class ChallengeActivity extends AppCompatActivity implements SDKWebView.SDKWebViewClientListener{

    private Gson gson;

    private CReq cReq;

    private String acsUrl;

    private byte sdkCounterStoA;

    private byte sdkCounterAtoS;

    private String acsUiType;

    private LinearLayout linearLayoutMultipleSelect;
    private RadioGroup radioGroupSingleSelect;
    private TextView inputText;

    private Button submitButton;
    private Button resendButton;

    private SecretKey secretKey;

    private EncryptionMethod encryptionMethod = EncryptionMethod.A128CBC_HS256;

    private SDKWebView webView;

    private String challengeAddInfo;

    private String acsHTMLRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        inputText = (TextView) findViewById(R.id.inputText);
        radioGroupSingleSelect = (RadioGroup) findViewById(R.id.uitype_single);
        linearLayoutMultipleSelect = (LinearLayout) findViewById(R.id.uitype_multiple);
        submitButton = (Button) findViewById(R.id.submit);
        resendButton = (Button) findViewById(R.id.resend);

        webView = (SDKWebView) findViewById(R.id.display_challenge_html);

        String aresContent = getIntent().getExtras().getString("ares");
        byte[] secretEncoded = getIntent().getExtras().getByteArray("secret");
        acsUrl = getIntent().getExtras().getString("acsurl");

        secretKey = new SecretKeySpec(secretEncoded, "AES");

        JsonEmvDeserializer jsonEmvDeserializer = new JsonEmvDeserializer(LoggerFactory.getLogger(JsonEmvDeserializer.class));
        jsonEmvDeserializer.setTestMode(true);

        gson = new GsonBuilder().registerTypeAdapter(EMVmessage.class, jsonEmvDeserializer).create();

        submitButton.setOnClickListener(submitButtonClickListener);

        resendButton.setOnClickListener(resendButtonClickListener);

        processAres(aresContent);
    }

    private View.OnClickListener submitButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendEntry();
        }
    };

    private void sendEntry(){
        switch (acsUiType) {
            case CRes.UITYPE_TEXT:
                cReq.setChallengeDataEntry(inputText.getText().toString());

                break;

            case CRes.UITYPE_SINGLESELECT: {

                for (int i = 0; i < radioGroupSingleSelect.getChildCount(); i++) {
                    RadioButton radioButton = (RadioButton) radioGroupSingleSelect.getChildAt(i);
                    if (radioButton.isChecked()) {
                        cReq.setChallengeDataEntry((String) radioButton.getTag());
                    }
                }

                break;
            }
            case CRes.UITYPE_MULTISELECT: {

                StringBuilder challengeDataEntryBuilder = new StringBuilder();
                for (int i = 0; i < linearLayoutMultipleSelect.getChildCount(); i++) {
                    CheckBox checkBox = (CheckBox) linearLayoutMultipleSelect.getChildAt(i);
                    if (checkBox.isChecked()) {
                        if (challengeDataEntryBuilder.length() != 0) {
                            challengeDataEntryBuilder.append(",");
                        }
                        challengeDataEntryBuilder.append((String) checkBox.getTag());
                    }
                }

                cReq.setChallengeDataEntry(challengeDataEntryBuilder.toString());

                break;
            }
            case CRes.UITYPE_OOB:


                break;

        }

        sendCreq();
    }

    private View.OnClickListener resendButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            resend();
        }
    };

    private void resend(){
        cReq.setResendChallenge("Y");

        sendCreq();

        cReq.setResendChallenge(null);
    }

    private void processAres(String aresContent) {
        try {

            ARes ares = (ARes) gson.fromJson(aresContent, EMVmessage.class);

            generateAndSendCreq(ares);

        } catch (Exception e) {
            Intent myIntent = new Intent(ChallengeActivity.this, ErrorActivity.class);
            ChallengeActivity.this.startActivity(myIntent);

            finish();
        }
    }

    private void generateAndSendCreq(ARes ares) {
        cReq = new CReq();
        cReq.setMessageVersion(EMVmessage.CURRENT_THREEDS_MESSAGE_VERSION);
        cReq.setThreeDSServerTransID(ares.getThreeDSServerTransID());
        cReq.setAcsTransID(ares.getAcsTransID());
        cReq.setSdkTransID(ares.getSdkTransID());

        sendCreq();
    }

    @Override
    public void challengeHtmlDataEntered(String htmlData) {

        cReq.setChallengeHTMLDataEntry(htmlData);

        sendCreq();
    }

    private void sendCreq(){
        cReq.setSdkCounterStoA(String.format("%03d", sdkCounterStoA));
        String creqContent = gson.toJson(cReq);

        AppFlowCryptoService appFlowCryptoHelper = new AppFlowCryptoService();

        try {
            JWEObject encrypted = appFlowCryptoHelper.encryptCreq(encryptionMethod, creqContent.getBytes(), secretKey, cReq.getAcsTransID(),
                    sdkCounterStoA);

            SendCreqAsyncTask sendCreqAsyncTask = new SendCreqAsyncTask();
            sendCreqAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, acsUrl, encrypted.serialize());
        } catch (JOSEException e) {
            e.printStackTrace();
        }

        sdkCounterStoA++;
        if (sdkCounterStoA == 0) {
            // error

            Intent myIntent = new Intent(ChallengeActivity.this, ErrorActivity.class);
            ChallengeActivity.this.startActivity(myIntent);

            finish();
            return;
        }
    }


    private class SendCreqAsyncTask extends AsyncTask<String, Integer, String> {

        private WeakReference<ChallengeActivity> challengeActivityWeakReference;

        @Override
        protected void onPreExecute() {
            challengeActivityWeakReference = new WeakReference<>(ChallengeActivity.this);

            findViewById(R.id.display_challenge).setVisibility(View.GONE);
            findViewById(R.id.display_challenge_html).setVisibility(View.GONE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            JSONSenderUtilsSim jsonSenderUtilsSim = new JSONSenderUtilsSim(LoggerFactory.getLogger(JSONSenderUtilsSim.class));
            return jsonSenderUtilsSim.sendPostRequest(params[0], params[1]);
        }

        @Override
        protected void onPostExecute(String encrypted) {

            ChallengeActivity challengeActivity = challengeActivityWeakReference.get();

            if (challengeActivity == null || challengeActivity.isFinishing()){
                return;
            }

            AppFlowCryptoService appFlowCryptoHelper = new AppFlowCryptoService();

            String result = null;
            try {
                JWEObject jweObject = JWEObject.parse(encrypted);

                appFlowCryptoHelper.decryptCres(jweObject, secretKey);

                result = jweObject.getPayload().toString();

            } catch (ParseException | JOSEException e) {
                e.printStackTrace();
            }


            try {
                EMVmessage emvMessage = gson.fromJson(result, EMVmessage.class);

                if (emvMessage instanceof EmvError) {
                    Intent myIntent = new Intent(challengeActivity, ErrorActivity.class);
                    challengeActivity.startActivity(myIntent);

                    finish();

                } else if (emvMessage instanceof CRes) {

                    CRes cres = (CRes) emvMessage;

                    boolean verification = verifyAndIncrementCounter(cres);

                    if (verification) {
                        populateUi(cres);

                    } else {
                        // FIXME send error

                        Intent myIntent = new Intent(challengeActivity, ErrorActivity.class);
                        challengeActivity.startActivity(myIntent);

                        finish();
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
                Intent myIntent = new Intent(challengeActivity, ErrorActivity.class);
                challengeActivity.startActivity(myIntent);

                finish();

            }
        }

        private boolean verifyAndIncrementCounter(CRes cres) {
            if (String.format("%03d", sdkCounterAtoS).equals(cres.getAcsCounterAtoS())) {
                sdkCounterAtoS++;
                return true;
            }
            return false;
        }

        private void populateUi(CRes cres) {

            if (cres.getIssuerImage() != null){
                new DownloadImageTask((ImageView) findViewById(R.id.issuerimage))
                        .execute(cres.getIssuerImage().getHigh());
            }

            if (cres.getPsImage() != null){
                new DownloadImageTask((ImageView) findViewById(R.id.psimage))
                        .execute(cres.getPsImage().getHigh());
            }

            if (CRes.COMPLETION_INDICATOR_Y.equals(cres.getChallengeCompletionInd())) {

                if (CRes.TRANSSTATUS_Y.equals(cres.getTransStatus())) {
                    Intent myIntent = new Intent(ChallengeActivity.this, SuccessActivity.class);
                    ChallengeActivity.this.startActivity(myIntent);
                } else {
                    Intent myIntent = new Intent(ChallengeActivity.this, ErrorActivity.class);
                    ChallengeActivity.this.startActivity(myIntent);
                }

                finish();

                return;

            }

            findViewById(R.id.progressBar).setVisibility(View.GONE);

            ((TextView) findViewById(R.id.challenge_info_header)).setText(cres.getChallengeInfoHeader());
            ((TextView) findViewById(R.id.challenge_info_text)).setText(cres.getChallengeInfoText());
            ((TextView) findViewById(R.id.challenge_info_label)).setText(cres.getChallengeInfoLabel());

            if (cres.getResendInformationLabel() != null) {
                resendButton.setVisibility(View.VISIBLE);
                resendButton.setText(cres.getResendInformationLabel());
            } else {
                resendButton.setVisibility(View.GONE);
            }

            acsUiType = cres.getAcsUiType();

            switch (acsUiType) {
                case CRes.UITYPE_TEXT:

                    findViewById(R.id.display_challenge).setVisibility(View.VISIBLE);

                    inputText.setVisibility(View.VISIBLE);
                    radioGroupSingleSelect.setVisibility(View.GONE);
                    linearLayoutMultipleSelect.setVisibility(View.GONE);

                    submitButton.setText(cres.getSubmitAuthenticationLabel());

                    break;

                case CRes.UITYPE_SINGLESELECT: {

                    findViewById(R.id.display_challenge).setVisibility(View.VISIBLE);

                    radioGroupSingleSelect.setVisibility(View.VISIBLE);
                    inputText.setVisibility(View.GONE);
                    linearLayoutMultipleSelect.setVisibility(View.GONE);

                    radioGroupSingleSelect.removeAllViews();

                    Map<String, String> challengeSelectInfo = cres.getChallengeSelectInfo();

                    Set<Map.Entry<String, String>> entries = challengeSelectInfo.entrySet();
                    for (Map.Entry<String, String> entry : entries) {
                        RadioButton radioButton = new RadioButton(ChallengeActivity.this);

                        radioButton.setText(entry.getValue());
                        radioButton.setTag(entry.getKey());

                        radioGroupSingleSelect.addView(radioButton);
                    }

                    submitButton.setText(cres.getSubmitAuthenticationLabel());

                    break;
                }
                case CRes.UITYPE_MULTISELECT: {

                    findViewById(R.id.display_challenge).setVisibility(View.VISIBLE);

                    linearLayoutMultipleSelect.setVisibility(View.VISIBLE);
                    inputText.setVisibility(View.GONE);
                    radioGroupSingleSelect.setVisibility(View.GONE);

                    linearLayoutMultipleSelect.removeAllViews();

                    Map<String, String> challengeSelectInfo = cres.getChallengeSelectInfo();

                    Set<Map.Entry<String, String>> entries = challengeSelectInfo.entrySet();
                    for (Map.Entry<String, String> entry : entries) {
                        CheckBox checkBox = new CheckBox(ChallengeActivity.this);

                        checkBox.setText(entry.getValue());
                        checkBox.setTag(entry.getKey());

                        linearLayoutMultipleSelect.addView(checkBox);
                    }

                    submitButton.setText(cres.getSubmitAuthenticationLabel());

                    break;
                }
                case CRes.UITYPE_HTML: {


                    webView.setVisibility(View.VISIBLE);
                    webView.setSdkWebViewClientListener(ChallengeActivity.this);

                    String acshtml = new String(Base64.decode(cres.getAcsHTML(), Base64.URL_SAFE));

                    Log.d("test", acshtml);

                    webView.loadData(acshtml, "text/html", null);

                    submitButton.setText(cres.getSubmitAuthenticationLabel());

                    acsHTMLRefresh = cres.getAcsHTMLRefresh();

                    break;

                }
                case CRes.UITYPE_OOB: {
                    findViewById(R.id.display_challenge).setVisibility(View.VISIBLE);

                    submitButton.setText(cres.getOobContinueLabel());

                    ((TextView) findViewById(R.id.challenge_add_info)).setText(cres.getChallengeAddInfo());

                    challengeAddInfo = cres.getChallengeAddInfo();

                    break;
                }
            }


        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (challengeAddInfo != null){

            findViewById(R.id.challenge_info_text).setVisibility(View.GONE);

            findViewById(R.id.challenge_add_info).setVisibility(View.VISIBLE);

        } else if (acsHTMLRefresh !=null){
            webView.loadData(new String(Base64.decode(acsHTMLRefresh, Base64.URL_SAFE)), "text/html", null);

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.challenge_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.menu_cancel) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        cReq.setChallengeCancel(CReq.CHALLENGECANCEL_CARDHOLDER);
        cReq.setChallengeDataEntry(null);
        cReq.setChallengeHTMLDataEntry(null);

        sendCreq();

        super.onBackPressed();
    }


    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);

                int height = 120;
                int width = (height*mIcon11.getWidth())/mIcon11.getHeight();
                mIcon11 = Bitmap.createScaledBitmap(mIcon11,width,height, true);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }


}
