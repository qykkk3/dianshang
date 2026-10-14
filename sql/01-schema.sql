-- =============================================================================
--  电商管理系统 —— 数据库结构脚本
--  Database : mall_db
--  Engine   : InnoDB / utf8mb4 / utf8mb4_general_ci
--  DB       : MySQL 8.0
--  说明     : 本脚本可重复执行（先 DROP 再 CREATE），不会删除数据库本身
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `mall_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `mall_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 一、买家与账户
-- =============================================================================

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`        VARCHAR(50)  NOT NULL                COMMENT '登录账号',
  `password`        VARCHAR(100) NOT NULL                COMMENT '密码(BCrypt)',
  `nickname`        VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
  `phone`           VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `email`           VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
  `avatar`          VARCHAR(255) DEFAULT NULL            COMMENT '头像地址',
  `gender`          TINYINT      NOT NULL DEFAULT 0      COMMENT '性别 0未知 1男 2女',
  `birthday`        DATE         DEFAULT NULL            COMMENT '生日',
  `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态 1正常 0禁用',
  `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_phone` (`phone`),
  KEY `idx_user_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '买家用户表';

DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '用户ID',
  `receiver`    VARCHAR(50)  NOT NULL                COMMENT '收货人',
  `phone`       VARCHAR(20)  NOT NULL                COMMENT '联系电话',
  `province`    VARCHAR(50)  NOT NULL                COMMENT '省',
  `city`        VARCHAR(50)  NOT NULL                COMMENT '市',
  `district`    VARCHAR(50)  NOT NULL                COMMENT '区/县',
  `detail`      VARCHAR(255) NOT NULL                COMMENT '详细地址',
  `post_code`   VARCHAR(10)  DEFAULT NULL            COMMENT '邮编',
  `is_default`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否默认 1是 0否',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_addr_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收货地址表';

DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `product_id`  BIGINT   NOT NULL COMMENT '商品ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_product` (`user_id`, `product_id`),
  KEY `idx_favorite_product` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品收藏表';

