package com.daiwenzh5.mapper.entry;

import com.daiwenzh5.mapper.annation.Join;
import com.daiwenzh5.mapper.util.Reflections;
import com.daiwenzh5.mapper.util.Strings;
import lombok.Getter;
import lombok.Setter;
import tk.mybatis.mapper.annotation.ColumnType;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.*;


/**
 * @author daiwenzh5
 * @date 2020-07-17 21:45
 */
@Getter
@Setter
public class TableEntry {

    private String alias;

    private Class<?> source;

    private List<ColumnEntry> columnEntries;

    private Map<String, NestEntry> nestEntryMap;


    private TableEntry(Class<?> source, Join.Id id, String alias) {
        this.alias = alias;
        this.source = source;
        this.columnEntries = new ArrayList<>();
        this.nestEntryMap = new HashMap<>();
        // 获取所有字段
        Field[] fieldArray = Reflections.getAllFields(source);
        Arrays.stream(fieldArray).forEach(field -> {
            ColumnEntry columnEntry = new ColumnEntry();
            columnEntry.setProperty(field.getName());
            columnEntry.setJavaType(field.getType());
            columnEntry.setColumn(deCamelPropertyName(field.getName()));
            // 处理主键注解
            if (Objects.nonNull(id)) {
                columnEntry.setPrimaryKey(id.value().equals(columnEntry.getProperty()));
            } else {
                columnEntry.setPrimaryKey(Objects.nonNull(field.getAnnotation(javax.persistence.Id.class)));
            }
            // 处理类型处理器注解
            columnEntry.setColumnType(field.getAnnotation(ColumnType.class));
            // 处理字段别名 @Column 注解
            Optional.ofNullable(field.getAnnotation(Column.class))
                    .ifPresent(it -> columnEntry.setColumn(deCamelPropertyName(it.name())));
            // 处理嵌套属性
            Optional.ofNullable(field.getAnnotation(Join.class))
                    .ifPresent(join -> {
                        // 需要注意集合中的泛型类型
                        Class<?> javaType = join.many() ? Reflections.getFieldType(field) : field.getType();
                        this.nestEntryMap.put(columnEntry.getProperty(),
                                NestEntry.of(Strings.getOrDefault(join.as(), columnEntry.getProperty()),
                                        javaType, join.joinId()));
                    });
            this.columnEntries.add(columnEntry);
        });
        // 当存在多个主键时
        long idCount = this.columnEntries.stream().filter(ColumnEntry::isPrimaryKey).count();
        if (idCount > 1) {
            throw new RuntimeException("不能存在多个主键");
        }
        // 当不存在注解时
        if (idCount == 0) {
            Optional<ColumnEntry> idEntry = this.columnEntries.stream().filter(columnEntry -> "id".equals(columnEntry.getProperty()))
                    .findFirst();
            if (idEntry.isPresent()) {
                idEntry.get().setPrimaryKey(true);
            } else {
                throw new RuntimeException("未设置主键，请确认存在 id 字段，或其被 @Id 注解标注");
            }
        }
    }

    public static TableEntry of(Class<?> type) {
        return new TableEntry(type, null, "");
    }

    public static TableEntry of(NestEntry nestEntry) {
        return new TableEntry(nestEntry.getSource(), nestEntry.getJoinId(), nestEntry.getAlias());
    }


    private static String deCamelPropertyName(String propertyName) {
        StringBuilder result = new StringBuilder();
        if (propertyName != null && propertyName.length() > 0) {
            for (int i = 0; i < propertyName.length(); i++) {
                char ch = propertyName.charAt(i);
                if (Character.isUpperCase(ch)) {
                    result.append("_");
                    result.append(Character.toLowerCase(ch));
                } else {
                    result.append(ch);
                }
            }
        }
        return result.toString();
    }

}
