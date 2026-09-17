import { API_BASE, authHeaders } from './config.js'

const MENU_API = `${API_BASE}/api/menu`

export async function getMenuRoots(group) {
  try {
    const res = await fetch(`${MENU_API}/roots/${group}`, { headers: authHeaders() })
    if (!res.ok) return { ok: false, items: [] }
    return { ok: true, items: await res.json() }
  } catch (err) {
    return { ok: false, items: [] }
  }
}

export async function getMenuChildren(id) {
  try {
    const res = await fetch(`${MENU_API}/${id}/children`, { headers: authHeaders() })
    if (!res.ok) return { ok: false, items: [] }
    return { ok: true, items: await res.json() }
  } catch (err) {
    return { ok: false, items: [] }
  }
}
