package cn.edu.zafu.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import cn.edu.zafu.camera.activity.CameraActivity;
import cn.edu.zafu.coreprogress.helper.ProgressHelper;
import cn.edu.zafu.coreprogress.listener.impl.UIProgressRequestListener;


/**
 * User:lizhangqu(513163535@qq.com)
 * Date:2015-09-05
 * Time: 13:49
 */
public class MainActivity extends Activity {
    OkHttpClient mOkHttpClient=new OkHttpClient();
    private Button mCatch, mRecognize;
    private EditText mPath,mName,mType;
    private ImageView mPhoto;
    private TextView mTextView;
    private ProgressBar mProgressBar;
    private String mPhotoPath;
    private static final int UPDATE_TEXTVIEW=1;
    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case UPDATE_TEXTVIEW:
                    mTextView.setText(msg.obj.toString());
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initClient();
        initView();
        initClickListener();
    }

    private void initClickListener() {
        mCatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                String pathStr = mPath.getText().toString();
                String nameStr = mName.getText().toString();
                String typeStr = mType.getText().toString();
                if (!TextUtils.isEmpty(pathStr)) {
                    intent.putExtra("path", pathStr);
                }
                if (!TextUtils.isEmpty(nameStr)) {
                    intent.putExtra("name", nameStr);
                }
                if (!TextUtils.isEmpty(typeStr)) {
                    intent.putExtra("type", typeStr);
                }
                startActivityForResult(intent, 100);
            }
        });

        mRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadAndRecognize();
            }
        });
    }

    private void uploadAndRecognize() {
        if (!TextUtils.isEmpty(mPhotoPath)){
            File file=new File(mPhotoPath);
            //构造上传请求，类似web表单
            RequestBody requestBody = new MultipartBuilder().type(MultipartBuilder.FORM)
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"callbackurl\""), RequestBody.create(null, "/idcard/"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"action\""), RequestBody.create(null, "idcard"))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"img\"; filename=\"idcardFront_user.jpg\""), RequestBody.create(MediaType.parse("image/jpeg"), file))
                    .build();
            //这个是ui线程回调，可直接操作UI
            final UIProgressRequestListener uiProgressRequestListener = new UIProgressRequestListener() {
                @Override
                public void onUIRequestProgress(long bytesWrite, long contentLength, boolean done) {
                    Log.e("TAG", "bytesWrite:" + bytesWrite);
                    Log.e("TAG", "contentLength" + contentLength);
                    Log.e("TAG", (100 * bytesWrite) / contentLength + " % done ");
                    Log.e("TAG", "done:" + done);
                    Log.e("TAG", "================================");
                    //ui层回调
                    mProgressBar.setProgress((int) ((100 * bytesWrite) / contentLength));
                }
            };
            //进行包装，使其支持进度回调
            final Request request = new Request.Builder()
                    .header("Host", "ocr.ccyunmai.com")
                    .header("Origin", "http://ocr.ccyunmai.com")
                    .header("Referer", "http://ocr.ccyunmai.com/idcard/")
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2398.0 Safari/537.36")
                    .url("http://ocr.ccyunmai.com/UploadImg.action")
                    .post(ProgressHelper.addProgressRequestListener(requestBody, uiProgressRequestListener))
                    .build();
            //开始请求
            mOkHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    Log.e("TAG", "error");
                }
                @Override
                public void onResponse(Response response) throws IOException {
                    String result=response.body().string();
                    Document parse = Jsoup.parse(result);
                    Elements select = parse.select("div.left fieldset");
                    Log.e("TAG",select.text());
                    Document parse1 = Jsoup.parse(select.text());
                    StringBuilder builder=new StringBuilder();
                    String name=parse1.select("name").text();
                    String cardno=parse1.select("cardno").text();
                    String sex=parse1.select("sex").text();
                    String folk=parse1.select("folk").text();
                    String birthday=parse1.select("birthday").text();
                    String address=parse1.select("address").text();
                    String issue_authority=parse1.select("issue_authority").text();
                    String valid_period=parse1.select("valid_period").text();
                    builder.append("name:"+name)
                            .append("\n")
                            .append("cardno:" + cardno)
                            .append("\n")
                            .append("sex:" + sex)
                            .append("\n")
                            .append("folk:" + folk)
                            .append("\n")
                            .append("birthday:" + birthday)
                            .append("\n")
                            .append("address:" + address)
                            .append("\n")
                            .append("issue_authority:" + issue_authority)
                            .append("\n")
                            .append("valid_period:" + valid_period)
                            .append("\n");
                    Log.e("TAG", "name:" + name);
                    Log.e("TAG","cardno:"+cardno);
                    Log.e("TAG","sex:"+sex);
                    Log.e("TAG","folk:"+folk);
                    Log.e("TAG","birthday:"+birthday);
                    Log.e("TAG","address:"+address);
                    Log.e("TAG","issue_authority:"+issue_authority);
                    Log.e("TAG","valid_period:"+valid_period);
                    Message obtain = Message.obtain();
                    obtain.what=UPDATE_TEXTVIEW;
                    obtain.obj=builder.toString();
                    mHandler.sendMessage(obtain);
                }
            });
        }
    }

    private void initView() {
        mPath= (EditText) findViewById(R.id.path);
        mName= (EditText) findViewById(R.id.name);
        mType= (EditText) findViewById(R.id.type);
        mCatch = (Button) findViewById(R.id.btn);
        mRecognize = (Button) findViewById(R.id.recognize);
        mTextView = (TextView) findViewById(R.id.tv);
        mPhoto = (ImageView) findViewById(R.id.photo);
        mProgressBar= (ProgressBar) findViewById(R.id.upload_progress);
        //File file=getFilesDir();
        File externalFile=getExternalFilesDir("/idcard/");
        mPath.setText(externalFile.getAbsolutePath());
        Log.e("TAG", externalFile.getAbsolutePath() + "\n" + externalFile.getAbsolutePath());
    }

    private void initClient() {
        mOkHttpClient.setConnectTimeout(1000, TimeUnit.MINUTES);
        mOkHttpClient.setReadTimeout(1000, TimeUnit.MINUTES);
        mOkHttpClient.setWriteTimeout(1000, TimeUnit.MINUTES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("TAG","onActivityResult");
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                String path=extras.getString("path");
                String type=extras.getString("type");
                Toast.makeText(getApplicationContext(),"path:"+ path + " type:" + type, Toast.LENGTH_LONG).show();
                mPhotoPath =path;
                File file = new File(path);
                FileInputStream inStream = null;
                try {
                    inStream = new FileInputStream(file);
                    Bitmap bitmap = BitmapFactory.decodeStream(inStream);
                    mPhoto.setImageBitmap(bitmap);
                    inStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
