# Request correlation id

Operations wants to trace requests across systems. Every response must carry the header
`X-Request-Id`.

**Acceptance criteria**
- If the request has `X-Request-Id`, the response echoes it unchanged.
- Otherwise the server generates a new UUID.
- On error responses, the OperationOutcome's first issue has `diagnostics` that contain the
  request id.
