export interface TokenProfile {
  userId?: number
  username?: string
  email?: string
  role?: string
}

function decodeBase64Url(value: string) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  return atob(padded)
}

export function parseTokenProfile(token?: string | null): TokenProfile {
  if (!token) {
    return {}
  }

  try {
    const payload = token.split('.')[1]
    if (!payload) {
      return {}
    }

    const decoded = decodeBase64Url(payload)
    const parsed = JSON.parse(decoded) as {
      userId?: number
      username?: string
      email?: string
      sub?: string
      role?: string
    }

    return {
      userId: parsed.userId,
      username: parsed.username,
      email: parsed.email ?? parsed.sub,
      role: parsed.role,
    }
  } catch {
    return {}
  }
}

export function getStoredToken() {
  return localStorage.getItem('token')
}

export function getCurrentUserProfile() {
  const token = getStoredToken()
  return parseTokenProfile(token)
}

export function persistAuthSession(token: string, role?: string) {
  localStorage.setItem('token', token)

  const profile = parseTokenProfile(token)
  const normalizedRole = role || profile.role || 'user'
  localStorage.setItem('role', normalizedRole)

  if (profile.userId !== undefined) {
    localStorage.setItem('profileUserId', String(profile.userId))
  }
  if (profile.username) {
    localStorage.setItem('profileName', profile.username)
  }
  if (profile.email) {
    localStorage.setItem('profileEmail', profile.email)
  }
  localStorage.setItem('profileRole', normalizedRole)
}

export function clearAuthSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('profileUserId')
  localStorage.removeItem('profileName')
  localStorage.removeItem('profileEmail')
  localStorage.removeItem('profileRole')
}
