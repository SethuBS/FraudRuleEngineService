param(
    [Alias("Profile")]
    [ValidateSet("system-ingestor", "fraud-analyst", "rule-admin")]
    [string] $TokenProfile = "fraud-analyst",

    [string] $Subject = "local-reviewer",
    [string] $Issuer,
    [string] $Audience,
    [string[]] $Scopes = @(),
    [int] $ExpiresInSeconds = 0,
    [string] $OutFile
)

$ErrorActionPreference = "Stop"

$LocalDevelopmentPrivateKeyDer = @'
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
'@

function Resolve-StringValue
{
    param(
        [string] $Value,
        [string] $EnvironmentValue,
        [string] $DefaultValue
    )

    if (-not [string]::IsNullOrWhiteSpace($Value))
    {
        return $Value
    }
    if (-not [string]::IsNullOrWhiteSpace($EnvironmentValue))
    {
        return $EnvironmentValue
    }

    return $DefaultValue
}

function Resolve-IntValue
{
    param(
        [int] $Value,
        [string] $EnvironmentValue,
        [int] $DefaultValue
    )

    if ($Value -gt 0)
    {
        return $Value
    }
    if (-not [string]::IsNullOrWhiteSpace($EnvironmentValue))
    {
        return [int] $EnvironmentValue
    }

    return $DefaultValue
}

function ConvertTo-Base64Url
{
    param([byte[]] $Bytes)

    return [Convert]::ToBase64String($Bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function ConvertTo-JwtPart
{
    param($Value)

    $json = $Value | ConvertTo-Json -Compress -Depth 8

    return ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($json))
}

$profileScopes = @{
    "system-ingestor" = @("transactions:evaluate")
    "fraud-analyst" = @("fraud-alerts:read", "actuator:read")
    "rule-admin" = @("rules:read", "rules:admin", "actuator:read")
}

$resolvedIssuer = Resolve-StringValue $Issuer $env:FRAUD_SECURITY_JWT_ISSUER_URI "fraud-rule-engine-local"
$resolvedAudience = Resolve-StringValue $Audience $env:FRAUD_SECURITY_JWT_AUDIENCES "fraud-rule-engine-service"
$resolvedExpiresInSeconds = Resolve-IntValue $ExpiresInSeconds $env:FRAUD_LOCAL_JWT_TTL_SECONDS 3600
$resolvedScopes = if ($Scopes.Count -gt 0) { $Scopes } else { $profileScopes[$TokenProfile] }
$audiences = $resolvedAudience.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

$header = [ordered] @{
    alg = "RS256"
    typ = "JWT"
    kid = "local-dev-rsa"
}
$payload = [ordered] @{
    iss = $resolvedIssuer
    sub = $Subject
    aud = @($audiences)
    scope = ($resolvedScopes -join " ")
    iat = $now
    exp = $now + $resolvedExpiresInSeconds
    token_use = "local-dev"
}

$encodedHeader = ConvertTo-JwtPart $header
$encodedPayload = ConvertTo-JwtPart $payload
$signingInput = "$encodedHeader.$encodedPayload"

$rsa = [System.Security.Cryptography.RSA]::Create()
$privateKeyBytes = [Convert]::FromBase64String(($LocalDevelopmentPrivateKeyDer -replace "\s", ""))
$bytesRead = 0
$rsa.ImportPkcs8PrivateKey($privateKeyBytes, [ref] $bytesRead)
$signature = $rsa.SignData(
    [Text.Encoding]::UTF8.GetBytes($signingInput),
    [System.Security.Cryptography.HashAlgorithmName]::SHA256,
    [System.Security.Cryptography.RSASignaturePadding]::Pkcs1)

$token = "$signingInput.$(ConvertTo-Base64Url $signature)"

if (-not [string]::IsNullOrWhiteSpace($OutFile))
{
    $resolvedOutFile = [IO.Path]::GetFullPath($OutFile)
    $outDirectory = [IO.Path]::GetDirectoryName($resolvedOutFile)
    if (-not [string]::IsNullOrWhiteSpace($outDirectory))
    {
        New-Item -ItemType Directory -Path $outDirectory -Force | Out-Null
    }
    [IO.File]::WriteAllText($resolvedOutFile, $token)
}

Write-Output $token
