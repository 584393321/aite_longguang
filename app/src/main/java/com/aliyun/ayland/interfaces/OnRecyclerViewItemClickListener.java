package com.aliyun.ayland.interfaces;

import android.view.View;

public  interface OnRecyclerViewItemClickListener<T> {
    void onItemClick(View view, T t, int position);
}