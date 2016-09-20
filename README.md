README
====
MyBatis page plugin support MyBatis3.4.x.Both support DB2 and MySql database.
#Usage
#####1. Dependency  
```Xml
<dependency>
	<groupId>com.fred</groupId>
	<artifactId>mybatis-page</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```

#####2. Configuration  
* With Spring  
    ```Xml
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">  
        <property name="dataSource" ref="dataSource" />  
        <property name="plugins">
    	    <list>
			    <bean class="com.fred.page.plugin.PagePlugin">
				    <property name="dialect">
                                            <!-- or DB2Dialect -->
					    <bean class="com.fred.page.dialect.MySqlDialect" />
			    	</property>
			    </bean>
		    </list>
	    </property>
	    <property name="mapperLocations" value="classpath*:mybatis/*Mapper.xml" />
    </bean>
    ```  
    
* With Spring Boot  
    ```Java
    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSourceWrite") DataSource dataSource) throws Exception {
		SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
		sessionFactoryBean.setDataSource(dataSource);
		PagePlugin pagePlugin = new PagePlugin();
                // or DB2Dialect
		pagePlugin.setDialect(new MySqlDialect());
		sessionFactoryBean.setPlugins(new Interceptor[] { pagePlugin });
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		sessionFactoryBean.setMapperLocations(resolver.getResources("classpath*:mybatis/*Mapper.xml"));
		return sessionFactoryBean.getObject();
	}
    ```  
    
#####3. Example  
* Mapper.xml  
```Xml
	<select id="queryAll" resultType="com.fred.pojo.User
