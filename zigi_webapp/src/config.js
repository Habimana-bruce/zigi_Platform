export const API_BASE = "http://localhost:6157"
export const CLIENT_CHANNEL = "WEB"

let authToken = null

export function getAuthToken() {
  return authToken
}

export function setAuthToken(token) {
  authToken = token
}

export function clearAuthToken() {
  authToken = null
}

export function authHeaders(extra = {}) {
  const headers = { "X-Client-Channel": CLIENT_CHANNEL, ...extra }
  if (authToken) headers["Authorization"] = `Bearer ${authToken}`
  return headers
}
