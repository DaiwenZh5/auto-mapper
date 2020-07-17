package com.daiwenzh5.mapper.entry;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import tk.mybatis.mapper.annotation.ColumnType;

import java.util.Objects;

/**
 * @author daiwenzh5
 * @date 2020-07-16 12:38
 */
@Getter
@Setter
@ToString
public final class ColumnEntry {

    private String property;

    private String column;

    private Class<?> javaType;

    private JdbcType jdbcType;

    private Class<? extends TypeHandler<?>> typeHandler;

    private boolean isPrimaryKey;

    public ColumnEntry(){}

    private ColumnEntry(String property, String column, Class<?> javaType, JdbcType jdbcType, boolean isPrimaryKey, Class<? extends TypeHandler<?>> typeHandler) {
        this.property = property;
        this.column = column;
        this.javaType = javaType;
        this.jdbcType = jdbcType;
        this.isPrimaryKey = isPrimaryKey;
        this.typeHandler = typeHandler;
    }

    public void setColumnType(ColumnType columnType) {
        if (Objects.nonNull(columnType)) {
            this.jdbcType = columnType.jdbcType();
            this.typeHandler = columnType.typeHandler();
        }
    }
    public static ColumnEntry of(String property, String column, Class<?> javaType, JdbcType jdbcType, boolean isPrimaryKey, Class<? extends TypeHandler<?>> typeHandler) {
        return new ColumnEntry(property, column, javaType, jdbcType, isPrimaryKey, typeHandler);
    }

}
