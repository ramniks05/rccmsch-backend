# External API Data Source (Token-Authenticated)

Form fields can load options from an **external API** that requires login (username/password) to obtain a token; the backend then uses that token to call the data endpoint.

## Flow

1. **Login**: Backend POSTs to the external login URL (e.g. `https://revenue.chd.gov.in/rccmsapi/UserLogin`) with `username` and `password` from configuration.
2. **Token**: Response is parsed for a token (e.g. `token`, `accessToken`, or `data.accessToken`); configurable via `token-response-path`.
3. **Cache**: Token is cached per API config key for a TTL (e.g. 50 minutes).
4. **Data**: When a form needs options, backend GETs the data URL with `Authorization: Bearer <token>` and returns the list.

Credentials are **not** stored in form field JSON; they are in `application.yml` / environment variables.

---

## Configuration

In `application.yml` under `app.external-apis.apis`:

```yaml
app:
  external-apis:
    apis:
      chd-revenue:
        base-url: https://revenue.chd.gov.in
        login-path: /rccmsapi/UserLogin
        username: ${CHD_REVENUE_API_USERNAME:Admin}
        password: ${CHD_REVENUE_API_PASSWORD:}
        token-cache-ttl-minutes: 50
        token-response-path: "token"
```

**SSL certificate errors (PKIX path building failed):**  
If the external server uses a self-signed or internal CA certificate, Java may reject it. Set in `application.yml`:

```yaml
app:
  external-apis:
    ssl-verify: false   # Disables SSL verification for external API calls (dev/internal only)
```

Or set env `EXTERNAL_API_SSL_VERIFY=false`. For production, prefer adding the server certificate to Java's truststore instead.

**Environment variables** (recommended for production):

- `CHD_REVENUE_API_USERNAME` – login username
- `CHD_REVENUE_API_PASSWORD` – login password (e.g. `Nic@123`)
- `CHD_REVENUE_BASE_URL` – optional override for base URL
- `CHD_REVENUE_LOGIN_PATH` – optional override for login path
- `EXTERNAL_API_SSL_VERIFY` – set to `false` to disable SSL verification when the server cert is not in the truststore

---

## Form Field dataSource (type = API)

For a field that gets options from this external API, set `dataSource` to a JSON string like:

```json
{
  "type": "API",
  "apiConfigKey": "chd-revenue",
  "dataEndpoint": "/rccmsapi/YourDataPath"
}
```

**Choose which fields show in the dropdown** (value = stored value, label = displayed text):

```json
{
  "type": "API",
  "apiConfigKey": "chd-revenue",
  "dataEndpoint": "/rccmsapi/YourDataPath",
  "valueField": "id",
  "labelField": "name"
}
```

Optional query params:

```json
{
  "type": "API",
  "apiConfigKey": "chd-revenue",
  "dataEndpoint": "/rccmsapi/YourDataPath",
  "valueField": "id",
  "labelField": "name",
  "queryParams": {
    "param1": "value1"
  }
}
```

- **apiConfigKey**: Must match a key under `app.external-apis.apis` (e.g. `chd-revenue`).
- **dataEndpoint**: Path relative to that API’s `base-url` (e.g. `/rccmsapi/SomeList`).
- **valueField**: (optional) Property name from each API item to use as dropdown **value** (e.g. `"id"`, `"code"`). If set along with `labelField`, backend returns `[{ "value": ..., "label": ... }]`.
- **labelField**: (optional) Property name from each API item to use as dropdown **label** (e.g. `"name"`, `"title"`).
- **queryParams**: Optional map of query parameters for the data GET request.

---

## Public Endpoints (Frontend)

### GET (for short dataSource)

```
GET /api/public/form-data-sources/external-api?dataSource={"type":"API","apiConfigKey":"chd-revenue","dataEndpoint":"/rccmsapi/YourDataPath"}
```

### POST (recommended for long dataSource)

```
POST /api/public/form-data-sources/external-api
Content-Type: application/json

{
  "dataSource": "{\"type\":\"API\",\"apiConfigKey\":\"chd-revenue\",\"dataEndpoint\":\"/rccmsapi/YourDataPath\"}",
  "runtimeParams": { "Nvcode": "00007" }
}
```

**runtimeParams:** Backend forwards these as query parameters **using the same key names**. Do not rename keys (e.g. the CHD Revenue API expects **Nvcode**, not `parentId`).

- **Wrong:** `"runtimeParams": { "parentId": "00003" }` → backend calls `?parentId=00003` → CHD returns 400 "Nvcode field is required".
- **Correct:** `"runtimeParams": { "Nvcode": "00003" }` → backend calls `?Nvcode=00003`.

For CHD Revenue endpoints (e.g. `GetMustkhas_Rccms`), use **Nvcode** as the key for the parent/village code value.

**Multiple params (two-level dependency):** The backend supports **any number** of keys in `runtimeParams`. Each key is sent as a query parameter with the same name. Example for **GetOwnerDetailsByMustKhas**, which needs both **MUST** (mustkhas value) and **NVCODE** (village value):

```json
{
  "dataSource": "{\"type\":\"API\",\"apiConfigKey\":\"chd-revenue\",\"dataEndpoint\":\"/rccmsapi/GetOwnerDetailsByMustKhas\",\"valueField\":\"...\",\"labelField\":\"...\"}",
  "runtimeParams": { "NVCODE": "00007", "MUST": "0//104/1" }
}
```

