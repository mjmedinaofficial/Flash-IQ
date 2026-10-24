"""Queue ordering policies and override handling."""

from __future__ import annotations

from datetime import datetime, timedelta
from typing import List, Dict, Any

POLICIES = ["FIFO", "Risk-only", "Aging-weighted"]


def parse_dt(s: str) -> datetime:
    return datetime.fromisoformat(s)


def waiting_minutes(registered_at: str, now: str = None) -> float:
    now = parse_dt(now) if now else datetime.now()
    reg = parse_dt(registered_at)
    delta = (now - reg).total_seconds() / 60.0
    return max(0.0, delta)


def queue_score(row: Dict[str, Any], policy: str, now: str = None, aging_weight: float = 1.0, aging_cap_minutes: float = 10000.0) -> float:
    """Higher score = higher priority (served sooner)."""
    risk = float(row.get("risk_score", 0) or 0)
    wm = waiting_minutes(row["registered_at"], now)
    wm = min(wm, aging_cap_minutes)
    if policy == "FIFO":
        # earlier registration => higher priority; use negative timestamp
        ts = parse_dt(row["registered_at"]).timestamp()
        return -ts
    if policy == "Risk-only":
        return risk
    if policy == "Aging-weighted":
        return risk + aging_weight * (wm / 60.0)  # aging_weight points per hour
    raise ValueError(f"Unknown policy: {policy}")


def sort_queue(rows: List[Dict[str, Any]], policy: str, now: str = None, aging_weight: float = 1.0, aging_cap_minutes: float = 10000.0) -> List[Dict[str, Any]]:
    """Return ordered list. Urgent escalations always first (by escalation time)."""
    urgent = [r for r in rows if r.get("urgent")]
    routine = [r for r in rows if not r.get("urgent")]

    urgent_sorted = sorted(urgent, key=lambda r: r.get("urgent_at") or r["registered_at"])
    routine_sorted = sorted(routine, key=lambda r: r["registered_at"])
    routine_sorted = sorted(routine_sorted, key=lambda r: queue_score(r, policy, now, aging_weight, aging_cap_minutes), reverse=True)
    ordered = urgent_sorted + routine_sorted
    # assign positions
    for idx, r in enumerate(ordered, start=1):
        r["position"] = idx
    return ordered


def apply_override(rows: List[Dict[str, Any]], patient_id: str, new_position: int, reason: str) -> List[Dict[str, Any]]:
    """Move patient to new_position (1-based) in current ordered list; record handled by caller."""
    ordered = list(rows)
    idx = next((i for i, r in enumerate(ordered) if r["patient_id"] == patient_id), None)
    if idx is None:
        raise ValueError("Patient not in queue.")
    item = ordered.pop(idx)
    new_position = max(1, min(new_position, len(ordered) + 1))
    ordered.insert(new_position - 1, item)
    for i, r in enumerate(ordered, start=1):
        r["position"] = i
    item["clinician_override"] = True
    return ordered
