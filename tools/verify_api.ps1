# =============================================================================
#  后端接口端到端验证脚本（前后台全链路）
#  用法： pwsh -File tools/verify_api.ps1 [-Base http://127.0.0.1:8080]
#  说明：
#    1) 只使用 HTTP（非 HTTPS），因此不受本机 Schannel 凭证问题影响
#    2) 跨步骤共享状态统一放在 $S 哈希表中（PowerShell 函数作用域下变量赋值不外传）
# =============================================================================
param(
    [string]$Base = 'http://127.0.0.1:8080'
)

$ErrorActionPreference = 'Stop'
$S = @{
    pass             = 0
    fail             = 0
    failures         = @()
    adminToken       = ''
    buyerToken       = ''
    productId        = $null
    skuId            = $null
    cartId           = $null
    addressId        = $null
    previewPay       = 0
    orderNo          = ''
    createdProductId = $null
}

function Ok($msg) {
    Write-Host "  [PASS] $msg" -ForegroundColor Green
    $S.pass++
}

function Bad($msg) {
    Write-Host "  [FAIL] $msg" -ForegroundColor Red
    $S.fail++
    $S.failures += $msg
}

function Step($name, [scriptblock]$action) {
    Write-Host "`n== $name" -ForegroundColor Cyan
    try {
        . $action
    }
    catch {
        Bad "$name -> $($_.Exception.Message)"
    }
}

function Check($condition, $msg) {
    if ($condition) { Ok $msg } else { Bad $msg }
}

function Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token = ''
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    $params = @{
        Method     = $Method
        Uri        = "$Base$Path"
        Headers    = $headers
        TimeoutSec = 60
    }
    if ($null -ne $Body) {
        $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 12 -Compress }
        $params['Body'] = [System.Text.Encoding]::UTF8.GetBytes($json)
        $params['ContentType'] = 'application/json; charset=utf-8'
    }
    $resp = Invoke-RestMethod @params
    if ($resp.code -ne 200) {
        throw "code=$($resp.code), message=$($resp.message)"
    }
    return $resp.data
}

Write-Host "==============================================" -ForegroundColor Yellow
Write-Host " 电商管理系统 接口端到端验证  Base=$Base" -ForegroundColor Yellow
Write-Host "==============================================" -ForegroundColor Yellow

# ------------------------------------------------------------------ 0. 文档与静态资源
Step '0. 接口文档与静态资源' {
    $doc = Invoke-WebRequest -Uri "$Base/doc.html" -UseBasicParsing -TimeoutSec 20
    Check ($doc.StatusCode -eq 200) "/doc.html 可访问 (HTTP $($doc.StatusCode))"
    $openapi = Invoke-RestMethod -Uri "$Base/v3/api-docs" -TimeoutSec 30
    $apiCount = ($openapi.paths.PSObject.Properties | Measure-Object).Count
    Check ($apiCount -ge 50) "OpenAPI 文档可解析，接口路径数 = $apiCount"
    $img = Invoke-WebRequest -Uri "$Base/api/files/product/p1_1.svg" -UseBasicParsing -TimeoutSec 20
    Check ($img.StatusCode -eq 200) "商品图片静态资源可访问 (HTTP $($img.StatusCode))"
}

# ------------------------------------------------------------------ 1. 未登录拦截
Step '1. 未登录访问受保护接口应返回 401' {
    try {
        Invoke-RestMethod -Uri "$Base/api/cart/list" -TimeoutSec 15 | Out-Null
        Bad '未登录访问 /api/cart/list 竟然成功，鉴权失效'
    }
    catch {
        $status = $_.Exception.Response.StatusCode.value__
        Check ($status -eq 401) "未登录访问 /api/cart/list 返回 $status"
    }
    try {
        Invoke-RestMethod -Uri "$Base/api/admin/dashboard/overview" -TimeoutSec 15 | Out-Null
        Bad '未登录访问管理端接口竟然成功'
    }
    catch {
        $status = $_.Exception.Response.StatusCode.value__
        Check ($status -eq 401) "未登录访问 /api/admin/** 返回 $status"
    }
}

