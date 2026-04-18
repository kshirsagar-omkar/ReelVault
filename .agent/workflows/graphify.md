# /graphify — Build Knowledge Graph

Build a knowledge graph from any folder. Reads code, docs, papers, images — extracts concepts and relationships, clusters into communities, produces an interactive HTML graph, queryable JSON, and audit report.

## Trigger
`/graphify`

## Steps

### 1. Ensure graphify is installed
```bash
PYTHON=$(cat graphify-out/.graphify_python 2>/dev/null)
if [ -z "$PYTHON" ]; then
    GRAPHIFY_BIN=$(which graphify 2>/dev/null)
    if [ -n "$GRAPHIFY_BIN" ]; then
        PYTHON=$(head -1 "$GRAPHIFY_BIN" | tr -d '#!')
        case "$PYTHON" in *[!a-zA-Z0-9/_.-]*) PYTHON="python3" ;; esac
    else
        PYTHON="python3"
    fi
    mkdir -p graphify-out
    "$PYTHON" -c "import sys; open('graphify-out/.graphify_python', 'w').write(sys.executable)"
fi
"$PYTHON" -c "import graphify; print(f'graphify {graphify.__version__} ready')" 2>/dev/null || echo "ERROR: graphify not installed. Run: pip3 install graphifyy"
```

### 2. Follow the full graphify pipeline
Execute the graphify skill pipeline for the user command:
- `/graphify` or `/graphify .` — full pipeline on current directory
- `/graphify <path>` — full pipeline on specific path
- `/graphify <path> --update` — incremental update only
- `/graphify query "<question>"` — query existing graph
- `/graphify path "A" "B"` — shortest path between nodes
- `/graphify explain "Node"` — explain a node
- `/graphify add <url>` — add URL content to graph

### 3. Present results
Show God Nodes, Surprising Connections, and Suggested Questions from GRAPH_REPORT.md.
