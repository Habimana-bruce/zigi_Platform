import { API_BASE, authHeaders } from './config.js'

const CUSTOMERS_URL = `${API_BASE}/api/customers`

export async function getBalanceByPhone(phone) {
  try {
    const res = await fetch(`${CUSTOMERS_URL}/phone/${encodeURIComponent(phone)}`, {
      headers: authHeaders(),
    })
    const data = await res.json()
    return { ok: res.ok, data }
  } catch (err) {
    return { ok: false, data: { message: "Could not reach the server. Is the backend running on port 6157?" } }
  }
}
