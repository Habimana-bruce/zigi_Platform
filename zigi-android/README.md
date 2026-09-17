# Zigi Android (Native, Java)

A native Android app built in Java, calling the exact same Spring Boot
backend as the web app and React Native app. This is a genuine Android
Studio project - no Expo, no React, no JavaScript.

## Project structure

```
app/src/main/java/com/zigi/mobile/
├── MainActivity.java       - all chat/menu/USSD/payment logic (ported from ChatScreen.js)
├── Config.java              - the ONE place you set your backend URL
├── api/
│   ├── HttpUtil.java          - shared HttpURLConnection wrapper, runs off the main thread
│   ├── ApiCallback.java       - generic async result callback
│   ├── MenuApi.java           - GET /api/menu/roots/{group}, /api/menu/{id}/children
│   ├── UssdApi.java            - POST /api/ussd (form-urlencoded, reads X-Ussd-Status header)
│   ├── PaymentApi.java         - POST /api/payments + bounded polling on GET /api/payments/{id}
│   └── CustomerApi.java        - GET /api/customers/phone/{phone}
├── model/
│   ├── ChatMessage.java        - the 6 bubble types shown in the chat
│   └── MenuOption.java         - parses the MenuItemDto JSON shape
├── adapter/
│   └── ChatAdapter.java        - RecyclerView adapter, one view type per bubble kind
└── data/
    └── MenuData.java           - suggested prompts, quick buttons, group titles (matches
                                   data/menu.js in the web/RN versions)

app/src/main/res/
├── layout/    - activity_main.xml (welcome + chat screens) + one layout per bubble type
├── drawable/  - rounded bubble backgrounds, buttons
└── values/    - colors.xml, themes.xml
```

## Why Java networking looks different here

Android forbids network calls on the main/UI thread (the app would crash
with `NetworkOnMainThreadException`). `HttpUtil.java` handles this: every
request runs on a background thread pool, and results are delivered back on
the main thread via a `Handler`, so the callback code in `MainActivity.java`
can safely touch views directly - this is the Java-native equivalent of
`await fetch(...)` in the web/React Native versions.

## 1. Open in Android Studio

- Android Studio → **Open** → select the `zigi-android` folder
- Let Gradle sync (first sync will download dependencies - needs internet access)

## 2. Point it at your backend

Open `app/src/main/java/com/zigi/mobile/Config.java`:

```java
public static final String API_BASE = "http://10.0.2.2:6157";
```

This default (`10.0.2.2`) is correct **only for the Android Emulator** -
it's Android's special alias that means "the computer running the
emulator." Change it depending on what you're testing on:

| Testing on | Set API_BASE to |
|---|---|
| Android Emulator | `http://10.0.2.2:6157` (default, no change needed) |
| Physical phone, same WiFi | `http://<your-computer's-LAN-IP>:6157` (e.g. `http://192.168.1.42:6157`) |
| Deployed backend | Your real URL, e.g. `https://zigi-backend.onrender.com` |

## 3. Start your backend first

Same as always: `mvn spring-boot:run` in the `backend` folder, with
MySQL/XAMPP running and `zigi_db` imported. The Android app is just a UI -
it has nothing to show without the backend running.

## 4. Run it

- Pick a device: **Tools → Device Manager** to create/start an emulator, or
  plug in a physical phone with USB debugging enabled
- Click the green ▶ **Run** button (or `Shift+F10`)

## Known simplifications vs. the web/React Native versions

- **Menu option rows are plain `TextView`s added dynamically** inside a
  `LinearLayout` (see `ChatAdapter.MenuVH`), not a nested `RecyclerView` -
  simpler and perfectly fine at this list size, but worth knowing if you
  ever want ripple/press animations on each row (would need a
  `Button`/`MaterialButton` or a custom selectable background drawable).
- **`Html.fromHtml()` handles `<b>`/`<br>`** in bot messages - same subset
  the web app relies on via `dangerouslySetInnerHTML`, works natively here
  without a custom parser (unlike the RN version, which needed one).
- No `AbortController`-equivalent timeout is wired in yet beyond
  `HttpURLConnection`'s built-in 10s connect/read timeout in `HttpUtil.java`.
- No loading/disabled state on the input while a request is in flight - same
  gap flagged in the earlier web app review, still applies here.

## If Gradle sync fails

Almost always one of:
- No internet access (Gradle needs to download `com.android.tools.build`,
  AndroidX libraries, etc. from Google's Maven repo the first time)
- Android Studio's bundled JDK mismatch - use the JDK Android Studio ships
  with (Android Studio → Settings → Build Tools → Gradle → Gradle JDK)
