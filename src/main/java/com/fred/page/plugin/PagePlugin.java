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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
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
import com.fred.page.domain.DalPage;

@Intercepts({ @Signature(type = Executor.class, method = "query", args = {
		MappedStatement.class, Object.class, RowBounds.class,
		ResultHandler.class }) })
public class PagePlugin extends AbstractIntercetor {

	private final static ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
	private final static ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
	private final static ReflectorFactory DEFAULT_REFLECTOR_FACTORY = new DefaultReflectorFactory();
	private final static int MAPPED_STATEMENT_INDEX = 0;
	private final static int PARAMETER_INDEX = 1;

	private Dialect dialect;

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
		BoundSql boundSql = ms.getBoundSql(queryArgs[PARAMETER_INDEX]);
		DalPage page = getDalPage(boundSql.getParameterObject());
		// if there is no DalPage parameter,skip
		if (page == null) {
			return invocation.proceed();
		}
		long totalCount = queryCount(getConnection(invocation), ms, boundSql);
		if (totalCount == 0) {
			return Collections.emptyList();
		}
		page.setCount(totalCount);
		// replace original boundSql with page boundSql
		boundSql = changeBoundSql(ms, boundSql,
				dialect.getLimitString(boundSql.getSql()),
				new RowBounds(page.getIndex(), page.getPageSize()));
		// replace original MappedStatement with page MappedStatement
		queryArgs[MAPPED_STATEMENT_INDEX] = modifyMappedStatement(ms,
				new SimpleSqlSource(boundSql));
		return invocation.proceed();
	}

	/*
	 * get DalPage parameter
	 */
	private DalPage getDalPage(Object parameterObject) {
		if (parameterObject instanceof DalPage) {
			return (DalPage) parameterObject;
		}
		if (parameterObject instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) parameterObject;
			for (Object object : map.values()) {
				if (object instanceof DalPage) {
					return (DalPage) object;
				}
			}
		}
		return null;
	}

	/**
	 * If in Spring environment,Spring will close the connection.If not,you must
	 * close the connection by yourself
	 * 
	 * @param invocation
	 * @return
	 * @throws SQLException
	 */
	private Connection getConnection(Invocation invocation) throws SQLException {
		Executor executor = (Executor) invocation.getTarget();
		return executor.getTransaction().getConnection();
	}

	private long queryCount(Connection connection, MappedStatement ms,
			BoundSql boundSql) throws Exception {
		PreparedStatement countStmt = null;
		ResultSet rs = null;
		try {
			String countSql = dialect.getCountSql(boundSql.getSql().trim());
			countStmt = connection.prepareStatement(countSql);
			BoundSql countBS = copyFromBoundSql(ms, boundSql, countSql);
			DefaultParameterHandler parameterHandler = new DefaultParameterHandler(
					ms, boundSql.getParameterObject(), countBS);
			parameterHandler.setParameters(countStmt);
			rs = countStmt.executeQuery();
			long totalCount = 0;
			if (rs.next()) {
				totalCount = rs.getLong(1);
			}
			return totalCount;
		} catch (Exception e) {
			throw e;
		} finally {
			// maybe need to close the connection,see getConnection method
			close(rs, countStmt);
		}
	}

	/**
	 * close database connection
	 * 
	 * @param closeable
	 */
	private void close(AutoCloseable... closeable) {
		for (AutoCloseable autoCloseable : closeable) {
			try {
				autoCloseable.close();
			} catch (Exception e) {
				// ignore exception
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
		return dialect.useMaxForLimit() ? rowBounds.getOffset()
				+ rowBounds.getLimit() : rowBounds.getLimit();
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
