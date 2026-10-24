"""Provisional rules-based triage indicator engine.

This is NOT a validated clinical model. It produces a triage indicator score
for decision support only. It is not a probability of infection or severe disease.
"""

from __future__ import annotations

from typing import List, Tuple, Optional
from .schemas import (
    EXPOSURE_TYPES,
    SYMPTOM_STATUSES,
    PatientEncounter,
)


EXPOSURE_WEIGHTS = {
    "no direct contact": 0,
    "contact with intact skin": 5,
    "contact involving an open wound": 18,
    "ingestion": 22,
}

# Feature contributions are points, not probabilities.
def assess_encounter(
    age: int,
    sex: str,
    locality: str,
    exposure_type: str,
    exposure_duration_hours: float,
    days_since_exposure: int,
    temperature_c: Optional[float],
    temperature_measured: bool,
    calf_tenderness: str,
    conjunctival_suffusion: str,
    systolic_bp: Optional[int],
    bp_measured: bool,
    notes_missing: str = "",
) -> Tuple[float, str, List[str], float]:
    """Return (score 0-100, priority tier, key factors, data completeness 0-1)."""
    score = 0.0
    factors: List[str] = []

    # Exposure-based triage indicators (not diagnosis)
    score += EXPOSURE_WEIGHTS.get(exposure_type, 0)
    if EXPOSURE_WEIGHTS.get(exposure_type, 0) > 0:
        factors.append(f"Exposure type: {exposure_type} (+{EXPOSURE_WEIGHTS[exposure_type]})")

    if exposure_duration_hours >= 12:
        score += 8
        factors.append("Prolonged water exposure >=12h (+8)")
    elif exposure_duration_hours >= 3:
        score += 4
        factors.append("Water exposure >=3h (+4)")

    # Incubation window 2-30 days; peak 5-14
    if 2 <= days_since_exposure <= 30:
        score += 6
        factors.append("Within leptospirosis incubation window (+6)")
        if 5 <= days_since_exposure <= 14:
            score += 6
            factors.append("Peak incubation period 5-14 days (+6)")
    elif days_since_exposure > 30:
        factors.append("Beyond typical incubation window (no added score)")

    # Symptoms
    if calf_tenderness == "assessed_present":
        score += 12
        factors.append("Calf tenderness present (+12)")
    elif calf_tenderness == "not_assessed":
        factors.append("Calf tenderness not assessed (unknown, not treated as absent)")

    if conjunctival_suffusion == "assessed_present":
        score += 12
        factors.append("Conjunctival suffusion present (+12)")
    elif conjunctival_suffusion == "not_assessed":
        factors.append("Conjunctival suffusion not assessed (unknown, not treated as absent)")

    if temperature_measured and temperature_c is not None:
        if temperature_c >= 38.0:
            score += 12
            factors.append(f"Fever >=38.0C (+12)")
        elif temperature_c >= 37.5:
            score += 6
            factors.append(f"Low-grade fever (+6)")
    else:
        factors.append("Temperature not measured (unknown, not treated as absent)")

    if bp_measured and systolic_bp is not None:
        if systolic_bp < 90:
            score += 15
            factors.append("Hypotension systolic <90 (+15)")
        elif systolic_bp < 100:
            score += 6
            factors.append("Low-normal blood pressure (+6)")
    else:
        factors.append("Blood pressure not measured (unknown, not treated as absent)")

    # Age risk
    if age < 5 or age >= 65:
        score += 6
        factors.append("Age in higher-risk group (+6)")
    elif age < 15:
        score += 3
        factors.append("Pediatric age (+3)")

    # Data completeness
    measured_items = 0
    total_items = 4  # temp, calf, conjunctival, bp
    if temperature_measured:
        measured_items += 1
    if calf_tenderness != "not_assessed":
        measured_items += 1
    if conjunctival_suffusion != "not_assessed":
        measured_items += 1
    if bp_measured:
        measured_items += 1
    completeness = measured_items / total_items

    if completeness < 0.5:
        factors.append("Low data completeness; assessment provisional")

    # Cap
    score = min(100.0, score)

    # Priority tier
    if score >= 55 or (bp_measured and systolic_bp is not None and systolic_bp < 90):
        tier = "High"
    elif score >= 30:
        tier = "Moderate"
    else:
        tier = "Routine"

    # Urgent clinical red flags: hypotension + fever in incubation window
    if bp_measured and systolic_bp is not None and systolic_bp < 90 and temperature_measured and temperature_c is not None and temperature_c >= 38.0:
        tier = "High"
        factors.append("URGENT CLINICAL RED FLAG: hypotension with fever (clinician review required)")

    return score, tier, factors, completeness


def feature_contributions(encounter: PatientEncounter) -> List[dict]:
    """Return list of {feature, contribution, note} for display."""
    rows = []
    rows.append({"feature": "exposure_type", "contribution": EXPOSURE_WEIGHTS.get(encounter.exposure_type, 0), "note": "Exposure route indicator"})
    rows.append({"feature": "exposure_duration_hours", "contribution": 8 if encounter.exposure_duration_hours >= 12 else (4 if encounter.exposure_duration_hours >= 3 else 0), "note": "Duration"})
    rows.append({"feature": "days_since_exposure", "contribution": (6 + (6 if 5 <= encounter.days_since_exposure <= 14 else 0)) if 2 <= encounter.days_since_exposure <= 30 else 0, "note": "Incubation window"})
    rows.append({"feature": "calf_tenderness", "contribution": 12 if encounter.calf_tenderness == "assessed_present" else 0, "note": "Unknown if not assessed"})
    rows.append({"feature": "conjunctival_suffusion", "contribution": 12 if encounter.conjunctival_suffusion == "assessed_present" else 0, "note": "Unknown if not assessed"})
    if encounter.temperature_measured and encounter.temperature_c is not None:
        tc = 12 if encounter.temperature_c >= 38 else (6 if encounter.temperature_c >= 37.5 else 0)
    else:
        tc = 0
    rows.append({"feature": "temperature_c", "contribution": tc, "note": "0 if unmeasured"})
    if encounter.bp_measured and encounter.systolic_bp is not None:
        bp = 15 if encounter.systolic_bp < 90 else (6 if encounter.systolic_bp < 100 else 0)
    else:
        bp = 0
    rows.append({"feature": "systolic_bp", "contribution": bp, "note": "0 if unmeasured"})
    rows.append({"feature": "age", "contribution": 6 if (encounter.age < 5 or encounter.age >= 65) else (3 if encounter.age < 15 else 0), "note": "Age group"})
    return rows
