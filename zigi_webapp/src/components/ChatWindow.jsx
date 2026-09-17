import LoginForm from './LoginForm.jsx'
import OtpForm from './OtpForm.jsx'

export default function ChatWindow({ messages, onMenuOptionClick, onBack, onLoginSubmit, onOtpVerify, onOtpResend, endRef }) {
  return (
    <div className="chat-window">
      {messages.map(msg => {
        if (msg.type === 'user') {
          return <div key={msg.id} className="bubble user">{msg.text}</div>
        }

        if (msg.type === 'menu') {
          return (
            <div key={msg.id} className="bubble bot">
              <p>{msg.menu.title} 👇</p>
              <ul>
                {msg.menu.options.map((opt, i) => (
                  <li key={opt.id} onClick={() => onMenuOptionClick(opt)}>
                    {i + 1}. {opt.icon} {opt.label}
                  </li>
                ))}
                <li onClick={onBack} className="back-option">0. Back</li>
              </ul>
            </div>
          )
        }

        if (msg.type === 'suggestions') {
          return (
            <div key={msg.id} className="bubble bot">
              <p>What are we doing today? 🤩</p>
              <p>Type your request 👇 For example:</p>
              {msg.suggestions.map((s, i) => (
                <p key={i}><b>{s}</b></p>
              ))}
            </div>
          )
        }

        if (msg.type === 'login-form' || msg.type === 'identify-form') {
          return (
            <div key={msg.id} className="bubble bot">
              <p>Please <b>Sign In</b> or <b>Register</b> to get started 👇</p>
              <p style={{ fontSize: '0.85em', opacity: 0.8 }}>
                First enter your credentials to request a verification OTP code.
              </p>
              {msg.active && (
                <LoginForm onSubmit={onLoginSubmit} />
              )}
            </div>
          )
        }

        if (msg.type === 'otp-form') {
          const destination = msg.method === 'EMAIL' ? msg.email : msg.phone
          const via = msg.method === 'EMAIL' ? 'email' : 'phone'
          return (
            <div key={msg.id} className="bubble bot">
              <p>We sent a 6-digit code to your {via}: <b>{destination}</b> 👇</p>
              <p style={{ fontSize: '0.85em', opacity: 0.8 }}>
                Enter it below to confirm it's really you.
              </p>
              {msg.active && (
                <OtpForm
                  phone={msg.phone}
                  devCode={msg.devCode}
                  error={msg.error}
                  onVerify={code => onOtpVerify(msg.phone, code)}
                  onResend={() => onOtpResend(msg.phone)}
                />
              )}
            </div>
          )
        }

        if (msg.type === 'ussd') {
          return (
            <div key={msg.id} className="bubble bot ussd-bubble">
              <pre>{msg.text}</pre>
            </div>
          )
        }

        // default plain bot text
        return <div key={msg.id} className="bubble bot" dangerouslySetInnerHTML={{ __html: msg.text }} />
      })}
      <div ref={endRef} />
    </div>
  )
}