# ------------------------------------------------------------------ 2. 管理端登录
Step '2. 管理端登录与权限菜单' {
    $data = Api POST '/api/admin/auth/login' @{ username = 'admin'; password = '123456' }
    $S.adminToken = $data.token
    Check ([bool]$S.adminToken) "管理员 admin 登录成功，token 长度=$($S.adminToken.Length)"
    Check ($data.userInfo.roleCode -eq 'ADMIN') "角色编码 = $($data.userInfo.roleCode)"
    Check ($data.permissions.Count -gt 50) "权限标识数量 = $($data.permissions.Count)"
    Check ($data.menus.Count -ge 5) "菜单树顶级节点数 = $($data.menus.Count)"
    $me = Api GET '/api/admin/auth/me' $null $S.adminToken
    Check ($me.userInfo.username -eq 'admin') 'GET /api/admin/auth/me 返回当前管理员'
}

Step '2b. 错误密码应被拒绝' {
    try {
        Api POST '/api/admin/auth/login' @{ username = 'admin'; password = 'wrong-password' } | Out-Null
        Bad '错误密码竟然登录成功'
    }
    catch {
        Ok "错误密码登录被拒绝：$($_.Exception.Message)"
    }
}

Step '2c. 运营角色应无系统管理权限（403）' {
    $op = Api POST '/api/admin/auth/login' @{ username = 'operator'; password = '123456' }
    Check ($op.userInfo.roleCode -eq 'OPERATOR') "operator 登录成功，角色 = $($op.userInfo.roleCode)"
    Check (-not ($op.permissions -contains 'sys:user:list')) '运营角色权限中不含 sys:user:list'
    try {
        Api GET '/api/admin/sys/users' $null $op.token | Out-Null
        Bad '运营角色竟然可以查询管理员列表（权限校验失效）'
    }
    catch {
        Ok "运营角色访问 /api/admin/sys/users 被拒绝：$($_.Exception.Message)"
    }
}

# ------------------------------------------------------------------ 3. 商城公开接口
Step '3. 商城公开接口（游客浏览）' {
    $categories = Api GET '/api/shop/categories'
    Check ($categories.Count -ge 4) "分类树顶级节点 = $($categories.Count)"
    Check ($categories[0].children.Count -ge 1) "一级分类「$($categories[0].name)」下二级分类 = $($categories[0].children.Count)"

    $page = Api GET '/api/shop/products?page=1&size=8'
    Check ($page.total -ge 20) "商品总数 = $($page.total)，本页返回 $($page.records.Count) 条"

    $sorted = Api GET '/api/shop/products?sort=price_asc&page=1&size=5'
    $prices = @($sorted.records | ForEach-Object { [double]$_.price })
    $isAsc = $true
    for ($i = 1; $i -lt $prices.Count; $i++) { if ($prices[$i] -lt $prices[$i - 1]) { $isAsc = $false } }
    Check $isAsc "价格升序排序生效（$($prices -join ', ')）"

    $search = Api GET '/api/shop/products?keyword=%E6%89%8B%E6%9C%BA&page=1&size=10'
    Check ($search.total -ge 1) "关键词搜索「手机」命中 = $($search.total) 件"

    $detail = Api GET '/api/shop/products/1'
    Check ($detail.skus.Count -ge 2) "商品详情含 SKU = $($detail.skus.Count) 个"
    Check ($detail.specs.Count -ge 1) "商品详情含规格定义 = $($detail.specs.Count) 组"
    Check ([bool]$detail.categoryName) "商品详情含分类名 = $($detail.categoryName)"
    $S.productId = $detail.id
    $S.skuId = $detail.skus[0].id
    Check ([bool]$S.productId -and [bool]$S.skuId) "取到测试用商品ID=$($S.productId)、SKU=$($S.skuId)（库存 $($detail.skus[0].stock)）"

    Check ((Api GET '/api/shop/products/hot?limit=6').Count -ge 1) '热销商品接口可用'
    Check ((Api GET '/api/shop/products/recommend?limit=6').Count -ge 1) '推荐商品接口可用'
    Check ((Api GET '/api/shop/products/new?limit=6').Count -ge 1) '新品接口可用'
    Check ((Api GET '/api/shop/banners?position=1').Count -ge 1) '首页轮播图接口可用'
    Check ((Api GET '/api/shop/notices?page=1&size=5').total -ge 1) '公告列表可用（仅已发布）'
    Check ((Api GET '/api/shop/coupons').Count -ge 1) '可领取优惠券列表可用'

    $stats = Api GET '/api/shop/products/1/comment-stats'
    $cmts = Api GET '/api/shop/products/1/comments?page=1&size=5'
    Check ($null -ne $stats.total) "评价统计可用：总数 $($stats.total)，平均分 $($stats.avgStar)，本页评价 $($cmts.records.Count) 条"
}

