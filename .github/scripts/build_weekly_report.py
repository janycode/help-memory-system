#!/usr/bin/env python3
"""生成开源周报：GitHub Trending(weekly) + HelloGitHub 最新一期，输出企业微信 markdown payload。

用法：
    python build_weekly_report.py                 # 打印周报正文，便于本地查看
    python build_weekly_report.py --payload a.json # 同时写出企微 payload JSON

退出码：0 正常（含单来源失败）；2 两个来源均失败。
"""
import argparse
import json
import re
import sys
from datetime import date
from urllib.parse import parse_qs, unquote, urlparse

import requests
from bs4 import BeautifulSoup

UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
TRENDING_URL = "https://github.com/trending?since=weekly"
HG_README_URL = "https://raw.githubusercontent.com/521xueweihan/HelloGitHub/master/README.md"
HG_CONTENT_URL = "https://raw.githubusercontent.com/521xueweihan/HelloGitHub/master/content/HelloGitHub{}.md"

MAX_TRENDING = 10
MAX_HELLOGITHUB = 10
# 企业微信 markdown 消息 content 上限 4096 字节
BYTE_BUDGET = 4096


def fetch(url):
    resp = requests.get(url, headers={"User-Agent": UA}, timeout=30)
    resp.raise_for_status()
    return resp.text


def parse_trending(html):
    """解析 GitHub Trending weekly 页面，返回仓库条目列表。"""
    soup = BeautifulSoup(html, "html.parser")
    articles = soup.select("article.Box-row") or soup.select("article")
    items = []
    for art in articles:
        link = art.select_one("h2 a") or art.select_one("a[href^='/']")
        if not link:
            continue
        repo = (link.get("href") or "").strip("/")
        if not repo or "/" not in repo:
            continue
        desc_tag = art.select_one("p.color-fg-muted") or art.find("p")
        desc = desc_tag.get_text(" ", strip=True) if desc_tag else ""

        stars = ""
        m = re.search(r"([\d,\.]+)\s*(k?)\s*stars?\s*this\s*week",
                      art.get_text(" ", strip=True), re.I)
        if m:
            num = m.group(1).replace(",", "")
            if m.group(2).lower() == "k":
                num = f"{float(num):.1f}".rstrip("0").rstrip(".")
            else:
                num = str(int(float(num)))
            stars = f" ⭐+{num} 本周"

        items.append({
            "repo": repo,
            "url": f"https://github.com/{repo}",
            "desc": desc,
            "stars": stars,
        })
        if len(items) >= MAX_TRENDING:
            break
    return items


def latest_hello_github_volume(readme_md):
    """从 HelloGitHub README 的期刊表格中找出最大期号。"""
    volumes = [int(n) for n in re.findall(r"HelloGitHub(\d+)\.md", readme_md)]
    return max(volumes) if volumes else None


def real_url(url):
    """HelloGitHub 正文链接包了一层点击统计跳转，取出 target 里的真实地址。"""
    if "hellogithub.com/periodical/statistics/click" in url:
        target = parse_qs(urlparse(url).query).get("target", [""])[0]
        if target:
            return unquote(target)
    return url


def parse_hello_github(content_md):
    """解析 HelloGitHub 某期正文，返回项目条目列表（项目名 + 真实链接）。"""
    body = content_md.split("## 内容", 1)[-1]
    items = []
    for line in body.splitlines():
        if line.startswith("### "):
            continue
        m = re.match(r"^\d+、\[([^\]]+)\]\(([^)]+)\)[：:]?(.*)$", line.strip())
        if m:
            items.append({"name": m.group(1).strip(), "url": real_url(m.group(2))})
        if len(items) >= MAX_HELLOGITHUB:
            break
    return items


def render(trending, hg_volume, hg_items, today, desc_len, t_count, h_count):
    lines = [f"🔥 开源周报 | {today}", ""]

    if trending:
        lines.append(f"**GitHub Trending · 本周热门 Top {t_count}**")
        for i, it in enumerate(trending[:t_count], 1):
            lines.append(f"{i}. [{it['repo']}]({it['url']}){it['stars']}")
            desc = it["desc"]
            if desc and desc_len:
                lines.append(f"   {desc[:desc_len]}")
    else:
        lines.append("⚠️ GitHub Trending 本板块抓取失败")

    lines.append("")
    if hg_items:
        title = f"**HelloGitHub 第 {hg_volume} 期精选**" if hg_volume else "**HelloGitHub 最新刊精选**"
        lines.append(title)
        for i, it in enumerate(hg_items[:h_count], 1):
            lines.append(f"{i}. [{it['name']}]({it['url']})")
    else:
        lines.append("⚠️ HelloGitHub 本板块抓取失败")

    lines.append("")
    lines.append("数据来源：[GitHub Trending](https://github.com/trending?since=weekly) "
                 "· [HelloGitHub](https://github.com/521xueweihan/HelloGitHub)")
    return "\n".join(lines)


def build_message(trending, hg_volume, hg_items, today):
    """按字节预算选择描述长度与条数组合，优先保条目数、压缩描述。"""
    candidates = [
        (60, 10, 10), (40, 10, 10), (24, 10, 10), (0, 10, 10),
        (40, 8, 8), (24, 8, 8), (0, 8, 8),
        (24, 6, 6), (0, 6, 6), (0, 4, 4),
    ]
    for desc_len, t_count, h_count in candidates:
        msg = render(trending, hg_volume, hg_items, today, desc_len, t_count, h_count)
        if len(msg.encode("utf-8")) <= BYTE_BUDGET:
            return msg
    return render(trending, hg_volume, hg_items, today, 0, 1, 1)


def main():
    parser = argparse.ArgumentParser(description="生成开源周报企微 payload")
    parser.add_argument("--payload", help="将企微 payload JSON 写入指定文件")
    args = parser.parse_args()

    today = date.today().isoformat()
    trending, hg_volume, hg_items = [], None, []
    errors = []

    try:
        trending = parse_trending(fetch(TRENDING_URL))
    except Exception as e:  # noqa: BLE001 - 单来源失败不阻断整体
        errors.append(f"GitHub Trending: {e}")

    try:
        readme = fetch(HG_README_URL)
        volume = latest_hello_github_volume(readme)
        if volume:
            hg_volume = volume
            hg_items = parse_hello_github(fetch(HG_CONTENT_URL.format(volume)))
    except Exception as e:  # noqa: BLE001
        errors.append(f"HelloGitHub: {e}")

    if not trending and hg_volume is None:
        print("开源周报抓取失败，两个来源均不可用：" + "; ".join(errors), file=sys.stderr)
        return 2

    message = build_message(trending, hg_volume, hg_items, today)
    print(message)

    if args.payload:
        payload = {"msgtype": "markdown", "markdown": {"content": message}}
        with open(args.payload, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False)
        print(f"\npayload 已写入 {args.payload}", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
