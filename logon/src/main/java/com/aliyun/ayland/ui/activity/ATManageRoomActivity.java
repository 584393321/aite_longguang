package com.aliyun.ayland.ui.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.ayland.base.ATBaseActivity;
import com.aliyun.ayland.base.autolayout.util.ATAutoUtils;
import com.aliyun.ayland.contract.ATMainContract;
import com.aliyun.ayland.data.ATDeviceBean;
import com.aliyun.ayland.data.ATEventIntent;
import com.aliyun.ayland.data.ATHouseBean;
import com.aliyun.ayland.data.ATRoomBean1;
import com.aliyun.ayland.global.ATGlobalApplication;
import com.aliyun.ayland.global.ATConstants;
import com.aliyun.ayland.interfaces.ATOnRVItemClickListener;
import com.aliyun.ayland.presenter.ATMainPresenter;
import com.aliyun.ayland.ui.adapter.ATManageRoomRVAdapter;
import com.aliyun.ayland.ui.adapter.ATManageRoomSMRVAdapter;
import com.aliyun.ayland.widget.ATRecycleViewItemDecoration;
import com.aliyun.ayland.widget.titlebar.ATMyTitleBar;
import com.anthouse.xuhui.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.yanzhenjie.recyclerview.swipe.SwipeMenu;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;
import com.yanzhenjie.recyclerview.swipe.touch.OnItemMoveListener;
import com.yanzhenjie.recyclerview.swipe.touch.OnItemStateChangedListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ATManageRoomActivity extends ATBaseActivity implements ATMainContract.View {
    private ATMainPresenter mPresenter;
    private ATManageRoomRVAdapter mATManageRoomRVAdapter;
    private ATManageRoomSMRVAdapter mATManageRoomSMRVAdapter;
    private boolean mEditable = false;
    private List<ATRoomBean1> mRoomNameList = new ArrayList<>();
    private List<ATRoomBean1> mTempRoomList = new ArrayList<>();
    private JSONArray deleteList = new JSONArray();
    private JSONObject mAllDevice;
    private int mCurrentPosition = 0;
    private ATHouseBean houseBean;
    private String mAllDeviceData;
    private boolean device, room;
    private ATHouseBean mATHouseBean;
    private ATMyTitleBar titlebar;
    private LinearLayout llAddRoom;
    private RecyclerView rvManageRoom;
    private SwipeMenuRecyclerView smrvManageRoom;
    private SmartRefreshLayout smartRefreshLayout;
    private Dialog dialog;
    private SwipeMenuBridge mMenuBridge;

    @Override
    protected int getLayoutId() {
        return R.layout.at_activity_manage_room;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ATEventIntent ATEventIntent) {
        if ("ATManageRoomActivity".equals(ATEventIntent.getClazz())) {
            mAllDeviceData = ATEventIntent.getData().getStringExtra("allDeviceData");
            mAllDevice = JSON.parseObject(mAllDeviceData);
            String room_name = ATEventIntent.getData().getStringExtra("room_name");
            if (!TextUtils.isEmpty(room_name)) {
                String iotSpaceId = ATEventIntent.getData().getStringExtra("iotSpaceId");
                String room_type = ATEventIntent.getData().getStringExtra("room_type");
                if (!TextUtils.isEmpty(iotSpaceId)) {
                    //????????????
                    ATRoomBean1 roomBean = new ATRoomBean1();
                    roomBean.setName(room_name);
                    roomBean.setType(room_type);
                    roomBean.setIotSpaceId(iotSpaceId);
                    roomBean.setOrderCode(mRoomNameList.size() + "");
                    mRoomNameList.add(roomBean);
                } else {
                    mRoomNameList.get(mCurrentPosition).setName(room_name);
                    mRoomNameList.get(mCurrentPosition).setType(room_type);
                }
                mTempRoomList.clear();
                mTempRoomList.addAll(mRoomNameList);
                mATManageRoomRVAdapter.setLists(mRoomNameList);
                mATManageRoomSMRVAdapter.setLists(mTempRoomList);
            }
        }
    }

    @Override
    protected void findView() {
        titlebar = findViewById(R.id.titlebar);
        llAddRoom = findViewById(R.id.ll_add_room);
        rvManageRoom = findViewById(R.id.rv_manage_room);
        smrvManageRoom = findViewById(R.id.smrv_manage_room);
        smartRefreshLayout = findViewById(R.id.smartRefreshLayout);
        EventBus.getDefault().register(this);
        init();
    }

    @Override
    protected void initPresenter() {
        mPresenter = new ATMainPresenter(this);
        mPresenter.install(this);

        mAllDeviceData = getIntent().getStringExtra("allDevice");
        if (TextUtils.isEmpty(mAllDeviceData)) {
            request();
        } else {
            mRoomNameList = (List<ATRoomBean1>) getIntent().getSerializableExtra("mRoomNameList");
            mAllDevice = JSON.parseObject(mAllDeviceData);
            mRoomNameList.remove(0);
            mTempRoomList.addAll(mRoomNameList);
            mATManageRoomRVAdapter.setLists(mRoomNameList);
            mATManageRoomSMRVAdapter.setLists(mTempRoomList);
        }
    }

    private void request() {
        mATHouseBean = gson.fromJson(ATGlobalApplication.getHouse(), ATHouseBean.class);
        if (mATHouseBean == null)
            return;
        showBaseProgressDlg();
        findOrderRoom();
        getHouseDevice();
    }

    private void findOrderRoom() {
        room = false;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("spaceId", mATHouseBean.getIotSpaceId());
        jsonObject.put("villageId", mATHouseBean.getVillageId());
        jsonObject.put("iotToken", ATGlobalApplication.getIoTToken());
        jsonObject.put("personCode", ATGlobalApplication.getATLoginBean().getPersonCode());
        mPresenter.request(ATConstants.Config.SERVER_URL_FINDORDERROOM, jsonObject);
    }

    private void getHouseDevice() {
        device = false;
        JSONObject operator = new JSONObject();
        operator.put("hid", ATGlobalApplication.getOpenId());
        operator.put("hidType", "OPEN");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operator", operator);
        jsonObject.put("iotSpaceId", mATHouseBean.getIotSpaceId());
        jsonObject.put("iotToken", ATGlobalApplication.getIoTToken());
        jsonObject.put("username", ATGlobalApplication.getAccount());
        mPresenter.request(ATConstants.Config.SERVER_URL_HOUSEDEVICE, jsonObject);
    }

    private void init() {
        titlebar.setSendText(getString(R.string.at_edit));
        titlebar.setPublishClickListener(() -> {
            if (mEditable) {
                editRoom();
                smartRefreshLayout.setEnableRefresh(true);
            } else {
//                llAddRoom.setVisibility(View.GONE);
                rvManageRoom.setVisibility(View.GONE);
                smrvManageRoom.setVisibility(View.VISIBLE);
                titlebar.setSendText(getString(R.string.at_done));
                smartRefreshLayout.setEnableRefresh(false);
            }
            mEditable = !mEditable;
        });
        llAddRoom.setOnClickListener(v ->
            startActivity(new Intent(ATManageRoomActivity.this, ATRoomPicActivity.class)
                    .putExtra("allDeviceData", mAllDeviceData)));

        titlebar.setTitleBarClickBackListener(() -> {
            if (mEditable) {
                mEditable = false;
//                llAddRoom.setVisibility(View.VISIBLE);
                rvManageRoom.setVisibility(View.VISIBLE);
                smrvManageRoom.setVisibility(View.GONE);
                titlebar.setSendText(getString(R.string.at_edit));
                smartRefreshLayout.setEnableRefresh(true);
                mATManageRoomRVAdapter.setLists(mRoomNameList);
                mATManageRoomSMRVAdapter.setLists(mRoomNameList);
                deleteList.clear();
                mTempRoomList.clear();
                mTempRoomList.addAll(mRoomNameList);
            } else {
                finish();
            }
        });
        rvManageRoom.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        mATManageRoomRVAdapter = new ATManageRoomRVAdapter(this);
        mATManageRoomRVAdapter.setOnRVClickListener((view, position) -> {
            if(mAllDevice == null){
                request();
                return;
            }
            mCurrentPosition = position;
            Intent intent = new Intent();
            if (mAllDevice.containsKey(mRoomNameList.get(position).getIotSpaceId())) {
                Gson gson = new Gson();
                List<ATDeviceBean> ATDeviceBeanList = gson.fromJson(mAllDevice.getString(mRoomNameList.get(position).getIotSpaceId()), new TypeToken<List<ATDeviceBean>>() {
                }.getType());
                intent.putExtra("deviceBeanList", (Serializable) ATDeviceBeanList);
            }
            intent.putExtra("allDeviceData", mAllDeviceData);
            intent.putExtra("roombean", mTempRoomList.get(position));
            intent.setClass(ATManageRoomActivity.this, ATEditRoomActivity.class);
            startActivity(intent);
        });
        mATManageRoomRVAdapter.setLists(mRoomNameList);
        rvManageRoom.setAdapter(mATManageRoomRVAdapter);
        rvManageRoom.addItemDecoration(new ATRecycleViewItemDecoration(ATAutoUtils.getPercentHeightSize(30)));
        rvManageRoom.setNestedScrollingEnabled(false);

        mATManageRoomSMRVAdapter = new ATManageRoomSMRVAdapter(smrvManageRoom, this);
        smrvManageRoom.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        smrvManageRoom.setOnItemMoveListener(onItemMoveListener);// Item?????????/???????????????????????????????????????????????????
        smrvManageRoom.setOnItemStateChangedListener(mOnItemStateChangedListener); // ??????Item?????????????????????
        smrvManageRoom.addItemDecoration(new ATRecycleViewItemDecoration(ATAutoUtils.getPercentHeightSize(30)));
        smrvManageRoom.setSwipeMenuCreator(mSwipeMenuCreator);
        smrvManageRoom.setSwipeMenuItemClickListener(mMenuItemClickListener); // Item???Menu?????????
        smrvManageRoom.setLongPressDragEnabled(false); // ???????????????
        smrvManageRoom.setItemViewSwipeEnabled(false); // ?????????????????????
        smrvManageRoom.setAdapter(mATManageRoomSMRVAdapter);
        smrvManageRoom.setNestedScrollingEnabled(false);

        smartRefreshLayout.setOnRefreshListener(refreshLayout -> {
            refreshLayout.finishRefresh(2000);
            request();
        });

        initDialog();

//        llAddRoom.setOnClickListener(view -> {
//            startActivity(new Intent(this, ATRoomPicActivity.class)
//                    .putExtra("allDeviceData", mAllDeviceData));
//        });
    }

    private void editRoom() {
        if (!TextUtils.isEmpty(ATGlobalApplication.getHouse())) {
            houseBean = gson.fromJson(ATGlobalApplication.getHouse(), ATHouseBean.class);
        }
        if (houseBean == null)
            return;
        showBaseProgressDlg();

        JSONObject operator = new JSONObject();
        operator.put("hid", ATGlobalApplication.getOpenId());
        operator.put("hidType", "OPEN");

        JSONArray orderList = new JSONArray();
        for (ATRoomBean1 roomBean : mTempRoomList) {
            orderList.add(roomBean.getIotSpaceId());
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("operator", operator);
        jsonObject.put("deleteList", deleteList);
        jsonObject.put("rootSpaceId", houseBean.getRootSpaceId());
        jsonObject.put("orderList", orderList);
        jsonObject.put("iotToken", ATGlobalApplication.getIoTToken());
        mPresenter.request(ATConstants.Config.SERVER_URL_DELETEROOM, jsonObject);
    }

    @SuppressLint("InflateParams")
    private void initDialog() {
        dialog = new Dialog(this, R.style.nameDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.at_dialog_normal, null, false);
        ((TextView) view.findViewById(R.id.tv_title)).setText(getString(R.string.at_sure_delete_room));
        view.findViewById(R.id.tv_cancel).setOnClickListener(view1 -> dialog.dismiss());
        view.findViewById(R.id.tv_sure).setOnClickListener(view2 -> {
            mMenuBridge.closeMenu();
            int adapterPosition = mMenuBridge.getAdapterPosition();
            deleteList.add(mTempRoomList.get(adapterPosition).getIotSpaceId());
            mTempRoomList.remove(adapterPosition);
            mATManageRoomSMRVAdapter.removeItem(adapterPosition);
            dialog.dismiss();
        });
        dialog.setContentView(view);
    }

    /**
     * ????????????????????????????????????UI???????????????
     */
    private OnItemMoveListener onItemMoveListener = new OnItemMoveListener() {
        @Override
        public boolean onItemMove(RecyclerView.ViewHolder srcHolder, RecyclerView.ViewHolder targetHolder) {
            // ?????????ViewType????????????????????????
            if (srcHolder.getItemViewType() != targetHolder.getItemViewType())
                return false;

            int fromPosition = srcHolder.getAdapterPosition();
            int toPosition = targetHolder.getAdapterPosition();

            Collections.swap(mTempRoomList, fromPosition, toPosition);
            mATManageRoomSMRVAdapter.notifyItemMoved(fromPosition, toPosition);
            return true;// ??????true??????????????????????????????????????????false??????????????????????????????????????????
        }

        @Override
        public void onItemDismiss(RecyclerView.ViewHolder srcHolder) {
            mTempRoomList.remove(srcHolder.getAdapterPosition());
//            mATManageRoomSMRVAdapter.notifyItemRemoved(srcHolder.getAdapterPosition());
        }
    };

    /**
     * Item?????????/???????????????????????????????????????????????????
     */
    private OnItemStateChangedListener mOnItemStateChangedListener = new OnItemStateChangedListener() {
        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState == OnItemStateChangedListener.ACTION_STATE_DRAG) {
//                ???????????????"???????????????");
                // ?????????????????????????????????????????????????????????????????????????????????
//                viewHolder.itemView.setBackgroundColor(ContextCompat.getColor(ATManageRoomActivity.this, R.color._CFCFCF));
            } else if (actionState == OnItemStateChangedListener.ACTION_STATE_SWIPE) {
//                ???????????????"?????????????????????");
            } else if (actionState == OnItemStateChangedListener.ACTION_STATE_IDLE) {
//                ???????????????"?????????????????????");
                // ????????????????????????????????????
//                ViewCompat.setBackground(viewHolder.itemView, ContextCompat.getDrawable(ATManageRoomActivity.this, R.drawable.selector_white));
            }
        }
    };

    /**
     * ??????????????????
     */
    public SwipeMenuCreator mSwipeMenuCreator = new SwipeMenuCreator() {
        @Override
        public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
            // 1. MATCH_PARENT ???????????????????????????Item?????????;
            // 2. ???????????????????????????80;
            // 3. WRAP_CONTENT???????????????????????????;
            int width = ATAutoUtils.getPercentWidthSize(171);
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            SwipeMenuItem closeItem = new SwipeMenuItem(ATManageRoomActivity.this)
                    .setImage(R.drawable.at_ioc_hdshanchu)
                    .setWidth(width)
                    .setHeight(height);
            swipeRightMenu.addMenuItem(closeItem); // ????????????????????????????????????
        }
    };

    /**
     * RecyclerView???Item???Menu???????????????
     */
    private SwipeMenuItemClickListener mMenuItemClickListener = new SwipeMenuItemClickListener() {
        @Override
        public void onItemClick(SwipeMenuBridge menuBridge) {
            int adapterPosition = menuBridge.getAdapterPosition(); // RecyclerView???Item???position???
            if (mAllDevice.containsKey(mTempRoomList.get(adapterPosition).getIotSpaceId())) {
                //?????????
                mMenuBridge = menuBridge;
                dialog.show();
            }else {
                menuBridge.closeMenu();
                deleteList.add(mTempRoomList.get(adapterPosition).getIotSpaceId());
                mTempRoomList.remove(adapterPosition);
                mATManageRoomSMRVAdapter.removeItem(adapterPosition);
            }
//            mATManageRoomRVAdapter.setLists(mTempRoomList);
//            mATManageRoomSMRVAdapter.notifyItemRemoved(adapterPosition);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mEditable) {
                mEditable = false;
                rvManageRoom.setVisibility(View.VISIBLE);
                smrvManageRoom.setVisibility(View.GONE);
                titlebar.setSendText(getString(R.string.at_edit));
                smartRefreshLayout.setEnableRefresh(true);
                mATManageRoomRVAdapter.setLists(mRoomNameList);
                deleteList.clear();
                mTempRoomList.clear();
                mTempRoomList.addAll(mRoomNameList);

                mATManageRoomSMRVAdapter.setLists(mTempRoomList);
            } else {
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void requestResult(String result, String url) {
        try {
            org.json.JSONObject jsonResult = new org.json.JSONObject(result);
            if ("200".equals(jsonResult.getString("code"))) {
                switch (url) {
                    case ATConstants.Config.SERVER_URL_DELETEROOM:
                        mRoomNameList.clear();
                        mRoomNameList.addAll(mTempRoomList);
                        deleteList.clear();
                        mATManageRoomRVAdapter.setLists(mRoomNameList);
                        rvManageRoom.setVisibility(View.VISIBLE);
                        smrvManageRoom.setVisibility(View.GONE);
                        titlebar.setSendText(getString(R.string.at_edit));
                        smartRefreshLayout.setEnableRefresh(true);
                        break;
                    case ATConstants.Config.SERVER_URL_HOUSEDEVICE:
                        mAllDeviceData = jsonResult.getString("data");
                        mAllDevice = JSON.parseObject(mAllDeviceData);
                        break;
                    case ATConstants.Config.SERVER_URL_FINDORDERROOM:
                        mRoomNameList = gson.fromJson(jsonResult.getString("data"), new TypeToken<List<ATRoomBean1>>() {
                        }.getType());
                        mTempRoomList.clear();
                        mTempRoomList.addAll(mRoomNameList);
                        mATManageRoomRVAdapter.setLists(mRoomNameList);
                        mATManageRoomSMRVAdapter.setLists(mTempRoomList);
                        break;
                }
            } else {
                showToast(jsonResult.getString("message"));
            }
            smartRefreshLayout.finishRefresh();
            closeBaseProgressDlg();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}