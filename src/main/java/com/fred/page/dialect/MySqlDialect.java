/**
 * TODO
 * @author zhangchao
 * Create on 2016-9-17
 */
package com.fred.page.dialect;

public class MySqlDialect extends AbstractDialect {

	@Override
	public String getLimitString(String sql) {
		return new StringBuilder(sql.length() + 20).append(sql)
				.append(" limit ? , ?").toString();
	}

	@Override
	public boolean useMaxForLimit() {
		return false;
	}

}
