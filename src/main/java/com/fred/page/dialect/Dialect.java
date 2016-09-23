/**
 * TODO
 * @author zhangchao
 * Create on 2016-9-17
 */
package com.fred.page.dialect;

public interface Dialect {

	String getLimitString(String sql);

	String getCountSql(String sql);

	boolean useMaxForLimit();

}