Backend will call:  
`GET {baseUrl}/rccmsapi/GetOwnerDetailsByMustKhas?NVCODE=00007&MUST=0%2F%2F104%2F1`  
(Values are URL-encoded automatically; e.g. `0//104/1` → `0%2F%2F104%2F1`.)

So for a dropdown that depends on **two** parents (e.g. Village + Mustkhas → Owner), the frontend sends **both** param names the API expects in `runtimeParams`; no backend change is required.

Response when **valueField** and **labelField** are set in dataSource:

```json
{
  "success": true,
  "message": "External API data retrieved successfully",
  "data": [
    { "value": 1, "label": "Option 1" },
    { "value": 2, "label": "Option 2" }
  ]
}
```

Response when valueField/labelField are **not** set (raw API items):

```json
{
  "success": true,
  "message": "External API data retrieved successfully",
  "data": [
    { "id": 1, "name": "Option 1", "code": "O1" },
    { "id": 2, "name": "Option 2", "code": "O2" }
  ]
}
```

The backend expects the external data endpoint to return JSON that is either an array at the root or an object with an array in `data`, `result`, or `items`. If `valueField` and `labelField` are set, each item is normalized to `{ value, label }` for the dropdown.

**On-change detail (single object):** The same endpoint is used when a dropdown change should trigger a “detail” API call and fill text fields from the response (e.g. GetOwnerDetailsByMustKhas). If the external API returns a **single JSON object** (at root or in `data`), the backend returns it as a **one-element array**: `{ "success": true, "data": [ { "ownerName": "...", "block": "1", ... } ] }`. The frontend uses `data[0]` and maps fields via its `onChangeResponseMapping` config. No separate URL or endpoint is required; use the same `POST /api/public/form-data-sources/external-api` with a `dataSource` that points to the detail endpoint and the same `runtimeParams` (e.g. `NVCODE`, `MUST`).

**Form schema (on-change config):** The backend form field definition does not currently store `onChangeApi` or `onChangeResponseMapping`. The frontend can keep the on-change API config and response mapping in its own schema or config and send the appropriate `dataSource` (and `runtimeParams`) when calling this endpoint on dropdown change.

---

## Frontend implementation

### 1. Detect API data source

When rendering a form field, check if the field uses the external API:

```ts
// field is from form schema (e.g. formSchema.fields[i])
const dataSource = field.dataSource ? JSON.parse(field.dataSource) : null;
const isApiDataSource = dataSource?.type === 'API';
```

### 2. Load options from backend

For dropdown/select fields with `dataSource.type === 'API'`, call the backend (do **not** call the external API directly; the backend handles login and token).

**Option A – GET** (for short dataSource string):

```ts
const dataSourceStr = field.dataSource; // already a string from schema
const res = await fetch(
  `${API_BASE}/api/public/form-data-sources/external-api?dataSource=${encodeURIComponent(dataSourceStr)}`
);
const json = await res.json();
const options = json?.data ?? [];
```

**Option B – POST** (recommended for long dataSource or special characters):

```ts
const res = await fetch(`${API_BASE}/api/public/form-data-sources/external-api`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ dataSource: field.dataSource })
});
const json = await res.json();
const options = json?.data ?? [];
```

### 3. Bind options to the dropdown

- If the field’s dataSource includes **valueField** and **labelField**, the backend returns `[{ value, label }, ...]`. Use as-is:

```ts
// options = [ { value: 1, label: "Option 1" }, ... ]
<select value={formValue[field.fieldName]} onChange={...}>
  {options.map(opt => (
    <option key={opt.value} value={opt.value}>{opt.label}</option>
  ))}
</select>
```

- If you did **not** set valueField/labelField, the backend returns raw API objects. Pick the properties you need (e.g. `id` for value, `name` for label):

```ts
const valueKey = dataSource.valueField ?? 'id';
const labelKey = dataSource.labelField ?? 'name';
options.map(opt => (
  <option key={opt[valueKey]} value={opt[valueKey]}>{opt[labelKey]}</option>
));
```

### 4. Admin: which field to show in dropdown

When configuring the form field (admin), set in **dataSource**:

- **valueField**: the API property to use as the selected value (e.g. `"id"`, `"code"`).
- **labelField**: the API property to show in the dropdown (e.g. `"name"`, `"title"`).

Example: API returns `[{ "id": 1, "code": "DIST", "name": "District 1" }]`. Use `valueField: "id"` and `labelField: "name"` so the user sees “District 1” and the form stores `1`. Backend then returns `[{ "value": 1, "label": "District 1" }]` and the frontend only needs to bind `value` and `label`.

---

## Login API (CHD Revenue)

Example login request the backend performs:

```bash
curl -X 'POST' \
  'https://revenue.chd.gov.in/rccmsapi/UserLogin' \
  -H 'Content-Type: application/json' \
  -d '{"username": "Admin", "password": "Nic@123"}'
```

Configure `CHD_REVENUE_API_USERNAME` and `CHD_REVENUE_API_PASSWORD` (or the defaults in `application.yml`) so the backend can authenticate and then call other endpoints under the same base URL with the returned token.
