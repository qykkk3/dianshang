package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 会员详情：在 {@link MemberVO} 基础上追加生日、行为统计与最近订单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "会员详情")
public class MemberDetailVO extends MemberVO {

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "收货地址数量")
    private Long addressCount = 0L;

    @Schema(description = "收藏商品数量")
    private Long favoriteCount = 0L;

    @Schema(description = "评价数量")
    private Long commentCount = 0L;

    @Schema(description = "最近订单，最多 10 条：orderNo/payAmount/status/statusName/createTime")
    private List<Map<String, Object>> recentOrders = new ArrayList<>();
}
