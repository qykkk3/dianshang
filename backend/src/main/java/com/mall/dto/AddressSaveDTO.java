package com.mall.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 收货地址新增/修改请求参数
 */
@Data
@Schema(description = "收货地址新增/修改请求参数")
public class AddressSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "地址ID，新增时为空")
    private Long id;

    @Schema(description = "收货人", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "收货人不能为空")
    private String receiver;

    @Schema(description = "联系电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138001")
    @NotBlank(message = "联系电话不能为空")
    private String phone;

    @Schema(description = "省", requiredMode = Schema.RequiredMode.REQUIRED, example = "江苏省")
    @NotBlank(message = "省份不能为空")
    private String province;

    @Schema(description = "市", requiredMode = Schema.RequiredMode.REQUIRED, example = "南京市")
    @NotBlank(message = "城市不能为空")
    private String city;

    @Schema(description = "区/县", requiredMode = Schema.RequiredMode.REQUIRED, example = "鼓楼区")
    @NotBlank(message = "区/县不能为空")
    private String district;

    @Schema(description = "详细地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "中山北路 105 号 1 栋 1973 室")
    @NotBlank(message = "详细地址不能为空")
    private String detail;

    @Schema(description = "邮政编码", example = "210009")
    private String postCode;

    @Schema(description = "是否默认地址 1是 0否", example = "1")
    private Integer isDefault;
}
