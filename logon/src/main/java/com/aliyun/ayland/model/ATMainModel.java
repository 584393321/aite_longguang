package com.aliyun.ayland.model;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.ayland.base.ATBaseModel;
import com.aliyun.ayland.data.ATJsonObjectBean;
import com.aliyun.ayland.global.ATConstants;
import com.aliyun.ayland.global.ATGlobalApplication;
import com.aliyun.ayland.presenter.ATMainPresenter;
import com.aliyun.ayland.utils.ATRxUtils;

import java.io.IOException;
import java.util.Vector;

import at.smarthome.AT_Aes;
import at.smarthome.HttpUtils2;

public class ATMainModel extends ATBaseModel<ATMainPresenter> {
    public static final String TAG = "ATMainModel";
    private Vector<ATJsonObjectBean> serverCommands = new Vector<>();

    public ATMainModel(ATMainPresenter mPresenter) {
        super(mPresenter);
    }

    @Override
    public void onMainEvent(int what, Object event) {
        switch (what) {
            case ATConstants.EventType.GET_TOKEN_SUCCESS:
                if (serverCommands.size() > 0)
                    synchronized (serverCommands) {
                        while (serverCommands.size() > 0) {
                            request(serverCommands.get(0).getUrl(), serverCommands.get(0).getJsonObject());
                            serverCommands.remove(0);
                        }
                    }
                break;
        }
    }

    @Override
    public void onThreadEvent(int what, Object event) {

    }

    @Override
    public void uninstall() {
        super.uninstall();
        synchronized (serverCommands) {
            serverCommands.clear();
        }
    }

    public void request(String url, JSONObject jsonObject) {
        ATRxUtils.singleTaskOnThread(() -> {
            try {
                Log.e(TAG, String.format(ATConstants.Config.SERVER_BASE_URL, url) + jsonObject.toString() + "---" + ATGlobalApplication.getAccessToken());
//                String result = AT_Aes.setEncodeByKey(jsonObject.toString(), ATConstants.Config.AESPWD);
                if (TextUtils.isEmpty(ATGlobalApplication.getAccessToken())) {
                    serverCommands.add(new ATJsonObjectBean(url, jsonObject));
                    getToken();
                    return;
                }
                jsonObject.put("customerCode", ATConstants.Config.CUSTOMER_CODE);
                JSONObject mJsonObject = new JSONObject();
                for (String s : jsonObject.keySet()) {
                    if (TextUtils.isEmpty(String.valueOf(jsonObject.get(s))))
                        mJsonObject.put(s, jsonObject.get(s));
                    else
                        mJsonObject.put(s, AT_Aes.setEncodeByKey(String.valueOf(jsonObject.get(s)), ATConstants.Config.AESPWD));
                }
                Log.e(TAG, String.format(ATConstants.Config.SERVER_BASE_URL, url) + mJsonObject.toString());
                String result = HttpUtils2.doHttpPostAccessToken(String.format(ATConstants.Config.SERVER_BASE_URL, url), mJsonObject.toString(),
                        ATGlobalApplication.getAccessToken());
                if ("401".equals(result)) {
                    serverCommands.add(new ATJsonObjectBean(url, jsonObject));
                    getToken();
                    mPresenter.requestResult("", url);
                } else if (result.length() > 0) {
                    result = AT_Aes.getDecodeByKey(result, ATConstants.Config.AESPWD);
                    Log.e(TAG, String.format(ATConstants.Config.SERVER_BASE_URL, url) + result);
                    if (result != null)
                        result = result.replace("\"data\":{}", "\"data\":[]");
                    mPresenter.requestResult(result, url);
                } else {
                    mPresenter.requestResult("", url);
                }
            } catch (IOException e) {
                e.printStackTrace();
                mPresenter.requestResult("", url);
            }
        });
    }
}