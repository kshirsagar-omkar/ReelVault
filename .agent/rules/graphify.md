# graphify — Always-On Knowledge Graph Rules

When working in this project, follow these rules:

## Graph-First Navigation

1. **Before grepping or globbing files**, check if `graphify-out/GRAPH_REPORT.md` exists in the workspace root.
2. If it exists, **read `graphify-out/GRAPH_REPORT.md` first** — it contains:
   - **God nodes** — the highest-degree concepts everything connects through
   - **Community structure** — how the codebase clusters into logical groups
   - **Surprising connections** — non-obvious cross-cutting relationships
   - **Suggested questions** — what the graph is uniquely positioned to answer
3. Use the graph structure to navigate the codebase instead of keyword-searching through every file.
4. When answering architecture questions, **cite the graph** (node names, community labels, edge types).

## Graph Queries

For specific structural questions, use the graph directly:

```bash
PYTHON=$(cat graphify-out/.graphify_python 2>/dev/null || echo python3)
$PYTHON -m graphify.cli query "your question" --graph graphify-out/graph.json
$PYTHON -m graphify.cli path "NodeA" "NodeB" --graph graphify-out/graph.json
$PYTHON -m graphify.cli explain "NodeName" --graph graphify-out/graph.json
```

## When to Rebuild

- After significant code changes: suggest running `/graphify . --update`
- After adding new files: suggest running `/graphify .`
- The graph uses SHA256 caching — re-runs only process changed files

## Token Efficiency

The knowledge graph provides **71x fewer tokens per query** vs reading raw files.
Always prefer `GRAPH_REPORT.md` + targeted graph queries over reading entire source files.
