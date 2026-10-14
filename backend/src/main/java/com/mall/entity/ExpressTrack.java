package com.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流轨迹表
 */
@Data
@TableName("express_track")
public class ExpressTrack implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 轨迹ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 快递公司 */
    private String expressCompany;

    /** 快递单号 */
    private String expressNo;

    /** 节点状态 已揽收/运输中/派送中/已签收 */
    private String status;

    /** 轨迹描述 */
    private String context;

    /** 所在城市 */
    private String location;

    /** 节点时间 */
    private LocalDateTime acceptTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
