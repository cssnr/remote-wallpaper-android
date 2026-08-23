# TODO

## Known Issues

### HomeFragment.kt:310

DownloadResult.Downloaded wraps a closed OkHttp Response (body consumed in use{} block).
Safe today: callers only read .code/.request.url.

FIX IF: any new caller needs response body.
FIX: store code+url strings instead of Response.

### HomeFragment.kt:192

showAddDialog ignores DownloadResult; a 304 would still toast "Done.".
Unreachable in practice: request sends no validators (etag=null) and client has no Cache.

STATUS: Intentionally not handled - unreachable dead branch.
