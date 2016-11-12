/**
 * TODO
 * @author zhangchao
 * Create on 2016-11-12
 */
package com.fred.page.plugin;

import java.util.Properties;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Plugin;

public abstract class AbstractIntercetor implements Interceptor {

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {
	}

}