# ------------------------------------------------------------------ 4. 买家登录
Step '4. 买家注册 / 登录' {
    $suffix = Get-Random -Minimum 10000 -Maximum 99999
    $newUser = "autotest$suffix"
    Api POST '/api/auth/register' @{ username = $newUser; password = '123456'; nickname = '自动化测试'; phone = '13900001111' } | Out-Null
    $login = Api POST '/api/auth/login' @{ username = $newUser; password = '123456' }
    Check ([bool]$login.token) "新注册买家 $newUser 登录成功"
    Check ($login.userInfo.nickname -eq '自动化测试') "注册信息回显昵称 = $($login.userInfo.nickname)"

    $dup = $false
    try { Api POST '/api/auth/register' @{ username = $newUser; password = '123456' } | Out-Null; $dup = $true } catch { }
    Check (-not $dup) '重复用户名注册被拒绝'

    $b = Api POST '/api/auth/login' @{ username = 'zhangsan'; password = '123456' }
    $S.buyerToken = $b.token
    Check ([bool]$S.buyerToken) '演示买家 zhangsan 登录成功'
}

Step '4b. 买家个人中心' {
    $profile = Api GET '/api/user/profile' $null $S.buyerToken
    Check ($profile.username -eq 'zhangsan') "个人资料接口返回 = $($profile.username)"
    $addrs = Api GET '/api/address/list' $null $S.buyerToken
    Check ($addrs.Count -ge 1) "收货地址 = $($addrs.Count) 条"
    $S.addressId = ($addrs | Where-Object { $_.isDefault -eq 1 } | Select-Object -First 1).id
    if (-not $S.addressId) { $S.addressId = $addrs[0].id }
    Check ([bool]$S.addressId) "使用收货地址 id=$($S.addressId)"
    Check ((Api GET '/api/user/favorites?page=1&size=5' $null $S.buyerToken).total -ge 0) '收藏分页可用'
    Check ((Api GET '/api/user/history?page=1&size=5' $null $S.buyerToken).total -ge 0) '浏览记录分页可用'
    Check ((Api GET '/api/user/coupons?page=1&size=10' $null $S.buyerToken).total -ge 0) '我的优惠券分页可用'
    $counts = Api GET '/api/order/counts' $null $S.buyerToken
    Check ($null -ne $counts.unpaid) "订单状态统计可用（待付款=$($counts.unpaid)，待收货=$($counts.shipped)，待评价=$($counts.toComment)）"
}

# ------------------------------------------------------------------ 5. 购物车与结算
Step '5. 购物车加购与结算预览' {
    Api POST '/api/cart' @{ productId = $S.productId; skuId = $S.skuId; quantity = 2 } $S.buyerToken | Out-Null
    $cart = Api GET '/api/cart/list' $null $S.buyerToken
    $item = $cart | Where-Object { $_.skuId -eq $S.skuId } | Select-Object -First 1
    Check ($null -ne $item) "购物车已加入 SKU $($S.skuId)（数量 $($item.quantity)，单价 $($item.price)）"
    $S.cartId = $item.id

    $preview = Api POST '/api/order/preview' @{ cartIds = @($S.cartId) } $S.buyerToken
    Check ($preview.items.Count -ge 1) "结算预览商品行 = $($preview.items.Count)"
    Check ([double]$preview.totalAmount -gt 0) "预览金额：商品 $($preview.totalAmount) + 运费 $($preview.freightAmount) - 优惠 $($preview.discountAmount) = 实付 $($preview.payAmount)"
    Check ($null -ne $preview.address) "预览返回默认收货地址：$($preview.address.fullAddress)"
    $S.previewPay = [double]$preview.payAmount
}

