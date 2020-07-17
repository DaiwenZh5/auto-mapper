package com.daiwenzh5.mapper.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

/**
 * @author daiwenzh5
 * @date 2020-07-16 12:52
 */
@Slf4j
public class Reflections {

    public static Field[] getAllFields(Class<?> type) {
        List<Field> fieldList = new ArrayList<>();
        Set<String> fieldNames = new HashSet<>();
        while (type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            Arrays.stream(fields).filter(field -> !fieldNames.contains(field.getName()))
                    .collect(Collectors.toList())
                    .forEach(field -> {
                        fieldNames.add(field.getName());
                        fieldList.add(field);
                    });
            type = type.getSuperclass();
        }
        return fieldList.toArray(new Field[0]);
    }

    public static Class<?> getFieldType(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            //support one generic type only
            Type genericType = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (genericType instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) genericType).getRawType();
            } else if (genericType instanceof Class<?>) {
                return (Class<?>) genericType;
            }
        } else if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        return field.getType();
    }
    /**
     * 根据目标对象的字段类型获取属性值
     *
     * @param object    目标对象
     * @param fieldType 字段类型
     * @param <T>       泛型类型
     * @return 属性值
     * @throws IllegalAccessException 异常
     */
    public static  <T> T getFieldValueByType(Object object, Class<T> fieldType) throws IllegalAccessException {
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType() == fieldType) {
                return getValue(field, object);
            }
        }
        return null;
    }

    public static  <T> T getValue(Field field, Object obj) throws IllegalAccessException {
        field.setAccessible(TRUE);
        try {
            return (T) field.get(obj);
        } catch (IllegalAccessException e) {
            log.error("get field:{} value from:{} fail", field.getName(), obj.getClass().getName(), e);
            throw e;
        }
    }
}
