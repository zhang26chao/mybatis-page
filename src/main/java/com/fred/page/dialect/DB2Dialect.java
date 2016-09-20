/**
 * TODO
 * @author zhangchao
 * Create on 2016-9-20
 */
package com.fred.page.dialect;

public class DB2Dialect implements Dialect {

	@Override
	public String getLimitString(String sql, int offset, int limit) {
		int startOfSelect = sql.toLowerCase().indexOf("select");
		StringBuilder pagingSelect = new StringBuilder(sql.length() + 100)
				.append(sql.substring(0, startOfSelect)) // add the comment
				.append("select * from ( select ").append(getRowNumber(sql));
		if (hasDistinct(sql)) {
			pagingSelect.append(" row_.* from ( ")
					.append(sql.substring(startOfSelect)).append(" ) as row_");
		} else {
			pagingSelect.append(sql.substring(startOfSelect + 6));
		}
		pagingSelect.append(" ) as temp_ where rownumber_ ");
		// add the restriction to the outer select
		pagingSelect.append("between ?+1 and ?");
		return pagingSelect.toString();
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	private String getRowNumber(String sql) {
		StringBuffer rownumber = new StringBuffer(50)
				.append("rownumber() over(");
		int orderByIndex = sql.toLowerCase().indexOf("order by");
		if (orderByIndex > 0 && !hasDistinct(sql)) {
			rownumber.append(sql.substring(orderByIndex));
		}
		rownumber.append(") as rownumber_,");
		return rownumber.toString();
	}

	private static boolean hasDistinct(String sql) {
		return sql.toLowerCase().indexOf("select distinct") >= 0;
	}

}
