package com.hps.esecure.appflowsimulator;


import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hps.esecure.appflowsimulator.settings.SettingsPreferenceHelper;
import com.hps.esecure.common.security.emv.crypto.impl.AResCryptoParams;
import com.hps.esecure.common.security.emv.crypto.impl.AppFlowCryptoService;
import com.hps.esecure.common.security.utils.Base64EncodageUtils;
import com.hps.esecure.common.utils.JSONSenderUtilsSim;
import com.hps.esecure.common.utils.JSONUtils;
import com.hps.esecure.model.emv.dto.network.AReq;
import com.hps.esecure.model.emv.dto.network.ARes;
import com.hps.esecure.model.emv.dto.network.DeviceInfo;
import com.hps.esecure.model.emv.dto.network.EMVmessage;
import com.hps.esecure.model.emv.dto.validation.JsonEmvDeserializer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jose.util.X509CertUtils;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.SecretKey;

import static com.hps.esecure.common.security.emv.crypto.impl.AppFlowCryptoService.encodeStringData;

public class AuthenticationActivity extends AppCompatActivity {

    private static final String SDK_REF_NUMBER = "SDKREFNUM123456";

    private AutoCompleteTextView panEditText;
    private EditText cardExpiryEditText;
    private String messageCategory;
    private EditText productPrice;

    private KeyPair keyPair;


    private static final String[] CARDS = new String[] {
            "4447201900252201","8891668778866889", "2291668778866776"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        messageCategory = getIntent().getExtras().getString(AReq.MESSAGECATEGORY_FIELD);

        panEditText = (AutoCompleteTextView) findViewById(R.id.pan);
        cardExpiryEditText = (EditText) findViewById(R.id.card_expiry);
        productPrice = (EditText) findViewById(R.id.product_price);


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, CARDS);

        panEditText.setAdapter(adapter);

