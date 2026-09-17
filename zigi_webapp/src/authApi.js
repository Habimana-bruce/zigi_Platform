import { API_BASE, authHeaders, setAuthToken, clearAuthToken } from './config.js'

const AUTH_URL = `${API_BASE}/api/auth`

export async function requestOtp(phone, email, deliveryMethod, fullName = null, isRegister = false) {
  try {
    const res = await fetch(`${AUTH_URL}/request-otp`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify({ phoneNumber: phone, email, deliveryMethod, fullName, isRegister }),
    })
    const data = await res.json()
    return { ok: res.ok, data }
  } catch (err) {
    return { ok: false, data: { message: "Could not reach the server. Is the backend running on port 6157?" } }
  }
}

export async function verifyOtp(phone, code) {
  try {
    const res = await fetch(`${AUTH_URL}/verify-otp`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify({ phoneNumber: phone, code }),
    })
    const data = await res.json()
    if (res.ok && data.token) setAuthToken(data.token)
    return { ok: res.ok, data }
  } catch (err) {
    return { ok: false, data: { message: "Could not reach the server. Is the backend running on port 6157?" } }
  }
}

export async function logout() {
  try {
    await fetch(`${AUTH_URL}/logout`, { method: "POST", headers: authHeaders() })
  } catch (err) {
    
  } finally {
    clearAuthToken()
  }
}
