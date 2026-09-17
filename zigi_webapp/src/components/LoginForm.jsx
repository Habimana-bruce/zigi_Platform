import { useState } from 'react'

const MTN_RWANDA_PATTERN = /^(?:\+250|0)(7[89]\d{7})$/

function isValidMtnPhone(phone) {
  return MTN_RWANDA_PATTERN.test(phone.trim().replace(/[\s-]/g, ''))
}

export default function LoginForm({ onSubmit }) {
  const [mode, setMode] = useState('login') 
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [method, setMethod] = useState('PHONE') 
  const [phoneError, setPhoneError] = useState('')

  const isRegister = mode === 'register'
  const canSubmit = isRegister
    ? fullName.trim() && phone.trim() && email.trim()
    : phone.trim() && email.trim()

  function submit(e) {
    if (e) e.preventDefault()
    if (!canSubmit) return
    if (!isValidMtnPhone(phone)) {
      setPhoneError('Please enter a valid MTN Rwanda number (078XXXXXXX, 079XXXXXXX, or +2507XXXXXXXX).')
      return
    }
    setPhoneError('')
    onSubmit({
      phone: phone.trim(),
      email: email.trim(),
      method: isRegister ? 'EMAIL' : method, 
      isRegister,
      fullName: isRegister ? fullName.trim() : null
    })
  }

  return (
    <div className="balance-form auth-login-form">
      {}
      <div className="auth-tabs">
        <button
          type="button"
          className={`auth-tab ${mode === 'login' ? 'active' : ''}`}
          onClick={() => setMode('login')}
        >
          🔑 Sign In
        </button>
        <button
          type="button"
          className={`auth-tab ${mode === 'register' ? 'active' : ''}`}
          onClick={() => setMode('register')}
        >
          📝 Register
        </button>
      </div>

      <form onSubmit={submit}>
        {isRegister && (
          <div className="form-group">
            <input
              type="text"
              placeholder="Full Name"
              value={fullName}
              onChange={e => setFullName(e.target.value)}
              required
            />
          </div>
        )}

        <div className="form-group">
          <input
            type="text"
            placeholder="Phone Number (e.g. 078XXXXXXX)"
            value={phone}
            onChange={e => { setPhone(e.target.value); setPhoneError('') }}
            required
          />
          {phoneError && <p style={{ color: '#c0392b', fontSize: '0.8em', margin: '4px 0 0' }}>{phoneError}</p>}
        </div>

        <div className="form-group">
          <input
            type="email"
            placeholder="Email Address"
            value={email}
            onChange={e => setEmail(e.target.value)}
            required
          />
        </div>

        {}
        {!isRegister ? (
          <>
            <p className="delivery-label">Send login verification code via:</p>
            <div className="delivery-methods">
              <button
                type="button"
                onClick={() => setMethod('PHONE')}
                className={`method-btn ${method === 'PHONE' ? 'active' : ''}`}
              >
                📱 Phone (SMS)
              </button>
              <button
                type="button"
                onClick={() => setMethod('EMAIL')}
                className={`method-btn ${method === 'EMAIL' ? 'active' : ''}`}
              >
                ✉️ Email
              </button>
            </div>
          </>
        ) : (
          <p className="delivery-label" style={{ fontSize: '0.82em', color: '#666', fontStyle: 'italic', marginBottom: 12 }}>
            📧 A 6-digit verification code will be sent directly to your Email address.
          </p>
        )}

        <button
          type="submit"
          className="submit-btn login-submit-btn"
          disabled={!canSubmit}
          style={{ opacity: canSubmit ? 1 : 0.5 }}
        >
          {isRegister ? 'CREATE ACCOUNT & VERIFY EMAIL 🚀' : 'SIGN IN & GET OTP 🔒'}
        </button>
      </form>
    </div>
  )
}
