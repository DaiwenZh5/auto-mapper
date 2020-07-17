package com.daiwenzh5.mapper.plugin;

import com.daiwenzh5.mapper.util.Reflections;
import com.daiwenzh5.mapper.util.ResultMaps;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.type.TypeAliasRegistry;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

/**
 * @author daiwenzh5
 * @date 2020-07-17 21:45
 */
@Intercepts({
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
@Slf4j
public class ResultSetHandlerInterceptor implements Interceptor {


    private static final Set<String> MAPPED_STATEMENT_CACHE = new ConcurrentSkipListSet<>();

    private static final TypeAliasRegistry ALIAS_REGISTRY = new TypeAliasRegistry();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取默认的结果集处理器
        DefaultResultSetHandler handler = (DefaultResultSetHandler) invocation.getTarget();
        // 获取 ms
        MappedStatement statement = Reflections.getFieldValueByType(handler, MappedStatement.class);
        if (Objects.isNull(statement)) {
            throw new RuntimeException("无效的 sql");
        }
        // 检查当前 sql 是否需要进行映射
        if (!needAutoMapper(statement)) {
            return invocation.proceed();
        }
        // 重建标志，起始为 false
        boolean rebuildFlag = FALSE;
        List<ResultMap> rebuildMapList = new ArrayList<>();
        for (ResultMap resultMap : statement.getResultMaps()) {
            if (!needAutoMapper(resultMap)) {
                rebuildMapList.add(resultMap);
                continue;
            }
            // 一旦存在需要进行自动映射的结果集，则将重建标志置为 true
            rebuildFlag = true;
            ResultMap rebuildResultMap = ResultMaps.rebuild(handler, statement, resultMap);
            rebuildMapList.add(rebuildResultMap);
        }
        // 需要重建结果集
        if (rebuildFlag) {
            rebuildResultMapValue(statement, rebuildMapList);
        }
        MAPPED_STATEMENT_CACHE.add(statement.getId());
        return invocation.proceed();
    }

    /**
     * 检查是否需要进行自动映射
     *
     * @param ms 映射语句
     * @return 当缓存存在或非查询语句时不需要进行自动映射，返回 false， 否则返回 true
     */
    private boolean needAutoMapper(MappedStatement ms) {
        return !MAPPED_STATEMENT_CACHE.contains(ms.getId())
                && SqlCommandType.SELECT.equals(ms.getSqlCommandType());
    }

    private boolean needAutoMapper(ResultMap resultMap) {
        Collection<Class<?>> simpleTypes = ALIAS_REGISTRY.getTypeAliases().values();
        if (resultMap.getAutoMapping() != null && resultMap.getAutoMapping()) {
            return FALSE;
        } else if (simpleTypes.contains(resultMap.getType())) {
            return FALSE;
        } else if (resultMap.getResultMappings() == null || resultMap.getResultMappings().isEmpty()) {
            return TRUE;
        }
        return FALSE;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }


    private void rebuildResultMapValue(MappedStatement ms, List<ResultMap> resultMaps) throws NoSuchFieldException, IllegalAccessException {
        try {
            Field resultMapsField = ms.getClass().getDeclaredField("resultMaps");
            resultMapsField.setAccessible(TRUE);
            resultMapsField.set(ms, resultMaps);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("replace resultMap value for ms fail", e);
            throw e;
        }
    }

}
