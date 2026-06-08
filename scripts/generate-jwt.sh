#!/usr/bin/env bash
set -euo pipefail

profile="system-ingestor"
subject="local-reviewer"
issuer="${FRAUD_SECURITY_JWT_ISSUER_URI:-fraud-rule-engine-local}"
audience="${FRAUD_SECURITY_JWT_AUDIENCES:-fraud-rule-engine-service}"
expires_in_seconds="${FRAUD_LOCAL_JWT_TTL_SECONDS:-3600}"
out_file=""
custom_scopes=()

usage() {
  cat <<'USAGE'
Usage: scripts/generate-jwt.sh [--profile system-ingestor|fraud-analyst|rule-admin] [options]

Options:
  --profile VALUE            JWT profile. Default: system-ingestor
  --subject VALUE             JWT subject. Default: local-reviewer
  --issuer VALUE              JWT issuer. Default: FRAUD_SECURITY_JWT_ISSUER_URI or fraud-rule-engine-local
  --audience VALUE            Comma-separated audiences. Default: FRAUD_SECURITY_JWT_AUDIENCES or fraud-rule-engine-service
  --expires-in-seconds VALUE  Token lifetime. Default: FRAUD_LOCAL_JWT_TTL_SECONDS or 3600
  --scope VALUE               Add a custom scope. Can be repeated.
  --out-file PATH             Also write the token to PATH.
  --help                      Show this help text.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile)
      profile="$2"
      shift 2
      ;;
    --subject)
      subject="$2"
      shift 2
      ;;
    --issuer)
      issuer="$2"
      shift 2
      ;;
    --audience)
      audience="$2"
      shift 2
      ;;
    --expires-in-seconds)
      expires_in_seconds="$2"
      shift 2
      ;;
    --scope)
      custom_scopes+=("$2")
      shift 2
      ;;
    --out-file)
      out_file="$2"
      shift 2
      ;;
    --help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if ! command -v openssl >/dev/null 2>&1; then
  echo "OpenSSL is required. On Windows, use scripts/generate-jwt.ps1 instead." >&2
  exit 1
fi

case "$profile" in
  system-ingestor)
    profile_scopes=("transactions:evaluate")
    ;;
  fraud-analyst)
    profile_scopes=("fraud-alerts:read" "actuator:read")
    ;;
  rule-admin)
    profile_scopes=("rules:read" "rules:admin" "actuator:read")
    ;;
  *)
    echo "Unknown profile: $profile" >&2
    usage >&2
    exit 2
    ;;
esac

