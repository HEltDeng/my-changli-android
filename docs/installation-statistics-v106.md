# V1.0.6 安装身份与可选设备统计

包名 com.mychangli.app，versionCode 38。正式签名沿用既有证书，ARM64 正式构建未变。旧 V1.0.5 APK 不覆盖。

- InstallationIdentity 在 noBackupFilesDir 用 AtomicFile 保存随机 ID 与安装证明；同步首次创建。覆盖升级仅在确有旧安装标识和 package upgrade 证据时复用旧 ID。登录退出、页面切换、网络重试不生成新 ID。
- 云备份和 device-transfer 显式排除应用身份及凭据；缺少 no-backup 身份而存在迁移标记时清除旧平台会话。厂商私有迁移行为不能承诺永久完全一致。
- DeviceStatisticsConsentDialog 独立告知，旧基础隐私同意不等于新增统计同意。设置→隐私→设备与使用统计可拒绝或撤回；每轮选择只问一次。
- Build 厂商/品牌/型号、Android/API 和 App 版本仅在专项同意后采集。ANDROID_ID 经应用隔离派生后发送，由服务器再次 HMAC；不采集 IMEI、IMSI、序列号、广告标识、通讯录、精确位置或应用列表。不新增 READ_PHONE_STATE。
- InstallationTelemetry 使用现有 Retrofit/协程，前台 15 分钟节流；无后台保活。稳定 eventId 待发文件重试；撤回立即删除待发资料，仅保留最小撤回指令。统计失败不清除账号会话；认证过期可凭安装证明提交自己的统计，不取得账号权限。
- 匿名身份与设备辅助键分离；服务端关联只接受真实登录令牌。派生设备标识不是登录凭据。
- 首次上报提供 NEW/UPGRADE 与 firstInstallTime，服务端将升级和历史补登记排除于今日新安装增长。

验证：127 项 JVM 测试；本地隔离后端的 InstallationIntegrationTest 验证 80 个并发读取、拒绝、激活、15 次前台触发、退出、撤回。模拟器 x86_64，仅 Debug 用 -PidentityTestAbi=x86_64；正式任务禁止该参数。
PENDING_REAL_DEVICE：正式 ARM64 APK 真实手机覆盖升级、课表/成绩/学校绑定本地数据、双设备/多账号、卸载重装和厂商迁移。没有把模拟器或构建成功当作实机验收。

本次普通更新，不提高最低版本，不启用强制更新。原有强更撤回与公告去重逻辑保留，相关 JVM 回归测试继续执行。
