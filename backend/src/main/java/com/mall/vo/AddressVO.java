package com.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 收货地址
 */
@Data
@Schema(description = "收货地址")
public class AddressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "地址ID")
    private Long id;

    @Schema(description = "收货人")
    private String receiver;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "详细地址")
    private String detail;

    @Schema(description = "邮政编码")
    private String postCode;

    @Schema(description = "是否默认 1是 0否")
    private Integer isDefault;

    @Schema(description = "完整地址（省市区 + 详细地址）")
    private String fullAddress;
}
