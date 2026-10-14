package com.mall.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一分页结果
 *
 * @param <T> 记录类型
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数据")
    private List<T> records = new ArrayList<>();

    @Schema(description = "总记录数")
    private Long total = 0L;

    @Schema(description = "当前页码，从 1 开始")
    private Long page = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;

    @Schema(description = "总页数")
    private Long pages = 0L;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long page, Long size) {
        this.records = records == null ? new ArrayList<>() : records;
        this.total = total == null ? 0L : total;
        this.page = page == null ? 1L : page;
        this.size = size == null || size <= 0 ? 10L : size;
        this.pages = (this.total + this.size - 1) / this.size;
    }

    /**
     * 由 MyBatis-Plus 分页对象构建
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 由 MyBatis-Plus 分页对象构建，并完成实体到 VO 的转换
     */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        List<T> list = page.getRecords().stream().map(converter).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>();
    }
}
