package com.tencent.qcloud.xiaozhibo.logic;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.tencent.qcloud.xiaozhibo.base.TCConstants;
import com.tencent.qcloud.xiaozhibo.base.TCHttpEngine;
import com.tencent.cos.model.COSRequest;
import com.tencent.cos.model.COSResult;
import com.tencent.cos.model.PutObjectRequest;
import com.tencent.cos.model.PutObjectResult;
import com.tencent.cos.task.listener.IUploadTaskListener;
import com.tencent.cos.utils.FileUtils;
import com.tencent.cos.COSClient;
import com.tencent.cos.COSClientConfig;
import com.tencent.cos.common.COSEndPoint;
import org.json.JSONObject;

import java.io.File;

/**
 * Cos人图片上传类
 */
public class TCUploadHelper {
    private static final String TAG = "TCUploadHelper";

    private final static int MAIN_CALL_BACK = 1;
    private final static int MAIN_PROCESS = 2;
    private final static int UPLOAD_AGAIN = 3;

    private Context mContext;
    private OnUploadListener mListerner;
    private Handler mMainHandler;

    public TCUploadHelper(final Context context, OnUploadListener listener) {
        mContext = context;
        mListerner = listener;

        mMainHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MAIN_CALL_BACK:
                        if (mListerner != null) {
                            mListerner.onUploadResult(msg.arg1, (String) msg.obj);
                        }
                        break;
                    case UPLOAD_AGAIN:
                        Bundle taskBundle = (Bundle) msg.obj;
                        doUploadCover(taskBundle.getString("path",""),taskBundle.getString("sig",""),false);
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private String createNetUrl() {
        return "/" + TCUserInfoMgr.getInstance().getUserId() + "/" + System.currentTimeMillis();
    }

    private void doUploadCover(final String path, String sig, boolean uploadAgain) {
        Log.d(TAG,"uploadCover do upload path:"+path);
        String netUrl = createNetUrl();

        PutObjectRequest putObjectRequest = new PutObjectRequest();
        putObjectRequest.setBucket(TCConstants.COS_BUCKET);
        putObjectRequest.setCosPath(netUrl);
        putObjectRequest.setSign(sig);
        putObjectRequest.setBiz_attr(null);
        putObjectRequest.setSrcPath(path);
        putObjectRequest.setListener(new com.tencent.cos.task.listener.IUploadTaskListener() {
            @Override
            public void onProgress(COSRequest cosRequest, long l, long l1) {

            }

            @Override
            public void onCancel(COSRequest cosRequest, COSResult cosResult) {

            }

            @Override
            public void onSuccess(COSRequest cosRequest, COSResult cosResult) {
                PutObjectResult result = (PutObjectResult) cosResult;
                Log.d(TAG,"uploadCover do upload sucess, url:"+result.access_url);
                Message msg = new Message();
                msg.what = MAIN_CALL_BACK;
                msg.arg1 = 0;
                msg.obj = result.access_url;

                mMainHandler.sendMessage(msg);
            }

            @Override
            public void onFailed(COSRequest cosRequest, COSResult cosResult) {
                Log.w(TAG, "uploadCover do upload fail, error code:" + cosResult.code + ",msg:" + cosResult.msg);
                Message msg = new Message();
                msg.what = MAIN_CALL_BACK;
                msg.arg1 = cosResult.code;
                msg.obj = cosResult.msg;

                mMainHandler.sendMessage(msg);
            }
        });

        COSClientConfig config = new COSClientConfig();
        COSEndPoint cosEndPoint = TCConstants.COS_REGION;
        config.setEndPoint(cosEndPoint);
        COSClient cos = new COSClient(mContext, TCConstants.COS_APPID, config, "qcloudphoto");
        PutObjectResult result = cos.putObject(putObjectRequest);
        if (result.code != 0) {
            File file = new File(path);
            Log.w(TAG, "uploadCover start fail, file exists:" + file.exists() + ", length:" + file.length());
            if (uploadAgain) {
                Log.w(TAG, "uploadCover start fail, try again after 1000ms");
                Bundle taskBundle = new Bundle();
                taskBundle.putString("path", path);
                taskBundle.putString("sig", sig);
                Message msg = new Message();
                msg.what = UPLOAD_AGAIN;
                msg.obj = taskBundle;

                mMainHandler.sendMessageDelayed(msg, 1000);
            } else {
                Log.w(TAG, "uploadCover start fail");
                if (mListerner != null) {
                    mListerner.onUploadResult(-1, null);
                }
            }

        }
    }

    public void uploadCover(final String path) {
        JSONObject req = new JSONObject();
        try {
            req.put("Action","GetCOSSign");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG,"uploadCover get cos sig");
        TCHttpEngine.getInstance().post(req, new TCHttpEngine.Listener() {
            @Override
            public void onResponse(int retCode, String retMsg, JSONObject retData) {
                if (retCode == 0 && retData != null) {
                    try {
                        String sig = retData.getString("sign");
                        Log.d(TAG, "uploadCover got cos sig succeed");
                        doUploadCover(path, sig, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (mListerner != null) {
                        mListerner.onUploadResult(-1,null);
                    }
                    Log.d(TAG, "uploadCover got cos sig fail, error code:"+retCode);
                }

            }
        });
    }

    public interface OnUploadListener {
        public void onUploadResult(int code, String url);
    }
}
