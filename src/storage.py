"""SQLite persistence for LeptoShield local records."""

from __future__ import annotations

import sqlite3
import json
import os
from datetime import datetime
from typing import List, Dict, Any, Optional

DEFAULT_DB = "data/processed/leptoshield.db"


def get_conn(db_path: str = DEFAULT_DB) -> sqlite3.Connection:
    os.makedirs(os.path.dirname(db_path) or ".", exist_ok=True)
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    return conn


def init_db(db_path: str = DEFAULT_DB) -> None:
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS encounters (
            patient_id TEXT PRIMARY KEY,
            registered_at TEXT NOT NULL,
            age INTEGER,
            sex TEXT,
            locality TEXT,
            exposure_datetime TEXT,
            exposure_type TEXT,
            exposure_duration_hours REAL,
            days_since_exposure INTEGER,
            temperature_c REAL,
            temperature_measured INTEGER,
            calf_tenderness TEXT,
            conjunctival_suffusion TEXT,
            systolic_bp INTEGER,
            bp_measured INTEGER,
            notes_missing TEXT,
            risk_score REAL,
            priority_tier TEXT,
            key_factors TEXT,
            data_completeness REAL,
            urgent INTEGER DEFAULT 0,
            urgent_reason TEXT DEFAULT '',
            urgent_at TEXT DEFAULT '',
            clinician_override INTEGER DEFAULT 0
        )
        """
    )
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS override_audit (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id TEXT NOT NULL,
            previous_position INTEGER,
            new_position INTEGER,
            timestamp TEXT NOT NULL,
            reason TEXT NOT NULL,
            actor TEXT DEFAULT 'demo_clinician'
        )
        """
    )
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS escalation_audit (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id TEXT NOT NULL,
            timestamp TEXT NOT NULL,
            reason TEXT NOT NULL,
            actor TEXT DEFAULT 'demo_clinician'
        )
        """
    )
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS dataset_provenance (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            filename TEXT NOT NULL,
            source_type TEXT NOT NULL,
            imported_at TEXT NOT NULL,
            row_count INTEGER,
            columns TEXT,
            notes TEXT
        )
        """
    )
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS model_results (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            model_name TEXT NOT NULL,
            trained_at TEXT NOT NULL,
            metrics_json TEXT,
            notes TEXT
        )
        """
    )
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS weather_cache (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            locality TEXT NOT NULL,
            payload_json TEXT NOT NULL,
            retrieved_at TEXT NOT NULL
        )
        """
    )
    conn.commit()
    conn.close()


def save_encounter(enc: Dict[str, Any], db_path: str = DEFAULT_DB) -> None:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute(
        """
        INSERT OR REPLACE INTO encounters (
            patient_id, registered_at, age, sex, locality, exposure_datetime, exposure_type,
            exposure_duration_hours, days_since_exposure, temperature_c, temperature_measured,
            calf_tenderness, conjunctival_suffusion, systolic_bp, bp_measured, notes_missing,
            risk_score, priority_tier, key_factors, data_completeness, urgent, urgent_reason,
            urgent_at, clinician_override
        ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """,
        (
            enc["patient_id"],
            enc["registered_at"],
            enc["age"],
            enc["sex"],
            enc["locality"],
            enc["exposure_datetime"],
            enc["exposure_type"],
            enc["exposure_duration_hours"],
            enc["days_since_exposure"],
            enc.get("temperature_c"),
            1 if enc.get("temperature_measured") else 0,
            enc["calf_tenderness"],
            enc["conjunctival_suffusion"],
            enc.get("systolic_bp"),
            1 if enc.get("bp_measured") else 0,
            enc.get("notes_missing", ""),
            enc["risk_score"],
            enc["priority_tier"],
            enc.get("key_factors", ""),
            enc.get("data_completeness", 0.0),
            1 if enc.get("urgent") else 0,
            enc.get("urgent_reason", ""),
            enc.get("urgent_at", ""),
            1 if enc.get("clinician_override") else 0,
        ),
    )
    conn.commit()
    conn.close()


