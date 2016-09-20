README
====
MyBatis page plugin support MyBatis3.4.x
#Usage
#####1. Dependency  
```Xml
<dependency>
	<groupId>com.fred</groupId>
	<artifactId>mybatis-page</artifactId>
	<version>0.0.1</version>
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
	<select id="queryAll" resultType="com.fred.pojo.User">
        select
        	ID as id,
			USERNAME as username,
			PASSWORD as password
		from
			USER
    </select>
	
	<select id="queryById" resultType="com.fred.pojo.User">
        select
        	ID as id,
			USERNAME as username,
			PASSWORD as password
		where
			ID = #{id}
		from
			USER
    </select>
```  

* Mapper.java  
```Java
@Mapper
public interface UserMapper {
	
	public List<User> queryAll(DalPage page);
	
	public List<User> queryById(Map<String,Object> map);
	
}
```  
* Service.java
```Java
@Service
public class UserService {

	@Autowired
	private UserMapper userMapper;

	public List<User> queryAll(DalPage page) {
		return userMapper.queryAll(page);
	}
	
	public List<User> queryById(Map<String,Object> map) {
		return userMapper.queryById(map);
	}
	
}
```  
* Controller.java
```Java
@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userService;
	
	@RequestMapping("page/{pageNumber}")
	public ModelAndView queryAll(@PathVariable("pageNumber") Integer pageNumber) {
		DalPage page = new DalPage();
		page.setCurrentPage(pageNumber);
		List<User> list = userService.queryAll(page);
		ModelAndView mav = new ModelAndView("user/list");
		mav.addObject("list", list);
		// the pagination information were saved in page Object,see DalPage Class.
		mav.addObject("page", page);
		return mav;
	}

	@RequestMapping("{id}/page/{pageNumber}")
	public ModelAndView queryAll(@PathVariable("id") String id,@PathVariable("pageNumber") Integer pageNumber) {
		DalPage page = new DalPage();
		page.setCurrentPage(pageNumber);
		// With multi query condition must use Map parameter
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("id",id);
		map.put("page",page);
		List<User> list = userService.queryById(map);
		ModelAndView mav = new ModelAndView("user/list");
		mav.addObject("list", list);
		mav.addObject("page", page);
		return mav;
	}
	
}
```

    


