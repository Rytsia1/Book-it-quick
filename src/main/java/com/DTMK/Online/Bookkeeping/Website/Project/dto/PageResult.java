package com.DTMK.Online.Bookkeeping.Website.Project.dto;

import java.util.List;

/**
 * Generic paginated response envelope used by the REST API.
 * <p>
 * Designed to mirror the shape that {@code <el-pagination>} expects on the
 * frontend (it needs {@code total} to render the page count and prev/next
 * buttons). Adding extra fields such as {@code totalPages} or {@code hasNext}
 * is forward-compatible — old clients simply ignore them.
 *
 * @param <T> the type of items on each page (e.g. {@code Bill})
 */
public class PageResult<T> {

    private List<T> items;
    private long total;
    private int page;
    private int size;

    public PageResult() {
    }

    public PageResult(List<T> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /** Convenience: number of pages given {@link #size} and {@link #total}. */
    public int getTotalPages() {
        if (size <= 0 || total <= 0) return 0;
        return (int) Math.ceil((double) total / (double) size);
    }
}
