import { useState } from 'react'

export default function OtpForm({ phone, devCode, onVerify, onResend, error }) {
  const [code, setCode] = useState('')

  return (
    <div className="balance-form">
      <input
        type="text"
        inputMode="numeric"
        maxLength={6}
        placeholder="Enter 6-digit code"
        value={code}
        onChange={e => setCode(e.target.value.replace(/\D/g, ''))}
        onKeyDown={e => { if (e.key === 'Enter' && code.trim()) onVerify(code.trim()) }}
      />
      <button className="submit-btn" onClick={() => code.trim() && onVerify(code.trim())}>
        VERIFY
      </button>
      <button className="cancel-btn" onClick={onResend}>Resend code</button>
      {error && <p style={{ color: '#c0392b', fontSize: '0.85em', marginTop: 4 }}>{error}</p>}
      {devCode && (
        <p style={{ fontSize: '0.8em', opacity: 0.7, marginTop: 4 }}>
          Demo mode (no real SMS sent) - your code is <b>{devCode}</b>
        </p>
      )}
    </div>
  )
}
