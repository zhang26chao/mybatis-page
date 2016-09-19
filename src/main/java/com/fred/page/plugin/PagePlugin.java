/**
 * TODO
 * @author zhangchao
 * Create on 2016-9-17
 */
package com.fred.page.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.fred.page.dialect.Dialect;
import com.fred.page.dialect.MySqlDialect;
import com.fred.page.domain.DalPage;

@Intercepts({ @Signature(type = Executor.class, method = "query", args = {
		MappedStatement.class, Object.class, RowBounds.class,
		ResultHandler.class }) })
public class PagePlugin implements Interceptor {

	private final static ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
	private final static ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
	private final static ReflectorFactory DEFAULT_REFLECTOR_FACTORY = new DefaultReflectorFactory();
	private final static int MAPPED_STATEMENT_INDEX = 0;
	private final static int PARAMETER_INDEX = 1;

	private Dialect dialect = new MySqlDialect();

	public Dialect getDialect() {
		return dialect;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		Object[] queryArgs = invocation.getArgs();
		MappedStatement ms = (MappedStatement) queryArgs[MAPPED_STATEMENT_INDEX];
		Object parameter = queryArgs[PARAMETER_INDEX];
		BoundSql boundSql = ms.getBoundSql(parameter);
		Object parameterObject = boundSql.getParameterObject();
		DalPage page = null;
		if (parameterObject instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) parameterObject;
			for (Object object : map.values()) {
				if (object instanceof DalPage) {
					page = (DalPage) object;
					break;
				}
			}
		} else if (parameterObject instanceof DalPage) {
			page = (DalPage) parameterObject;
		} else {
			return invocation.proceed();
		}
		if (page == null) {
			return invocation.proceed();
		}
		// 计算总数
		queryCount(ms, boundSql, page);
		int offset = page.getIndex();
		int limit = page.getPageSize();
		// replace original sql with page sql
		String pageSql = dialect.getLimitString(boundSql.getSql(), offset,
				limit);
		BoundSql pageBoundSql = changeBoundSql(ms, boundSql, pageSql,
				new RowBounds(page.getIndex(), page.getPageSize()));
		// replace original MappedStatement with page MappedStatement
		queryArgs[MAPPED_STATEMENT_INDEX] = modifyMappedStatement(ms,
				new SimpleSqlSource(pageBoundSql));
		return invocation.proceed();
	}

	private void queryCount(MappedStatement ms, BoundSql boundSql, DalPage page) {
		Connection connection = null;
		PreparedStatement countStmt = null;
		ResultSet rs = null;
		try {
			String countSql = getCountSql(boundSql.getSql().trim());
			connection = ms.getConfiguration().getEnvironment().getDataSource()
					.getConnection();
			countStmt = connection.prepareStatement(countSql);
			BoundSql countBS = copyFromBoundSql(ms, boundSql, countSql);
			DefaultParameterHandler parameterHandler = new DefaultParameterHandler(
					ms, boundSql.getParameterObject(), countBS);
			parameterHandler.setParameters(countStmt);
			rs = countStmt.executeQuery();
			int totpage = 0;
			if (rs.next()) {
				totpage = rs.getInt(1);
			}
			page.setCount(totpage);
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (countStmt != null) {
				try {
					countStmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private MappedStatement modifyMappedStatement(MappedStatement ms,
			SqlSource sqlSource) {
		org.apache.ibatis.mapping.MappedStatement.Builder builder = new MappedStatement.Builder(
				ms.getConfiguration(), ms.getId(), sqlSource,
				ms.getSqlCommandType());
		builder.resource(ms.getResource());
		builder.fetchSize(ms.getFetchSize());
		builder.statementType(ms.getStatementType());
		builder.keyGenerator(ms.getKeyGenerator());
		builder.keyProperty(jointKeyProperties(ms.getKeyProperties()));
		builder.timeout(ms.getTimeout());
		builder.parameterMap(ms.getParameterMap());
		builder.resultMaps(ms.getResultMaps());
		builder.cache(ms.getCache());
		builder.resultSetType(ms.getResultSetType());
		return builder.build();
	}

	private String jointKeyProperties(String[] keyProperties) {
		if (keyProperties == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (String string : keyProperties) {
			if (builder.length() > 0) {
				builder.append(",");
			}
			builder.append(string);
		}
		return builder.toString();
	}

	private BoundSql changeBoundSql(MappedStatement ms, BoundSql boundSql,
			String sql, RowBounds rowBounds) {
		ParameterMap mappings = changeParameterMap(ms,
				boundSql.getParameterMappings());
		BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), sql,
				mappings.getParameterMappings(), boundSql.getParameterObject());
		MetaObject countBsObject = MetaObject.forObject(newBoundSql,
				DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY,
				DEFAULT_REFLECTOR_FACTORY);
		MetaObject boundSqlObject = MetaObject.forObject(boundSql,
				DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY,
				DEFAULT_REFLECTOR_FACTORY);
		countBsObject.setValue("metaParameters",
				boundSqlObject.getValue("metaParameters"));
		countBsObject.setValue("additionalParameters",
				boundSqlObject.getValue("additionalParameters"));
		newBoundSql.setAdditionalParameter("offset", rowBounds.getOffset());
		newBoundSql.setAdditionalParameter("limit", getMaxOrLimit(rowBounds));
		return newBoundSql;
	}

	private ParameterMap changeParameterMap(MappedStatement ms,
			List<ParameterMapping> mappings) {
		List<ParameterMapping> dest = new LinkedList<ParameterMapping>();
		dest.addAll(mappings);
		ParameterMapping.Builder builer = new ParameterMapping.Builder(
				ms.getConfiguration(), "offset", Integer.class);
		dest.add(builer.build());
		builer = new ParameterMapping.Builder(ms.getConfiguration(), "limit",
				Integer.class);
		dest.add(builer.build());
		ParameterMap.Builder parameterMapBuiler = new ParameterMap.Builder(
				ms.getConfiguration(), ms.getParameterMap().getId(), ms
						.getParameterMap().getType(), dest);
		return parameterMapBuiler.build();
	}

	private String getCountSql(String sql) {
		return "SELECT COUNT(*) FROM (" + sql + ") aliasForPage";
	}

	private BoundSql copyFromBoundSql(MappedStatement ms, BoundSql boundSql,
			String sql) {
		BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), sql,
				boundSql.getParameterMappings(), boundSql.getParameterObject());
		for (ParameterMapping mapping : boundSql.getParameterMappings()) {
			String prop = mapping.getProperty();
			if (boundSql.hasAdditionalParameter(prop)) {
				newBoundSql.setAdditionalParameter(prop,
						boundSql.getAdditionalParameter(prop));
			}
		}
		return newBoundSql;
	}

	private int getMaxOrLimit(RowBounds rowBounds) {
		final int firstRow = rowBounds.getOffset();
		final int limit = rowBounds.getLimit();
		if (dialect.useMaxForLimit()) {
			return firstRow + limit;
		} else {
			return limit;
		}
	}

	@Override
	public Object plugin(Object target) {
		return Plugin.wrap(target, this);
	}

	@Override
	public void setProperties(Properties properties) {
	}

	public static class SimpleSqlSource implements SqlSource {
		BoundSql boundSql;

		public SimpleSqlSource(BoundSql boundSql) {
			this.boundSql = boundSql;
		}

		public BoundSql getBoundSql(Object parameterObject) {
			return boundSql;
		}
	}

}