def load_encounters(db_path: str = DEFAULT_DB) -> List[Dict[str, Any]]:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute("SELECT * FROM encounters ORDER BY registered_at")
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()
    for r in rows:
        r["temperature_measured"] = bool(r["temperature_measured"])
        r["bp_measured"] = bool(r["bp_measured"])
        r["urgent"] = bool(r["urgent"])
        r["clinician_override"] = bool(r["clinician_override"])
        r["key_factors_list"] = [x.strip() for x in (r.get("key_factors") or "").split(";") if x.strip()]
    return rows


def count_encounters(db_path: str = DEFAULT_DB) -> int:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) FROM encounters")
    n = cur.fetchone()[0]
    conn.close()
    return n


def next_patient_counter(db_path: str = DEFAULT_DB) -> int:
    return count_encounters(db_path) + 1


def record_override(patient_id: str, prev_pos: int, new_pos: int, reason: str, db_path: str = DEFAULT_DB) -> None:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO override_audit (patient_id, previous_position, new_position, timestamp, reason) VALUES (?,?,?,?,?)",
        (patient_id, prev_pos, new_pos, datetime.now().isoformat(timespec="seconds"), reason),
    )
    conn.commit()
    conn.close()


def load_overrides(db_path: str = DEFAULT_DB) -> List[Dict[str, Any]]:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute("SELECT * FROM override_audit ORDER BY timestamp DESC")
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()
    return rows


def record_escalation(patient_id: str, reason: str, db_path: str = DEFAULT_DB) -> None:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO escalation_audit (patient_id, timestamp, reason) VALUES (?,?,?)",
        (patient_id, datetime.now().isoformat(timespec="seconds"), reason),
    )
    conn.commit()
    conn.close()


def load_escalations(db_path: str = DEFAULT_DB) -> List[Dict[str, Any]]:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute("SELECT * FROM escalation_audit ORDER BY timestamp DESC")
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()
    return rows


def record_dataset_provenance(filename: str, source_type: str, row_count: int, columns: List[str], notes: str = "", db_path: str = DEFAULT_DB) -> None:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO dataset_provenance (filename, source_type, imported_at, row_count, columns, notes) VALUES (?,?,?,?,?,?)",
        (filename, source_type, datetime.now().isoformat(timespec="seconds"), row_count, json.dumps(columns), notes),
    )
    conn.commit()
    conn.close()


def load_dataset_provenance(db_path: str = DEFAULT_DB) -> List[Dict[str, Any]]:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute("SELECT * FROM dataset_provenance ORDER BY imported_at DESC")
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()
    return rows


def record_model_result(model_name: str, metrics: Dict[str, Any], notes: str = "", db_path: str = DEFAULT_DB) -> None:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO model_results (model_name, trained_at, metrics_json, notes) VALUES (?,?,?,?)",
        (model_name, datetime.now().isoformat(timespec="seconds"), json.dumps(metrics), notes),
    )
    conn.commit()
    conn.close()


def load_model_results(db_path: str = DEFAULT_DB) -> List[Dict[str, Any]]:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute("SELECT * FROM model_results ORDER BY trained_at DESC")
    rows = [dict(r) for r in cur.fetchall()]
    conn.close()
    for r in rows:
        try:
            r["metrics"] = json.loads(r.get("metrics_json") or "{}")
        except Exception:
            r["metrics"] = {}
    return rows


def cache_weather(locality: str, payload: Dict[str, Any], db_path: str = DEFAULT_DB) -> None:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute("INSERT INTO weather_cache (locality, payload_json, retrieved_at) VALUES (?,?,?)",
                (locality, json.dumps(payload), payload.get("retrieved_at", datetime.now().isoformat(timespec="seconds"))))
    conn.commit()
    conn.close()


def latest_weather_cache(locality: str, db_path: str = DEFAULT_DB) -> Optional[Dict[str, Any]]:
    init_db(db_path)
    conn = get_conn(db_path)
    cur = conn.cursor()
    cur.execute("SELECT payload_json, retrieved_at FROM weather_cache WHERE locality=? ORDER BY id DESC LIMIT 1", (locality,))
    row = cur.fetchone()
    conn.close()
    if not row:
        return None
    try:
        payload = json.loads(row["payload_json"])
        payload["_cached_at"] = row["retrieved_at"]
        return payload
    except Exception:
        return None
