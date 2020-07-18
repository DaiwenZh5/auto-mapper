# Mybatis 结果集自动映射插件
## 目的
Mybatis 只对单表提供自动映射，当进行关联查询时，需要在 ResultMap 中使用使用 association  或 collection  标签为子对象添加映射关系。 
为了简化操作，此处通过实现 mybatis 的结果集处理器的拦截器插件，来对表示多表关联的嵌套对象完成结果集的自动映射。
## 使用
### 配置
1. 插件提供 spring boot 的自动装载，引入依赖后自动生效。 
2. 对于非 spring boot，需要在 sessionFactory 中，为其 configuration 属性手动添加拦截器：
```
Configuration configuration = sqlSessionFactory.getConfiguration();
configuration.addInterceptor(new ResultSetHandlerInterceptor());
```
### 注解
对于关联对象，需要将其作为属性添加到实体类中，并使用 @Join 注解标注，如：
```java
import com.daiwenzh5.mapper.annation.Join;
public class Person extends PersonDo{

    @Join(as = "f")
    private PersonDo father;

    @Join(many = true)
    private List<PersonDo> parent;
}
```
@Join 表示其注解的字段为关联对象，其属性为：
1. as: 别名，默认为空，表示结果集字段的映射名前缀，如上例中，
    - f_列名 ---> father.属性
    - parent_列名 ---> parent.属性
2. joinId: 关联主键，默认为 id，用于表示字段为 resultMap 中的主键，实际测试时，并不是必须的；
3. many: 是否为集合映射，默认为 false，对于一对多的嵌套属性（集合）必须为 true，否则会报错；
## 参考
实现原理参考自 [andyxuq/mybatis-automapper-plugin](https://github.com/andyxuq/mybatis-automapper-plugin#mybatis%E7%BB%93%E6%9E%9C%E9%9B%86%E8%87%AA%E5%8A%A8%E6%98%A0%E5%B0%84%E6%8F%92%E4%BB%B6) 。