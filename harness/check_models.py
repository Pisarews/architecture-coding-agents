"""Print whether every OpenRouter model in models.yaml is currently listed (no key needed)."""
import json
import urllib.request
from pathlib import Path

import yaml

ids = {m["id"] for m in json.load(urllib.request.urlopen("https://openrouter.ai/api/v1/models"))["data"]}
for key, cfg in yaml.safe_load((Path(__file__).parent / "models.yaml").read_text()).items():
    if "openrouter" in cfg.get("model_class", "").lower():
        print(f"  {key:16} {cfg['model_name']:45} {'OK' if cfg['model_name'] in ids else 'MISSING'}")
