package com.daiwenzh5.mapper.util;

import com.daiwenzh5.mapper.entry.NestEntry;
import com.daiwenzh5.mapper.entry.TableEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.type.UnknownTypeHandler;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 结果集映射集合工具
 * 用于根据注解自动生成映射
 *
 * @author daiwenzh5
 * @date 2020-07-16 12:56
 */
@Slf4j
public class ResultMaps {
    private static final AtomicInteger NUMBER_COUNTER = new AtomicInteger(0);

    /**
     * 重新构建结果集映射
     *
     * @param handler   结果集处理器
     * @param ms        映射语句
     * @param resultMap 原始结果集映射
     * @return 重建后的结果集映射
     */
    public static ResultMap rebuild(ResultSetHandler handler, MappedStatement ms, ResultMap resultMap) {
        try {
            MapperBuilderAssistant assistant = getMapperBuilderAssistant(handler, ms);
            return rebuild(assistant, TableEntry.of(resultMap.getType()));
        } catch (IllegalAccessException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 重新构建结果集映射
     *
     * @param builderAssistant 映射器构建助手
     * @param tableEntry        表对象元数据
     * @return 重建后的结果集映射
     */
    private static ResultMap rebuild(MapperBuilderAssistant builderAssistant, TableEntry tableEntry) {
        Class<?> type = tableEntry.getSource();
        List<ResultMapping> resultMappings = getResultMappings(builderAssistant, tableEntry);
        // 注册 ResultMap 到 configuration
        ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant,
                getCustomIdentifier(type),
                type, null, null, resultMappings, null);
        return resultMapResolver.resolve();
    }

    /**
     * 获取映射器构建助手
     *
     * @param handler 结果集处理器
     * @param ms      映射语句
     * @return 映射器构建助手
     * @throws IllegalAccessException 异常
     */
    private static MapperBuilderAssistant getMapperBuilderAssistant(ResultSetHandler handler, MappedStatement ms) throws IllegalAccessException {
        Configuration configuration = Reflections.getFieldValueByType(handler, Configuration.class);
        String resource = ms.getResource();
        String nameSpace = ms.getId().substring(0, ms.getId().lastIndexOf("."));
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, resource);
        builderAssistant.setCurrentNamespace(nameSpace);
        return builderAssistant;
    }

    /**
     * 获取自定义结果集映射的 id
     * 需要保证在注册 resultMap 时，其 id 唯一
     *
     * @param type 映射类型
     * @return 返回 '类型名_[自增序号]' 形式的字符串,如 'user_1'
     */
    private static String getCustomIdentifier(Class<?> type) {
        return String.format("%s_%d", type.getSimpleName(), NUMBER_COUNTER.getAndIncrement());
    }

    /**
     * 获取结果映射对象
     *
     * @param builderAssistant 映射器构建助手
     * @param tableEntry        表对象的元数据
     * @return 返回结果映射对象集合
     */
    private static List<ResultMapping> getResultMappings(MapperBuilderAssistant builderAssistant,
                                                         TableEntry tableEntry) {
        List<ResultMapping> resultMappings = new ArrayList<>();
        tableEntry.getColumnEntries().forEach(columnEntry -> {
            List<ResultFlag> flags = new ArrayList<>();
            String nestResultMapId = null;
            NestEntry nestEntry = tableEntry.getNestEntryMap().get(columnEntry.getProperty());
            if (Objects.nonNull(nestEntry)) {
                nestResultMapId = rebuild(builderAssistant, TableEntry.of(nestEntry)).getId();
            }
            if (columnEntry.isPrimaryKey()) {
                flags.add(ResultFlag.ID);
            }
            ResultMapping resultMapping = new ResultMapping.Builder(builderAssistant.getConfiguration(),
                    columnEntry.getProperty())
                    .column(getColumn(columnEntry.getColumn(), tableEntry.getAlias()))
                    .javaType(columnEntry.getJavaType())
                    .jdbcType(columnEntry.getJdbcType())
                    .flags(flags)
                    .nestedResultMapId(nestResultMapId)
                    .typeHandler(resolveTypeHandler(builderAssistant,
                            columnEntry.getJavaType(),
                            columnEntry.getTypeHandler()))
                    .build();
            resultMappings.add(resultMapping);
        });
        return resultMappings;
    }

    /**
     * 获取映射时的列名
     * 主要处理嵌套的复杂对象，为其列名添加前缀
     *
     * @param column 列名
     * @param prefix 前缀
     * @return 返回最终映射时的列名
     */
    private static String getColumn(String column, String prefix) {
        return Strings.isNotEmpty(prefix) ? prefix + "_" + column : column;
    }


    /**
     * 解析类型处理器
     *
     * @param mapperBuilderAssistant 映射器构建助手
     * @param javaType               Java 类型
     * @param typeHandler            类型处理器
     * @return 真实的类型处理器
     */
    private static TypeHandler<?> resolveTypeHandler(MapperBuilderAssistant mapperBuilderAssistant,
                                                     Class<?> javaType,
                                                     Class<? extends TypeHandler<?>> typeHandler) {

        if (Objects.isNull(typeHandler) || typeHandler == UnknownTypeHandler.class) {
            return null;
        }
        TypeHandlerRegistry typeHandlerRegistry = mapperBuilderAssistant.getConfiguration().getTypeHandlerRegistry();
        TypeHandler<?> handler = typeHandlerRegistry.getMappingTypeHandler(typeHandler);
        if (Objects.isNull(handler)) {
            handler = typeHandlerRegistry.getInstance(javaType, typeHandler);
        }
        return handler;
    }
}