# ------------------------------------------------------------------ 6. 下单与支付
Step '6. 提交订单并模拟支付（校验库存扣减）' {
    $before = (Api GET "/api/shop/products/$($S.productId)").skus | Where-Object { $_.id -eq $S.skuId } | Select-Object -First 1
    $stockBefore = [int]$before.stock

    $submit = Api POST '/api/order/submit' @{ addressId = $S.addressId; cartIds = @($S.cartId); remark = '接口自动化测试订单' } $S.buyerToken
    $S.orderNo = $submit.orderNo
    Check ([bool]$submit.orderNo) "下单成功，订单号 = $($submit.orderNo)，应付 = $($submit.payAmount)"

    $after = (Api GET "/api/shop/products/$($S.productId)").skus | Where-Object { $_.id -eq $S.skuId } | Select-Object -First 1
    Check ([int]$after.stock -eq ($stockBefore - 2)) "下单后 SKU 库存由 $stockBefore 扣减为 $($after.stock)（-2）"

    $cartAfter = Api GET '/api/cart/list' $null $S.buyerToken
    Check ($null -eq ($cartAfter | Where-Object { $_.id -eq $S.cartId })) '下单成功后对应购物车记录已清除'

    Api POST "/api/order/$($S.orderNo)/pay" $null $S.buyerToken | Out-Null
    $detail = Api GET "/api/order/$($S.orderNo)" $null $S.buyerToken
    Check ($detail.status -eq 1) "模拟支付成功，订单状态 = $($detail.status)（1=待发货）"
    Check ([bool]$detail.payTime) "已写入支付时间 = $($detail.payTime)"
    Check ([double]$detail.payAmount -eq $S.previewPay) "实付金额与预览一致 = $($detail.payAmount)"
}

Step '7. 订单取消应回滚库存' {
    Api POST '/api/cart' @{ productId = $S.productId; skuId = $S.skuId; quantity = 1 } $S.buyerToken | Out-Null
    $cart = Api GET '/api/cart/list' $null $S.buyerToken
    $cid = ($cart | Where-Object { $_.skuId -eq $S.skuId } | Select-Object -First 1).id

    $s0 = ((Api GET "/api/shop/products/$($S.productId)").skus | Where-Object { $_.id -eq $S.skuId } | Select-Object -First 1).stock
    $o = Api POST '/api/order/submit' @{ addressId = $S.addressId; cartIds = @($cid) } $S.buyerToken
    $s1 = ((Api GET "/api/shop/products/$($S.productId)").skus | Where-Object { $_.id -eq $S.skuId } | Select-Object -First 1).stock
    Check ([int]$s1 -eq ([int]$s0 - 1)) "下单扣减 1 件：$s0 -> $s1"

    Api POST "/api/order/$($o.orderNo)/cancel" $null $S.buyerToken | Out-Null
    $s2 = ((Api GET "/api/shop/products/$($S.productId)").skus | Where-Object { $_.id -eq $S.skuId } | Select-Object -First 1).stock
    Check ([int]$s2 -eq [int]$s0) "取消订单后库存回滚：$s1 -> $s2（恢复为 $s0）"
    Check ((Api GET "/api/order/$($o.orderNo)" $null $S.buyerToken).status -eq 4) '订单状态 = 4（已取消）'
}

