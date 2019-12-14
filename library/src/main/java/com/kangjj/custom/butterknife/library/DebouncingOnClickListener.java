package com.kangjj.custom.butterknife.library;

import android.view.View;

/**
 * @Description:
 * @Author: jj.kang
 * @Email: 345498912@qq.com
 * @ProjectName: 3.6.3_Custom_ButterKnife
 * @Package: com.kangjj.custom.butterknife.library
 * @CreateDate: 2019/12/14 11:25
 */
public abstract class DebouncingOnClickListener implements View.OnClickListener {
    @Override
    public void onClick(View v) {
        doClick(v);
    }

    protected abstract void doClick(View v);
}
