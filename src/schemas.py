"""Shared data schemas and validation helpers for LeptoShield."""

from __future__ import annotations

from dataclasses import dataclass, asdict
from datetime import datetime
from typing import Optional, List, Dict, Any
import re


EXPOSURE_TYPES = [
    "no direct contact",
    "contact with intact skin",
    "contact involving an open wound",
    "ingestion",
]

SEX_OPTIONS = ["female", "male", "unknown", "undisclosed"]

SYMPTOM_STATUSES = ["not_assessed", "assessed_absent", "assessed_present"]

PRIORITY_TIERS = ["Routine", "Moderate", "High"]

METRO_MANILA_LOCALITIES = {
    "Manila": (14.5995, 120.9842),
    "Quezon City": (14.6760, 121.0437),
    "Makati": (14.5547, 121.0244),
    "Pasig": (14.5764, 121.0851),
    "Taguig": (14.5176, 121.0509),
    "Caloocan": (14.6488, 120.9830),
    "Pasay": (14.5378, 121.0014),
    "Marikina": (14.6507, 121.1029),
    "Mandaluyong": (14.5794, 121.0359),
    "San Juan": (14.6019, 121.0355),
    "Parañaque": (14.4793, 121.0198),
    "Las Piñas": (14.4386, 120.9870),
    "Muntinlupa": (14.4081, 121.0415),
    "Valenzuela": (14.7014, 120.9830),
    "Navotas": (14.6614, 120.9408),
    "Malabon": (14.6625, 120.9568),
}


@dataclass
class PatientEncounter:
    patient_id: str
    registered_at: str
    age: int
    sex: str
    locality: str
    exposure_datetime: str
    exposure_type: str
    exposure_duration_hours: float
    days_since_exposure: int
    temperature_c: Optional[float]
    temperature_measured: bool
    calf_tenderness: str  # one of SYMPTOM_STATUSES
    conjunctival_suffusion: str
    systolic_bp: Optional[int]
    bp_measured: bool
    notes_missing: str
    risk_score: float
    priority_tier: str
    key_factors: List[str]
    data_completeness: float
    urgent: bool = False
    urgent_reason: str = ""
    urgent_at: str = ""
    clinician_override: bool = False

    def to_dict(self) -> Dict[str, Any]:
        d = asdict(self)
        d["key_factors"] = "; ".join(self.key_factors)
        return d


def validate_age(age) -> int:
    if age is None or age == "":
        raise ValueError("Age is required.")
    age = int(age)
    if age < 0 or age > 120:
        raise ValueError("Age must be between 0 and 120.")
    return age


def validate_exposure_type(value) -> str:
    if value not in EXPOSURE_TYPES:
        raise ValueError(f"Exposure type must be one of: {', '.join(EXPOSURE_TYPES)}.")
    return value


def validate_sex(value) -> str:
    if value not in SEX_OPTIONS:
        raise ValueError(f"Sex must be one of: {', '.join(SEX_OPTIONS)}.")
    return value


def validate_symptom_status(value) -> str:
    if value not in SYMPTOM_STATUSES:
        raise ValueError(f"Symptom status must be one of: {', '.join(SYMPTOM_STATUSES)}.")
    return value


def validate_duration(value) -> float:
    if value is None or value == "":
        raise ValueError("Exposure duration is required.")
    value = float(value)
    if value < 0 or value > 1000:
        raise ValueError("Exposure duration must be between 0 and 1000 hours.")
    return value


def validate_days_since(value) -> int:
    if value is None or value == "":
        raise ValueError("Days since exposure is required.")
    value = int(value)
    if value < 0 or value > 365:
        raise ValueError("Days since exposure must be between 0 and 365.")
    return value


def validate_temperature(value, measured: bool) -> Optional[float]:
    if not measured:
        return None
    if value is None or value == "":
        raise ValueError("Temperature marked as measured but no value provided.")
    value = float(value)
    if value < 30 or value > 45:
        raise ValueError("Temperature must be between 30 and 45 C.")
    return value


def validate_bp(value, measured: bool) -> Optional[int]:
    if not measured:
        return None
    if value is None or value == "":
        raise ValueError("Blood pressure marked as measured but no value provided.")
    # accept "120/80" or systolic only
    s = str(value).strip()
    m = re.match(r"^(\d{2,3})(?:/\d{2,3})?$", s)
    if not m:
        raise ValueError("Blood pressure must be like 120 or 120/80.")
    sys = int(m.group(1))
    if sys < 50 or sys > 260:
        raise ValueError("Systolic blood pressure out of range.")
    return sys


def parse_dt(value: str) -> datetime:
    return datetime.fromisoformat(value)


def now_iso() -> str:
    return datetime.now().isoformat(timespec="seconds")


def generate_patient_id(counter: int) -> str:
    return f"SYN-{counter:04d}"
