import sys
from pathlib import Path
import pytest
sys.path.insert(0, str(Path(__file__).parents[1] / 'companion'))
from settings import cooldown_seconds, load_settings, save_settings

def test_cooldown_validation_and_persistence(tmp_path):
    path = tmp_path / 'settings.json'
    assert load_settings(path) == 300
    assert cooldown_seconds('0', '0') == 0
    assert cooldown_seconds('12', '34') == 754
    save_settings(path, 754)
    assert load_settings(path) == 754
    for minutes, seconds in [('1', '60'), ('1000', '0'), ('-1', '0'), ('x', '0')]:
        with pytest.raises(ValueError):
            cooldown_seconds(minutes, seconds)
    path.write_text('{broken', encoding='utf-8')
    assert load_settings(path) == 300
