# Issue tracker：GitHub

本仓库的 issue 与规格文档存放于 GitHub Issues，所有操作通过 `gh` CLI 完成。

## 约定

- **创建 issue**：`gh issue create --title "..." --body "..."`。多行正文用 heredoc。
- **读取 issue**：`gh issue view <number> --json number,title,body,labels,comments`。
- **列出 issue**：`gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`，按需加 `--label` 与 `--state` 过滤。
- **把 issue 设为子 issue**：`gh issue create --parent <parent> ...`，或事后 `gh issue edit <parent> --add-sub-issue <child>`（`gh` 2.94+）。旧版 `gh` 用 `gh api --method POST repos/<owner>/<repo>/issues/<parent>/sub_issues -F sub_issue_id=<child-db-id>`（此处为数据库 id，见下文 **阻塞**）。若仓库未启用子 issue，在子 issue 正文首行写 `Part of #<parent>`。
- **评论 issue**：`gh issue comment <number> --body "..."`。
- **加/移除标签**：`gh issue edit <number> --add-label "..."` / `--remove-label "..."`。
- **关闭**：`gh issue close <number> --comment "..."`。

仓库由 `git remote -v` 推断；在克隆目录内执行时 `gh` 会自动识别。

## 以 PR 作为 triage 入口

**PRs as a request surface: no.**（本仓库不把外部 PR 当作需求入口。若需 `/triage` 覆盖 PR，改为 `yes`。）

置为 `yes` 时，PR 走与 issue 相同的标签与状态流转，使用对应的 `gh pr` 命令：

- **读取 PR**：`gh pr view <number> --comments`；看 diff 用 `gh pr diff <number>`。
- **列出待 triage 的外部 PR**：`gh api --paginate 'repos/{owner}/{repo}/pulls?state=open' --jq '.[] | select(.author_association | IN("OWNER","MEMBER","COLLABORATOR") | not) | {number, title, author: .user.login, author_association, labels: [.labels[].name]}'`。
- **评论 / 打标签 / 关闭**：`gh pr comment`、`gh pr edit --add-label`/`--remove-label`、`gh pr close`。

GitHub 的 issue 与 PR 共用同一编号空间，裸写 `#42` 可能是两者之一：先 `gh pr view 42`，失败再回退 `gh issue view 42`。

## 当技能要求【发布到 issue tracker】

创建一条 GitHub issue。

## 当技能要求【取出对应的 ticket】

按上文 **读取 issue** 执行。

## Wayfinding 操作

供 `/wayfinder` 使用。**地图**是一条 issue，**子 issue** 即 ticket。

- **地图**：一条带 `wayfinder:map` 标签的 issue，正文承载 Notes / Decisions-so-far / Fog。`gh issue create --label wayfinder:map`。
- **子 ticket**：通过 GitHub 子 issue 关联到地图（见 **把 issue 设为子 issue**）。未启用子 issue 时，把子 issue 加进地图正文的 task list，并在子 issue 正文首行写 `Part of #<map>`。标签：`wayfinder:<type>`（`research`/`prototype`/`grilling`/`task`）。认领后指派给执行的开发者。
- **阻塞**：使用 GitHub **原生 issue 依赖**，这是标准且在 UI 可见的表达方式。添加边：`gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`，其中 `<blocker-db-id>` 是阻塞方的数字**数据库 id**（`gh api repos/<owner>/<repo>/issues/<n> --jq .id`，不是 `#number`，也不是 `node_id`）。GitHub 通过 `issue_dependencies_summary.blocked_by` 汇报（仅未关闭的阻塞方，即实时门禁）。若环境不支持依赖，回退为子 issue 正文首行 `Blocked by: #<n>, #<n>`。所有阻塞方关闭后该 ticket 即解除阻塞。
- **前沿查询**：列出地图下未关闭的子 issue（`gh issue list --state open`，范围限定在地图的子 issue / task list），剔除有未关闭阻塞方者（`issue_dependencies_summary.blocked_by > 0`，或 `Blocked by` 行中仍有未关闭 issue）或已有指派人者；按地图顺序取第一个。
- **认领**：`gh issue edit <n> --add-assignee @me`，这是本次会话的第一次写操作。
- **解决**：`gh issue comment <n> --body "<answer>"`，再 `gh issue close <n>`，最后把上下文指针（gist + 链接）追加到地图的 Decisions-so-far。
