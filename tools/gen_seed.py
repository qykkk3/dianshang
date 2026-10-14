# -*- coding: utf-8 -*-
"""
电商管理系统 演示数据生成器
------------------------------------------------------------------
运行:  python tools/gen_seed.py
产出:
  1) sql/02-data.sql                  —— 全部演示数据（可重复执行）
  2) backend/uploads/**/*.svg         —— 商品图/轮播图/头像占位图（本地磁盘，无需外网）

设计要点:
  * 商品销量、库存由订单数据反推，保证看板/热销榜与订单一致
  * 订单覆盖 0待付款/1待发货/2待收货/3已完成/4已取消/5售后中 全部状态
  * 密码哈希从 tools/pwd_hash.txt 读取（由 Java BCrypt 生成），保证与后端校验一致
------------------------------------------------------------------
"""
import os
import random
from datetime import datetime, timedelta

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SQL_OUT = os.path.join(BASE, 'sql', '02-data.sql')
UPLOAD = os.path.join(BASE, 'backend', 'uploads')
HASH_FILE = os.path.join(BASE, 'tools', 'pwd_hash.txt')

random.seed(20261013)
NOW = datetime(2026, 10, 13, 15, 0, 0)

# ---------------------------------------------------------------- 密码哈希
FALLBACK_HASH = '$2a$10$PLACEHOLDER.PLACEHOLDER.PLACEHOLDER.PLACEHOLDER.PLACEHOLD'
if os.path.exists(HASH_FILE):
    with open(HASH_FILE, encoding='utf-8') as f:
        PWD = f.read().strip()
else:  # pragma: no cover - 仅在未生成哈希时兜底
    PWD = FALLBACK_HASH
    print('!! 未找到 tools/pwd_hash.txt，密码哈希使用占位符')

# ---------------------------------------------------------------- 工具函数
def esc(v):
    """SQL 字符串转义"""
    if v is None:
        return 'NULL'
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, datetime):
        return "'" + v.strftime('%Y-%m-%d %H:%M:%S') + "'"
    return "'" + str(v).replace('\\', '\\\\').replace("'", "''") + "'"


def money(v):
    return f'{round(float(v) + 1e-9, 2):.2f}'


def dt(v):
    return "'" + v.strftime('%Y-%m-%d %H:%M:%S') + "'"


def rand_dt(start, end):
    delta = int((end - start).total_seconds())
    return start + timedelta(seconds=random.randint(0, max(delta, 1)))


def svg_write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)


def product_svg(name, sub, c1, c2, tag):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="800" height="800" viewBox="0 0 800 800">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="100%" stop-color="{c2}"/>
    </linearGradient>
  </defs>
  <rect width="800" height="800" fill="url(#g)"/>
  <circle cx="640" cy="160" r="180" fill="#ffffff" opacity="0.10"/>
  <circle cx="150" cy="680" r="220" fill="#ffffff" opacity="0.08"/>
  <text x="400" y="330" font-family="Microsoft YaHei,SimHei,sans-serif" font-size="46" fill="#ffffff"
        text-anchor="middle" font-weight="bold">{name}</text>
  <text x="400" y="400" font-family="Microsoft YaHei,SimHei,sans-serif" font-size="26" fill="#ffffff"
        text-anchor="middle" opacity="0.85">{sub}</text>
  <text x="400" y="700" font-family="Microsoft YaHei,SimHei,sans-serif" font-size="22" fill="#ffffff"
        text-anchor="middle" opacity="0.75">{tag}</text>
</svg>'''


def banner_svg(title, sub, c1, c2):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="420" viewBox="0 0 1200 420">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="100%" stop-color="{c2}"/>
    </linearGradient>
  </defs>
  <rect width="1200" height="420" fill="url(#g)"/>
  <circle cx="1050" cy="90" r="200" fill="#ffffff" opacity="0.10"/>
  <circle cx="180" cy="360" r="160" fill="#ffffff" opacity="0.08"/>
  <text x="80" y="200" font-family="Microsoft YaHei,SimHei,sans-serif" font-size="64" fill="#ffffff" font-weight="bold">{title}</text>
  <text x="80" y="270" font-family="Microsoft YaHei,SimHei,sans-serif" font-size="30" fill="#ffffff" opacity="0.9">{sub}</text>
</svg>'''


def avatar_svg(text, c1, c2):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="100%" stop-color="{c2}"/>
    </linearGradient>
  </defs>
  <rect width="200" height="200" fill="url(#g)"/>
  <text x="100" y="128" font-family="Microsoft YaHei,SimHei,sans-serif" font-size="80" fill="#ffffff"
        text-anchor="middle" font-weight="bold">{text}</text>
