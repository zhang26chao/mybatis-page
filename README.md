README
====
MyBatis page plugin support MyBatis3.4.x
#Usage
#####1. Configuration  
* With Spring  
    ```Xml
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">  
        <property name="dataSource" ref="dataSource" />  
        <property name="plugins">
    	    <list>
			    <bean class="com.fred.page.plugin.PagePlugin">
				    <property name="dialect">
					    <bean class="com.fred.page.dialect.MySqlDialect "></bean>
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
		pagePlugin.setDialect(new MySqlDialect());
		sessionFactoryBean.setPlugins(new Interceptor[] { pagePlugin });
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		sessionFactoryBean.setMapperLocations(resolver.getResources("classpath*:mybatis/*Mapper.xml"));
		return sessionFactoryBean.getObject();
	}
    ```  
    
#####2. Example  

    


