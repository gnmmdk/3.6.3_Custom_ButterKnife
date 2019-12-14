package com.kangjj.custom.butterknife.library;

import android.app.Activity;

/**
 * @Description:
 * @Author: jj.kang
 * @Email: 345498912@qq.com
 * @ProjectName: 3.6.3_Custom_ButterKnife
 * @Package: com.kangjj.custom.butterknife.library
 * @CreateDate: 2019/12/14 11:28
 */
public class ButterKnife {

    public static void bind(Activity target){
        String className = target.getClass().getName()+"$ViewBinder";
        try {
            Class<?> viewBindClazz = Class.forName(className);
            ViewBinder viewBinder = (ViewBinder) viewBindClazz.newInstance();
            viewBinder.bind(target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
