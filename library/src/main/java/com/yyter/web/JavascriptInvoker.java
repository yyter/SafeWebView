package com.yyter.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类似于 {@link android.webkit.JavascriptInterface JavascriptInterface},
 * 所有提供给js的方法都必需加上此annotation用于防止js反射.<br/>
 *
 * <font color=red>注意: </font>所有加上此annotation的方法需要具备:
 * <ul>
 *     <li>是public的</li>
 *     <li>要么是无参的,要么所有参数都是{@link java.lang.String String}</li>
 * </ul>
 * Created by liyang on 5/12/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JavascriptInvoker {
}
