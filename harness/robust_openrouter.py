"""OpenRouter model for mini-swe-agent with two robustness fixes needed by free endpoints:

1. Upstream errors that come back as HTTP 200 with an `error` body and no `choices` raise
   OpenRouterAPIError, so the standard retry loop handles them (stock 2.4.6 crashes with KeyError).
2. Request timeout of 300 s instead of 60 s (large reasoning models on free endpoints are slow).

Behaviour is otherwise identical to minisweagent.models.openrouter_model.OpenRouterModel.
"""

import json

import requests

from minisweagent.models.openrouter_model import (
    BASH_TOOL,
    OpenRouterAPIError,
    OpenRouterAuthenticationError,
    OpenRouterModel,
    OpenRouterRateLimitError,
)

TIMEOUT_S = 300


class RobustOpenRouterModel(OpenRouterModel):
    def _query(self, messages, **kwargs):
        headers = {"Authorization": f"Bearer {self._api_key}", "Content-Type": "application/json"}
        payload = {
            "model": self.config.model_name,
            "messages": messages,
            "tools": [BASH_TOOL],
            "usage": {"include": True},
            **(self.config.model_kwargs | kwargs),
        }
        try:
            response = requests.post(self._api_url, headers=headers, data=json.dumps(payload), timeout=TIMEOUT_S)
            response.raise_for_status()
            body = response.json()
        except requests.exceptions.HTTPError as e:
            if response.status_code == 401:
                raise OpenRouterAuthenticationError("Authentication failed.") from e
            if response.status_code == 429:
                raise OpenRouterRateLimitError("Rate limit exceeded") from e
            raise OpenRouterAPIError(f"HTTP {response.status_code}: {response.text[:500]}") from e
        except (requests.exceptions.RequestException, ValueError) as e:
            raise OpenRouterAPIError(f"Request failed: {e}") from e
        if not body.get("choices"):
            err = body.get("error") or {}
            if str(err.get("code")) == "429":
                raise OpenRouterRateLimitError(f"Rate limit exceeded (in body): {str(err)[:300]}")
            raise OpenRouterAPIError(f"No choices in response: {json.dumps(body)[:500]}")
        return body
