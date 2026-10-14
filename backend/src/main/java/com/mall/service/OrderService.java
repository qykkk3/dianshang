package com.mall.service;

import com.mall.common.PageResult;
import com.mall.dto.OrderPreviewDTO;
import com.mall.dto.OrderQueryDTO;
import com.mall.dto.OrderSubmitDTO;
import com.mall.vo.ExpressTrackVO;
import com.mall.vo.OrderCountVO;
import com.mall.vo.OrderDetailVO;
import com.mall.vo.OrderPreviewVO;
import com.mall.vo.OrderSubmitVO;
import com.mall.vo.OrderVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

/**
 * 订单服务
 *
 * <p>买家端下单主流程：预览 → 提交（扣库存/用券）→ 支付 → 发货 → 收货，
 * 以及取消（回滚库存/券）、售后中状态流转。</p>
 */
public interface OrderService {

    /**
     * 结算预览：重算金额与可用优惠券，不落库
     *
     * @param dto 来源参数（购物车 或 立即购买）
     * @return 预览结果
     */
    OrderPreviewVO preview(OrderPreviewDTO dto);

    /**
     * 提交订单：服务端重算金额、扣减库存、核销优惠券、清理购物车
     *
     * @param dto 提交参数
     * @return 订单信息
     */
    OrderSubmitVO submit(OrderSubmitDTO dto);

    /**
     * 模拟支付：待付款 → 待发货
     *
     * @param orderNo 订单号
     */
    void pay(String orderNo);

    /**
     * 买家取消订单：待付款 → 已取消，回滚库存/销量/优惠券
     *
     * @param orderNo 订单号
     */
    void cancel(String orderNo);

    /**
     * 确认收货：待收货 → 已完成
     *
     * @param orderNo 订单号
     */
    void confirmReceive(String orderNo);

    /**
     * 删除订单（仅已完成/已取消，逻辑删除）
     *
     * @param orderNo 订单号
     */
    void deleteOrder(String orderNo);

    /**
     * 买家订单分页
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<OrderVO> pageBuyerOrders(OrderQueryDTO query);

    /**
     * 买家订单详情
     *
     * @param orderNo 订单号
     * @return 订单详情（含物流轨迹与售后单）
     */
    OrderDetailVO getBuyerDetail(String orderNo);

    /**
     * 买家查询订单物流轨迹
     *
     * @param orderNo 订单号
     * @return 轨迹列表（节点时间倒序）
     */
    List<ExpressTrackVO> listTracks(String orderNo);

    /**
     * 买家各状态订单数量
     *
     * @return 数量统计
     */
    OrderCountVO countByStatus();

    /* ==================== 以下方法供管理端复用 ==================== */

    /**
     * 管理端订单分页（可按买家、状态、时间、关键词筛选）
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<OrderVO> pageAdminOrders(OrderQueryDTO query);

    /**
     * 管理端订单详情（按订单主键）
     *
     * @param id 订单ID
     * @return 订单详情
     */
    OrderDetailVO getAdminDetail(Long id);

    /**
     * 管理端订单发货：待发货 → 待收货，并自动生成两条物流轨迹
     *
     * @param id      订单ID
     * @param company 快递公司
     * @param no      快递单号
     */
    void ship(Long id, String company, String no);

    /**
     * 管理端追加物流轨迹
     *
     * @param id      订单ID
     * @param status  节点状态
     * @param context 轨迹描述
     * @param location 所在城市
     */
    void addTrack(Long id, String status, String context, String location);

    /**
     * 管理端修改商家备注
     *
     * @param id          订单ID
     * @param adminRemark 备注
     */
    void updateAdminRemark(Long id, String adminRemark);

    /**
     * 管理端改价（仅待付款）
     *
     * @param id        订单ID
     * @param payAmount 修改后的实付金额
     */
    void updatePrice(Long id, java.math.BigDecimal payAmount);

    /**
     * 管理端关闭订单（仅待付款）：回滚库存/销量/优惠券
     *
     * @param id 订单ID
     */
    void close(Long id);

    /**
     * 管理端订单导出（最多 5000 条，条件与列表一致）
     *
     * @param query    查询条件
     * @param response 响应
     */
    void exportAdminOrders(OrderQueryDTO query, HttpServletResponse response);

    /**
     * 管理端订单统计：各状态订单数
     *
     * @return 键名与 OrderCountVO 字段一致
     */
    Map<String, Object> adminStatistics();
}
