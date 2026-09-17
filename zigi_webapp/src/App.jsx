import { useState, useRef, useEffect } from 'react'
import WelcomeScreen from './components/WelcomeScreen.jsx'
import ChatHeader from './components/ChatHeader.jsx'
import ChatWindow from './components/ChatWindow.jsx'
import QuickButtons from './components/QuickButtons.jsx'
import { getBalanceByPhone } from './api.js'
import { getMenuRoots, getMenuChildren } from './menuApi.js'
import { callUssd } from './ussdApi.js'
import { initiatePayment, pollPaymentUntilDone } from './paymentApi.js'
import { requestOtp, verifyOtp, logout } from './authApi.js'
import { suggestedPrompts, quickButtonsMain, quickButtonsHelp, groupTitles } from './data/menu.js'

const uid = () => crypto.randomUUID()
const USSD_DEMO_PHONE = '0700000000' 

export default function App() {
  const [view, setView] = useState('welcome') 
  const [messages, setMessages] = useState([])
  const [quickButtons, setQuickButtons] = useState(['Menu', 'Help', 'Exit'])
  const [inputValue, setInputValue] = useState('')
  const [activeGroup, setActiveGroup] = useState('MAIN') 

  const [customerPhone, setCustomerPhone] = useState(null)

  const [currentMenu, setCurrentMenu] = useState(null)
  const [menuStack, setMenuStack] = useState([])

  const [ussdMode, setUssdMode] = useState(false)
  const [ussdText, setUssdText] = useState('')
  const ussdSessionId = useRef(uid())

  const endRef = useRef(null)
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  function addMessage(msg) {
    setMessages(prev => [...prev, { id: uid(), ...msg }])
  }

  function patchMessage(id, patch) {
    setMessages(prev => prev.map(m => (m.id === id ? { ...m, ...patch } : m)))
  }

  function resetSession() {
    setMessages([{ id: uid(), type: 'login-form', active: true }])
    setQuickButtons(['Menu', 'Help', 'Exit'])
    setActiveGroup('MAIN')
    setCurrentMenu(null)
    setMenuStack([])
    setUssdMode(false)
    setUssdText('')
    setCustomerPhone(null)
    ussdSessionId.current = uid()
  }

  function startConversation() {
    setView('chat')
    resetSession()
  }

  async function handleLoginSubmit({ phone, email, method, isRegister, fullName }) {
    await sendOtpFor(phone, email, method, fullName, isRegister)
  }

  async function sendOtpFor(phone, email, method, fullName = null, isRegister = false) {
    setMessages(prev => prev.map(m => (m.type === 'login-form' || m.type === 'identify-form' ? { ...m, active: false } : m)))
    const { ok, data } = await requestOtp(phone, email, method, fullName, isRegister)
    if (!ok) {
      addMessage({ type: 'bot', text: `⚠️ ${data.message || 'Could not send a code. Please try again.'}` })
      addMessage({ type: 'login-form', active: true })
      return
    }
    addMessage({ type: 'otp-form', phone, email, method, devCode: data.devCode, active: true })
  }

  async function handleOtpVerify(phone, code) {
    const otpMsg = [...messages].reverse().find(m => m.type === 'otp-form' && m.phone === phone)
    const { ok, data } = await verifyOtp(phone, code)
    if (!ok) {
      if (otpMsg) patchMessage(otpMsg.id, { error: data.message || 'Incorrect code.' })
      return
    }
    if (otpMsg) patchMessage(otpMsg.id, { active: false, error: null })
    setCustomerPhone(phone)
    addMessage({ type: 'bot', text: `✅ Authenticated! Welcome, we'll use <b>${phone}</b> for your account actions in this chat.` })
    addMessage({ type: 'suggestions', suggestions: suggestedPrompts })
  }

  async function handleOtpResend(phone) {
    const otpMsg = [...messages].reverse().find(m => m.type === 'otp-form' && m.phone === phone)
    const { ok, data } = await requestOtp(phone, otpMsg?.email, otpMsg?.method)
    if (otpMsg) {
      patchMessage(otpMsg.id, ok
        ? { devCode: data.devCode, error: null }
        : { error: data.message || 'Could not resend the code.' })
    }
  }

  function requireAuth() {
    if (customerPhone) return true
    addMessage({ type: 'bot', text: '🔒 Please sign in or register first so we can verify your account.' })
    const alreadyOpen = messages.some(m => (m.type === 'login-form' || m.type === 'identify-form' || m.type === 'otp-form') && m.active)
    if (!alreadyOpen) {
      addMessage({ type: 'login-form', active: true })
    }
    return false
  }

  async function showMenuGroup(group) {
    const { ok, items } = await getMenuRoots(group)
    if (!ok) {
      addMessage({ type: 'bot', text: '⚠️ Could not load the menu. Is the backend running on port 6157, and is MySQL (XAMPP) started with zigi_db imported?' })
      return
    }
    if (items.length === 0) {
      addMessage({ type: 'bot', text: `No items found for "${group}" yet. Add some via /api/menu.` })
      return
    }
    const menu = { title: groupTitles[group] || group, options: items }
    addMessage({ type: 'menu', menu })
    setQuickButtons(group === 'HELP' ? quickButtonsHelp : quickButtonsMain)
    setActiveGroup(group)
    setCurrentMenu(menu)
    setMenuStack([]) 
  }

  function handleBack() {
    if (menuStack.length === 0) {
      addMessage({ type: 'bot', text: "You're already at the top of this menu." })
      if (currentMenu) addMessage({ type: 'menu', menu: currentMenu })
      return
    }
    const previous = menuStack[menuStack.length - 1]
    setMenuStack(menuStack.slice(0, -1))
    setCurrentMenu(previous)
    addMessage({ type: 'menu', menu: previous })
  }

  async function handleBalanceCheck(item) {
    addMessage({ type: 'bot', text: '⏳ Your request is being processed...' })
    const { ok, data } = await getBalanceByPhone(customerPhone)
    if (!ok) {
      addMessage({ type: 'bot', text: '⏳ Your request is being processed. You will receive your balance information via SMS shortly.' })
    } else if (item.balanceType === 'AIRTIME') {
      addMessage({ type: 'bot', text: `<b>Airtime Balance</b><br>${data.airtimeBalance} RWF` })
    } else if (item.balanceType === 'DATA') {
      addMessage({ type: 'bot', text: `<b>Data Balance</b><br>${data.dataBalanceMb} MB` })
    } else {
      addMessage({
        type: 'bot',
        text: `<b>Balance Summary</b><br>Name: ${data.fullName}<br>Main Balance: ${data.mainBalance} RWF<br>Airtime: ${data.airtimeBalance} RWF<br>Data: ${data.dataBalanceMb} MB`,
      })
    }
    showMenuGroup(activeGroup)
  }

  async function handleMenuOptionClick(option) {
    addMessage({ type: 'user', text: option.label })
    if (!requireAuth()) return

    if (option.actionType === 'BALANCE' || option.actionType === 'PAYMENT') {
      if (option.actionType === 'BALANCE') {
        handleBalanceCheck(option)
      } else {
        handlePaymentSubmit(option)
      }
      return
    }

    if (option.hasChildren) {
      const { ok, items } = await getMenuChildren(option.id)
      if (!ok) {
        addMessage({ type: 'bot', text: '⚠️ Could not load that submenu. Is the backend running on port 6157?' })
        return
      }
      const childMenu = { title: option.label, options: items }
      if (currentMenu) setMenuStack(prev => [...prev, currentMenu])
      setCurrentMenu(childMenu)
      addMessage({ type: 'menu', menu: childMenu })
      return
    }

    addMessage({ type: 'bot', text: option.responseText || `You selected <b>${option.label}</b>.` })
    showMenuGroup(activeGroup)
  }

  async function handlePaymentSubmit(item) {
    addMessage({ type: 'bot', text: `⏳ Your request is being processed - confirming purchase of <b>${item.label}</b> (${item.price} RWF) via MTN MoMo...` })

    const { ok, data } = await initiatePayment(customerPhone, item.id, 'MTN')
    if (!ok) {
      addMessage({ type: 'bot', text: `⚠️ ${data.message || 'Could not start the payment.'}` })
      showMenuGroup(activeGroup)
      return
    }

    const result = await pollPaymentUntilDone(data.id)
    if (!result.ok) {
      addMessage({ type: 'bot', text: `Still waiting on confirmation for transaction TXN${data.id}. Check back shortly - it may complete after this chat.` })
    } else if (result.data.status === 'SUCCESS') {
      addMessage({ type: 'bot', text: `✅ Payment confirmed! You've purchased <b>${item.label}</b>. Check My Balance to see your updated balance.` })
    } else {
      addMessage({ type: 'bot', text: `❌ Payment failed for <b>${item.label}</b>. Please try again.` })
    }
    showMenuGroup(activeGroup)
  }

  async function handleQuickButtonClick(label) {
    addMessage({ type: 'user', text: label })

    if (!label.includes('Exit') && !requireAuth()) return

    if (label.includes('Menu')) {
      showMenuGroup('MAIN')
    } else if (label.includes('Help')) {
      showMenuGroup('HELP')
    } else if (label.includes('Data')) {
      showMenuGroup('DATA')
    } else if (label.includes('Airtime')) {
      showMenuGroup('AIRTIME')
    } else if (label.includes('Assist')) {
      const { ok, items } = await getMenuRoots('ASSIST')
      if (!ok) {
        addMessage({ type: 'bot', text: '⚠️ Could not reach the server. Is the backend running on port 6157?' })
        return
      }
      if (items.length === 1 && !items[0].hasChildren) {
        addMessage({ type: 'bot', text: items[0].responseText })
      } else if (items.length > 0) {
        const menu = { title: 'Assist', options: items }
        addMessage({ type: 'menu', menu })
        setCurrentMenu(menu)
        setMenuStack([])
        return
      }
      showMenuGroup(activeGroup)
    } else if (label.includes('Exit')) {

      await logout()
      resetSession()
      addMessage({ type: 'bot', text: "Thanks for using Zigi! 👋 Session ended - enter a phone number to start again." })
    } else {
      addMessage({ type: 'bot', text: `You selected <b>${label}</b>. (Demo response — wire this up to a real backend action.)` })
      showMenuGroup(activeGroup)
    }
  }

  async function startUssd() {
    setUssdMode(true)
    setUssdText('')
    const { isFinal, display } = await callUssd(ussdSessionId.current, USSD_DEMO_PHONE, '')
    addMessage({ type: 'ussd', text: display })
    if (isFinal) setUssdMode(false)
  }

  async function handleUssdStep(step) {
    const { isFinal, display } = await callUssd(ussdSessionId.current, USSD_DEMO_PHONE, step)
    addMessage({ type: 'ussd', text: display })
    if (isFinal) {
      setUssdMode(false)
      setUssdText('')
    }
  }

  function handleSend() {
    const text = inputValue.trim()
    if (!text) return
    setInputValue('')

    addMessage({ type: 'user', text })

    if (ussdMode) {
      handleUssdStep(text)
      return
    }

    const normalized = text.toLowerCase().replace(/[^a-z]/g, '')
    const greetings = ['hi', 'hello', 'hy', 'hey', 'yello', 'mwaramutse', 'muraho']
    if (greetings.includes(normalized)) {
      addMessage({ type: 'suggestions', suggestions: suggestedPrompts })
      return
    }

    addMessage({ type: 'bot', text: "Got it! Use the buttons below or type 'menu' to see options." })
    if (!requireAuth()) return
    showMenuGroup(activeGroup)
  }

  return (
    <div className="page-host">
      <div className="widget-shell">
        {view === 'welcome' && (
          <WelcomeScreen onStart={startConversation} />
        )}

        {view === 'chat' && (
          <div className="widget chat-screen">
            <ChatHeader onBack={() => setView('welcome')} />
            <ChatWindow
              messages={messages}
              onMenuOptionClick={handleMenuOptionClick}
              onBack={handleBack}
              onLoginSubmit={handleLoginSubmit}
              onOtpVerify={handleOtpVerify}
              onOtpResend={handleOtpResend}
              endRef={endRef}
            />
            <QuickButtons buttons={quickButtons} onClick={handleQuickButtonClick} />
            <div className="input-row">
              <input
                type="text"
                placeholder="Write your message..."
                value={inputValue}
                onChange={e => setInputValue(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') handleSend() }}
              />
              <button onClick={handleSend}>➤</button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
