package com.aliyun.ayland.base.autolayout.attr;

import android.view.View;

import com.aliyun.ayland.base.autolayout.util.AutoUtils;
import com.aliyun.ayland.utils.L;

/**
 * Created by zhy on 15/12/4.
 */
public abstract class AutoAttr {
    protected int pxVal;
    protected int baseWidth;
    protected int baseHeight;

    public AutoAttr(int pxVal, int baseWidth, int baseHeight) {
        this.pxVal = pxVal;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
    }

    public void apply(View view) {
        boolean log = view.getTag() != null && view.getTag().toString().equals("auto");
        if (log) {
            L.e(" pxVal = " + pxVal + " ," + this.getClass().getSimpleName());
        }
        int val;
        if (useDefault()) {
            val = defaultBaseWidth() ? getPercentWidthSize() : getPercentHeightSize();
            if (log) {
                L.e(" useDefault val= " + val);
            }
        } else if (baseWidth()) {
            val = getPercentWidthSize();
            if (log) {
                L.e(" baseWidth val= " + val);
            }
        } else {
            val = getPercentHeightSize();
            if (log) {
                L.e(" baseHeight val= " + val);
            }
        }
        execute(view, val);
    }

    protected int getPercentWidthSize() {
        return AutoUtils.getPercentWidthSize(pxVal);
    }

    protected int getPercentHeightSize() {
        return AutoUtils.getPercentHeightSize(pxVal);
    }

    protected boolean baseWidth() {
        return contains(baseWidth, attrVal());
    }

    protected boolean useDefault() {
        return !contains(baseHeight, attrVal()) && !contains(baseWidth, attrVal());
    }

    protected boolean contains(int baseVal, int flag) {
        return (baseVal & flag) != 0;
    }

    protected abstract int attrVal();

    protected abstract boolean defaultBaseWidth();

    protected abstract void execute(View view, int val);

    @Override
    public String toString() {
        return "ATAutoAttr{" +
                "pxVal=" + pxVal +
                ", baseWidth=" + baseWidth() +
                ", defaultBaseWidth=" + defaultBaseWidth() +
                '}';
    }
}