# ------------------------------------------------------------------ 8. 管理端订单
Step '8. 管理端订单列表 / 发货 / 物流 / 备注' {
    Check ((Api GET '/api/admin/orders?page=1&size=5' $null $S.adminToken).total -ge 40) '管理端订单总数 >= 40'
    $pending = Api GET '/api/admin/orders?status=1&page=1&size=5' $null $S.adminToken
    Check ($pending.total -ge 1) "待发货订单 = $($pending.total)"

    $target = $pending.records[0]
    Api PUT "/api/admin/orders/$($target.id)/ship" @{ expressCompany = '顺丰速运'; expressNo = 'SF999888777666' } $S.adminToken | Out-Null
    $shipped = Api GET "/api/admin/orders/$($target.id)" $null $S.adminToken
    Check ($shipped.status -eq 2) "发货成功，订单状态 = $($shipped.status)（2=待收货）"
    Check ($shipped.tracks.Count -ge 1) "已自动生成物流轨迹 = $($shipped.tracks.Count) 条"

    Api POST "/api/admin/orders/$($target.id)/track" @{ status = '运输中'; context = '快件已到达【深圳转运中心】'; location = '深圳市' } $S.adminToken | Out-Null
    Check ((Api GET "/api/admin/orders/$($target.id)" $null $S.adminToken).tracks.Count -ge 2) '追加物流轨迹成功'

    Api PUT "/api/admin/orders/$($target.id)/remark" @{ adminRemark = '已优先安排发货' } $S.adminToken | Out-Null
    Check ((Api GET "/api/admin/orders/$($target.id)" $null $S.adminToken).adminRemark -eq '已优先安排发货') '商家备注写入成功'
}

Step '8b. 管理端售后与买家售后' {
    $refunds = Api GET '/api/admin/refunds?page=1&size=5' $null $S.adminToken
    Check ($refunds.total -ge 1) "售后台账 total = $($refunds.total)"
    Check ((Api GET '/api/admin/refunds?status=0&page=1&size=5' $null $S.adminToken).total -ge 0) '待审核售后筛选可用'
    Check ((Api GET '/api/refund/list?page=1&size=5' $null $S.buyerToken).total -ge 0) '买家售后列表可用'

    # 买家对刚支付的订单申请售后
    $order = Api GET "/api/order/$($S.orderNo)" $null $S.buyerToken
    $applied = Api POST '/api/refund/apply' @{
        orderId      = $order.id
        orderItemId  = $order.items[0].id
        type         = 1
        reason       = '不想要了'
        description  = '接口自动化测试：申请仅退款'
        refundAmount = [double]$order.items[0].totalAmount
    } $S.buyerToken
    Check ([bool]$applied) "买家申请售后成功，售后ID = $applied"
    $afterApply = Api GET "/api/order/$($S.orderNo)" $null $S.buyerToken
    Check ($afterApply.status -eq 5) "申请售后后订单状态 = $($afterApply.status)（5=售后中）"

    # 管理端审核通过
    Api PUT "/api/admin/refunds/$applied/audit" @{ status = 1; auditRemark = '同意买家申请'; refundAmount = [double]$order.items[0].totalAmount } $S.adminToken | Out-Null
    $audited = Api GET "/api/admin/refunds/$applied" $null $S.adminToken
    Check ($audited.status -in @(1, 4)) "售后审核通过，状态 = $($audited.status)（$($audited.statusName)）"
    Check ([bool]$audited.auditUser) "记录审核人 = $($audited.auditUser)"
}

