package com.mall.controller;

import com.mall.common.Result;
import com.mall.dto.AddressSaveDTO;
import com.mall.service.AddressService;
import com.mall.vo.AddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址接口（需买家登录）
 */
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@Tag(name = "收货地址", description = "收货地址的增删改查与默认地址设置")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/list")
    @Operation(summary = "地址列表")
    public Result<List<AddressVO>> list() {
        return Result.success(addressService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "地址详情")
    public Result<AddressVO> detail(@PathVariable Long id) {
        return Result.success(addressService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增地址")
    public Result<Void> save(@Valid @RequestBody AddressSaveDTO dto) {
        addressService.save(dto);
        return Result.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改地址")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AddressSaveDTO dto) {
        dto.setId(id);
        addressService.save(dto);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认地址")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.success();
    }
}
