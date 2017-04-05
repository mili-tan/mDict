package cc.milione.me.mdict;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    EditText editTextWord;
    TextView tViewWord;
    TextView tViewEp;
    TextView tViewPos1;
    TextView tViewPos2;
    TextView tViewMn1;
    TextView tViewMn2;
    String bingDictPath = "http://xtk.azurewebsites.net/BingService.aspx";
    String yoodaoDictPath = "http://fanyi.youdao.com/openapi.do";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (!isOnline(this) || !inNetwork()) {
            new AlertDialog.Builder(this)
                    .setTitle("很抱歉")
                    .setMessage("无法使用mDict，请连接至互联网。")
                    .setNegativeButton("网络设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .show()
                    .setCancelable(false);
        }

        editTextWord = (EditText) findViewById(R.id.editTextWord);
        tViewWord = (TextView) findViewById(R.id.textViewWord);
        tViewEp = (TextView) findViewById(R.id.textViewEp);
        tViewPos1 = (TextView) findViewById(R.id.textViewPos1);
        tViewPos2 = (TextView) findViewById(R.id.textViewPos2);
        tViewMn1 = (TextView) findViewById(R.id.textVieMn1);
        tViewMn2 = (TextView) findViewById(R.id.textVieMn2);
    }

    public void searchOnClick(View view) {
        if (!isOnline(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("很抱歉")
                    .setMessage("无法使用mDict，请连接至互联网。")
                    .setNegativeButton("网络设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .show()
                    .setCancelable(false);
        } else {
            if (isChinese(editTextWord.getText().toString())) {
                String wordVal = "keyfrom=mdict-milione&key=900659837&type=data&doctype=json&version=1.1&q=" + editTextWord.getText().toString();
                String wordExplain = postWeb(yoodaoDictPath, wordVal);
                String basic = praseJson(wordExplain, "basic");
                if (wordExplain == null || wordExplain.equals("") || !praseJson(wordExplain, "errorCode").equals("0")) {
                    Toast.makeText(this, "mDict查询失败，请检查", Toast.LENGTH_SHORT).show();
                } else {
                    tViewEp.setText(replaceJson(praseJson(basic, "phonetic")));
                    tViewWord.setText(praseJson(wordExplain, "query"));
                    tViewPos1.setText("基本");
                    tViewMn1.setText(replaceJson(praseJson(wordExplain, "translation")));
                    if (!praseJson(basic, "explains").equals(" ")) {
                        tViewPos2.setText("其他");
                        tViewMn2.setText(replaceJson(replaceChn(praseJson(basic, "explains"))).trim());
                    }
                    else{
                        tViewPos2.setText(" ");
                        tViewMn2.setText(" ");
                    }
                }
            } else {
                String wordVal = "Action=search&Format=jsonwv&Word=" + editTextWord.getText().toString();
                String wordExplain = postWeb(bingDictPath, wordVal);

                if (wordExplain == null || wordExplain.equals("")) {
                    wordVal = "keyfrom=mdict-milione&key=900659837&type=data&doctype=json&version=1.1&q=" + editTextWord.getText().toString();
                    wordExplain = postWeb(yoodaoDictPath, wordVal);
                    String basic = praseJson(wordExplain, "basic");
                    tViewEp.setText(replaceJson(praseJson(basic, "phonetic")));
                    tViewWord.setText(praseJson(wordExplain, "query"));
                    tViewPos1.setText("其他");
                    tViewMn1.setText(replaceJson(praseJson(wordExplain, "translation")));
                } else {
                    String mn1 = praseJson(wordExplain, "mn1");
                    if (mn1.equals("") || mn1.equals(" ") || mn1.equals(", ") || mn1.equals("; ")) {
                        Toast.makeText(this, "mDict未查询到相关内容，请检查", Toast.LENGTH_SHORT).show();
                    } else {
                        tViewWord.setText(praseJson(wordExplain, "word"));
                        tViewEp.setText(praseJson(wordExplain, "brep"));
                        tViewPos1.setText(praseJson(wordExplain, "pos1"));
                        tViewPos2.setText(praseJson(wordExplain, "pos2"));
                        tViewMn1.setText(praseJson(wordExplain, "mn1"));
                        tViewMn2.setText(praseJson(wordExplain, "mn2"));
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.about:
                Toast.makeText(this, "一个简单的词典软件", Toast.LENGTH_SHORT).show();
                break;
            case R.id.exit:
                finish();
                break;
            default:
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            try {
                finish();
                onStop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public String praseJson(String JsonStr, String JsonData) {
        String getData = " ";
        try {
            JSONArray jsonArray = new JSONArray("["+JsonStr+"]");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject JSONObject = jsonArray.getJSONObject(i);
                getData = JSONObject.getString(JsonData);
                Log.d("mDict","prase:" + getData + "val:" + JsonData);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
            getData = " ";
        }
        finally {
            return getData;
        }
    }

    public String postWeb(String webPath, final String webVal){
        String webStr = "";
        try {
            webStr = new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... params) {
                    String line = null;

                    try{
                        URL url = new URL(params[0]);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setRequestMethod("POST");

                        OutputStreamWriter outputStreamWriter =new OutputStreamWriter(connection.getOutputStream(),"utf-8");
                        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                        bufferedWriter.write(webVal);
                        bufferedWriter.flush();

                        InputStream inputStream = connection.getInputStream();
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"utf-8");
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        String linestr;
                        while ((linestr = bufferedReader.readLine()) != null){
                            Log.d("mDict","Json:" + linestr);
                            line = linestr;
                        }
                        bufferedReader.close();
                        inputStreamReader.close();
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return line;
                }
            }.execute(webPath).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setTitle("很抱歉")
                    .setMessage("出现了一些意外的网络故障，请重试。")
                    .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .show()
                    .setCancelable(false);
        }
        return webStr;
    }

    public String replaceJson(String replaceStr){
        return replaceStr.replace("[","").replace("]","").replace("\"","").replace(","," ; ");
    }

    public String replaceChn(String replaceStr) {
        String regEx = "[\\u4e00-\\u9fa5]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(replaceStr);
        return m.replaceAll("").replace("  "," ").trim();
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected())
            {
                if (info.getState() == NetworkInfo.State.CONNECTED)
                {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean inNetwork() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("ping -c 1 fanyi.youdao.com");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean isChinese(String string){
        int n = 0;
        for(int i = 0; i < string.length(); i++) {
            n = (int)string.charAt(i);
            if(!(19968 <= n && n <40869)) {
                return false;
            }
        }
        return true;
    }
}
