package com.daiwenzh5.mapper.util;

import lombok.experimental.UtilityClass;


/**
 * @author daiwenzh5
 * @date 2020-07-17 21:45
 */
@UtilityClass
public class Strings {


    @org.jetbrains.annotations.Contract(value = "null -> true", pure = true)
    public boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public String getOrDefault(String string, String notEmptyString) {
        return isNotEmpty(string) ? string : notEmptyString;
    }
//    public static boolean isEmpty(String str) {
//        return str == null || str.length() == 0;
//    }


//    public static String get(String string, String notEmptyString) {
//        return StringUtil.isNotEmpty(string)? string: notEmptyString;
//    }
}
