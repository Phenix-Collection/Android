package com.gostreamyourself.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import butterknife.BindView;
import butterknife.ButterKnife;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    @BindView(R.id.login_loginButton) Button loginBtn;
    @BindView(R.id.login_user) EditText userTxt;
    @BindView(R.id.login_errorTxt) TextView errorTxt;
    @BindView(R.id.login_progress) ProgressBar progressBar;

    private String token;
    private PrivateKey privateKey;
    private String url = "http://certifcation.herokuapp.com/login";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                //chooseFile.setType("*/*");
                //chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                //startActivityForResult(chooseFile, PICKFILE_RESULT_CODE);
                selectPrivateKey();
            }
        });
    }

    public static PrivateKey loadPrivateKey(String stored) throws GeneralSecurityException
    {
        byte [] pkcs8EncodedBytes = android.util.Base64.decode(stored, android.util.Base64.DEFAULT);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privKey = kf.generatePrivate(keySpec);

        return privKey;
    }

    private void selectPrivateKey() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

        FilePickerDialog dialog = new FilePickerDialog(LoginActivity.this,properties);
        dialog.setTitle("Selecteer private key");

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {

                loginBtn.setVisibility(View.INVISIBLE);
                userTxt.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);

                for(String path : files){
                    File file = new File(path);

                    loadPrivateKey(file);
                }
            }
        });

        dialog.show();
    }

    private void loadPrivateKey(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int) file.length()];

            dis.readFully(keyBytes);
            dis.close();
            String temp = new String(keyBytes);

            Log.i("WATAS", "onSelectedFilePaths: " + temp);

            String privKeyPEM = temp.replace("-----BEGIN PRIVATE KEY-----", "");
            privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "");

            privateKey = loadPrivateKey(privKeyPEM);

            Log.i("test", "onSelectedFilePaths: " + privateKey.toString());

            String username = userTxt.getText().toString();

            getFirstToken(username);

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void getFirstToken(final String username) {
        RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);
        CustomLoginRequest stringRequest = new CustomLoginRequest(Request.Method.GET, url,
                new Response.Listener<CustomLoginRequest.ResponseM>() {
                    @Override
                    public void onResponse(CustomLoginRequest.ResponseM result) {
                        token = result.headers.get("Token");
                        login(token, username);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(LoginActivity.this,error.toString(),Toast.LENGTH_LONG ).show();
                        disableLoadScreen();
                    }
                }) {
        };

        queue.add(stringRequest);
    }

    private void login(String token, final String username) {
        try {

//            token = "8f5c8dbe-a237-4405-bb0a-848e9cfb8ad8";
            final String hexString = Sign(token);

            final String finalToken = token;
            CustomLoginRequest postRequest = new CustomLoginRequest(Request.Method.GET, url,
                    new Response.Listener<CustomLoginRequest.ResponseM>() {
                        @Override
                        public void onResponse(CustomLoginRequest.ResponseM response) {
                            // response
                            Log.d("Response", response.headers.get("Token"));
                            errorTxt.setVisibility(View.INVISIBLE);
                            getStreamId(finalToken, username);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub
                            Log.d("MANIZZLE", "error => " + error.toString());
                            disableLoadScreen();
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();

                    //Log.i("EncryptedToken", "getHeaders: " + encryptedToken.toString());
                    params.put("token", finalToken);
                    params.put("signature", hexString);
                    params.put("name", username);

                    return params;
                }
            };

            RequestQueue queue1 = Volley.newRequestQueue(LoginActivity.this);
            queue1.add(postRequest);

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void getStreamId(final String token, final String username) {
        try {

            final String hexString = Sign(token);

            CustomLoginRequest postRequest = new CustomLoginRequest(Request.Method.GET, "http://certifcation.herokuapp.com/streams/ByName/Gerben%20Droogers",
                    new Response.Listener<CustomLoginRequest.ResponseM>() {
                        @Override
                        public void onResponse(CustomLoginRequest.ResponseM response) {
                            // response
                            Log.d("Response", response.headers.get("Token"));
                            Log.e(TAG, "onResponse: " + response );


                            errorTxt.setVisibility(View.INVISIBLE);
                            goBackToMainIntent();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub
                            Log.d("MANIZZLE", "error => " + error.toString());
                            disableLoadScreen();
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<String, String>();

                    //Log.i("EncryptedToken", "getHeaders: " + encryptedToken.toString());
                    params.put("token", token);
                    params.put("signature", hexString);
                    params.put("name", username);

                    return params;
                }
            };

            RequestQueue queue1 = Volley.newRequestQueue(LoginActivity.this);
            queue1.add(postRequest);

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void goBackToMainIntent() {
        Intent gotoMain = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(gotoMain);
        finish();
    }

    private String Sign (String token) throws SignatureException, NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        //From here you will get headers
        Log.i("Token", "onResponse: " + token);
        String json = "{\"token\":\"" + token + "\"}";
//        String json = "{}";
        Log.i(TAG, "Sign: TOKEN TO SIGN: "  + token);
        byte[] data = json.getBytes();
        Log.e(TAG, "Sign: " + json );

        Signature instance = Signature.getInstance("SHA256withRSA");
        instance.initSign(privateKey);
        instance.update(data);
        byte[] signatureBytes = instance.sign();

        Log.i("NDOE", "onResponse: " + signatureBytes.toString());

        instance.update(data);

        StringBuilder sb = new StringBuilder();
        for (int i=0; i<signatureBytes.length; i++) {
            sb.append(String.format("%02X ",signatureBytes[i]));
        }

        final String hexStringSpaces = sb.toString();

        final String hexStringUpper = hexStringSpaces.replace(" ", "");

        final String hexString = hexStringUpper.toLowerCase();

        Log.i("After hex", "onResponse: " + hexStringSpaces);
        Log.i("Without spaces", "onResponse: " + hexString);

        return hexString;
    }

    private void disableLoadScreen() {
        progressBar.setVisibility(View.INVISIBLE);
        errorTxt.setVisibility(View.VISIBLE);
        loginBtn.setVisibility(View.VISIBLE);
        userTxt.setVisibility(View.VISIBLE);
    }



}