# ------------------------------------------------------------------ 9. 管理端商品
Step '9. 管理端商品新增 / 编辑 / 上下架 / SKU 汇总' {
    $tree = Api GET '/api/admin/categories' $null $S.adminToken
    Check ($tree.Count -ge 4) "管理端分类树顶级 = $($tree.Count)"
    $leaf = $tree[0].children[0]

    $body = @{
        categoryId  = $leaf.id
        name        = '接口测试商品-多规格'
        subtitle    = '自动化测试用，请勿手工修改'
        brand       = '测试品牌'
        unit        = '件'
        status      = 1
        isRecommend = 0
        isNew       = 1
        isHot       = 0
        sort        = 999
        warnStock   = 10
        mainImage   = '/api/files/product/p1_1.svg'
        images      = @('/api/files/product/p1_1.svg')
        detail      = '<p>自动化测试商品详情</p>'
        specs       = @(
            @{ specName = '颜色'; specValues = @('红色', '蓝色') },
            @{ specName = '尺码'; specValues = @('M', 'L') }
        )
        skus        = @(
            @{ skuCode = 'AUTO-01'; specData = '{"颜色":"红色","尺码":"M"}'; specInfo = '红色 / M'; price = 99.00; originalPrice = 129.00; stock = 30; status = 1 },
            @{ skuCode = 'AUTO-02'; specData = '{"颜色":"红色","尺码":"L"}'; specInfo = '红色 / L'; price = 109.00; originalPrice = 139.00; stock = 20; status = 1 },
            @{ skuCode = 'AUTO-03'; specData = '{"颜色":"蓝色","尺码":"M"}'; specInfo = '蓝色 / M'; price = 119.00; originalPrice = 149.00; stock = 10; status = 1 }
        )
    }
    $S.createdProductId = Api POST '/api/admin/products' $body $S.adminToken
    Check ([bool]$S.createdProductId) "新增商品成功，商品ID = $($S.createdProductId)"

    $detail = Api GET "/api/admin/products/$($S.createdProductId)" $null $S.adminToken
    Check ($detail.skus.Count -eq 3) "SKU 生成 = $($detail.skus.Count) 个"
    Check ([double]$detail.price -eq 99.00) "商品展示价取 SKU 最低价 = $($detail.price)"
    Check ([int]$detail.stock -eq 60) "商品总库存 = SKU 库存之和 = $($detail.stock)"

    # 编辑：改价格与库存
    $detail.skus[0].price = 88.00
    $detail.skus[0].stock = 50
    Api PUT "/api/admin/products/$($S.createdProductId)" @{
        categoryId = $leaf.id
        name       = '接口测试商品-多规格（已编辑）'
        unit       = '件'
        status     = 1
        warnStock  = 10
        mainImage  = '/api/files/product/p1_1.svg'
        detail     = '<p>编辑后的详情</p>'
        specs      = @(@{ specName = '颜色'; specValues = @('红色', '蓝色') }, @{ specName = '尺码'; specValues = @('M', 'L') })
        skus       = @(
            @{ id = $detail.skus[0].id; skuCode = 'AUTO-01'; specData = '{"颜色":"红色","尺码":"M"}'; specInfo = '红色 / M'; price = 88.00; originalPrice = 129.00; stock = 50; status = 1 },
            @{ id = $detail.skus[1].id; skuCode = 'AUTO-02'; specData = '{"颜色":"红色","尺码":"L"}'; specInfo = '红色 / L'; price = 109.00; originalPrice = 139.00; stock = 20; status = 1 },
            @{ id = $detail.skus[2].id; skuCode = 'AUTO-03'; specData = '{"颜色":"蓝色","尺码":"M"}'; specInfo = '蓝色 / M'; price = 119.00; originalPrice = 149.00; stock = 10; status = 1 }
        )
    } $S.adminToken | Out-Null
    $edited = Api GET "/api/admin/products/$($S.createdProductId)" $null $S.adminToken
    Check ([double]$edited.price -eq 88.00) "编辑后最低价 = $($edited.price)"
    Check ([int]$edited.stock -eq 80) "编辑后总库存 = $($edited.stock)（50+20+10）"

    Api PUT "/api/admin/products/$($S.createdProductId)/status?status=0" $null $S.adminToken | Out-Null
    Check ((Api GET "/api/admin/products/$($S.createdProductId)" $null $S.adminToken).status -eq 0) '商品下架成功'
    Api PUT "/api/admin/products/$($S.createdProductId)/status?status=1" $null $S.adminToken | Out-Null
    Check ((Api GET "/api/admin/products/$($S.createdProductId)" $null $S.adminToken).status -eq 1) '商品重新上架成功'
}

Step '9b. 库存预警与阈值配置' {
    $warn = Api GET '/api/admin/stock/warnings?page=1&size=10' $null $S.adminToken
    Check ($warn.total -ge 1) "库存预警条目 = $($warn.total)"
    Check ((Api GET '/api/admin/stock/config' $null $S.adminToken).threshold -eq 10) '当前预警阈值 = 10'
    Api PUT '/api/admin/stock/config' @{ threshold = 12 } $S.adminToken | Out-Null
    Check ((Api GET '/api/admin/stock/config' $null $S.adminToken).threshold -eq 12) '阈值修改为 12 成功'
    Api PUT '/api/admin/stock/config' @{ threshold = 10 } $S.adminToken | Out-Null

    $sku = (Api GET "/api/admin/products/$($S.createdProductId)" $null $S.adminToken).skus[0]
    Api PUT "/api/admin/stock/$($sku.id)" @{ stock = 3 } $S.adminToken | Out-Null
    $warn2 = Api GET '/api/admin/stock/warnings?page=1&size=100' $null $S.adminToken
    $hit = $warn2.records | Where-Object { $_.skuId -eq $sku.id }
    Check ($null -ne $hit) "调整库存为 3 后出现在预警列表（stock=$($hit.stock)，阈值=$($hit.warnStock)）"
}

