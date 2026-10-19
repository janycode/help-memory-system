# 领域文档

工程技能在探索代码库时，应如何消费本仓库的领域文档。

## 探索前先读这些

- 仓库根目录的 **`GLOSSARY.md`**；或
- 若存在 **`GLOSSARY-MAP.md`**：它指向每个上下文各自的 `GLOSSARY.md`，读取与当前主题相关的那些。
- **`docs/adr/`**：读取涉及待改动区域的 ADR。多上下文仓库还要看 `src/<context>/docs/adr/` 下的上下文级决策。

上述文件不存在时**静默继续**：不提示缺失，也不提前建议创建。`/domain-modeling` 技能（由 `/grill-with-docs` 与 `/improve-codebase-architecture` 进入）会在术语或决策真正落地时按需创建。

## 文件结构

单上下文仓库（本仓库属此类）：

```
/
├── GLOSSARY.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

多上下文仓库（根目录存在 `GLOSSARY-MAP.md`）：

```
/
├── GLOSSARY-MAP.md
├── docs/adr/                          ← 全系统级决策
└── src/
    ├── ordering/
    │   ├── GLOSSARY.md
    │   └── docs/adr/                  ← 上下文级决策
    └── billing/
        ├── GLOSSARY.md
        └── docs/adr/
```

## 使用术语表的词汇

输出中出现领域概念时（issue 标题、重构提案、假设、测试名），使用 `GLOSSARY.md` 定义的术语，不要漂移到术语表明确回避的同义词。

需要的概念尚未收录时，这是一个信号：要么你在发明项目并不使用的语言（重新考虑），要么确有空缺（记下来交给 `/domain-modeling`）。

## 标出 ADR 冲突

输出与既有 ADR 矛盾时，显式说明，而不是静默覆盖：

> 与 ADR-0007（事件溯源订单）冲突，但值得重新讨论，因为……
