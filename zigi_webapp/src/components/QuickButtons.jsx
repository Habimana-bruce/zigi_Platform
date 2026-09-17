export default function QuickButtons({ buttons, onClick }) {
  if (!buttons || buttons.length === 0) return null
  return (
    <div className="quick-buttons">
      {buttons.map(label => (
        <button key={label} onClick={() => onClick(label)}>{label}</button>
      ))}
    </div>
  )
}
