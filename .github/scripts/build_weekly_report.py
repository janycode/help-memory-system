#!/usr/bin/env python3
"""生成开源周报：GitHub Trending(weekly) + HelloGitHub 最新一期，输出企业微信纯文本 payload。

用法：
    python build_weekly_report.py                 # 打印周报正文，便于本地查看
    python build_weekly_report.py --payload a.json # 同时写出企微 payload JSON
    python build_weekly_report.py --translate      # 调用 GitHub Models 把英文简介翻译为中文

退出码：0 正常（含单来源失败、翻译失败回退英文）；2 两个来源均失败。
"""
import argparse
import json
import os
import re
import sys
from datetime import date

import requests
from bs4 import BeautifulSoup

UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
TRENDING_URL = "https://github.com/trending?since=weekly"
HG_README_URL = "https://raw.githubusercontent.com/521xueweihan/HelloGitHub/master/README.md"
HG_CONTENT_URL = "https://raw.githubusercontent.com/521xueweihan/HelloGitHub/master/content/HelloGitHub{}.md"
MODELS_URL = "https://models.github.ai/inference/chat/completions"

MAX_TRENDING = 10
MAX_HELLOGITHUB = 10
# 企业微信 text 消息 content 上限 2048 字节
BYTE_BUDGET = 2048


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
            stars = f"⭐+{num} 本周"

        items.append({
            "repo": repo,
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


def clean_text(text):
    """去掉 Markdown 链接、图片与 HTML 标签，压成一行纯文本。"""
    text = re.sub(r"!\[[^\]]*\]\([^)]*\)", "", text)
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = re.sub(r"<[^>]+>", "", text)
    return re.sub(r"\s+", " ", text).strip()


def parse_hello_github(content_md):
    """解析 HelloGitHub 某期正文，返回项目条目列表（项目名 + 一句话简介）。"""
    body = content_md.split("## 内容", 1)[-1]
    items = []
    for line in body.splitlines():
        if line.startswith("### "):
            continue
        m = re.match(r"^\d+、\[([^\]]+)\]\([^)]*\)[：:]?(.*)$", line.strip())
        if not m:
            continue
        name = clean_text(m.group(1))
        desc = clean_text(m.group(2))
        items.append({"name": name, "desc": desc})
        if len(items) >= MAX_HELLOGITHUB:
            break
    return items


def translate_texts(texts, token, model="gpt-4o-mini"):
    """调用 GitHub Models 把英文简介批量翻译成简洁中文，返回 {下标: 译文}。失败返回空字典。"""
    if not texts or not token:
        return {}
    try:
        resp = requests.post(
            MODELS_URL,
            headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"},
            json={
                "model": model,
                "messages": [
                    {"role": "system", "content": "你是翻译助手。把用户给的 JSON 数组中每条英文项目简介翻译成简洁中文，"
                                                  "每条不超过 30 个汉字，保留原意，不要添加任何解释。"
                                                  "直接返回同长度的 JSON 字符串数组。"},
                    {"role": "user", "content": json.dumps(texts, ensure_ascii=False)},
                ],
                "temperature": 0.2,
            },
            timeout=60,
        )
        resp.raise_for_status()
        content = resp.json()["choices"][0]["message"]["content"].strip()
        content = re.sub(r"^```(?:json)?\s*|\s*```$", "", content)
        arr = json.loads(content)
        return {i: str(s).strip() for i, s in enumerate(arr) if str(s).strip()}
    except Exception as e:  # noqa: BLE001 - 翻译失败只回退英文，不阻断周报
        print(f"翻译失败，回退英文原文：{e}", file=sys.stderr)
        return {}


def render(trending, hg_volume, hg_items, today, trans, desc_len, t_count, h_count):
    lines = [f"🔥 开源周报 | {today}", ""]

    if trending:
        lines.append(f"【GitHub Trending · 本周热门 Top {t_count}】")
        for i, it in enumerate(trending[:t_count], 1):
            line = f"{i}. {it['repo']}"
            if it["stars"]:
                line += f"（{it['stars']}）"
            lines.append(line)
            desc = trans.get(i - 1, it["desc"])
            if desc and desc_len:
                lines.append(f"   {desc[:desc_len]}")
    else:
        lines.append("⚠️ GitHub Trending 本板块抓取失败")

    lines.append("")
    if hg_items:
        title = f"【HelloGitHub 第 {hg_volume} 期精选】" if hg_volume else "【HelloGitHub 最新刊精选】"
        lines.append(title)
        base = len(trending)
        for j, it in enumerate(hg_items[:h_count], 1):
            lines.append(f"{j}. {it['name']}")
            desc = trans.get(base + j - 1, it["desc"])
            if desc and desc_len:
                lines.append(f"   {desc[:desc_len]}")
    else:
        lines.append("⚠️ HelloGitHub 本板块抓取失败")

    return "\n".join(lines)


def build_message(trending, hg_volume, hg_items, today, trans):
    """按字节预算选择描述长度与条数组合，优先保条目数、压缩描述。"""
    candidates = [
        (40, 10, 10), (30, 10, 10), (24, 10, 10), (0, 10, 10),
        (30, 8, 8), (24, 8, 8), (0, 8, 8),
        (24, 6, 6), (0, 6, 6), (0, 4, 4),
    ]
    for desc_len, t_count, h_count in candidates:
        msg = render(trending, hg_volume, hg_items, today, trans, desc_len, t_count, h_count)
        if len(msg.encode("utf-8")) <= BYTE_BUDGET:
            return msg
    return render(trending, hg_volume, hg_items, today, trans, 0, 1, 1)


def main():
    parser = argparse.ArgumentParser(description="生成开源周报企微 payload")
    parser.add_argument("--payload", help="将企微 payload JSON 写入指定文件")
    parser.add_argument("--translate", action="store_true", help="调用 GitHub Models 把英文简介翻译为中文")
    parser.add_argument("--token", help="GitHub Models 访问令牌，缺省读环境变量 GH_MODELS_TOKEN")
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

    trans = {}
    if args.translate:
        descs = [it["desc"] for it in trending] + [it["desc"] for it in hg_items]
        pending_index = [i for i, d in enumerate(descs) if d]
        raw = translate_texts([descs[i] for i in pending_index],
                              args.token or os.environ.get("GH_MODELS_TOKEN"))
        for pos, i in enumerate(pending_index):
            if pos in raw:
                trans[i] = raw[pos]

    message = build_message(trending, hg_volume, hg_items, today, trans)
    print(message)

    if args.payload:
        payload = {"msgtype": "text", "text": {"content": message}}
        with open(args.payload, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False)
        print(f"\npayload 已写入 {args.payload}", file=sys.stderr)

    return 0


if __name__ == "__main__":
    sys.exit(main())
