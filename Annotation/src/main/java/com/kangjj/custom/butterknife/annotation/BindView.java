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
 * @CreateDate: 2019/12/13 22:36
 */

// SOURCE 注解仅在源码中保留,class文件中不存在
// CLASS 注解在源码和class文件中都存在,但运行时不存在
// RUNTIME 注解在源码,class文件中存在且运行时可以通过反射机制获取到
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface BindView {
    int value();
}