</svg>'''


# =================================================================
# 1. 分类（10 个：4 个一级 + 6 个二级）
# =================================================================
CATEGORIES = [
    # id, parent, name, level, sort, icon
    (1, 0, '数码电器', 1, 1, 'Monitor'),
    (2, 0, '服饰鞋包', 1, 2, 'ShoppingBag'),
    (3, 0, '食品生鲜', 1, 3, 'Food'),
    (4, 0, '家居日用', 1, 4, 'HomeFilled'),
    (5, 1, '手机通讯', 2, 1, None),
    (6, 1, '笔记本电脑', 2, 2, None),
    (7, 2, '男装', 2, 1, None),
    (8, 2, '女装', 2, 2, None),
    (9, 3, '休闲零食', 2, 1, None),
    (10, 4, '厨房用品', 2, 2, None),
]

# =================================================================
# 2. 商品定义（24 个，含多规格）
#    结构: (分类, 名称, 副标题, 品牌, 规格定义{规格名:[值...]}, SKU单价矩阵, 原价系数)
# =================================================================
PHONE_COLORS = ['曜石黑', '月光银', '晴空蓝']
PRODUCTS_DEF = [
    # ---------------- 手机通讯 (cat 5) ----------------
    dict(cat=5, name='星辰 X9 Pro 5G 智能手机', sub='骁龙旗舰芯片 | 2K 曲面屏 | 5000mAh',
         brand='星辰', unit='台', specs={'颜色': PHONE_COLORS, '存储': ['12GB+256GB', '16GB+512GB']},
         base=3299, step=600, colors=('#2b3a67', '#5c6bc0')),
    dict(cat=5, name='星辰 X9 5G 智能手机', sub='轻薄机身 | 66W 快充 | 一亿像素',
         brand='星辰', unit='台', specs={'颜色': ['曜石黑', '晴空蓝'], '存储': ['8GB+128GB', '12GB+256GB']},
         base=1999, step=400, colors=('#14532d', '#22c55e')),
    dict(cat=5, name='极光 Note 12 长续航手机', sub='6000mAh 大电池 | 学生优选',
         brand='极光', unit='台', specs={'颜色': ['幻夜黑', '冰川蓝'], '存储': ['8GB+128GB', '8GB+256GB']},
         base=1299, step=300, colors=('#7c2d12', '#fb923c')),
    dict(cat=5, name='极光 Play 6 入门智能手机', sub='高清大屏 | 双卡双待 | 老人机优选',
         brand='极光', unit='台', specs={'颜色': ['极光绿', '星耀黑'], '存储': ['6GB+128GB', '8GB+256GB']},
         base=899, step=300, colors=('#134e4a', '#2dd4bf')),
    # ---------------- 笔记本电脑 (cat 6) ----------------
    dict(cat=6, name='星辰 Book Air 14 轻薄本', sub='1.2kg 轻薄 | 2.8K 护眼屏 | 长续航',
         brand='星辰', unit='台', specs={'颜色': ['深空灰', '星光银'], '内存': ['16GB+512GB', '32GB+1TB']},
         base=4999, step=1000, colors=('#334155', '#94a3b8')),
    dict(cat=6, name='星辰 Book Pro 16 创作本', sub='RTX 独显 | 4K 高色域 | 专业创作',
         brand='星辰', unit='台', specs={'颜色': ['深空灰'], '内存': ['32GB+1TB', '32GB+2TB']},
         base=8999, step=2000, colors=('#1e1b4b', '#6366f1')),
    dict(cat=6, name='极光 MagicBook X 商务本', sub='指纹解锁 | 全尺寸键盘 | 商务办公',
         brand='极光', unit='台', specs={'颜色': ['银色', '深灰色'], '内存': ['16GB+512GB', '16GB+1TB']},
         base=4299, step=600, colors=('#0f172a', '#475569')),
    dict(cat=6, name='极光 GameBook 15 游戏本', sub='165Hz 电竞屏 | 双风扇散热 | 高帧畅玩',
         brand='极光', unit='台', specs={'颜色': ['曜石黑'], '内存': ['16GB+1TB', '32GB+1TB']},
         base=7499, step=1500, colors=('#450a0a', '#ef4444')),
    # ---------------- 男装 (cat 7) ----------------
    dict(cat=7, name='简约纯棉圆领短袖T恤', sub='新疆长绒棉 | 不易变形 | 三色可选',
         brand='简致', unit='件', specs={'颜色': ['白色', '黑色', '藏青'], '尺码': ['M', 'L', 'XL', 'XXL']},
         base=89, step=0, colors=('#0c4a6e', '#38bdf8')),
    dict(cat=7, name='商务免烫长袖衬衫', sub='免烫抗皱 | 修身版型 | 通勤必备',
         brand='简致', unit='件', specs={'颜色': ['白色', '浅蓝'], '尺码': ['M', 'L', 'XL', 'XXL']},
         base=159, step=0, colors=('#1e293b', '#64748b')),
    dict(cat=7, name='春秋休闲夹克外套', sub='防风面料 | 立领设计 | 多口袋',
         brand='简致', unit='件', specs={'颜色': ['黑色', '军绿', '卡其'], '尺码': ['L', 'XL', 'XXL']},
         base=299, step=0, colors=('#3f3f1f', '#a3a34d')),
    dict(cat=7, name='直筒弹力牛仔裤', sub='弹力舒适 | 不易掉色 | 直筒显瘦',
         brand='简致', unit='件', specs={'颜色': ['深蓝', '黑色'], '尺码': ['29', '30', '31', '32']},
         base=199, step=0, colors=('#172554', '#3b82f6')),
    # ---------------- 女装 (cat 8) ----------------
    dict(cat=8, name='法式碎花连衣裙', sub='收腰显瘦 | 垂感面料 | 度假通勤两穿',
         brand='云裳', unit='件', specs={'颜色': ['米白', '浅蓝'], '尺码': ['S', 'M', 'L']},
         base=259, step=0, colors=('#831843', '#f472b6')),
    dict(cat=8, name='慵懒风针织开衫毛衣', sub='柔软亲肤 | 不起球 | 三色可选',
         brand='云裳', unit='件', specs={'颜色': ['燕麦色', '雾霾蓝', '黑色'], '尺码': ['S', 'M', 'L']},
         base=189, step=0, colors=('#78350f', '#fbbf24')),
    dict(cat=8, name='高腰阔腿西装裤', sub='垂顺有型 | 显高显瘦 | 通勤百搭',
         brand='云裳', unit='件', specs={'颜色': ['黑色', '杏色'], '尺码': ['S', 'M', 'L', 'XL']},
         base=219, step=0, colors=('#44403c', '#a8a29e')),
    dict(cat=8, name='纯棉宽松卫衣', sub='加绒可选 | 落肩宽松 | 情侣同款',
         brand='云裳', unit='件', specs={'颜色': ['白色', '灰色', '粉色'], '尺码': ['M', 'L', 'XL']},
         base=149, step=0, colors=('#7f1d1d', '#fca5a5')),
    # ---------------- 休闲零食 (cat 9) ----------------
    dict(cat=9, name='每日坚果混合装', sub='7 种坚果果干 | 独立小包 | 新鲜锁鲜',
         brand='果之选', unit='袋', specs={'规格': ['500g', '1kg', '2kg']},
         base=39, step=30, colors=('#78350f', '#d97706')),
    dict(cat=9, name='手工黑糖曲奇饼干', sub='黄油香浓 | 酥脆掉渣 | 下午茶点心',
         brand='甜研社', unit='盒', specs={'规格': ['260g', '520g']},
         base=29, step=20, colors=('#7c2d12', '#f59e0b')),
    dict(cat=9, name='云南小粒咖啡豆', sub='中度烘焙 | 现磨手冲 | 果酸回甘',
         brand='云咖', unit='袋', specs={'规格': ['250g', '500g', '1kg']},
         base=59, step=49, colors=('#3f2212', '#b45309')),
    dict(cat=9, name='冻干草莓脆', sub='FD 冻干工艺 | 无添加蔗糖 | 酸甜可口',
         brand='果之选', unit='袋', specs={'规格': ['100g', '250g']},
         base=25, step=30, colors=('#881337', '#fb7185')),
    # ---------------- 厨房用品 (cat 10) ----------------
    dict(cat=10, name='麦饭石不粘炒锅', sub='无油烟 | 不粘易清洗 | 燃气电磁通用',
         brand='厨匠', unit='口', specs={'颜色': ['灰色', '蓝色'], '尺寸': ['28cm', '30cm', '32cm']},
         base=129, step=20, colors=('#1f2937', '#6b7280')),
    dict(cat=10, name='316 不锈钢保温杯', sub='12 小时保温 | 食品级内胆 | 便携提绳',
         brand='厨匠', unit='个', specs={'颜色': ['白色', '黑色', '蓝色'], '容量': ['400ml', '500ml']},
         base=89, step=20, colors=('#0c4a6e', '#0ea5e9')),
    dict(cat=10, name='北欧陶瓷餐具四件套', sub='釉下彩工艺 | 可进微波炉 | 送礼佳选',
         brand='厨匠', unit='套', specs={'颜色': ['纯白', '青花'], '规格': ['单人食', '双人食']},
         base=79, step=60, colors=('#134e4a', '#5eead4')),
    dict(cat=10, name='多功能硅胶铲勺套装', sub='耐高温 230℃ | 不伤锅 | 悬挂收纳',
         brand='厨匠', unit='套', specs={'颜色': ['灰色', '绿色'], '规格': ['5 件套', '8 件套']},
         base=39, step=30, colors=('#365314', '#84cc16')),
]

# 生成 SKU：两个规格做笛卡尔积，单个规格直接展开
products, specs, skus = [], [], []
sku_id = 1
for idx, d in enumerate(PRODUCTS_DEF, start=1):
    pid = idx
    main_img = f'/api/files/product/p{pid}_1.svg'
    images = ','.join(f'/api/files/product/p{pid}_{n}.svg' for n in (1, 2, 3))
    detail = (
        f'<h3>{d["name"]}</h3>'
        f'<p>{d["sub"]}</p>'
        f'<p><img src="{main_img}" alt="{d["name"]}" style="max-width:100%"/></p>'
        '<ul><li>正品保障，全国联保</li><li>七天无理由退换货</li><li>满 99 元包邮，闪电发货</li></ul>'
    )
    # 写占位图
    for n in (1, 2, 3):
        svg_write(os.path.join(UPLOAD, 'product', f'p{pid}_{n}.svg'),
                  product_svg(d['name'], d['sub'], d['colors'][0], d['colors'][1],
                              f'星辰商城 · 商品图 {n}/3'))
    # 规格定义
    spec_names = list(d['specs'].keys())
    for si, sn in enumerate(spec_names):
        specs.append(dict(id=len(specs) + 1, product_id=pid, spec_name=sn,
                          spec_values=','.join(d['specs'][sn]), sort=si + 1))
    # SKU 组合
    combos = []
    if len(spec_names) == 2:
        for v1 in d['specs'][spec_names[0]]:
            for v2 in d['specs'][spec_names[1]]:
                combos.append([(spec_names[0], v1), (spec_names[1], v2)])
    else:
        for v1 in d['specs'][spec_names[0]]:
            combos.append([(spec_names[0], v1)])

    sku_list = []
    for ci, combo in enumerate(combos):
        price = d['base'] + d['step'] * (ci // max(1, len(d['specs'][spec_names[0]])) if len(spec_names) == 2 else ci)
        if len(spec_names) == 1:
            price = d['base'] + d['step'] * ci
        price = round(price, 2)
        sku_list.append(dict(
            id=sku_id, product_id=pid,
            sku_code=f'P{pid:03d}-{ci + 1:02d}',
            spec_data='{' + ','.join(f'"{k}":"{v}"' for k, v in combo) + '}',
            spec_info=' / '.join(v for _, v in combo),
            price=price, original_price=round(price * 1.25, 2),
            stock=random.choice([0, 5, 8, 35, 60, 120, 260, 480]),
            sales=0, image=main_img, status=1))
        sku_id += 1
    stock_sum = sum(s['stock'] for s in sku_list)
    products.append(dict(
        id=pid, category_id=d['cat'], name=d['name'], subtitle=d['sub'], brand=d['brand'],
        unit=d['unit'], main_image=main_img, images=images, detail=detail,
        price=min(s['price'] for s in sku_list),
        original_price=min(s['original_price'] for s in sku_list),
        cost_price=round(min(s['price'] for s in sku_list) * 0.62, 2),
        stock=stock_sum, sales=0, warn_stock=10, status=1,
        is_recommend=1 if idx % 3 == 0 else 0,
        is_new=1 if idx % 4 == 0 else 0,
        is_hot=1 if idx % 5 == 0 else 0, sort=idx))
    skus.extend(sku_list)

# =================================================================
# 3. 买家 / 地址 / 收藏 / 浏览记录 / 购物车
# =================================================================
BUYERS = [
    ('zhangsan', '张三', 1, '13800138001', 'zhangsan@mall.com', '#1d4ed8', '#3b82f6'),
    ('lisi', '李四', 2, '13800138002', 'lisi@mall.com', '#be185d', '#f472b6'),
    ('wangwu', '王五', 1, '13800138003', 'wangwu@mall.com', '#047857', '#34d399'),
    ('zhaoliu', '赵六', 2, '13800138004', 'zhaoliu@mall.com', '#7c2d12', '#fb923c'),
    ('sunqi', '孙七', 1, '13800138005', 'sunqi@mall.com', '#4c1d95', '#a78bfa'),
    ('zhouba', '周八', 2, '13800138006', 'zhouba@mall.com', '#0f766e', '#2dd4bf'),
]
PROVINCES = [
    ('广东省', '深圳市', '南山区', '科技园南区深南大道 10000 号'),
    ('北京市', '北京市', '海淀区', '中关村大街 27 号'),
    ('上海市', '上海市', '浦东新区', '张江高科技园区博云路 2 号'),
    ('浙江省', '杭州市', '西湖区', '文三路 90 号东部软件园'),
    ('四川省', '成都市', '武侯区', '天府大道北段 1480 号'),
    ('江苏省', '南京市', '鼓楼区', '中山北路 105 号'),
]
users, addresses = [], []
addr_id = 1
for uid, (un, nick, gender, phone, mail, c1, c2) in enumerate(BUYERS, start=1):
    avatar = f'/api/files/avatar/u{uid}.svg'
    svg_write(os.path.join(UPLOAD, 'avatar', f'u{uid}.svg'), avatar_svg(nick[0], c1, c2))
    users.append(dict(id=uid, username=un, password=PWD, nickname=nick, phone=phone,
                      email=mail, avatar=avatar, gender=gender,
                      birthday=f'199{uid}-0{uid % 9 + 1}-1{uid}',
                      status=1,
                      last_login_time=NOW - timedelta(hours=random.randint(1, 200)),
                      create_time=NOW - timedelta(days=random.randint(60, 400))))
    for ai in range(random.randint(1, 3)):
        p, c, dist, det = random.choice(PROVINCES)
        addresses.append(dict(id=addr_id, user_id=uid, receiver=nick, phone=phone,
                              province=p, city=c, district=dist,
                              detail=f'{det} {random.randint(1, 30)} 栋 {random.randint(101, 2508)} 室',
                              post_code=f'{random.randint(100000, 618000)}',
                              is_default=1 if ai == 0 else 0))
        addr_id += 1

fav_id, favorites = 1, []
for u in users:
    for pid in random.sample([p['id'] for p in products], random.randint(2, 6)):
        favorites.append(dict(id=fav_id, user_id=u['id'], product_id=pid,
                              create_time=NOW - timedelta(days=random.randint(1, 60))))
        fav_id += 1

hist_id, histories = 1, []
for u in users:
    for pid in random.sample([p['id'] for p in products], random.randint(4, 10)):
        histories.append(dict(id=hist_id, user_id=u['id'], product_id=pid,
                              create_time=NOW - timedelta(days=random.randint(1, 30))))
        hist_id += 1

cart_id, carts = 1, []
for uid in (1, 2, 3):
    for s in random.sample(skus, random.randint(2, 4)):
        carts.append(dict(id=cart_id, user_id=uid, product_id=s['product_id'], sku_id=s['id'],
                          quantity=random.randint(1, 3), checked=random.choice([0, 1]),
                          create_time=NOW - timedelta(days=random.randint(1, 10))))
        cart_id += 1

# =================================================================
# 4. 优惠券
# =================================================================
COUPONS = [
    dict(id=1, name='新人专享 满100减20', type=1, amount=20, discount=10.00, min_amount=100,
         total_count=1000, per_limit=1, scope=1, scope_ids=None,
         start_time=NOW - timedelta(days=30), end_time=NOW + timedelta(days=60),
         status=1, description='新用户注册即可领取，全场通用'),
    dict(id=2, name='数码专场 满3000减300', type=1, amount=300, discount=10.00, min_amount=3000,
         total_count=500, per_limit=1, scope=2, scope_ids='1,5,6',
         start_time=NOW - timedelta(days=10), end_time=NOW + timedelta(days=20),
         status=1, description='仅限数码电器分类使用'),
    dict(id=3, name='全场 满500减50', type=1, amount=50, discount=10.00, min_amount=500,
         total_count=2000, per_limit=2, scope=1, scope_ids=None,
         start_time=NOW - timedelta(days=5), end_time=NOW + timedelta(days=25),
         status=1, description='全场通用，每人限领 2 张'),
    dict(id=4, name='全场 9 折优惠券', type=2, amount=0, discount=9.00, min_amount=200,
         total_count=800, per_limit=1, scope=1, scope_ids=None,
         start_time=NOW - timedelta(days=3), end_time=NOW + timedelta(days=15),
         status=1, description='订单金额满 200 元可用，最高优惠 100 元'),
    dict(id=5, name='零食生鲜 满50减10', type=1, amount=10, discount=10.00, min_amount=50,
         total_count=300, per_limit=3, scope=2, scope_ids='3,9',
         start_time=NOW - timedelta(days=90), end_time=NOW - timedelta(days=30),
         status=0, description='已过期活动（用于演示过期状态）'),
]
for c in COUPONS:
    c['received_count'] = random.randint(10, 200)
    c['used_count'] = random.randint(1, max(2, c['received_count'] // 3))

uc_id, user_coupons = 1, []
for u in users:
    for c in random.sample(COUPONS[:4], random.randint(1, 3)):
        received = NOW - timedelta(days=random.randint(1, 20))
        st, use_time, order_id = 0, None, None
        if random.random() < 0.35:
            st = 1
            use_time = received + timedelta(days=random.randint(1, 5))
        elif random.random() < 0.15:
            st = 2
        user_coupons.append(dict(id=uc_id, user_id=u['id'], coupon_id=c['id'],
                                 coupon_name=c['name'], status=st, order_id=order_id,
                                 receive_time=received, use_time=use_time,
                                 expire_time=c['end_time']))
        uc_id += 1

# =================================================================
# 5. 订单（40 笔，覆盖全部状态）+ 明细 + 物流 + 售后 + 评价
# =================================================================
STATUS_PLAN = ([0] * 5) + ([1] * 8) + ([2] * 8) + ([3] * 12) + ([4] * 4) + ([5] * 3)
random.shuffle(STATUS_PLAN)
EXPRESS = ['顺丰速运', '中通快递', '圆通速递', '京东物流', '韵达快递']
CITIES = ['深圳市', '广州市', '上海市', '杭州市', '北京市', '成都市', '南京市', '武汉市', '西安市']
COMMENT_TEXT = [
    '东西很不错，包装严实，物流也快，好评！', '质量比想象中好，做工细节到位，会回购的。',
    '性价比很高，用了一周没有出现问题，推荐。', '和描述一致，卖家发货速度快，服务态度好。',
    '整体满意，就是快递稍微慢了一点，其他都很好。', '第二次购买了，一如既往地稳定，五星好评。',
    '包装有点简陋，但商品本身没问题，价格实惠。', '收到货就迫不及待试用了，效果超出预期。',
    '送给家人的，说很好用，感谢商家的赠品。', '细节做得不错，客服也很有耐心，值得推荐。',
]
REPLY_TEXT = ['感谢您的支持，我们会继续努力提供更好的产品和服务！', '亲的认可是我们最大的动力，期待再次光临～',
              '感谢反馈，我们会持续优化包装与物流体验。']
REFUND_REASON = ['不想要了', '商品与描述不符', '尺码不合适', '质量问题', '发错货了', '快递太慢']

orders, order_items, tracks, refunds, comments = [], [], [], [], []
oid, oitem_id, track_id, refund_id, cmt_id = 1, 1, 1, 1, 1
sold_by_sku = {s['id']: 0 for s in skus}
base_sales = {s['id']: random.randint(0, 300) for s in skus}

for status in STATUS_PLAN:
    u = random.choice(users)
    addr = next(a for a in addresses if a['user_id'] == u['id'] and a['is_default'] == 1)
    create_time = rand_dt(NOW - timedelta(days=45), NOW - timedelta(hours=2))
    order_no = f'MO{create_time.strftime("%Y%m%d%H%M%S")}{oid:04d}'

    items = []
    for s in random.sample(skus, random.randint(1, 3)):
        qty = random.randint(1, 3)
        p = next(p for p in products if p['id'] == s['product_id'])
        items.append(dict(sku=s, product=p, qty=qty, price=s['price'],
                          total=round(s['price'] * qty, 2)))
    total_amount = round(sum(i['total'] for i in items), 2)
    freight = 0.00 if total_amount >= 99 else 10.00

    # ---- 优惠券抵扣
    discount, use_uc = 0.00, None
    if status != 4 and random.random() < 0.3:
        cands = [uc for uc in user_coupons if uc['user_id'] == u['id'] and uc['status'] == 0
                 and uc['coupon_id'] in (1, 2, 3, 4)]
        cands = [uc for uc in cands
                 if next(c for c in COUPONS if c['id'] == uc['coupon_id'])['min_amount'] <= total_amount]
        if cands:
            use_uc = random.choice(cands)
            c = next(c for c in COUPONS if c['id'] == use_uc['coupon_id'])
            discount = round(c['amount'], 2) if c['type'] == 1 else round(total_amount * (1 - c['discount'] / 10), 2)
            discount = min(discount, total_amount)
    pay_amount = round(max(total_amount + freight - discount, 0.01), 2)

    pay_time = ship_time = receive_time = finish_time = close_time = None
    express_company = express_no = None
    if status in (1, 5):
        pay_time = create_time + timedelta(minutes=random.randint(1, 60))
    elif status == 2:
        pay_time = create_time + timedelta(minutes=random.randint(1, 60))
        ship_time = pay_time + timedelta(hours=random.randint(2, 24))
        express_company = random.choice(EXPRESS)
        express_no = f'SF{random.randint(100000000000, 999999999999)}'
    elif status == 3:
        pay_time = create_time + timedelta(minutes=random.randint(1, 60))
        ship_time = pay_time + timedelta(hours=random.randint(2, 24))
        receive_time = ship_time + timedelta(days=random.randint(1, 5))
        finish_time = receive_time
        express_company = random.choice(EXPRESS)
        express_no = f'SF{random.randint(100000000000, 999999999999)}'
    elif status == 4:
        close_time = create_time + timedelta(hours=random.randint(1, 24))

    order = dict(id=oid, order_no=order_no, user_id=u['id'], total_amount=total_amount,
                 freight_amount=freight, discount_amount=discount, pay_amount=pay_amount,
                 user_coupon_id=use_uc['id'] if use_uc else None, status=status,
                 pay_type=1 if pay_time else None, pay_time=pay_time, ship_time=ship_time,
                 receive_time=receive_time, finish_time=finish_time, close_time=close_time,
                 receiver_name=addr['receiver'], receiver_phone=addr['phone'],
                 receiver_address=f"{addr['province']}{addr['city']}{addr['district']}{addr['detail']}",
                 express_company=express_company, express_no=express_no,
                 remark=random.choice([None, None, '请尽快发货，谢谢！', '工作日白天送货', '需要发票']),
                 admin_remark=None, item_count=sum(i['qty'] for i in items),
                 create_time=create_time)
    orders.append(order)

    if use_uc:
        use_uc['status'] = 1
        use_uc['order_id'] = oid
        use_uc['use_time'] = pay_time or create_time

    for i in items:
        order_items.append(dict(id=oitem_id, order_id=oid, order_no=order_no, user_id=u['id'],
                                product_id=i['product']['id'], sku_id=i['sku']['id'],
                                product_name=i['product']['name'],
                                product_image=i['product']['main_image'],
                                spec_info=i['sku']['spec_info'], price=i['price'],
                                quantity=i['qty'], total_amount=i['total'], is_commented=0))
        if status in (1, 2, 3, 5):
            sold_by_sku[i['sku']['id']] += i['qty']
        oitem_id += 1

    # ---- 物流轨迹
    if express_no:
        nodes = [('已揽收', f'快件已由{express_company}揽收', random.choice(CITIES), ship_time)]
        cur = ship_time
        for _ in range(random.randint(2, 3)):
            cur = cur + timedelta(hours=random.randint(4, 14))
            nodes.append(('运输中', f'快件已到达【{random.choice(CITIES)}转运中心】', random.choice(CITIES), cur))
        cur = cur + timedelta(hours=random.randint(6, 18))
        nodes.append(('派送中', '快递员正在派送，请保持电话畅通', random.choice(CITIES), cur))
        if status == 3:
            cur = cur + timedelta(hours=random.randint(1, 8))
            nodes.append(('已签收', '您的快件已签收，感谢使用', random.choice(CITIES), cur))
        for st, ctx, loc, tm in nodes:
            tracks.append(dict(id=track_id, order_id=oid, order_no=order_no,
                               express_company=express_company, express_no=express_no,
                               status=st, context=ctx, location=loc, accept_time=tm))
            track_id += 1

    # ---- 售后工单
    if status == 5:
        item = random.choice([i for i in order_items if i['order_id'] == oid])
        rstatus = random.choice([0, 1, 2, 3, 4])
        rtype = random.choice([1, 2, 3])
        audit_time = create_time + timedelta(days=1) if rstatus != 0 else None
        refunds.append(dict(id=refund_id, refund_no=f'RF{create_time.strftime("%Y%m%d%H%M%S")}{refund_id:04d}',
                            order_id=oid, order_no=order_no, order_item_id=item['id'],
                            user_id=u['id'], type=rtype, reason=random.choice(REFUND_REASON),
                            description='商品存在瑕疵，申请售后处理，希望尽快回复。',
                            images=None, refund_amount=item['total_amount'], status=rstatus,
                            audit_user_id=2 if audit_time else None,
                            audit_user='运营小美' if audit_time else None,
                            audit_remark=random.choice(['同意买家申请，请寄回商品', '已与买家协商一致',
                                                        '不符合退款条件，已驳回']) if audit_time else None,
                            audit_time=audit_time,
                            finish_time=audit_time + timedelta(days=2) if rstatus == 4 else None,
                            create_time=create_time + timedelta(days=1)))
        refund_id += 1

    # ---- 评价（已完成订单）
    if status == 3:
        for it in [i for i in order_items if i['order_id'] == oid]:
            if random.random() < 0.85:
                star = random.choices([5, 4, 3, 2], weights=[60, 25, 10, 5])[0]
                has_reply = random.random() < 0.5
                reply_time = receive_time + timedelta(days=random.randint(1, 3)) if has_reply else None
                comments.append(dict(id=cmt_id, product_id=it['product_id'], sku_id=it['sku_id'],
                                     order_id=oid, order_item_id=it['id'], user_id=u['id'],
                                     user_nickname=('匿名用户' if random.random() < 0.15 else u['nickname']),
                                     user_avatar=u['avatar'], star=star,
                                     content=random.choice(COMMENT_TEXT),
                                     images=None,
                                     is_anonymous=1 if random.random() < 0.15 else 0,
                                     reply_content=random.choice(REPLY_TEXT) if has_reply else None,
                                     reply_user='运营小美' if has_reply else None,
                                     reply_time=reply_time, status=1,
                                     create_time=receive_time + timedelta(days=random.randint(0, 2))))
                it['is_commented'] = 1
                cmt_id += 1

    oid += 1

# 销量/库存回填
for p in products:
    ps = [s for s in skus if s['product_id'] == p['id']]
    p['sales'] = sum(base_sales[s['id']] + sold_by_sku[s['id']] for s in ps)
    p['stock'] = sum(s['stock'] for s in ps)
for s in skus:
    s['sales'] = base_sales[s['id']] + sold_by_sku[s['id']]

# =================================================================
# 6. 轮播图 / 公告
# =================================================================
BANNERS = [
    ('星辰 X9 Pro 新品首发', '旗舰影像 · 2K 曲面屏 · 限时立减 300', '#0f172a', '#2563eb', '/shop/product/1'),
    ('数码焕新季 满3000减300', '手机 / 笔记本 全场钜惠', '#134e4a', '#10b981', '/shop/category/5'),
    ('秋冬新衣 低至 5 折', '男装女装 全场包邮', '#7f1d1d', '#f97316', '/shop/category/7'),
    ('零食囤货节 满50减10', '每日坚果 · 冻干水果 · 精品咖啡', '#78350f', '#fbbf24', '/shop/category/9'),
    ('厨房好物 品质生活', '不粘锅 / 保温杯 / 餐具套装', '#1e293b', '#64748b', '/shop/category/10'),
]
banners = []
for i, (t, s, c1, c2, link) in enumerate(BANNERS, start=1):
    img = f'/api/files/banner/b{i}.svg'
    svg_write(os.path.join(UPLOAD, 'banner', f'b{i}.svg'), banner_svg(t, s, c1, c2))
    banners.append(dict(id=i, title=t, image=img, link_url=link, position=1, sort=i, status=1))

NOTICES = [
    ('平台服务升级公告', '为提升购物体验，平台将于本周日凌晨 2:00-4:00 进行系统升级，期间下单功能可能短暂受影响，敬请谅解。', 1, 1),
    ('双十一预售活动规则', '活动时间：10 月 20 日 - 11 月 11 日。预售定金膨胀、跨店满减、限时秒杀等多重玩法，详情见活动页。', 2, 1),
    ('关于运费规则的说明', '全场订单满 99 元包邮，未满 99 元收取 10 元运费。偏远地区（新疆、西藏等）运费另计。', 1, 1),
    ('七天无理由退换货政策', '自签收之日起 7 天内，商品不影响二次销售的前提下，支持无理由退换货，退换货运费由责任方承担。', 3, 1),
    ('新用户注册领券说明', '新用户注册成功后可在「我的优惠券」中领取新人专享满 100 减 20 优惠券，有效期 60 天。', 3, 0),
]
notices = [dict(id=i, title=t, content=c, type=ty, status=st,
                publish_time=NOW - timedelta(days=i * 3) if st == 1 else None)
           for i, (t, c, ty, st) in enumerate(NOTICES, start=1)]

# =================================================================
# 7. 系统管理：角色 / 权限 / 管理员 / 配置 / 日志
# =================================================================
ROLES = [
    (1, 'ADMIN', '超级管理员', '拥有系统全部权限，可管理管理员、角色与系统配置'),
    (2, 'OPERATOR', '运营/客服', '负责商品运营、营销活动、订单跟进与售后审核，不能修改系统配置'),
    (3, 'WAREHOUSE', '仓储/发货员', '负责订单发货、物流跟踪与库存管理'),
]
SYS_USERS = [
    (1, 'admin', '系统管理员', 1, '13900139001', 'admin@mall.com', '#1e40af', '#3b82f6'),
    (2, 'operator', '运营小美', 2, '13900139002', 'operator@mall.com', '#9d174d', '#ec4899'),
    (3, 'warehouse', '仓储老张', 3, '13900139003', 'warehouse@mall.com', '#065f46', '#10b981'),
    (4, 'kefu', '客服小丽', 2, '13900139004', 'kefu@mall.com', '#7c2d12', '#f59e0b'),
]

# 权限：1 目录 / 2 菜单 / 3 按钮
PERMS = [
    (1, 0, '数据看板', 'dashboard:view', 2, '/admin/dashboard', 'dashboard/index', 'DataLine', 1),
    (2, 0, '商品管理', None, 1, '/admin/product', None, 'Goods', 2),
    (21, 2, '商品列表', 'product:list', 2, '/admin/product/list', 'product/list', 'List', 1),
    (22, 2, '商品分类', 'category:list', 2, '/admin/product/category', 'product/category', 'Grid', 2),
    (23, 2, '库存预警', 'stock:list', 2, '/admin/product/stock', 'product/stock', 'Warning', 3),
    (24, 2, '商品评价', 'comment:list', 2, '/admin/product/comment', 'product/comment', 'ChatDotSquare', 4),
    (211, 21, '新增商品', 'product:add', 3, None, None, None, 1),
    (212, 21, '编辑商品', 'product:edit', 3, None, None, None, 2),
    (213, 21, '删除商品', 'product:delete', 3, None, None, None, 3),
    (214, 21, '上下架', 'product:status', 3, None, None, None, 4),
    (215, 21, '导出商品', 'product:export', 3, None, None, None, 5),
    (216, 21, '导入商品', 'product:import', 3, None, None, None, 6),
    (221, 22, '新增分类', 'category:add', 3, None, None, None, 1),
    (222, 22, '编辑分类', 'category:edit', 3, None, None, None, 2),
    (223, 22, '删除分类', 'category:delete', 3, None, None, None, 3),
    (231, 23, '调整库存', 'stock:edit', 3, None, None, None, 1),
    (241, 24, '回复评价', 'comment:reply', 3, None, None, None, 1),
    (242, 24, '显示/隐藏', 'comment:status', 3, None, None, None, 2),
    (243, 24, '删除评价', 'comment:delete', 3, None, None, None, 3),
    (3, 0, '订单管理', None, 1, '/admin/order', None, 'Tickets', 3),
    (31, 3, '订单列表', 'order:list', 2, '/admin/order/list', 'order/list', 'Document', 1),
    (32, 3, '发货管理', 'order:ship', 2, '/admin/order/ship', 'order/ship', 'Van', 2),
    (33, 3, '售后工单', 'refund:list', 2, '/admin/order/refund', 'order/refund', 'RefreshLeft', 3),
    (311, 31, '订单详情', 'order:detail', 3, None, None, None, 1),
    (312, 31, '订单备注', 'order:remark', 3, None, None, None, 2),
    (313, 31, '订单改价', 'order:price', 3, None, None, None, 3),
    (314, 31, '关闭订单', 'order:close', 3, None, None, None, 4),
    (315, 31, '导出订单', 'order:export', 3, None, None, None, 5),
    (321, 32, '订单发货', 'order:doShip', 3, None, None, None, 1),
    (322, 32, '物流跟踪', 'order:track', 3, None, None, None, 2),
    (331, 33, '售后审核', 'refund:audit', 3, None, None, None, 1),
    (4, 0, '营销管理', None, 1, '/admin/marketing', None, 'Present', 4),
    (41, 4, '优惠券', 'coupon:list', 2, '/admin/marketing/coupon', 'marketing/coupon', 'Discount', 1),
    (42, 4, '轮播图', 'banner:list', 2, '/admin/marketing/banner', 'marketing/banner', 'Picture', 2),
    (43, 4, '公告管理', 'notice:list', 2, '/admin/marketing/notice', 'marketing/notice', 'Bell', 3),
    (411, 41, '新增优惠券', 'coupon:add', 3, None, None, None, 1),
    (412, 41, '编辑优惠券', 'coupon:edit', 3, None, None, None, 2),
    (413, 41, '删除优惠券', 'coupon:delete', 3, None, None, None, 3),
    (414, 41, '启用/停用', 'coupon:status', 3, None, None, None, 4),
    (421, 42, '新增轮播图', 'banner:add', 3, None, None, None, 1),
    (422, 42, '编辑轮播图', 'banner:edit', 3, None, None, None, 2),
    (423, 42, '删除轮播图', 'banner:delete', 3, None, None, None, 3),
    (431, 43, '新增公告', 'notice:add', 3, None, None, None, 1),
    (432, 43, '编辑公告', 'notice:edit', 3, None, None, None, 2),
    (433, 43, '删除公告', 'notice:delete', 3, None, None, None, 3),
    (434, 43, '发布公告', 'notice:publish', 3, None, None, None, 4),
    (5, 0, '会员管理', None, 1, '/admin/user', None, 'User', 5),
    (51, 5, '会员列表', 'member:list', 2, '/admin/user/list', 'user/list', 'Avatar', 1),
    (511, 51, '查看详情', 'member:detail', 3, None, None, None, 1),
    (512, 51, '启用/禁用', 'member:status', 3, None, None, None, 2),
    (6, 0, '系统管理', None, 1, '/admin/system', None, 'Setting', 6),
    (61, 6, '管理员管理', 'sys:user:list', 2, '/admin/system/user', 'system/user', 'UserFilled', 1),
    (62, 6, '角色权限', 'sys:role:list', 2, '/admin/system/role', 'system/role', 'Lock', 2),
    (63, 6, '系统配置', 'sys:config:list', 2, '/admin/system/config', 'system/config', 'Tools', 3),
    (64, 6, '操作日志', 'sys:log:list', 2, '/admin/system/log', 'system/log', 'DocumentCopy', 4),
    (611, 61, '新增管理员', 'sys:user:add', 3, None, None, None, 1),
    (612, 61, '编辑管理员', 'sys:user:edit', 3, None, None, None, 2),
    (613, 61, '删除管理员', 'sys:user:delete', 3, None, None, None, 3),
    (614, 61, '重置密码', 'sys:user:reset', 3, None, None, None, 4),
    (621, 62, '新增角色', 'sys:role:add', 3, None, None, None, 1),
    (622, 62, '编辑角色', 'sys:role:edit', 3, None, None, None, 2),
    (623, 62, '删除角色', 'sys:role:delete', 3, None, None, None, 3),
    (624, 62, '分配权限', 'sys:role:assign', 3, None, None, None, 4),
    (631, 63, '修改配置', 'sys:config:edit', 3, None, None, None, 1),
    (641, 64, '清空日志', 'sys:log:delete', 3, None, None, None, 1),
]

OPERATOR_PERMS = [1, 2, 21, 22, 24, 211, 212, 213, 214, 215, 216, 221, 222, 223, 241, 242,
                  3, 31, 33, 311, 312, 315, 331, 4, 41, 42, 43, 411, 412, 413, 414,
                  421, 422, 423, 431, 432, 433, 434, 5, 51, 511, 512]
WAREHOUSE_PERMS = [1, 2, 21, 23, 231, 2, 3, 31, 32, 311, 321, 322]
role_perms = []
rp_id = 1
for p in PERMS:  # ADMIN 全量
    role_perms.append(dict(id=rp_id, role_id=1, permission_id=p[0]))
    rp_id += 1
for pid in sorted(set(OPERATOR_PERMS)):
    role_perms.append(dict(id=rp_id, role_id=2, permission_id=pid))
    rp_id += 1
for pid in sorted(set(WAREHOUSE_PERMS)):
    role_perms.append(dict(id=rp_id, role_id=3, permission_id=pid))
    rp_id += 1

CONFIGS = [
    ('stock_warn_threshold', '库存预警阈值', '10', '商品/SKU 库存低于该值时进入库存预警列表'),
    ('freight_fee', '默认运费(元)', '10.00', '订单商品总额未达包邮门槛时收取的运费'),
    ('free_freight_amount', '包邮门槛(元)', '99.00', '订单商品总额达到该值免运费'),
    ('order_auto_cancel_minutes', '未支付自动取消(分钟)', '30', '超时未支付的订单自动取消'),
    ('order_auto_finish_days', '发货后自动收货(天)', '15', '发货后超过该天数自动确认收货'),
    ('after_sale_days', '可申请售后天数', '7', '签收后超过该天数不可申请售后'),
    ('shop_name', '商城名称', '星辰商城', '前台展示的商城名称'),
    ('shop_notice', '商城公告', '满 99 元包邮，七天无理由退换货', '前台顶部滚动公告'),
    ('hot_search_keywords', '热门搜索词', '智能手机,笔记本电脑,T恤,坚果,保温杯', '前台搜索框推荐词'),
]

logs = []
for i in range(1, 21):
    su = random.choice(SYS_USERS)
    mod, op, uri = random.choice([
        ('商品管理', '新增商品', '/api/admin/products'),
        ('商品管理', '编辑商品', '/api/admin/products/3'),
        ('订单管理', '订单发货', '/api/admin/orders/12/ship'),
        ('营销管理', '新增优惠券', '/api/admin/coupons'),
        ('系统管理', '修改系统配置', '/api/admin/sys/configs'),
        ('会员管理', '禁用会员', '/api/admin/users/5/status'),
    ])
    logs.append(dict(id=i, user_id=su[0], username=su[1], module=mod, operation=op,
                     method='POST', request_uri=uri, params='{"demo":true}',
                     ip=f'192.168.1.{random.randint(2, 250)}',
                     cost_time=random.randint(8, 320), status=1 if random.random() > 0.1 else 0,
                     error_msg=None if random.random() > 0.1 else '演示用失败日志',
                     create_time=NOW - timedelta(hours=random.randint(1, 400))))

# =================================================================
# 8. 输出 SQL
# =================================================================
L = []
w = L.append
w('-- =============================================================================')
w('--  电商管理系统 —— 演示数据（由 tools/gen_seed.py 自动生成，请勿手工修改）')
w('--  生成时间: ' + NOW.strftime('%Y-%m-%d %H:%M:%S'))
w('--  规模: 10 分类 / 24 商品 / %d SKU / 6 买家 / %d 订单 / %d 评价 / %d 售后'
  % (len(skus), len(orders), len(comments), len(refunds)))
w('-- =============================================================================')
w('USE `mall_db`;')
w('SET NAMES utf8mb4;')
for t in ['user', 'user_address', 'favorite', 'browse_history', 'category', 'product',
          'product_spec', 'product_sku', 'cart', 'orders', 'order_item', 'express_track',
          'refund', 'coupon', 'user_coupon', 'product_comment', 'banner', 'notice',
          'sys_user', 'sys_role', 'sys_permission', 'sys_role_permission', 'sys_config', 'sys_oper_log']:
    w('DELETE FROM `%s`;' % t)
w('')

w('-- ---------------- 分类 ----------------')
w('INSERT INTO `category` (`id`,`parent_id`,`name`,`level`,`sort`,`icon`,`status`) VALUES')
w(',\n'.join('(%d,%d,%s,%d,%d,%s,1)' % (c[0], c[1], esc(c[2]), c[3], c[4], esc(c[5])) for c in CATEGORIES) + ';')
w('')

w('-- ---------------- 商品 SPU ----------------')
rows = []
for p in products:
    rows.append('(%d,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d)'
                % (p['id'], p['category_id'], esc(p['name']), esc(p['subtitle']), esc(p['brand']),
                   esc(p['unit']), esc(p['main_image']), esc(p['images']), esc(p['detail']),
                   money(p['price']), money(p['original_price']), money(p['cost_price']),
                   p['stock'], p['sales'], p['warn_stock'], p['status'],
                   p['is_recommend'], p['is_new'], p['is_hot'], p['sort']))
w('INSERT INTO `product` (`id`,`category_id`,`name`,`subtitle`,`brand`,`unit`,`main_image`,`images`,'
  '`detail`,`price`,`original_price`,`cost_price`,`stock`,`sales`,`warn_stock`,`status`,'
  '`is_recommend`,`is_new`,`is_hot`,`sort`) VALUES')
w(',\n'.join(rows) + ';')
w('')

w('-- ---------------- 商品规格定义 ----------------')
w('INSERT INTO `product_spec` (`id`,`product_id`,`spec_name`,`spec_values`,`sort`) VALUES')
w(',\n'.join('(%d,%d,%s,%s,%d)' % (s['id'], s['product_id'], esc(s['spec_name']),
                                   esc(s['spec_values']), s['sort']) for s in specs) + ';')
w('')

w('-- ---------------- 商品 SKU ----------------')
rows = []
for s in skus:
    rows.append('(%d,%d,%s,%s,%s,%s,%s,%d,%d,%s,%d)'
                % (s['id'], s['product_id'], esc(s['sku_code']), esc(s['spec_data']),
                   esc(s['spec_info']), money(s['price']), money(s['original_price']),
                   s['stock'], s['sales'], esc(s['image']), s['status']))
w('INSERT INTO `product_sku` (`id`,`product_id`,`sku_code`,`spec_data`,`spec_info`,`price`,'
  '`original_price`,`stock`,`sales`,`image`,`status`) VALUES')
w(',\n'.join(rows) + ';')
w('')

w('-- ---------------- 买家 ----------------')
w('INSERT INTO `user` (`id`,`username`,`password`,`nickname`,`phone`,`email`,`avatar`,`gender`,'
  '`birthday`,`status`,`last_login_time`,`create_time`) VALUES')
w(',\n'.join('(%d,%s,%s,%s,%s,%s,%s,%d,%s,%d,%s,%s)'
             % (u['id'], esc(u['username']), esc(u['password']), esc(u['nickname']), esc(u['phone']),
                esc(u['email']), esc(u['avatar']), u['gender'], esc(u['birthday']), u['status'],
                dt(u['last_login_time']), dt(u['create_time'])) for u in users) + ';')
w('')

w('-- ---------------- 收货地址 ----------------')
w('INSERT INTO `user_address` (`id`,`user_id`,`receiver`,`phone`,`province`,`city`,`district`,'
  '`detail`,`post_code`,`is_default`) VALUES')
w(',\n'.join('(%d,%d,%s,%s,%s,%s,%s,%s,%s,%d)'
             % (a['id'], a['user_id'], esc(a['receiver']), esc(a['phone']), esc(a['province']),
                esc(a['city']), esc(a['district']), esc(a['detail']), esc(a['post_code']),
                a['is_default']) for a in addresses) + ';')
w('')

w('-- ---------------- 收藏 ----------------')
w('INSERT INTO `favorite` (`id`,`user_id`,`product_id`,`create_time`) VALUES')
w(',\n'.join('(%d,%d,%d,%s)' % (f['id'], f['user_id'], f['product_id'], dt(f['create_time']))
             for f in favorites) + ';')
w('')

w('-- ---------------- 浏览记录 ----------------')
w('INSERT INTO `browse_history` (`id`,`user_id`,`product_id`,`create_time`,`update_time`) VALUES')
w(',\n'.join('(%d,%d,%d,%s,%s)' % (h['id'], h['user_id'], h['product_id'],
                                   dt(h['create_time']), dt(h['create_time'])) for h in histories) + ';')
w('')

w('-- ---------------- 购物车 ----------------')
w('INSERT INTO `cart` (`id`,`user_id`,`product_id`,`sku_id`,`quantity`,`checked`,`create_time`) VALUES')
w(',\n'.join('(%d,%d,%d,%d,%d,%d,%s)' % (c['id'], c['user_id'], c['product_id'], c['sku_id'],
                                         c['quantity'], c['checked'], dt(c['create_time']))
             for c in carts) + ';')
w('')

w('-- ---------------- 优惠券 ----------------')
w('INSERT INTO `coupon` (`id`,`name`,`type`,`amount`,`discount`,`min_amount`,`total_count`,'
  '`received_count`,`used_count`,`per_limit`,`scope`,`scope_ids`,`start_time`,`end_time`,`status`,'
  '`description`) VALUES')
w(',\n'.join('(%d,%s,%d,%s,%s,%s,%d,%d,%d,%d,%d,%s,%s,%s,%d,%s)'
             % (c['id'], esc(c['name']), c['type'], money(c['amount']), money(c['discount']),
                money(c['min_amount']), c['total_count'], c['received_count'], c['used_count'],
                c['per_limit'], c['scope'], esc(c['scope_ids']), dt(c['start_time']),
                dt(c['end_time']), c['status'], esc(c['description'])) for c in COUPONS) + ';')
w('')

w('-- ---------------- 用户优惠券 ----------------')
w('INSERT INTO `user_coupon` (`id`,`user_id`,`coupon_id`,`coupon_name`,`status`,`order_id`,'
  '`receive_time`,`use_time`,`expire_time`) VALUES')
w(',\n'.join('(%d,%d,%d,%s,%d,%s,%s,%s,%s)'
             % (uc['id'], uc['user_id'], uc['coupon_id'], esc(uc['coupon_name']), uc['status'],
                esc(uc['order_id']), dt(uc['receive_time']),
                dt(uc['use_time']) if uc['use_time'] else 'NULL', dt(uc['expire_time']))
             for uc in user_coupons) + ';')
w('')

w('-- ---------------- 订单主表 ----------------')
w('INSERT INTO `orders` (`id`,`order_no`,`user_id`,`total_amount`,`freight_amount`,`discount_amount`,'
  '`pay_amount`,`user_coupon_id`,`status`,`pay_type`,`pay_time`,`ship_time`,`receive_time`,'
  '`finish_time`,`close_time`,`receiver_name`,`receiver_phone`,`receiver_address`,`express_company`,'
  '`express_no`,`remark`,`item_count`,`create_time`) VALUES')
w(',\n'.join('(%d,%s,%d,%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%d,%s)'
             % (o['id'], esc(o['order_no']), o['user_id'], money(o['total_amount']),
                money(o['freight_amount']), money(o['discount_amount']), money(o['pay_amount']),
                esc(o['user_coupon_id']), o['status'], esc(o['pay_type']),
                dt(o['pay_time']) if o['pay_time'] else 'NULL',
                dt(o['ship_time']) if o['ship_time'] else 'NULL',
                dt(o['receive_time']) if o['receive_time'] else 'NULL',
                dt(o['finish_time']) if o['finish_time'] else 'NULL',
                dt(o['close_time']) if o['close_time'] else 'NULL',
                esc(o['receiver_name']), esc(o['receiver_phone']), esc(o['receiver_address']),
                esc(o['express_company']), esc(o['express_no']), esc(o['remark']),
                o['item_count'], dt(o['create_time'])) for o in orders) + ';')
w('')

w('-- ---------------- 订单明细 ----------------')
w('INSERT INTO `order_item` (`id`,`order_id`,`order_no`,`user_id`,`product_id`,`sku_id`,'
  '`product_name`,`product_image`,`spec_info`,`price`,`quantity`,`total_amount`,`is_commented`,'
  '`create_time`) VALUES')
w(',\n'.join('(%d,%d,%s,%d,%d,%d,%s,%s,%s,%s,%d,%s,%d,%s)'
             % (i['id'], i['order_id'], esc(i['order_no']), i['user_id'], i['product_id'],
                i['sku_id'], esc(i['product_name']), esc(i['product_image']), esc(i['spec_info']),
                money(i['price']), i['quantity'], money(i['total_amount']), i['is_commented'],
                dt(next(o['create_time'] for o in orders if o['id'] == i['order_id'])))
             for i in order_items) + ';')
w('')

w('-- ---------------- 物流轨迹 ----------------')
w('INSERT INTO `express_track` (`id`,`order_id`,`order_no`,`express_company`,`express_no`,`status`,'
  '`context`,`location`,`accept_time`) VALUES')
w(',\n'.join('(%d,%d,%s,%s,%s,%s,%s,%s,%s)'
             % (t['id'], t['order_id'], esc(t['order_no']), esc(t['express_company']),
                esc(t['express_no']), esc(t['status']), esc(t['context']), esc(t['location']),
                dt(t['accept_time'])) for t in tracks) + ';')
w('')

w('-- ---------------- 售后工单 ----------------')
w('INSERT INTO `refund` (`id`,`refund_no`,`order_id`,`order_no`,`order_item_id`,`user_id`,`type`,'
  '`reason`,`description`,`images`,`refund_amount`,`status`,`audit_user_id`,`audit_user`,'
  '`audit_remark`,`audit_time`,`finish_time`,`create_time`) VALUES')
w(',\n'.join('(%d,%s,%d,%s,%s,%d,%d,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s)'
             % (r['id'], esc(r['refund_no']), r['order_id'], esc(r['order_no']),
                esc(r['order_item_id']), r['user_id'], r['type'], esc(r['reason']),
                esc(r['description']), esc(r['images']), money(r['refund_amount']), r['status'],
                esc(r['audit_user_id']), esc(r['audit_user']), esc(r['audit_remark']),
                dt(r['audit_time']) if r['audit_time'] else 'NULL',
                dt(r['finish_time']) if r['finish_time'] else 'NULL',
                dt(r['create_time'])) for r in refunds) + ';')
w('')

w('-- ---------------- 商品评价 ----------------')
if comments:
    w('INSERT INTO `product_comment` (`id`,`product_id`,`sku_id`,`order_id`,`order_item_id`,`user_id`,'
      '`user_nickname`,`user_avatar`,`star`,`content`,`images`,`is_anonymous`,`reply_content`,'
      '`reply_user`,`reply_time`,`status`,`create_time`) VALUES')
    w(',\n'.join('(%d,%d,%s,%s,%s,%d,%s,%s,%d,%s,%s,%d,%s,%s,%s,%d,%s)'
                 % (c['id'], c['product_id'], esc(c['sku_id']), esc(c['order_id']),
                    esc(c['order_item_id']), c['user_id'], esc(c['user_nickname']),
                    esc(c['user_avatar']), c['star'], esc(c['content']), esc(c['images']),
                    c['is_anonymous'], esc(c['reply_content']), esc(c['reply_user']),
                    dt(c['reply_time']) if c['reply_time'] else 'NULL', c['status'],
                    dt(c['create_time'])) for c in comments) + ';')
    w('')

w('-- ---------------- 轮播图 ----------------')
w('INSERT INTO `banner` (`id`,`title`,`image`,`link_url`,`position`,`sort`,`status`) VALUES')
w(',\n'.join('(%d,%s,%s,%s,%d,%d,%d)' % (b['id'], esc(b['title']), esc(b['image']),
                                         esc(b['link_url']), b['position'], b['sort'], b['status'])
             for b in banners) + ';')
w('')

w('-- ---------------- 公告 ----------------')
w('INSERT INTO `notice` (`id`,`title`,`content`,`type`,`status`,`publish_time`) VALUES')
w(',\n'.join('(%d,%s,%s,%d,%d,%s)' % (n['id'], esc(n['title']), esc(n['content']), n['type'],
                                      n['status'],
                                      dt(n['publish_time']) if n['publish_time'] else 'NULL')
             for n in notices) + ';')
w('')

w('-- ---------------- 后台角色 ----------------')
w('INSERT INTO `sys_role` (`id`,`role_code`,`role_name`,`description`,`status`) VALUES')
w(',\n'.join('(%d,%s,%s,%s,1)' % (r[0], esc(r[1]), esc(r[2]), esc(r[3])) for r in ROLES) + ';')
w('')

w('-- ---------------- 后台管理员（初始密码均为 123456） ----------------')
w('INSERT INTO `sys_user` (`id`,`username`,`password`,`real_name`,`avatar`,`phone`,`email`,'
  '`role_id`,`status`,`last_login_time`,`last_login_ip`) VALUES')
srows = []
for su in SYS_USERS:
    avatar = f'/api/files/avatar/s{su[0]}.svg'
    svg_write(os.path.join(UPLOAD, 'avatar', f's{su[0]}.svg'), avatar_svg(su[2][0], su[6], su[7]))
    srows.append('(%d,%s,%s,%s,%s,%s,%s,%d,1,%s,%s)'
                 % (su[0], esc(su[1]), esc(PWD), esc(su[2]), esc(avatar), esc(su[4]), esc(su[5]),
                    su[3], dt(NOW - timedelta(hours=random.randint(1, 72))), esc('127.0.0.1')))
w(',\n'.join(srows) + ';')
w('')

w('-- ---------------- 权限菜单 ----------------')
w('INSERT INTO `sys_permission` (`id`,`parent_id`,`name`,`perms`,`type`,`path`,`component`,`icon`,`sort`,`status`) VALUES')
w(',\n'.join('(%d,%d,%s,%s,%d,%s,%s,%s,%d,1)'
             % (p[0], p[1], esc(p[2]), esc(p[3]), p[4], esc(p[5]), esc(p[6]), esc(p[7]), p[8])
             for p in PERMS) + ';')
w('')

w('-- ---------------- 角色权限关联 ----------------')
w('INSERT INTO `sys_role_permission` (`id`,`role_id`,`permission_id`) VALUES')
w(',\n'.join('(%d,%d,%d)' % (rp['id'], rp['role_id'], rp['permission_id']) for rp in role_perms) + ';')
w('')

w('-- ---------------- 系统配置 ----------------')
w('INSERT INTO `sys_config` (`config_key`,`config_name`,`config_value`,`remark`) VALUES')
w(',\n'.join('(%s,%s,%s,%s)' % (esc(c[0]), esc(c[1]), esc(c[2]), esc(c[3])) for c in CONFIGS) + ';')
w('')

w('-- ---------------- 操作日志 ----------------')
w('INSERT INTO `sys_oper_log` (`id`,`user_id`,`username`,`module`,`operation`,`method`,`request_uri`,'
  '`params`,`ip`,`cost_time`,`status`,`error_msg`,`create_time`) VALUES')
w(',\n'.join('(%d,%d,%s,%s,%s,%s,%s,%s,%s,%d,%d,%s,%s)'
             % (g['id'], g['user_id'], esc(g['username']), esc(g['module']), esc(g['operation']),
                esc(g['method']), esc(g['request_uri']), esc(g['params']), esc(g['ip']),
                g['cost_time'], g['status'], esc(g['error_msg']), dt(g['create_time']))
             for g in logs) + ';')
w('')

os.makedirs(os.path.dirname(SQL_OUT), exist_ok=True)
with open(SQL_OUT, 'w', encoding='utf-8', newline='\n') as f:
    f.write('\n'.join(L) + '\n')

print('SQL  ->', SQL_OUT)
print('图片 ->', UPLOAD)
print('统计: 分类 %d / 商品 %d / 规格 %d / SKU %d / 买家 %d / 地址 %d / 收藏 %d / 浏览 %d / 购物车 %d'
      % (len(CATEGORIES), len(products), len(specs), len(skus), len(users), len(addresses),
         len(favorites), len(histories), len(carts)))
print('      优惠券 %d / 用户券 %d / 订单 %d / 明细 %d / 物流 %d / 售后 %d / 评价 %d'
      % (len(COUPONS), len(user_coupons), len(orders), len(order_items), len(tracks),
         len(refunds), len(comments)))
print('      轮播图 %d / 公告 %d / 角色 %d / 管理员 %d / 权限 %d / 角色权限 %d / 配置 %d / 日志 %d'
      % (len(banners), len(notices), len(ROLES), len(SYS_USERS), len(PERMS), len(role_perms),
         len(CONFIGS), len(logs)))
