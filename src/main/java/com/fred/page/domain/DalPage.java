/**
 * TODO
 * @author zhangchao
 * Create on 2016-9-17
 */
package com.fred.page.domain;

public class DalPage {

	protected int currentPage = 1;

	protected int pageSize = 10;

	protected long count;

	protected int pages;

	protected int index;

	/**
	 * 获取当前页
	 * 
	 * @return 当前页
	 */
	public int getCurrentPage() {
		return currentPage;
	}

	/**
	 * 设置当前页
	 * 
	 * @param currentPage
	 *            当前页
	 */
	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	/**
	 * 获取每页记录数
	 * 
	 * @return 每页记录数
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * 设置每页记录数
	 * 
	 * @param pageSize
	 *            每页记录数
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * 获取记录总数
	 * 
	 * @return 记录总数
	 */
	public long getCount() {
		return count;
	}

	/**
	 * 设置记录总数
	 * 
	 * @param count
	 *            记录总数
	 */
	public void setCount(long count) {
		this.count = count;
		if (count > 0) {
			// 计算页数
			this.pages = (int) (this.count / this.pageSize);
			if (this.count % this.pageSize > 0) {
				this.pages++;
			}
			// 调整当前页
			if (this.currentPage > this.pages) {
				this.currentPage = this.pages;
			}
			// 计算当前页的索引
			this.index = (this.currentPage - 1) * this.pageSize;
		}
	}

	/**
	 * 获取页数
	 * 
	 * @return 页数
	 */
	public int getPages() {
		return pages;
	}

	/**
	 * 获取当前页的索引
	 * 
	 * @return 当前页的索引
	 */
	public int getIndex() {
		return index;
	}

}
