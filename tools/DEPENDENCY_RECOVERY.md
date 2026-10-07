> 2026-10-07：现行依赖已切换为无认证值的 external-auth AAR；旧 no-sensitive-logs 文件和旧恢复步骤仅为历史记录。当前恢复方式、MIT 来源确认及本地 Debug 限制见 [认证配置与 Mac 恢复](../docs/csustdataget-debug-auth-recovery.md)。

# CSUSTDataGet 安全依赖恢复

现行源码保持原位置：`D:\Codex\01_App-应用开发\001_MyChangLi-我的长理\01_Repositories\CodexWorkspace-projects\CSUSTDataGet-1.1.4-safe`。
基础提交：`7c2dfa9384ab2117ec917ff52b7d3b6b3b2f2894`（浅历史边界保留）。10项 tracked 修改与未跟踪 SafeLog.kt 都是必要恢复内容。

Android 实际读取 Git 已跟踪的 `app/libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar`，SHA256：`9b03523fa04e9c8b3f5caaad49dc282b17eb2ac82e69a49e89764b21abf71c29`。完整远端 Android clone 包含此 AAR，不复制旧 build。

补丁源码使用受保护的第二阶段快照 repo-01 恢复，不新增远端、不公开授权未核清的源码：
1. 在新建、非开发入口的空目录将 shallow-complete-git.zip 解压到 .git；确保 .git/refs 存在，保留 shallow。
2. 仅在恢复目录执行 git restore --source=HEAD --worktree --staged . 取得基础源码，禁止在现行脏树运行。
3. 应用 staged.patch（非空时 git apply --index），再应用 unstaged.patch。
4. 解压 untracked-and-local-inputs.zip，再解压 modified-files-exact-bytes.zip，保留原 CRLF 字节及删除状态。
5. 核对下表及第三阶段 evidence/dependency-recovery.json、第二阶段 repo-01/state.json 的完整文件 SHA 和 Git 状态。

本轮已独立恢复，全部文件哈希与11项 dirty 一致。原快照未改；恢复检出仅为验收夹具，不作为开发入口。快照：`D:\Codex\98_Archive-已归档\MyChangLi-Phase2-Recovery-20261001-NOT_A_DEVELOPMENT_SOURCE`。

已证实源码与补丁可恢复、AAR 已跟踪；未证实补丁树重建能获得现存 AAR 的相同字节，缺少当时完整构建回执。不得声称完成源码到 AAR 的可重现构建证明，也不能将现在快照当作正式 APK 原始脏树。

| 文件 | 未跟踪 | SHA256 |
|---|---|---|
| csust_spider/src/main/java/com/dcelysia/csust_spider/campus_card/CampusCardHelper.kt | False | ba404f0bb2187a688333c0c0d86a208e4ef8bda88158bc6a318dbeccb639edc5 |
| csust_spider/src/main/java/com/dcelysia/csust_spider/core/AESUtils.kt | False | 183b13c3ca7bc82fa697ae8c24fe6af137b03b2741f79fda6bd5e5235dc0bec4 |
| csust_spider/src/main/java/com/dcelysia/csust_spider/core/NetWorkLogger.kt | False | 69b7d1fff7f2c07004717eabeb171f63a14d6a5c7d2703ed92d0659b2be8f972 |
| csust_spider/src/main/java/com/dcelysia/csust_spider/core/NetworkRetryInterceptor.kt | False | be80a242495494e08e70b056b07ccbc0e5bf6e352eac052ee97fee43e997d307 |
| csust_spider/src/main/java/com/dcelysia/csust_spider/education/data/remote/EducationHelper.kt | False | ae6410eba57ad9a224b2748dbe2f82d50218bee1be951dffd3b8111837e38805 |
| csust_spider/src/main/java/com/dcelysia/csust_spider/education/data/remote/repository/EducationRepository.kt | False | 134c6dbe87325c91057dbdd4ee7300eed904d24a66a545f2ae948dd525bc05ba |
| csust_spider/src/main/java/com/dcelysia/csust_spider/education/data/remote/services/AuthService.kt | False | 9147f2278deaf40521c2f642c583e419b580d5ffb101eb849284ca5321f890fc |
| csust_spider/src/main/java/com/dcelysia/csust_spider/education/data/remote/services/ExamArrangeService.kt | False | e5c5f69ac351a132e5d54dac3202a07a7bedae908f7aee24a83c6aee9f2452f9 |
| csust_spider/src/main/java/com/dcelysia/csust_spider/mooc/cookie/PersistentCookieJar.kt | False | c3eec80ae4d9648cf7eb0bbd9757024358258555abe0eb3f4d047391aa04b52d |
| csust_spider/src/main/java/com/dcelysia/csust_spider/mooc/data/remote/repository/MoocRepository.kt | False | 4a475e410172d81d2f564b5a1cbdd254d0d83963f36e481a2e989781cc8dd617 |
| csust_spider/src/main/java/com/dcelysia/csust_spider/core/SafeLog.kt | True | e70b4116ee35cd4b77f2ad0330d5a4bde9e74a6c44208a938b4cbb9df07f7bac |
