# TODO

## 删除好友功能 - 待办

### 1. 删不删会话和聊天记录（重要，待拍板）

**现状**：`FriendServiceImpl.deleteSessionRecords` 删除好友时会物理删除 `Session` 和 `UserSession`，但聊天记录 `Message`（挂在 `sessionId` 下）没有删除，导致：

- 消息物理上还在库里，但前端通过 `getSessionIdsByUserId` 已拿不到该 `sessionId`，聊天历史永远查不到
- 相当于「消息没真删，但成了孤儿数据」，两头不讨好

**二选一**：

- **方案 A**：删好友只断关系，保留会话和聊天记录（用户还能看历史，只是不能再发消息）
- **方案 B**：删好友彻底断干净，连同 `Message` 一起按 `sessionId` 删除

### 2. 小待办

- [ ] `FriendServiceImpl` 通配符 import（`model.entity.*`）改回显式 import
- [ ] 注释「与 MessageValidationServiceImpl 保持一致」——该类尚不存在，需实现或修正注释
- [ ] `evictFriendCache` 的 Javadoc 提到「拉黑 / 取消拉黑」场景，但这两个功能尚未实现
- [ ] 实现消息校验时的好友状态缓存读写（`msg:validate:friend:status:{userId}:{friendId}`）
