package com.kangjj.custom.butterknife.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description:
 * @Author: jj.kang
 * @Email: 345498912@qq.com
 * @ProjectName: 3.6.3_Custom_ButterKnife
 * @Package: com.kangjj.custom.butterknife.annotation
 * @CreateDate: 2019/12/13 22:38
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface OnClick {
    int value();
}
