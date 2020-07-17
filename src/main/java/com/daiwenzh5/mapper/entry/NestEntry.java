package com.daiwenzh5.mapper.entry;

import com.daiwenzh5.mapper.annation.Join;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author daiwenzh5
 * @date 2020-07-16 12:44
 */
@Getter
@Setter
@ToString
public final class NestEntry {

    private String alias;

    private Class<?> source;

    private Join.Id joinId;


    public static NestEntry of(String alias, Class<?> source, Join.Id joinId) {
        return new NestEntry(alias, source, joinId);
    }

    private NestEntry(String alias, Class<?> source, Join.Id joinId) {
        this.alias = alias;
        this.source = source;
        this.joinId = joinId;
    }


}
