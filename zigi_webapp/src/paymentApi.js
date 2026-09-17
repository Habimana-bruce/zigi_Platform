import { API_BASE, authHeaders } from './config.js'

const PAYMENTS_API = `${API_BASE}/api/payments`

export async function initiatePayment(phoneNumber, menuItemId, provider = "MTN") {
  try {
    const res = await fetch(PAYMENTS_API, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify({ phoneNumber, menuItemId, provider }),
    })
    const data = await res.json()
    return { ok: res.ok, data }
  } catch (err) {
    return { ok: false, data: { message: "Could not reach the server. Is the backend running on port 6157?" } }
  }
}

export async function getPaymentStatus(transactionId) {
  try {
    const res = await fetch(`${PAYMENTS_API}/${transactionId}`, { headers: authHeaders() })
    const data = await res.json()
    return { ok: res.ok, data }
  } catch (err) {
    return { ok: false, data: null }
  }
}

export async function pollPaymentUntilDone(transactionId, { intervalMs = 1500, maxAttempts = 8 } = {}) {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    await new Promise(resolve => setTimeout(resolve, intervalMs))
    const { ok, data } = await getPaymentStatus(transactionId)
    if (ok && data && data.status !== "PENDING") {
      return { ok: true, data }
    }
  }
  return { ok: false, data: null } 
}