if [[ ${#custom_scopes[@]} -gt 0 ]]; then
  scopes=("${custom_scopes[@]}")
else
  scopes=("${profile_scopes[@]}")
fi

json_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  printf '%s' "$value"
}

b64url() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

audience_json() {
  local output="["
  local first="true"
  IFS=',' read -r -a audience_values <<< "$audience"
  for value in "${audience_values[@]}"; do
    value="$(printf '%s' "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    [[ -z "$value" ]] && continue
    if [[ "$first" == "true" ]]; then
      first="false"
    else
      output+=","
    fi
    output+="\"$(json_escape "$value")\""
  done
  output+="]"
  printf '%s' "$output"
}

scope_string="${scopes[*]}"
issued_at="$(date -u +%s)"
expires_at="$((issued_at + expires_in_seconds))"
header='{"alg":"RS256","typ":"JWT","kid":"local-dev-rsa"}'
payload="$(printf '{"iss":"%s","sub":"%s","aud":%s,"scope":"%s","iat":%s,"exp":%s,"token_use":"local-dev"}' \
  "$(json_escape "$issuer")" \
  "$(json_escape "$subject")" \
  "$(audience_json)" \
  "$(json_escape "$scope_string")" \
  "$issued_at" \
  "$expires_at")"

encoded_header="$(printf '%s' "$header" | b64url)"
encoded_payload="$(printf '%s' "$payload" | b64url)"
signing_input="$encoded_header.$encoded_payload"
private_key_file="$(mktemp)"
trap 'rm -f "$private_key_file"' EXIT

private_key_label="PRIVATE KEY"
private_key_der='
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCk2N6ffat5vmlc
rj+C+6qGK3dzcmgXbCdqlUEdkWtTbNO6LzDlojYbHX49h2+sE498puJ6ffkbHYxC
zORsDiIJXSMoPzKGFCm6iMCZiptLf2tz9WWz6k2f2xcCsrACDwwbbyEcHTWhnQqX
5llxbZq5Cs/qy/jCj34tDUYsfK+4t8KQRegJ7B2hhxUWjF1iUloXT7foZGO2qCDD
YO6YWmvISypUV0FEdHES+jEz2w81f4KBYl/XFrDKUTQ+sGniw3ELWd+70Fg78vo0
5RzROZ4qWJ1AsriX8MD8l0lNr2dia+rGLj577QXf1NMh64Kh0f09SizMAUSiRslx
VBSpPcmJAgMBAAECggEAIrNCHZmjFHQIEsndNbHLQgX5VZRyI+gQ3gdAFzzKtlt0
sD8J0HTWZdvN/J67W6IcdLVefEFzRAlywocF2FcbRBRWd7zh4kZxSEJMJKYALaLL
WDQCbh52q50/WUKK+Uv4lPfOJPBjHzuDBNmo308eoDAN91NrcG0rNsmSCYBPrxas
/TW0ZjhKiV3RR2hOLP74qS7wUp3pFxTIDTJ/ES30Lb7Tsmp0Bk2tENBkM+AkdKiz
sedFtegt4G+6XizaQiUtDJ7TZnEZDF/G+Rqel7eCLlhacPmx+sqf9yzOx7n5YvKC
H7LvJKizFRqrxBhXaB3amxlGsaNfBJ1u42vRCQlfbwKBgQDRxdiRwnJa4ut7dBA+
Ndjlr3u48bgvZKO/a5dR2DlE/Nszel9tHiXsqkjz51M5NZai3RrZTV/NWJ3bblfh
3WwgiD7tJhAoTJDVFxMFrHnin4vykuV/FasCG3IiGXu5xgKIySbPS8JvmMI6Jgph
bXgv7Xs/cXalIiwg7nt9q/fNmwKBgQDJLJRegrIv5uD0ESWIyLXhxr6B0M2hbNMj
Kzj50/m/tUVshlR0udawdCIl2PY+G1oqWW5mXc+lr19AUhmY5fZ47FySwLgPP3Mn
tRWiJ8f247RQneVeKwQ8RXU+uVeOO6F4JC7JclL9a3nxU+knzV143FmpVd0t4wWd
Y5e3U8cJqwKBgQC7bLT6TqyTzemuDN4yZzQay/aUIMx688VmU0AJLVLF89H6JZ07
RlOGKAM0gPuXwuyLKVhCzWIKe+HW27kDoi1ox7LJele3WemRqqMhT6A7Pmfw3RTV
vktjf5gzJWepbWR4SJk3X64ivW7UO4bM090tnVagMcVa97RL3ChT+aFXfwKBgAhR
lZkK4n9klqzBAMJVOmAprbsEfVxNebWclfCOuWcaAdLpAxkIjj4hrz/NZvFOhD34
EL6e5nU9eTzZqEFQUQ7TB0jyOyo2P3bG4CwiZPxqkgw7Wz63nKc5YA3F8D7rbFPh
eyXNWm0sQpCeHagNr+3Nfs+nw0ugdZz6f02PY02dAoGBALb8CTmMlgucoMHp+PgR
+aghJ8KOLp+AKc5/VRqP+3Ng5+urLKXMpD8cImQNJ3HZfNc8grVSddI0GL2M7Pym
aLrJ/t8fTeMZUAGoanp4760M7zILFoF1Az68MALqthgdAEqAtzNrN9o2kbXhrsHP
SDSgmso/+3J0MD25PkOWJA2F
'
{
  printf '%s\n' "-----BEGIN ${private_key_label}-----"
  printf '%s' "$private_key_der" | tr -d '[:space:]' | fold -w 64
  printf '\n%s\n' "-----END ${private_key_label}-----"
} > "$private_key_file"

signature="$(printf '%s' "$signing_input" | openssl dgst -binary -sha256 -sign "$private_key_file" | b64url)"
token="$signing_input.$signature"

if [[ -n "$out_file" ]]; then
  mkdir -p "$(dirname "$out_file")"
  printf '%s' "$token" > "$out_file"
fi

printf '%s\n' "$token"
