/**
 * TODO
 * @author zhangchao
 * Create on 2016-9-23
 */
package com.fred.page.dialect;

public abstract class AbstractDialect implements Dialect {

	@Override
	public String getCountSql(String sql) {
		int startOfSelect = sql.toLowerCase().indexOf("select");
		StringBuilder builder = new StringBuilder(sql.length() + 100).append(
				sql.substring(0, startOfSelect))
		.append("select count(1) from ");
		if (hasDistinct(sql) || hasGroupBy(sql)) {
			builder.append(" ( ").append(sql.substring(startOfSelect))
					.append(" ) as temp");
		} else {
			int fromOfSelect = sql.toLowerCase().indexOf("from");
			builder.append(sql.substring(fromOfSelect + 4));
		}
		return builder.toString();
	}

	protected boolean hasDistinct(String sql) {
		return sql.toLowerCase().indexOf("select distinct") >= 0;
	}

	protected boolean hasGroupBy(String sql) {
		return sql.toLowerCase().indexOf("group by") >= 0;
	}

}
