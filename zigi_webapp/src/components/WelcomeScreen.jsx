export default function WelcomeScreen({ onStart }) {
  return (
    <div className="widget welcome-screen">
      <div className="welcome-top">
        <div className="zigi-avatar">🙂</div>
        <h2 className="yello-title">Y'ello <span>👋</span>!</h2>
        <p className="welcome-text">
          I'm Zigi, your MTN online assistant 🧑‍💼. What can I help you with today?
          <br />
          I'm here and happy to assist 🤩.
        </p>
      </div>

      <div className="welcome-bottom">
        <button className="start-btn" onClick={onStart}>
          <span className="arrow">➤</span> Start a New Conversation
        </button>

        <div className="channel-icons">
          <div className="channel messenger" title="Messenger">💬</div>
          <div className="channel whatsapp" title="WhatsApp">✆</div>
          <div className="channel telegram" title="Telegram">✈️</div>
          <div className="channel mymtn" title="MyMTN">
            <span className="mymtn-my">my</span>
            <span className="mymtn-mtn">MTN</span>
          </div>
        </div>
      </div>
    </div>
  )
}