Step '9c. 商品 Excel 导出 / 模板下载 / 导入' {
    $tmp = [System.IO.Path]::GetTempPath()
    $xlsx = Join-Path $tmp 'mall_products.xlsx'
    Invoke-WebRequest -Uri "$Base/api/admin/products/export?page=1&size=20" -Headers @{ Authorization = "Bearer $($S.adminToken)" } -OutFile $xlsx -TimeoutSec 60
    Check ((Get-Item $xlsx).Length -gt 2000) "商品导出 Excel 成功，大小 = $([math]::Round((Get-Item $xlsx).Length/1KB,1)) KB"

    $tpl = Join-Path $tmp 'mall_template.xlsx'
    Invoke-WebRequest -Uri "$Base/api/admin/products/template" -Headers @{ Authorization = "Bearer $($S.adminToken)" } -OutFile $tpl -TimeoutSec 60
    Check ((Get-Item $tpl).Length -gt 1000) "导入模板下载成功，大小 = $([math]::Round((Get-Item $tpl).Length/1KB,1)) KB"
}

Step '9d. 删除测试商品' {
    if ($S.createdProductId) {
        Api DELETE "/api/admin/products/$($S.createdProductId)" $null $S.adminToken | Out-Null
        $gone = $false
        try { Api GET "/api/admin/products/$($S.createdProductId)" $null $S.adminToken | Out-Null } catch { $gone = $true }
        Check $gone '测试商品已删除'
    }
}

# ------------------------------------------------------------------ 10. 看板
Step '10. 数据看板' {
    $overview = Api GET '/api/admin/dashboard/overview' $null $S.adminToken
    Check ([double]$overview.totalSales -gt 0) "累计销售额 = $($overview.totalSales)"
    Check ([int]$overview.totalOrders -ge 40) "累计订单数 = $($overview.totalOrders)"
    Check ([int]$overview.totalUsers -ge 6) "会员总数 = $($overview.totalUsers)"
    Check ($null -ne $overview.onlineUsers) "在线用户数 = $($overview.onlineUsers)，库存预警 = $($overview.stockWarnings)"
    $trend = Api GET '/api/admin/dashboard/sales-trend?days=7' $null $S.adminToken
    Check ($trend.Count -eq 7) "销售趋势返回连续 7 天（实际 $($trend.Count) 天）"
    $cat = Api GET '/api/admin/dashboard/category-sales' $null $S.adminToken
    Check ($cat.Count -ge 1) "分类销售统计 = $($cat.Count) 条"
    $top = Api GET '/api/admin/dashboard/top-products?limit=10' $null $S.adminToken
    Check ($top.Count -ge 1) "热销排行 = $($top.Count) 条"
    $status = Api GET '/api/admin/dashboard/order-status' $null $S.adminToken
    Check ($status.Count -ge 1) "订单状态分布 = $($status.Count) 组（示例：$($status[0].statusName) $($status[0].orderCount) 单）"
}

# ------------------------------------------------------------------ 11. 系统管理
Step '11. 系统管理（管理员/角色/权限/配置/日志）' {
    Check ((Api GET '/api/admin/sys/users?page=1&size=10' $null $S.adminToken).total -ge 4) '管理员列表可用'
    $roles = Api GET '/api/admin/sys/roles' $null $S.adminToken
    Check ($roles.Count -eq 3) "角色数 = $($roles.Count)"
    $roleWithPerms = $roles | Where-Object { $_.permissionIds.Count -gt 0 } | Select-Object -First 1
    Check ($null -ne $roleWithPerms) "角色权限已关联（$($roleWithPerms.roleName) 有 $($roleWithPerms.permissionIds.Count) 个权限）"
    Check ((Api GET '/api/admin/sys/permissions' $null $S.adminToken).Count -ge 5) '权限树可用'
    Check ((Api GET '/api/admin/sys/configs' $null $S.adminToken).Count -ge 9) '系统配置项 >= 9'
    $logs = Api GET '/api/admin/sys/logs?page=1&size=10' $null $S.adminToken
    Check ($logs.total -ge 1) "操作日志 total = $($logs.total)"
    $hasOpLog = ($logs.records | Where-Object { $_.operation -match '发货|备注|商品|库存|售后' }).Count -ge 1
    Check $hasOpLog 'AOP 已记录本次测试的管理操作'
}

