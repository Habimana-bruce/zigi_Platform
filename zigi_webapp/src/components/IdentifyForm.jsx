import { useState } from 'react'

export default function IdentifyForm({ onSubmit }) {
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [method, setMethod] = useState('PHONE') 

  const canSubmit = phone.trim() && email.trim()

  function submit() {
    if (!canSubmit) return
    onSubmit(phone.trim(), email.trim(), method)
  }

  return (
    <div className="balance-form">
      <input
        type="text"
        placeholder="Enter Phone Number"
        value={phone}
        onChange={e => setPhone(e.target.value)}
        onKeyDown={e => { if (e.key === 'Enter') submit() }}
      />
      <input
        type="email"
        placeholder="Enter Email"
        value={email}
        onChange={e => setEmail(e.target.value)}
        onKeyDown={e => { if (e.key === 'Enter') submit() }}
        style={{ marginTop: 8 }}
      />

      <p style={{ fontSize: '0.85em', margin: '10px 0 6px' }}>Send my code via:</p>
      <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
        <button
          type="button"
          onClick={() => setMethod('PHONE')}
          className={method === 'PHONE' ? 'submit-btn' : 'cancel-btn'}
          style={{ flex: 1 }}
        >
          📱 Phone
        </button>
        <button
          type="button"
          onClick={() => setMethod('EMAIL')}
          className={method === 'EMAIL' ? 'submit-btn' : 'cancel-btn'}
          style={{ flex: 1 }}
        >
          ✉️ Email
        </button>
      </div>

      <button className="submit-btn" disabled={!canSubmit} onClick={submit} style={{ opacity: canSubmit ? 1 : 0.5 }}>
        SUBMIT
      </button>
    </div>
  )
}
