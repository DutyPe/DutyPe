"""
Reads every .md and .csv file from web/content/marketing/ and generates:
1. Individual TSX page files under web/app/admin/marketing/ (one per document)
2. A shared data index (web/lib/marketing-data.ts) with section metadata
"""

import os
import re
import json

CONTENT_ROOT = os.path.join("web", "content", "marketing")
APP_MARKETING = os.path.join("web", "app", "admin", "marketing")
DATA_FILE = os.path.join("web", "lib", "marketing-data.ts")

SECTION_ICONS = {
    "assets": "🎨",
    "campaigns": "📣",
    "inputs": "📥",
    "outputs": "📤",
    "playbooks": "📘",
    "research": "🔬",
    "brand": "🪣",
    "channels": "📡",
    "go-to-market": "🚀",
    "growth": "🌱",
    "marketing": "📢",
}

def slugify(name):
    """Convert filename to URL-safe slug: investor_one_pager.md -> investor-one-pager"""
    name = re.sub(r'\.(md|csv)$', '', name, flags=re.IGNORECASE)
    return name.replace('_', '-').lower()

def escape_for_template(content):
    """Escape backticks and ${} for JS template literals"""
    content = content.replace('\\', '\\\\')
    content = content.replace('`', '\\`')
    content = content.replace('${', '\\${')
    return content

def title_from_filename(name):
    """Convert filename to display title: investor_one_pager.md -> Investor One Pager"""
    name = re.sub(r'\.(md|csv)$', '', name, flags=re.IGNORECASE)
    return name.replace('_', ' ').replace('-', ' ').title()

def collect_files(root):
    """Walk the content tree and collect all files with metadata"""
    files = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames.sort()
        filenames.sort()
        for fname in filenames:
            if not (fname.endswith('.md') or fname.endswith('.csv')):
                continue
            abs_path = os.path.join(dirpath, fname)
            rel = os.path.relpath(abs_path, root).replace('\\', '/')
            parts = rel.split('/')
            # parts like: ['growth', 'assets', 'investor_one_pager.md']
            with open(abs_path, 'r', encoding='utf-8') as f:
                content = f.read()
            files.append({
                'rel': rel,
                'parts': parts,
                'filename': fname,
                'ext': os.path.splitext(fname)[1].lower(),
                'content': content,
                'slug_parts': [slugify(p) if p.endswith(('.md', '.csv')) else p.replace('_', '-') for p in parts],
                'title': title_from_filename(fname),
            })
    return files

def build_section_tree(files):
    """Build a nested tree of sections for the landing page"""
    tree = {}
    for f in files:
        parts = f['slug_parts']
        node = tree
        for i, part in enumerate(parts[:-1]):
            if part not in node:
                node[part] = {'_children': {}, '_files': []}
            node = node[part]['_children']
        # Add file to the deepest directory
        leaf_dir = parts[-2] if len(parts) > 1 else '_root'
        if leaf_dir not in tree:
            # Navigate to the right spot
            pass
        # Simpler: just track section paths
    return tree

def generate_page_tsx(file_info):
    """Generate a TSX page file for a single document"""
    ext = file_info['ext']
    content_escaped = escape_for_template(file_info['content'])
    title = file_info['title']
    
    if ext == '.csv':
        return f'''import {{ CsvTable }} from "@/components/marketing-shell";

export default function Page() {{
  const csv = `{content_escaped}`;

  return (
    <div className="marketing-page-single">
      <h1>{title}</h1>
      <CsvTable csv={{csv}} />
    </div>
  );
}}
'''
    else:
        return f'''import {{ MarkdownView }} from "@/components/marketing-shell";

export default function Page() {{
  const content = `{content_escaped}`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={{content}} />
    </div>
  );
}}
'''

def generate_section_page(section_name, icon, children_info):
    """Generate a section index page that links to all docs in that section"""
    links_jsx = ""
    for child in children_info:
        slug = '/'.join(child['slug_parts'])
        links_jsx += f'            <li><a href="/admin/marketing/{slug}">{child["title"]}</a></li>\n'
    
    return f'''import Link from "next/link";

export default function Page() {{
  return (
    <div className="marketing-page-single">
      <h2>{icon} {section_name.replace("-", " ").title()}</h2>
      <ul className="marketing-section-links">
{links_jsx}      </ul>
    </div>
  );
}}
'''

