package com.shanyangcode.userservice.model.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据列表
     */
    private List<T> list;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 每页大小
     */
    private Long pageSize;

    /**
     * 当前页码
     */
    private Long pageNum;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;

    /**
     * 从 MyBatis-Plus IPage 转换
     *
     * @param page MyBatis-Plus 分页结果
     * @param <T>  数据类型
     * @return 统一分页响应
     */
    public static <T> PageResponse<T> of(IPage<T> page) {
        return PageResponse.<T>builder()
                .list(page.getRecords())
                .total(page.getTotal())
                .pageSize(page.getSize())
                .pageNum(page.getCurrent())
                .pages(page.getPages())
                .hasNext(page.getCurrent() < page.getPages())
                .hasPrevious(page.getCurrent() > 1)
                .build();
    }


    /**
     * 从 MyBatis-Plus IPage 转换（带数据转换）
     *
     * @param page      MyBatis-Plus 分页结果
     * @param converter 数据转换函数
     * @param <T>       源数据类型
     * @param <R>       目标数据类型
     * @return 统一分页响应
     */
    public static <T, R> PageResponse<R> of(IPage<T> page, Function<T, R> converter) {
        List<R> convertedList=page.getRecords().stream().map(converter).collect(Collectors.toList());

        return PageResponse.<R>builder()
                .list(convertedList)
                .total(page.getTotal())
                .pageSize(page.getSize())
                .pageNum(page.getCurrent())
                .pages(page.getPages())
                .hasNext(page.getCurrent() < page.getPages())
                .hasPrevious(page.getCurrent() > 1)
                .build();
    }

    /**
     * 手动构建分页响应（适用于内存分页场景）
     *
     * @param allData  全部数据列表
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 统一分页响应
     */
    public static <T> PageResponse<T> of(List<T> allData,int pageNum,int pageSize) {
        if (allData == null || allData.isEmpty()) {
            return PageResponse.<T>builder()
                    .list(Collections.emptyList())
                    .total(0L)
                    .pageSize((long) pageSize)
                    .pageNum((long) pageNum)
                    .pages(0L)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }

        long total = allData.size();
        long pages = (total + pageSize - 1) / pageSize;

        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allData.size());

        List<T> list = fromIndex < allData.size()
                ? allData.subList(fromIndex, toIndex)
                : Collections.emptyList();

        return PageResponse.<T>builder()
                .list(list)
                .total(total)
                .pageSize((long) pageSize)
                .pageNum((long) pageNum)
                .pages(pages)
                .hasNext(pageNum < pages)
                .hasPrevious(pageNum > 1)
                .build();
    }

    /**
     * 构建空的分页响应
     *
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @param <T>      数据类型
     * @return 空的分页响应
     */
    public static <T> PageResponse<T> empty(int pageNum,int pageSize) {
        return PageResponse.<T>builder()
                .list(Collections.emptyList())
                .total(0L)
                .pageSize((long) pageSize)
                .pageNum((long) pageNum)
                .pages(0L)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}
