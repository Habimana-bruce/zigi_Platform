import { API_BASE, CLIENT_CHANNEL, getAuthToken } from './config.js'

const USSD_URL = `${API_BASE}/api/ussd`

export async function callUssd(sessionId, phoneNumber, text) {
  try {
    const headers = {
      "Content-Type": "application/x-www-form-urlencoded",
      "X-Client-Channel": CLIENT_CHANNEL,
    }
    const token = getAuthToken()
    if (token) headers["Authorization"] = `Bearer ${token}`

    const res = await fetch(USSD_URL, {
      method: "POST",
      headers,
      body: new URLSearchParams({ sessionId, phoneNumber, text }),
    })

    const display = await res.text()
    const isFinal = res.headers.get("X-Ussd-Status") === "END"
    return { ok: res.ok, isFinal, display }
  } catch (err) {
    return { ok: false, isFinal: true, display: "Could not reach server. Is backend running on :6157?" }
  }
}
