package com.dispatchops.web.dto;

public class PageRequest {

    private int page = 0;
    private int size = 25;
    private String sort;
    private String dir = "asc";

    public PageRequest() {
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

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public int getOffset() {
        return page * size;
    }
}