DROP TABLE IF EXISTS `browse_history`;
CREATE TABLE `browse_history` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `product_id`  BIGINT   NOT NULL COMMENT '商品ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_history_user_product` (`user_id`, `product_id`),
  KEY `idx_history_user_time` (`user_id`, `update_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品浏览记录表';

-- =============================================================================
-- 二、商品
-- =============================================================================

DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id`   BIGINT      NOT NULL DEFAULT 0      COMMENT '父分类ID,0为顶级',
  `name`        VARCHAR(50) NOT NULL                COMMENT '分类名称',
  `icon`        VARCHAR(255) DEFAULT NULL           COMMENT '分类图标',
  `level`       TINYINT     NOT NULL DEFAULT 1      COMMENT '层级 1一级 2二级',
  `sort`        INT         NOT NULL DEFAULT 0      COMMENT '排序值,越小越前',
  `status`      TINYINT     NOT NULL DEFAULT 1      COMMENT '状态 1启用 0停用',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品分类表';

DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `category_id`    BIGINT        NOT NULL                COMMENT '分类ID',
  `name`           VARCHAR(200)  NOT NULL                COMMENT '商品名称',
  `subtitle`       VARCHAR(255)  DEFAULT NULL            COMMENT '副标题/卖点',
  `brand`          VARCHAR(50)   DEFAULT NULL            COMMENT '品牌',
  `unit`           VARCHAR(20)   NOT NULL DEFAULT '件'   COMMENT '单位',
  `main_image`     VARCHAR(255)  DEFAULT NULL            COMMENT '主图',
  `images`         VARCHAR(2000) DEFAULT NULL            COMMENT '轮播图,逗号分隔',
  `detail`         LONGTEXT      DEFAULT NULL            COMMENT '图文详情(HTML)',
  `price`          DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '最低售价(SPU展示价)',
  `original_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '最低原价',
  `cost_price`     DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '成本价',
  `stock`          INT           NOT NULL DEFAULT 0      COMMENT '总库存(SKU汇总)',
  `sales`          INT           NOT NULL DEFAULT 0      COMMENT '累计销量',
  `warn_stock`     INT           NOT NULL DEFAULT 10     COMMENT '库存预警阈值',
  `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '状态 1上架 0下架',
  `is_recommend`   TINYINT       NOT NULL DEFAULT 0      COMMENT '是否推荐',
  `is_new`         TINYINT       NOT NULL DEFAULT 0      COMMENT '是否新品',
  `is_hot`         TINYINT       NOT NULL DEFAULT 0      COMMENT '是否热销',
  `sort`           INT           NOT NULL DEFAULT 0      COMMENT '排序值',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_product_category` (`category_id`),
  KEY `idx_product_status` (`status`, `deleted`),
  KEY `idx_product_sales` (`sales`),
  KEY `idx_product_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品SPU表';

DROP TABLE IF EXISTS `product_spec`;
CREATE TABLE `product_spec` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规格ID',
  `product_id`  BIGINT       NOT NULL                COMMENT '商品ID',
  `spec_name`   VARCHAR(50)  NOT NULL                COMMENT '规格名 如 颜色',
  `spec_values` VARCHAR(1000) NOT NULL               COMMENT '规格值,逗号分隔 如 曜石黑,月光银',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序值',
  PRIMARY KEY (`id`),
  KEY `idx_spec_product` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品规格定义表';

DROP TABLE IF EXISTS `product_sku`;
CREATE TABLE `product_sku` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
  `product_id`     BIGINT        NOT NULL                COMMENT '商品ID',
  `sku_code`       VARCHAR(64)   DEFAULT NULL            COMMENT 'SKU编码',
  `spec_data`      VARCHAR(500)  DEFAULT NULL            COMMENT '规格键值对JSON 如 {"颜色":"曜石黑"}',
  `spec_info`      VARCHAR(200)  DEFAULT NULL            COMMENT '规格展示文本 如 曜石黑 / 256G',
  `price`          DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '售价',
  `original_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '原价',
  `stock`          INT           NOT NULL DEFAULT 0      COMMENT '库存',
  `sales`          INT           NOT NULL DEFAULT 0      COMMENT '销量',
  `image`          VARCHAR(255)  DEFAULT NULL            COMMENT 'SKU图片',
  `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '状态 1启用 0停用',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sku_product` (`product_id`),
  KEY `idx_sku_code` (`sku_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品SKU表';

-- =============================================================================
-- 三、交易
-- =============================================================================

DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `product_id`  BIGINT   NOT NULL COMMENT '商品ID',
  `sku_id`      BIGINT   NOT NULL COMMENT 'SKU ID',
  `quantity`    INT      NOT NULL DEFAULT 1 COMMENT '数量',
  `checked`     TINYINT  NOT NULL DEFAULT 1 COMMENT '是否选中 1是 0否',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_user_sku` (`user_id`, `sku_id`),
  KEY `idx_cart_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '购物车表';

DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`         VARCHAR(32)   NOT NULL                COMMENT '订单号',
  `user_id`          BIGINT        NOT NULL                COMMENT '买家ID',
  `total_amount`     DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '商品总额',
  `freight_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '运费',
  `discount_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '优惠金额',
  `pay_amount`       DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '实付金额',
  `user_coupon_id`   BIGINT        DEFAULT NULL            COMMENT '使用的用户优惠券ID',
  `status`           TINYINT       NOT NULL DEFAULT 0      COMMENT '状态 0待付款 1待发货 2待收货 3已完成 4已取消 5售后中',
  `pay_type`         TINYINT       DEFAULT NULL            COMMENT '支付方式 1模拟支付',
  `pay_time`         DATETIME      DEFAULT NULL            COMMENT '支付时间',
  `ship_time`        DATETIME      DEFAULT NULL            COMMENT '发货时间',
  `receive_time`     DATETIME      DEFAULT NULL            COMMENT '收货时间',
  `finish_time`      DATETIME      DEFAULT NULL            COMMENT '完成时间',
  `close_time`       DATETIME      DEFAULT NULL            COMMENT '关闭(取消)时间',
  `receiver_name`    VARCHAR(50)   NOT NULL                COMMENT '收货人(快照)',
  `receiver_phone`   VARCHAR(20)   NOT NULL                COMMENT '收货电话(快照)',
  `receiver_address` VARCHAR(255)  NOT NULL                COMMENT '收货地址(快照)',
  `express_company`  VARCHAR(50)   DEFAULT NULL            COMMENT '快递公司',
  `express_no`       VARCHAR(50)   DEFAULT NULL            COMMENT '快递单号',
  `remark`           VARCHAR(255)  DEFAULT NULL            COMMENT '买家备注',
  `admin_remark`     VARCHAR(255)  DEFAULT NULL            COMMENT '商家备注',
  `item_count`       INT           NOT NULL DEFAULT 0      COMMENT '商品件数',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_no` (`order_no`),
  KEY `idx_orders_user` (`user_id`, `status`),
  KEY `idx_orders_status` (`status`),
  KEY `idx_orders_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';

DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
  `order_id`      BIGINT        NOT NULL                COMMENT '订单ID',
  `order_no`      VARCHAR(32)   NOT NULL                COMMENT '订单号',
  `user_id`       BIGINT        NOT NULL                COMMENT '买家ID',
  `product_id`    BIGINT        NOT NULL                COMMENT '商品ID',
  `sku_id`        BIGINT        NOT NULL                COMMENT 'SKU ID',
  `product_name`  VARCHAR(200)  NOT NULL                COMMENT '商品名称(快照)',
  `product_image` VARCHAR(255)  DEFAULT NULL            COMMENT '商品图片(快照)',
  `spec_info`     VARCHAR(200)  DEFAULT NULL            COMMENT '规格(快照)',
  `price`         DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '成交单价',
  `quantity`      INT           NOT NULL DEFAULT 1      COMMENT '数量',
  `total_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '小计',
  `is_commented`  TINYINT       NOT NULL DEFAULT 0      COMMENT '是否已评价',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_item_order` (`order_id`),
  KEY `idx_item_product` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';

DROP TABLE IF EXISTS `express_track`;
CREATE TABLE `express_track` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '轨迹ID',
  `order_id`        BIGINT       NOT NULL COMMENT '订单ID',
  `order_no`        VARCHAR(32)  NOT NULL COMMENT '订单号',
  `express_company` VARCHAR(50)  DEFAULT NULL COMMENT '快递公司',
  `express_no`      VARCHAR(50)  DEFAULT NULL COMMENT '快递单号',
  `status`          VARCHAR(20)  NOT NULL COMMENT '节点状态 已揽收/运输中/派送中/已签收',
  `context`         VARCHAR(255) NOT NULL COMMENT '轨迹描述',
  `location`        VARCHAR(100) DEFAULT NULL COMMENT '所在城市',
  `accept_time`     DATETIME     NOT NULL COMMENT '节点时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_track_order` (`order_id`),
  KEY `idx_track_no` (`express_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '物流轨迹表';

DROP TABLE IF EXISTS `refund`;
CREATE TABLE `refund` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '售后ID',
  `refund_no`      VARCHAR(32)   NOT NULL                COMMENT '售后单号',
  `order_id`       BIGINT        NOT NULL                COMMENT '订单ID',
  `order_no`       VARCHAR(32)   NOT NULL                COMMENT '订单号',
  `order_item_id`  BIGINT        DEFAULT NULL            COMMENT '订单项ID(单品售后)',
  `user_id`        BIGINT        NOT NULL                COMMENT '买家ID',
  `type`           TINYINT       NOT NULL DEFAULT 1      COMMENT '类型 1仅退款 2退货退款 3换货',
  `reason`         VARCHAR(255)  NOT NULL                COMMENT '申请原因',
  `description`    VARCHAR(500)  DEFAULT NULL            COMMENT '问题描述',
  `images`         VARCHAR(1000) DEFAULT NULL            COMMENT '凭证图片,逗号分隔',
  `refund_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '申请退款金额',
  `status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '状态 0待审核 1已同意 2已拒绝 3待买家退货 4退款完成 5已撤销',
  `audit_user_id`  BIGINT        DEFAULT NULL            COMMENT '审核人ID',
  `audit_user`     VARCHAR(50)   DEFAULT NULL            COMMENT '审核人',
  `audit_remark`   VARCHAR(255)  DEFAULT NULL            COMMENT '审核意见',
  `audit_time`     DATETIME      DEFAULT NULL            COMMENT '审核时间',
  `finish_time`    DATETIME      DEFAULT NULL            COMMENT '完成时间',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_refund_order` (`order_id`),
  KEY `idx_refund_user` (`user_id`),
  KEY `idx_refund_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '售后工单表';

DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
  `name`           VARCHAR(100)  NOT NULL                COMMENT '券名称',
  `type`           TINYINT       NOT NULL DEFAULT 1      COMMENT '类型 1满减 2折扣',
  `amount`         DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '满减金额(type=1)',
  `discount`       DECIMAL(4,2)  NOT NULL DEFAULT 10.00  COMMENT '折扣率(type=2,如8.5表示8.5折)',
  `min_amount`     DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '使用门槛金额',
  `total_count`    INT           NOT NULL DEFAULT 0      COMMENT '发行总量',
  `received_count` INT           NOT NULL DEFAULT 0      COMMENT '已领取数量',
  `used_count`     INT           NOT NULL DEFAULT 0      COMMENT '已使用数量',
  `per_limit`      INT           NOT NULL DEFAULT 1      COMMENT '每人限领',
  `scope`          TINYINT       NOT NULL DEFAULT 1      COMMENT '适用范围 1全场 2指定分类 3指定商品',
  `scope_ids`      VARCHAR(500)  DEFAULT NULL            COMMENT '范围ID,逗号分隔',
  `start_time`     DATETIME      NOT NULL                COMMENT '生效时间',
  `end_time`       DATETIME      NOT NULL                COMMENT '失效时间',
  `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '状态 1启用 0停用',
  `description`    VARCHAR(255)  DEFAULT NULL            COMMENT '使用说明',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_coupon_status` (`status`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '优惠券表';

DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '用户券ID',
  `user_id`      BIGINT      NOT NULL COMMENT '用户ID',
  `coupon_id`    BIGINT      NOT NULL COMMENT '优惠券ID',
  `coupon_name`  VARCHAR(100) NOT NULL COMMENT '券名称(快照)',
  `status`       TINYINT     NOT NULL DEFAULT 0 COMMENT '状态 0未使用 1已使用 2已过期',
  `order_id`     BIGINT      DEFAULT NULL COMMENT '使用的订单ID',
  `receive_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `use_time`     DATETIME    DEFAULT NULL COMMENT '使用时间',
  `expire_time`  DATETIME    NOT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`),
  KEY `idx_uc_user` (`user_id`, `status`),
  KEY `idx_uc_coupon` (`coupon_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户优惠券表';

-- =============================================================================
-- 四、内容与互动
-- =============================================================================

DROP TABLE IF EXISTS `product_comment`;
CREATE TABLE `product_comment` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `product_id`    BIGINT       NOT NULL COMMENT '商品ID',
  `sku_id`        BIGINT       DEFAULT NULL COMMENT 'SKU ID',
  `order_id`      BIGINT       DEFAULT NULL COMMENT '订单ID',
  `order_item_id` BIGINT       DEFAULT NULL COMMENT '订单项ID',
  `user_id`       BIGINT       NOT NULL COMMENT '评价用户ID',
  `user_nickname` VARCHAR(50)  DEFAULT NULL COMMENT '昵称(快照)',
  `user_avatar`   VARCHAR(255) DEFAULT NULL COMMENT '头像(快照)',
  `star`          TINYINT      NOT NULL DEFAULT 5 COMMENT '评分1-5',
  `content`       VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  `images`        VARCHAR(1000) DEFAULT NULL COMMENT '晒图,逗号分隔',
  `is_anonymous`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否匿名',
  `reply_content` VARCHAR(500) DEFAULT NULL COMMENT '商家回复',
  `reply_user`    VARCHAR(50)  DEFAULT NULL COMMENT '回复人',
  `reply_time`    DATETIME     DEFAULT NULL COMMENT '回复时间',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1显示 0隐藏',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cmt_product` (`product_id`, `status`),
  KEY `idx_cmt_user` (`user_id`),
  KEY `idx_cmt_order` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '商品评价表';

DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
  `title`       VARCHAR(100) NOT NULL COMMENT '标题',
  `image`       VARCHAR(255) NOT NULL COMMENT '图片地址',
  `link_url`    VARCHAR(255) DEFAULT NULL COMMENT '跳转链接',
  `position`    TINYINT      NOT NULL DEFAULT 1 COMMENT '位置 1商城首页 2分类页',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序值',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_banner_position` (`position`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '首页轮播图表';

DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title`        VARCHAR(200) NOT NULL COMMENT '标题',
  `content`      LONGTEXT     NOT NULL COMMENT '正文',
  `type`         TINYINT      NOT NULL DEFAULT 1 COMMENT '类型 1平台公告 2促销活动 3帮助中心',
  `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 1已发布 0草稿',
  `publish_time` DATETIME     DEFAULT NULL COMMENT '发布时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_notice_status` (`status`, `type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '公告表';

-- =============================================================================
-- 五、系统管理（后台）
-- =============================================================================

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username`        VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password`        VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  `real_name`       VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  `avatar`          VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `email`           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `role_id`         BIGINT       NOT NULL COMMENT '角色ID',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `last_login_time` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip`   VARCHAR(50)  DEFAULT NULL COMMENT '最后登录IP',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_role` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '后台管理员表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code`   VARCHAR(50)  NOT NULL COMMENT '角色编码 ADMIN/OPERATOR/WAREHOUSE',
  `role_name`   VARCHAR(50)  NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '后台角色表';

DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级ID,0为顶级',
  `name`        VARCHAR(50)  NOT NULL COMMENT '名称',
  `perms`       VARCHAR(100) DEFAULT NULL COMMENT '权限标识 如 product:list',
  `type`        TINYINT      NOT NULL DEFAULT 2 COMMENT '类型 1目录 2菜单 3按钮',
  `path`        VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径',
  `component`   VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
  `icon`        VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序值',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1启用 0停用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_perm_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '权限菜单表';

DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`       BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限关联表';

DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key`   VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_name`  VARCHAR(100) NOT NULL COMMENT '配置名称',
  `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统配置表';

DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`     BIGINT       DEFAULT NULL COMMENT '操作人ID',
  `username`    VARCHAR(50)  DEFAULT NULL COMMENT '操作人',
  `module`      VARCHAR(50)  DEFAULT NULL COMMENT '模块',
  `operation`   VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
  `method`      VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
  `request_uri` VARCHAR(255) DEFAULT NULL COMMENT '请求URI',
  `params`      TEXT         DEFAULT NULL COMMENT '请求参数',
  `ip`          VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
  `cost_time`   BIGINT       DEFAULT 0 COMMENT '耗时(ms)',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
  `error_msg`   TEXT         DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_log_create_time` (`create_time`),
  KEY `idx_log_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '后台操作日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 结构创建完成：共 24 张表
--   user, user_address, favorite, browse_history,
--   category, product, product_spec, product_sku,
--   cart, orders, order_item, express_track, refund, coupon, user_coupon,
--   product_comment, banner, notice,
--   sys_user, sys_role, sys_permission, sys_role_permission, sys_config, sys_oper_log
-- =============================================================================