def main():
    files = collect_files(CONTENT_ROOT)
    print(f"Found {len(files)} content files")
    
    # Remove old catch-all route
    catchall_dir = os.path.join(APP_MARKETING, "[[...slug]]")
    catchall_page = os.path.join(catchall_dir, "page.tsx")
    if os.path.exists(catchall_page):
        os.remove(catchall_page)
        print(f"Removed old catch-all: {catchall_page}")
        try:
            os.rmdir(catchall_dir)
        except:
            pass
    
    # Generate individual page files
    created = 0
    for f in files:
        slug_parts = f['slug_parts']
        page_dir = os.path.join(APP_MARKETING, *slug_parts)
        os.makedirs(page_dir, exist_ok=True)
        page_file = os.path.join(page_dir, "page.tsx")
        tsx_content = generate_page_tsx(f)
        with open(page_file, 'w', encoding='utf-8') as out:
            out.write(tsx_content)
        created += 1
        print(f"  Created: {'/'.join(slug_parts)}/page.tsx")
    
    # Build sections for the landing page data
    sections = {}  # section_path -> list of files
    for f in files:
        parts = f['slug_parts']
        if len(parts) >= 2:
            section_key = parts[0]  # growth or marketing
            subsection = parts[1] if len(parts) >= 3 else '_root'
            key = f"{section_key}/{subsection}"
        else:
            key = '_root'
        if key not in sections:
            sections[key] = []
        sections[key].append({
            'slug': '/'.join(parts),
            'title': f['title'],
            'ext': f['ext'],
        })
    
    # Generate the landing page with all content inline
    landing_sections = []
    for f in files:
        content_escaped = escape_for_template(f['content'])
        landing_sections.append({
            'slug_parts': f['slug_parts'],
            'title': f['title'],
            'ext': f['ext'],
            'content_escaped': content_escaped,
        })
    
    # Group by top-level and sub-level directories
    growth_subs = {}
    marketing_subs = {}
    for f in files:
        parts = f['slug_parts']
        top = parts[0]  # growth or marketing
        sub = parts[1] if len(parts) >= 3 else '_root'
        target = growth_subs if top == 'growth' else marketing_subs
        if sub not in target:
            target[sub] = []
        target[sub].append(f)
    
    # Generate the landing page TSX
    landing_page = generate_landing_page(growth_subs, marketing_subs)
    
    landing_file = os.path.join(APP_MARKETING, "page.tsx")
    with open(landing_file, 'w', encoding='utf-8') as out:
        out.write(landing_page)
    print(f"\nCreated landing page: {landing_file}")
    print(f"\nTotal: {created} document pages + 1 landing page")

def generate_landing_page(growth_subs, marketing_subs):
    """Generate the main marketing landing page with all content embedded inline"""
    
    # Build the file blocks for each section
    def build_section_blocks(subs_dict, top_name):
        blocks = ""
        for sub_name in sorted(subs_dict.keys()):
            files = subs_dict[sub_name]
            icon = SECTION_ICONS.get(sub_name, "📁")
            display_name = sub_name.replace("-", " ").title()
            if sub_name == '_root':
                display_name = top_name.title()
                icon = SECTION_ICONS.get(top_name, "📁")
            
            blocks += f'''
        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>{icon}</span>
            <span>{display_name}</span>
            <small>{len(files)} doc{"s" if len(files) != 1 else ""}</small>
          </h3>
          <div className="marketing-full-files">
'''
            for f in files:
                content_escaped = escape_for_template(f['content'])
                title = f['title']
                slug_path = '/'.join(f['slug_parts'])
                
                if f['ext'] == '.csv':
                    blocks += f'''            <details className="marketing-full-file" open>
              <summary>
                <span className="marketing-full-file-icon">📊</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/{slug_path}">{title}</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <CsvTable csv={{`{content_escaped}`}} />
              </div>
            </details>
'''
                else:
                    blocks += f'''            <details className="marketing-full-file" open>
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/{slug_path}">{title}</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={{`{content_escaped}`}} />
              </div>
            </details>
'''
            blocks += '''          </div>
        </section>
'''
        return blocks
    
    growth_blocks = build_section_blocks(growth_subs, "growth")
    marketing_blocks = build_section_blocks(marketing_subs, "marketing")
    
    # Build TOC
    toc_items = ""
    for sub_name in sorted(growth_subs.keys()):
        icon = SECTION_ICONS.get(sub_name, "📁")
        display = sub_name.replace("-", " ").title()
        if sub_name == '_root':
            continue
        toc_items += f'            <li><a href="#growth-{sub_name}">{icon} {display}</a></li>\n'
    for sub_name in sorted(marketing_subs.keys()):
        icon = SECTION_ICONS.get(sub_name, "📁")
        display = sub_name.replace("-", " ").title()
        if sub_name == '_root':
            continue
        toc_items += f'            <li><a href="#marketing-{sub_name}">{icon} {display}</a></li>\n'
    
    total_files = sum(len(v) for v in growth_subs.values()) + sum(len(v) for v in marketing_subs.values())
    total_sections = len([k for k in growth_subs if k != '_root']) + len([k for k in marketing_subs if k != '_root'])
    
    return f'''"use client";

import {{ CsvTable, MarkdownView }} from "@/components/marketing-shell";

export default function MarketingLandingPage() {{
  return (
    <div className="marketing-landing">
      <header className="marketing-topbar">
        <div>
          <h1>Marketing & Growth</h1>
          <p>Every campaign, asset, input, output, playbook, channel, brand, go-to-market and research document.</p>
        </div>
      </header>

      <div className="marketing-full">
        <aside className="marketing-full-toc">
          <h3>On this page</h3>
          <small>{total_files} documents · {total_sections} sections</small>
          <ul>
{toc_items}          </ul>
        </aside>

        <div className="marketing-full-stream">
          <section className="marketing-full-section marketing-full-depth-0">
            <h2 className="marketing-full-heading">
              <span>🌱</span>
              <span>Growth</span>
              <small>{sum(len(v) for v in growth_subs.values())} docs</small>
            </h2>
{growth_blocks}
          </section>

          <section className="marketing-full-section marketing-full-depth-0">
            <h2 className="marketing-full-heading">
              <span>📢</span>
              <span>Marketing</span>
              <small>{sum(len(v) for v in marketing_subs.values())} docs</small>
            </h2>
{marketing_blocks}
          </section>
        </div>
      </div>
    </div>
  );
}}
'''

if __name__ == "__main__":
    main()
