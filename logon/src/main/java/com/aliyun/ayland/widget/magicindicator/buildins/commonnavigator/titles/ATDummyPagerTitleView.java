package com.aliyun.ayland.widget.magicindicator.buildins.commonnavigator.titles;

import android.content.Context;
import android.view.View;

import com.aliyun.ayland.widget.magicindicator.buildins.commonnavigator.abs.ATIPagerTitleView;

/**
 * 空指示器标题，用于只需要指示器而不需要title的需求
 * 博客: http://hackware.lucode.net
 * Created by hackware on 2016/6/26.
 */
public class ATDummyPagerTitleView extends View implements ATIPagerTitleView {

    public ATDummyPagerTitleView(Context context) {
        super(context);
    }

    @Override
    public void onSelected(int index, int totalCount) {
    }

    @Override
    public void onDeselected(int index, int totalCount) {
    }

    @Override
    public void onLeave(int index, int totalCount, float leavePercent, boolean leftToRight) {
    }

    @Override
    public void onEnter(int index, int totalCount, float enterPercent, boolean leftToRight) {
    }
}
