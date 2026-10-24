"""Open-Meteo weather integration with caching and timeouts."""

from __future__ import annotations

import json
import os
import requests
from datetime import datetime, date, timedelta
from typing import Optional, Dict, Any, List, Tuple

HISTORICAL_URL = "https://archive-api.open-meteo.com/v1/archive"
FORECAST_URL = "https://api.open-meteo.com/v1/forecast"

CACHE_FILE = "data/external/weather_cache.json"


def _load_file_cache() -> dict:
    if os.path.exists(CACHE_FILE):
        try:
            with open(CACHE_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return {}
    return {}


def _save_file_cache(cache: dict) -> None:
    os.makedirs(os.path.dirname(CACHE_FILE), exist_ok=True)
    try:
        with open(CACHE_FILE, "w", encoding="utf-8") as f:
            json.dump(cache, f, indent=2)
    except Exception:
        pass


def get_coordinates(locality: str) -> Optional[Tuple[float, float]]:
    from .schemas import METRO_MANILA_LOCALITIES
    return METRO_MANILA_LOCALITIES.get(locality)


def fetch_historical(locality: str, start_date: str, end_date: str, connect_timeout: float = 5, read_timeout: float = 15) -> Dict[str, Any]:
    coords = get_coordinates(locality)
    if coords is None:
        raise ValueError(f"No documented coordinates for locality: {locality}")
    lat, lon = coords
    params = {
        "latitude": lat,
        "longitude": lon,
        "start_date": start_date,
        "end_date": end_date,
        "daily": "precipitation_sum",
        "timezone": "Asia/Manila",
    }
    resp = requests.get(HISTORICAL_URL, params=params, timeout=(connect_timeout, read_timeout))
    resp.raise_for_status()
    data = resp.json()
    payload = {
        "source": "Open-Meteo Historical Weather API",
        "location": locality,
        "latitude": lat,
        "longitude": lon,
        "units": "mm (daily precipitation_sum)",
        "start_date": start_date,
        "end_date": end_date,
        "daily": data.get("daily", {}),
        "retrieved_at": datetime.now().isoformat(timespec="seconds"),
    }
    # cache
    cache = _load_file_cache()
    cache[locality] = payload
    _save_file_cache(cache)
    try:
        from .storage import cache_weather
        cache_weather(locality, payload)
    except Exception:
        pass
    return payload


def fetch_forecast(locality: str, days: int = 7, connect_timeout: float = 5, read_timeout: float = 15) -> Dict[str, Any]:
    coords = get_coordinates(locality)
    if coords is None:
        raise ValueError(f"No documented coordinates for locality: {locality}")
    lat, lon = coords
    params = {
        "latitude": lat,
        "longitude": lon,
        "daily": "precipitation_sum",
        "forecast_days": days,
        "timezone": "Asia/Manila",
    }
    resp = requests.get(FORECAST_URL, params=params, timeout=(connect_timeout, read_timeout))
    resp.raise_for_status()
    data = resp.json()
    payload = {
        "source": "Open-Meteo Forecast API",
        "location": locality,
        "latitude": lat,
        "longitude": lon,
        "units": "mm (daily precipitation_sum)",
        "daily": data.get("daily", {}),
        "retrieved_at": datetime.now().isoformat(timespec="seconds"),
    }
    cache = _load_file_cache()
    cache[locality + "_forecast"] = payload
    _save_file_cache(cache)
    return payload


def rolling_7day_total(payload: Dict[str, Any]) -> Optional[float]:
    daily = payload.get("daily", {}) or {}
    precip = daily.get("precipitation_sum")
    if not precip or len(precip) < 7:
        return None
    last7 = precip[-7:]
    if any(v is None for v in last7):
        return None
    return float(sum(last7))


def cache_age_minutes(payload: Dict[str, Any]) -> Optional[float]:
    ra = payload.get("retrieved_at") or payload.get("_cached_at")
    if not ra:
        return None
    try:
        dt = datetime.fromisoformat(ra)
        return (datetime.now() - dt).total_seconds() / 60.0
    except Exception:
        return None


def load_cached(locality: str) -> Optional[Dict[str, Any]]:
    cache = _load_file_cache()
    if locality in cache:
        return cache[locality]
    # try sqlite
    try:
        from .storage import latest_weather_cache
        return latest_weather_cache(locality)
    except Exception:
        return None
