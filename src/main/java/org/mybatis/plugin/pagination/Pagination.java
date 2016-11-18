package org.mybatis.plugin.pagination;

/**
 * Created by KAYEN on 2016/11/18.
 */
public class Pagination {
    private int page;
    private int pageSize;
    private int offset;
    private int limit;
    private long total;

    public Pagination(int page) {
        this(page, Integer.MAX_VALUE);
    }

    public Pagination(int page, int pageSize) {
        this.page = Math.max(page, 1);
        this.pageSize = pageSize;
        this.offset = (page - 1) * pageSize;
        this.limit = pageSize;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    @Override
    public String toString() {
        return "Pagination{" +
                "page=" + page +
                ", pageSize=" + pageSize +
                ", offset=" + offset +
                ", limit=" + limit +
                ", total=" + total +
                '}';
    }
}
