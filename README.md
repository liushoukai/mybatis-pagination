# mybatis-pagination
MyBatis分页插件

## 使用说明
1.配置Maven依赖
```xml
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-pagination</artifactId>
    <version>0.0.1</version>
</dependency>
```

2.修改mybatis-config.xml，注册插件
```xml
<plugins>
    <!-- 分页插件 -->
    <plugin interceptor="org.mybatis.plugin.pagination.PaginationInterceptor"></plugin>
</plugins>
```

## 例子
```java
@Select("select * from dp_user where status = #{userStatus} limit #{pagination.offset}, #{pagination.limit}")
List<User> findByPage(@Param("userStatus") User.UserStatus userStatus, @Param("pagination") Pagination pagination);
```

```java
@Test
public void testFindByPage() throws Exception {
    Pagination pagination = new Pagination(1, 5);

    List<User> users = userMapper.findByPage(User.UserStatus.normal, pagination);
    for(User user : users) {
        String format = String.format("yyuid: %-10s, type: %-4s, status: %s, ctime: %d", user.getYyuid(), user.getType(), user.getStatus(), user.getCtime());
        System.out.println(format);
    }
    System.out.println(pagination);
}
```