        Button checkoutButton = (Button) findViewById(R.id.checkout);
        checkoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkout();

            }
        });

        if (AReq.MESSAGECATEGORY_PA.equals(messageCategory)){
            findViewById(R.id.product_info).setVisibility(View.VISIBLE);
            findViewById(R.id.payment_info).setVisibility(View.GONE);

            findViewById(R.id.purchase).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    findViewById(R.id.product_info).setVisibility(View.GONE);
                    findViewById(R.id.payment_info).setVisibility(View.VISIBLE);
                }
            });
            checkoutButton.setText(R.string.check_out);
        } else {
            checkoutButton.setText(R.string.authenticate);
        }

    }

    private void checkout(){
        AReq aReq = new AReq();

        aReq.setThreeDSRequestorID("XXXXXXXXXXX");
        aReq.setThreeDSRequestorName("XXXXXXXXXXX");
        aReq.setThreeDSRequestorURL("http://www.example.com");
        aReq.setThreeDSServerRefNumber("XXXXXXXXXXX");
        aReq.setMerchantName("Amazone");
        aReq.setThreeDSRequestorAuthenticationInd("01");
        UUID threeDSServerTransID = UUID.randomUUID();
        aReq.setThreeDSServerTransID(threeDSServerTransID.toString());
   aReq.setShipAddrCountry("840");
   aReq.setBillAddrCountry("840");
        aReq.setMerchantCountryCode("840");
        aReq.setMcc("ABCD");
        aReq.setThreeDSServerURL("http://www.example.com");

        UUID dsTransID = UUID.randomUUID();
        aReq.setDsTransID(dsTransID.toString());

        aReq.setDsReferenceNumber("XXXXXXXXXXX");
        aReq.setDsURL(SettingsPreferenceHelper.getDsUrl(this));

        aReq.setMessageVersion(EMVmessage.CURRENT_THREEDS_MESSAGE_VERSION);

        aReq.setSdkMaxTimeout("05");

        if (AReq.MESSAGECATEGORY_NPA.equals(messageCategory)){ // NPA
            aReq.setMessageCategory(AReq.MESSAGECATEGORY_NPA);
           // aReq.setThreeDSRequestorNPAInd("01");
        } else { // PA
            aReq.setMessageCategory(AReq.MESSAGECATEGORY_PA);

            aReq.setAcquirerBIN("XXXXXX");
            aReq.setAcquirerMerchantID("XXXXXX");
            aReq.setPurchaseAmount(productPrice.getText().toString()+"00");
            aReq.setPurchaseCurrency("840");
            aReq.setPurchaseExponent("2");
            aReq.setPurchaseDate("20170807201622");

        }


        aReq.setAcctNumber(panEditText.getText().toString());
        aReq.setCardExpiryDate(cardExpiryEditText.getText().toString());


        // browser channel
        aReq.setDeviceChannel(AReq.DEVICECHANNEL_APP);
        aReq.setSdkAppID("SDKAPPID123456");

        try {
            aReq.setSdkEphemPubKey(generateEphemeralPublicKey());
        } catch (JOSEException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        aReq.setSdkReferenceNumber(SDK_REF_NUMBER);
        aReq.setSdkTransID(UUID.randomUUID().toString());


        AReq.DeviceRenderOptions deviceRenderOptions = new AReq.DeviceRenderOptions();
        deviceRenderOptions.setSdkInterface(SettingsPreferenceHelper.getUiInterface(this));

        Set<String> uiTypes = SettingsPreferenceHelper.getUiType(this);

        deviceRenderOptions.setSdkUiType(uiTypes.toArray(new String[uiTypes.size()]));

        aReq.setDeviceRenderOptions(deviceRenderOptions);

        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDV("1.0");

        Map<String, String> dd = new HashMap<>();
        dd.put("C001", "Android");
        dd.put("C002", "Android");
        dd.put("C003", "Android");
        dd.put("C004", "Android");
        dd.put("C005", "Android");
        dd.put("C006", "Android");
        dd.put("C007", "Android");
        dd.put("A001", "Android");
        dd.put("A002", "Android");
        dd.put("A003", "Android");
        dd.put("A004", "Android");
        dd.put("A005", "Android");
        dd.put("A006", "Android");
        dd.put("A007", "Android");

        deviceInfo.setDD(dd);



        Gson gson = new GsonBuilder().create();

        String deviceInfoJson = gson.toJson(deviceInfo);

        try {
            aReq.setDeviceInfo(Base64.encodeToString(deviceInfoJson.getBytes("UTF-8"), Base64.URL_SAFE));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String areqContent = gson.toJson(aReq);

        sendAreq(SettingsPreferenceHelper.getAcsUrl(this), areqContent);

    }

    private AReq.SdkEphemPubKey  generateEphemeralPublicKey() throws JOSEException, UnsupportedEncodingException {
        AppFlowCryptoService appFlowCryptoHelper = new AppFlowCryptoService();
        ECParameterSpec ecParameterSpec = Curve.P_256.toECParameterSpec();
        keyPair = appFlowCryptoHelper.generateEphemeralKeyPair(ecParameterSpec);

        ECKey qC = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic()).build();
        AReq areq=new AReq();
        AReq.SdkEphemPubKey sdkkey = areq.new SdkEphemPubKey();
        sdkkey.setCrv(qC.getCurve().toString());
        sdkkey.setKty(qC.getKeyType().toString());
        sdkkey.setY(qC.getY().toString());
        sdkkey.setX(qC.getX().toString());
        return sdkkey;
    }

    private class SendAreqAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            findViewById(R.id.content).setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(String... params) {
            JSONSenderUtilsSim jsonSenderUtilsSim = new JSONSenderUtilsSim(LoggerFactory.getLogger(JSONSenderUtilsSim.class));
            String result = jsonSenderUtilsSim.sendPostRequest(params[0], params[1]);

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            findViewById(R.id.content).setVisibility(View.VISIBLE);

            JsonEmvDeserializer jsonEmvDeserializer = new JsonEmvDeserializer(LoggerFactory.getLogger(JsonEmvDeserializer.class));
            jsonEmvDeserializer.setTestMode(true);

            Gson gson = new GsonBuilder().registerTypeAdapter(EMVmessage.class, jsonEmvDeserializer).create();

            try {
                EMVmessage emvMessage = gson.fromJson(result, EMVmessage.class);

                if (emvMessage instanceof ARes){

                    ARes ares = (ARes) emvMessage;

                    if (ARes.TRANSSTATUS_C.equals(ares.getTransStatus())){
                        Intent myIntent = new Intent(AuthenticationActivity.this, ChallengeActivity.class);

                        AppFlowCryptoService appFlowCryptoHelper = new AppFlowCryptoService();

                        String acsSignedContent = ares.getAcsSignedContent();

                        String content = jwsValidateSignatureAndReturnBody(acsSignedContent);

                        net.minidev.json.JSONObject parse = JSONObjectUtils.parse(content);
                        Gson gson1=new Gson();
                        String acsEphemPubKey=gson1.toJson(parse.get("acsEphemPubKey"));

                        //String acsEphemPubKey = (String) parse.get("acsEphemPubKey");
                       // String acsurl = (String) parse.get("ACSURL");
                        String acsurl="https://dev.acpqualife.com/3Dsecure/acsauthentication/customerChallengeAppServlet";
                        JWK acsEphemPubKeyJwk = JWK.parse(acsEphemPubKey);

                        ECPrivateKey dC = (ECPrivateKey) keyPair.getPrivate();

                        SecretKey secretKey = appFlowCryptoHelper.generateCEKforSDK(acsEphemPubKeyJwk, dC, SDK_REF_NUMBER);

                        myIntent.putExtra("secret", secretKey.getEncoded());
                        myIntent.putExtra("ares", result);
                        myIntent.putExtra("acsurl", acsurl);

                        AuthenticationActivity.this.startActivity(myIntent);
                    } else if (ARes.TRANSSTATUS_Y.equals(ares.getTransStatus())){
                        Intent myIntent = new Intent(AuthenticationActivity.this, SuccessActivity.class);
                        AuthenticationActivity.this.startActivity(myIntent);
                    } else {
                        Intent myIntent = new Intent(AuthenticationActivity.this, ErrorActivity.class);
                        AuthenticationActivity.this.startActivity(myIntent);
                    }

                    finish();
                } else {
                    Intent myIntent = new Intent(AuthenticationActivity.this, ErrorActivity.class);
                    AuthenticationActivity.this.startActivity(myIntent);

                    finish();
                }
            } catch (Exception e){
              displayException(e.getMessage());
                e.printStackTrace();

                Intent myIntent = new Intent(AuthenticationActivity.this, ErrorActivity.class);
                AuthenticationActivity.this.startActivity(myIntent);
                finish();

            }


        }
    }

    public void displayException(String message)
    {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }

    private void sendAreq(String acsUrl, String content){

        SendAreqAsyncTask sendAreqAsyncTask = new SendAreqAsyncTask();
        sendAreqAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, acsUrl, content);

    }

    public static String jwsValidateSignatureAndReturnBody(String jws) {
        JWSObject jwsObject;
        try {
            jwsObject = JWSObject.parse(jws);
        } catch (ParseException e) {
            throw new RuntimeException("JWS parsing failed");
        }

        try {
            JWSVerifier verifier = null;
            JWSAlgorithm alg = jwsObject.getHeader().getAlgorithm();

            if (alg.equals(JWSAlgorithm.PS256) || alg.equals(JWSAlgorithm.RS256)) {
                List<com.nimbusds.jose.util.Base64> x509CertChain = jwsObject.getHeader().getX509CertChain();
//                List x509CertChain = jwsObject.getHeader().getX509CertChain();

                verifier = new RSASSAVerifier((RSAPublicKey) X509CertUtils.parse(x509CertChain.get(x509CertChain.size()-1).decode()).getPublicKey());

//                // Convert chain to a List
////                List certList = Arrays.asList(cert);
//
//                // Instantiate a CertificateFactory for X.509
//                CertificateFactory cf = CertificateFactory.getInstance("X.509");
//
//                // Extract the certification path from
//                // the List of Certificates
//                CertPath cp = cf.generateCertPath(x509CertChain);
//
//                // Create CertPathValidator that implements the "PKIX" algorithm
//                CertPathValidator cpv = CertPathValidator.getInstance("PKIX");
//
//                // Set the PKIX parameters to trust everything in the keystore.
//                PKIXParameters params = new PKIXParameters(aStore);
//                params.setRevocationEnabled(false);
//
//                cpv.validate()

            } else if (alg.equals(JWSAlgorithm.ES256)) {

                verifier = new ECDSAVerifier(ECKey.parse(jwsObject.getHeader().getJWK().toJSONString()));

            } else {
                // unsupported algorithm
                throw new RuntimeException();
            }

//          if (!jwsObject.verify(verifier)) {
            //           throw new RuntimeException("JWS validation failed.");
            //        }
        } catch (Exception e) {
            throw new RuntimeException("JWS validation failed.");
        }
        return jwsObject.getPayload().toString();
    }
}