# ------------------------------------------------------------------ 12. 会员
Step '12. 会员管理（含消费统计）' {
    $members = Api GET '/api/admin/users?page=1&size=10' $null $S.adminToken
    Check ($members.total -ge 7) "会员总数 = $($members.total)（含注册的测试账号）"
    $withOrder = $members.records | Where-Object { [int]$_.orderCount -gt 0 } | Select-Object -First 1
    Check ($null -ne $withOrder) "会员消费统计生效（$($withOrder.username)：订单 $($withOrder.orderCount) 笔，消费 $($withOrder.totalConsumption)）"
    if ($withOrder) {
        $detail = Api GET "/api/admin/users/$($withOrder.id)" $null $S.adminToken
        Check ($null -ne $detail.addressCount) "会员详情可访问：地址 $($detail.addressCount) 个，收藏 $($detail.favoriteCount) 个，评价 $($detail.commentCount) 条"
    }
}

# ------------------------------------------------------------------ 13. 营销
Step '13. 营销内容（优惠券/轮播图/公告/评价）' {
    Check ((Api GET '/api/admin/coupons?page=1&size=10' $null $S.adminToken).total -ge 5) '优惠券列表可用'
    Check ((Api GET '/api/admin/banners?page=1&size=10' $null $S.adminToken).total -ge 5) '轮播图列表可用'
    Check ((Api GET '/api/admin/notices?page=1&size=10' $null $S.adminToken).total -ge 5) '公告列表可用'
    $cmts = Api GET '/api/admin/comments?page=1&size=10' $null $S.adminToken
    Check ($cmts.total -ge 1) "商品评价列表可用，总数 = $($cmts.total)"
    if ($cmts.records.Count -gt 0) {
        $cid = $cmts.records[0].id
        Api PUT "/api/admin/comments/$cid/reply" @{ replyContent = '感谢您的评价，欢迎再次光临！' } $S.adminToken | Out-Null
        $replied = Api GET '/api/admin/comments?page=1&size=10' $null $S.adminToken
        $hit = $replied.records | Where-Object { $_.id -eq $cid }
        Check ([bool]$hit.replyContent) "评价回复成功：$($hit.replyContent)"
    }
}

# ------------------------------------------------------------------ 14. 退出登录
Step '14. 退出登录后 Token 立即失效' {
    $tmpToken = (Api POST '/api/admin/auth/login' @{ username = 'warehouse'; password = '123456' }).token
    Api GET '/api/admin/orders?page=1&size=1' $null $tmpToken | Out-Null
    Ok 'warehouse 登录后可访问订单列表'
    Api POST '/api/admin/auth/logout' $null $tmpToken | Out-Null
    $invalid = $false
    try { Api GET '/api/admin/orders?page=1&size=1' $null $tmpToken | Out-Null } catch { $invalid = $true }
    Check $invalid '退出登录后旧 Token 立即失效（Redis 登录态已删除）'
}

# ------------------------------------------------------------------ 汇总
Write-Host "`n==============================================" -ForegroundColor Yellow
$color = if ($S.fail -eq 0) { 'Green' } else { 'Red' }
Write-Host (" 验证完成：通过 {0} 项，失败 {1} 项" -f $S.pass, $S.fail) -ForegroundColor $color
if ($S.fail -gt 0) {
    Write-Host ' 失败明细：' -ForegroundColor Red
    $S.failures | ForEach-Object { Write-Host "   - $_" -ForegroundColor Red }
}
Write-Host "==============================================" -ForegroundColor Yellow

if ($S.fail -gt 0) { exit 1 } else { exit 0 }
