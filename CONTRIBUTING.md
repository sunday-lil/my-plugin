# 🤝 贡献指南 (Contributing Guide)

感谢你对 EssentialsX-Clone 项目的关注！我们欢迎任何形式的贡献，包括但不限于：

- 🐛 Bug 报告和修复
- ✨ 新功能建议和实现
- 📝 文档改进
- 🎨 UI/UX 优化
- ⚡ 性能优化
- 🧪 测试用例补充

---

## 📋 贡献流程

### 1. Fork 和 Clone

```bash
# Fork 本仓库到你的 GitHub 账号
# 然后克隆到本地
git clone https://github.com/你的用户名/my-plugin.git
cd my-plugin
```

### 2. 创建分支

```bash
# 创建功能分支（使用有意义的分支名）
git checkout -b feature/新功能名称
# 或修复分支
git checkout -b fix/问题描述
```

### 3. 开发和测试

#### 环境要求
- **Java**: 21+
- **Maven**: 3.6+
- **IDE**: IntelliJ IDEA（推荐）

#### 构建项目
```bash
mvn clean package
```

#### 运行测试
```bash
mvn test
```

### 4. 代码规范

#### Java 代码风格
- 使用 **4 个空格缩进**（不要用 Tab）
- 类名使用 **PascalCase**
- 方法名和变量名使用 **camelCase**
- 常量名使用 **UPPER_SNAKE_CASE**
- 每行最大长度：**120 字符**

#### 注释规范
```java
/**
 * 功能描述
 *
 * @param paramName 参数说明
 * @return 返回值说明
 */
public void exampleMethod(String paramName) {
    // 单行注释说明
}
```

#### Commit Message 格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型：**
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整（不影响功能）
- `refactor`: 重构代码
- `test`: 测试相关
- `chore`: 构建/工具链相关

**示例：**
```
feat(bank): 添加利息计算功能

实现了银行系统的每日利息自动结算机制：
- 支持配置年利率
- 支持自定义结算时间
- 自动通知玩家收益情况

Closes #123
```

### 5. 提交 Pull Request

1. 推送你的分支到 GitHub
   ```bash
   git push origin feature/新功能名称
   ```

2. 在 GitHub 上创建 Pull Request
3. 填写 PR 模板（如果有的话）
4. 等待 Code Review

---

## 🧪 测试指南

### 运行现有测试
```bash
mvn test
```

### 编写新测试
测试类位于：`src/main/java/org/ljcode/myPlugin/tests/`

**示例：**
```java
public class ExampleTest {
    
    @Test
    public void testExample() {
        // Arrange
        int expected = 5;
        
        // Act
        int actual = calculateSomething();
        
        // Assert
        assertEquals(expected, actual);
    }
}
```

---

## 📝 文档规范

### README.md 更新
添加新功能时，请同步更新 README.md 的对应章节。

### 配置文件文档
修改 `config.yml` 或其他配置文件时，请更新：
- README.md 中的配置示例
- 配置文件中的注释说明

---

## 🐛 Bug 报告模板

提交 Issue 时，请包含以下信息：

```markdown
**描述**: 清晰描述 bug 表现

**复现步骤**:
1. 执行步骤一
2. 执行步骤二
3. 观察结果

**预期行为**: 应该发生什么

**实际行为**: 实际发生了什么

**环境信息**:
- Minecraft 版本: 1.21.x
- 服务器核心: Spigot/Paper/Purpur
- 插件版本: x.x.x
- Java 版本: 21
- 其他相关插件:

**错误日志**:
[粘贴控制台错误信息]
```

---

## 💡 功能请求模板

```markdown
**功能描述**: 清晰描述你想要的功能

**使用场景**: 这个功能在什么情况下会用到

**预期效果**: 你期望这个功能如何工作

**替代方案**: 目前是否有其他方式实现同样的效果

**附加信息**: 任何其他相关信息
```

---

## 🔍 Code Review 标准

PR 会经过以下检查：

✅ **功能性**：代码是否正常工作  
✅ **代码质量**：是否符合编码规范  
✅ **性能**：是否有明显的性能问题  
✅ **安全性**：是否存在安全漏洞  
✅ **可维护性**：代码是否易于理解和维护  
✅ **测试**：是否有足够的测试覆盖  
✅ **文档**：是否更新了相关文档  

---

## 📞 联系方式

如有问题，可以通过以下方式联系：

- **GitHub Issues**: [创建 Issue](https://github.com/sunday-lil/my-plugin/issues)
- **Discussions**: [参与讨论](https://github.com/sunday-lil/my-plugin/discussions)

---

## 📄 许可证

通过贡献代码，你同意你的贡献将在 **MIT License** 下被授权。

---

**再次感谢你的贡献！🎉**