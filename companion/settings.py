"""Persistent, validated settings for the companion."""
import json

DEFAULT_COOLDOWN = 300
MAX_MINUTES = 999

def cooldown_seconds(minutes, seconds):
    if not str(minutes).isdigit() or not str(seconds).isdigit():
        raise ValueError('Enter whole numbers for minutes and seconds.')
    minutes, seconds = int(minutes), int(seconds)
    if minutes > MAX_MINUTES or seconds > 59:
        raise ValueError('Use 0–999 minutes and 0–59 seconds.')
    return minutes * 60 + seconds

def load_settings(path):
    try:
        value = json.loads(path.read_text(encoding='utf-8'))['cooldown_seconds']
        if type(value) is int and 0 <= value <= MAX_MINUTES * 60 + 59:
            return value
    except (OSError, ValueError, TypeError, KeyError):
        pass
    return DEFAULT_COOLDOWN

def save_settings(path, value):
    path.write_text(json.dumps({'cooldown_seconds': value}), encoding='utf-8')
