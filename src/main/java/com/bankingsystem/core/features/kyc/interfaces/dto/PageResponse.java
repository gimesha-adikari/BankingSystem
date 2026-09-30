package com.bankingsystem.core.features.kyc.interfaces.dto;

import java.util.List;

public class PageResponse<T> {
    private int page;
    private int size;
    private long total;
    private List<T> content;

    public PageResponse() {}

    public PageResponse(int page, int size, long total, List<T> content) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.content = content;
    }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
}
