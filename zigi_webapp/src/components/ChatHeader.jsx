export default function ChatHeader({ onBack }) {
  return (
    <div className="chat-header">
      <button className="header-icon-btn" onClick={onBack} aria-label="Back">‹</button>
      <div className="mtn-logo">MTN</div>
      <div className="header-icon-btn" style={{ visibility: 'hidden' }}>‹</div>
    </div>
  )
}
