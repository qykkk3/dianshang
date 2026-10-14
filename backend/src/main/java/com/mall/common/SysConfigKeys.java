package com.mall.common;

/**
 * 系统配置项 Key 常量（对应 sys_config.config_key）
 *
 * <p>这些配置可在管理后台「系统管理 → 系统配置」中在线修改，
 * 业务代码通过 SysConfigMapper / SysConfigService 读取，读取不到时回退到 application.yml。</p>
 */
public final class SysConfigKeys {

    private SysConfigKeys() {
    }

    /** 库存预警阈值 */
    public static final String STOCK_WARN_THRESHOLD = "stock_warn_threshold";

    /** 默认运费（元） */
    public static final String FREIGHT_FEE = "freight_fee";

    /** 包邮门槛（元） */
    public static final String FREE_FREIGHT_AMOUNT = "free_freight_amount";

    /** 未支付订单自动取消（分钟） */
    public static final String ORDER_AUTO_CANCEL_MINUTES = "order_auto_cancel_minutes";

    /** 发货后自动收货（天） */
    public static final String ORDER_AUTO_FINISH_DAYS = "order_auto_finish_days";

    /** 可申请售后天数 */
    public static final String AFTER_SALE_DAYS = "after_sale_days";

    /** 商城名称 */
    public static final String SHOP_NAME = "shop_name";

    /** 商城公告（前台顶部） */
    public static final String SHOP_NOTICE = "shop_notice";

    /** 热门搜索词 */
    public static final String HOT_SEARCH_KEYWORDS = "hot_search_keywords";
}
