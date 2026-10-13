# Design and Delivery Guides

> 13 nodes · cohesion 0.22

## Key Concepts

- **json** (7 connections) — `scripts/collect_test_evidence.py`
- **collect_test_evidence.py** (7 connections) — `scripts/collect_test_evidence.py`
- **pathlib** (6 connections) — `scripts/collect_test_evidence.py`
- **repair_source_manifest.py** (6 connections) — `scripts/repair_source_manifest.py`
- **runtime_sbom.py** (6 connections) — `scripts/runtime_sbom.py`
- **argparse** (4 connections) — `scripts/collect_test_evidence.py`
- **datetime** (2 connections) — `scripts/collect_test_evidence.py`
- **hashlib** (2 connections) — `scripts/install_ci_scanners.py`
- **parse** (2 connections) — `scripts/runtime_sbom.py`
- **Capture actual Surefire XML before another database run overwrites it.** (1 connections) — `scripts/collect_test_evidence.py`
- **Fingerprint repair inputs independently of generated QA and Graphify output.** (1 connections) — `scripts/repair_source_manifest.py`
- **Convert Maven's resolved runtime dependency list, including transitives, to…** (1 connections) — `scripts/runtime_sbom.py`
- **elementtree** (1 connections) — `scripts/collect_test_evidence.py`

## Relationships

- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (2 shared connections)

## Source Files

- `scripts/collect_test_evidence.py`
- `scripts/install_ci_scanners.py`
- `scripts/repair_source_manifest.py`
- `scripts/runtime_sbom.py`

## Audit Trail

- EXTRACTED: 19 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*