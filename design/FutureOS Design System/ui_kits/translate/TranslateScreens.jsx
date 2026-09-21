/* FutureOS · תרגום — screens. Composed from the shipped components only.
   The one non-component surface is the translate input box: TextField is
   single-line by design, and translation input has to wrap, so the box repeats
   the field tokens verbatim. */
const { TopBar, Card, Divider, SectionHeader, ListItem, EmptyState, FosIcon: Icon, IconButton, TabRow, ProgressBar, TextField } = window.FutureOSDesignSystem_3ab611;

const LANGS = [
  { code: "auto", name: "זיהוי שפה", rtl: true },
  { code: "he", name: "עברית", rtl: true },
  { code: "en", name: "אנגלית", rtl: false },
  { code: "ar", name: "ערבית", rtl: true },
  { code: "ru", name: "רוסית", rtl: false },
  { code: "fr", name: "צרפתית", rtl: false },
  { code: "es", name: "ספרדית", rtl: false },
  { code: "de", name: "גרמנית", rtl: false },
  { code: "it", name: "איטלקית", rtl: false },
  { code: "am", name: "אמהרית", rtl: false },
  { code: "yi", name: "יידיש", rtl: true },
  { code: "tr", name: "טורקית", rtl: false },
  { code: "zh", name: "סינית", rtl: false },
  { code: "ja", name: "יפנית", rtl: false }
];
const RECENT_LANGS = ["en", "ar", "ru"];
const byCode = (c) => LANGS.find(l => l.code === c) || LANGS[1];

const HISTORY = [
  { src: "איפה תחנת הרכבת?", dst: "Where is the train station?", from: "he", to: "en", saved: true },
  { src: "כמה זה עולה", dst: "How much does it cost", from: "he", to: "en", saved: false },
  { src: "شكرا جزيلا", dst: "תודה רבה", from: "ar", to: "he", saved: true },
  { src: "אני צריך רופא", dst: "J'ai besoin d'un médecin", from: "he", to: "fr", saved: false }
];

const TALK = [
  { side: "he", text: "סליחה, איך מגיעים למוזיאון?", alt: "Excuse me, how do I get to the museum?" },
  { side: "en", text: "Take the number 5 bus, four stops.", alt: "קח את קו 5, ארבע תחנות." },
  { side: "he", text: "תודה רבה", alt: "Thank you very much" }
];

const ACTIONS = [
  { icon: "volume_up", label: "השמע" },
  { icon: "content_copy", label: "העתק" },
  { icon: "share", label: "שתף" },
  { icon: "star", label: "שמור" }
];

/* the chevron points left: left is forward in RTL */
const Chevron = () => <Icon name="keyboard_arrow_left" size={36} color="var(--fos-text-30)" />;
const Star = () => <Icon name="star" size={32} color="var(--fos-favorite)" />;

const capsule = (focused) => ({
  flex: 1, minWidth: 0, minHeight: 96, borderRadius: 999,
  background: focused ? "var(--fos-accent-20)" : "var(--fos-text-08)",
  border: "4px solid " + (focused ? "var(--fos-accent)" : "transparent"),
  boxSizing: "border-box", display: "flex", flexDirection: "column",
  alignItems: "center", justifyContent: "center", gap: 2,
  padding: "var(--fos-space-2) var(--fos-space-4)", cursor: "pointer",
  transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)"
});
const capLabel = { fontSize: "var(--fos-size-summary)", color: "var(--fos-text-55)", letterSpacing: "var(--fos-tracking-section)" };
const capName = { fontSize: "var(--fos-size-title)", fontWeight: "var(--fos-weight-medium)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: "100%" };
const sectionLabel = { fontSize: "var(--fos-size-summary)", color: "var(--fos-text-55)", letterSpacing: "var(--fos-tracking-section)" };

function LangBar({ from, to, focus, sub, onPick, onSwap }) {
  const f = (i) => focus === 0 && sub === i;
  return (
    <div style={{ display: "flex", alignItems: "center", gap: "var(--fos-space-3)", padding: "0 var(--fos-space-screen) var(--fos-space-5)" }}>
      <div style={capsule(f(0))} onClick={() => onPick("from")}>
        <div style={capLabel}>מ</div>
        <div style={capName}>{from.name}</div>
      </div>
      <IconButton icon="swap_horiz" focused={f(1)} onClick={onSwap} />
      <div style={capsule(f(2))} onClick={() => onPick("to")}>
        <div style={capLabel}>אל</div>
        <div style={capName}>{to.name}</div>
      </div>
    </div>
  );
}

function TranslateScreen({ from, to, text, out, focus, sub, typing, speaking, saved, onPick, onSwap, onFocusInput, onAction, onOpenHistory }) {
  const dir = (l) => (l.rtl ? "rtl" : "ltr");
  const align = (l) => (l.rtl ? "right" : "left");
  return (
    <div>
      <TopBar title="תרגום" onMenu={() => {}} />
      <div data-f="0"><LangBar from={from} to={to} focus={focus} sub={sub} onPick={onPick} onSwap={onSwap} /></div>

      <div data-f="1">
        <Card>
          <div style={{ padding: "var(--fos-space-5) var(--fos-space-6)", display: "flex", flexDirection: "column", gap: "var(--fos-space-3)" }}>
            <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between" }}>
              <div style={sectionLabel}>{from.name}</div>
              <div style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-40)", direction: "ltr" }}>{text.length}/1000</div>
            </div>
            <div onClick={onFocusInput} style={{
              background: "var(--fos-idle-bg-field)", borderRadius: "var(--fos-radius-textfield)",
              border: "var(--fos-focus-border-control) solid " + (focus === 1 ? "var(--fos-accent)" : "transparent"),
              padding: "var(--fos-space-5)", minHeight: 150, boxSizing: "border-box", cursor: "pointer",
              fontSize: "var(--fos-size-base)", lineHeight: "var(--fos-line-height)",
              color: text ? "var(--fos-text)" : "var(--fos-text-40)",
              direction: dir(from), textAlign: align(from), textWrap: "pretty",
              transition: "border-color var(--fos-transition-focus)"
            }}>
              {text || "הקלד טקסט לתרגום"}
              {typing && <span style={{ display: "inline-block", width: 3, height: 34, background: "var(--fos-accent)", verticalAlign: "-6px", marginInlineStart: 6 }} />}
            </div>
          </div>
        </Card>
      </div>

      {text ? (
        <div data-f="2">
          <Card>
            <div style={{ padding: "var(--fos-space-5) var(--fos-space-6)", display: "flex", flexDirection: "column", gap: "var(--fos-space-4)" }}>
              <div style={sectionLabel}>{to.name}</div>
              <div style={{ fontSize: "var(--fos-size-title)", fontWeight: "var(--fos-weight-medium)", lineHeight: "var(--fos-line-height)", direction: dir(to), textAlign: align(to), textWrap: "pretty" }}>{out}</div>
              {speaking && <ProgressBar value={0.35} />}
            </div>
            <Divider />
            <div style={{ display: "flex", padding: "var(--fos-space-2) var(--fos-space-5)", gap: "var(--fos-space-3)" }}>
              {ACTIONS.map((a, i) => {
                const f = focus === 2 && sub === i;
                const isSaved = a.icon === "star" && saved;
                return (
                  <div key={a.icon} onClick={() => onAction(i)} style={{
                    flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 4,
                    padding: "var(--fos-space-2) 0", borderRadius: "var(--fos-radius-item)",
                    background: f ? "var(--fos-accent-20)" : "transparent",
                    border: "4px solid " + (f ? "var(--fos-accent)" : "transparent"),
                    boxSizing: "border-box", cursor: "pointer",
                    transition: "background var(--fos-transition-focus), border-color var(--fos-transition-focus)"
                  }}>
                    <Icon name={a.icon} size={40} color={isSaved ? "var(--fos-favorite)" : "var(--fos-accent)"} />
                    <div style={{ fontSize: "var(--fos-size-label)", color: "var(--fos-text-60)" }}>{a.label}</div>
                  </div>
                );
              })}
            </div>
          </Card>
        </div>
      ) : (
        <EmptyState icon="translate" title="אין מה לתרגם" subtitle="לחץ על אישור בשדה כדי להקליד" />
      )}

      <SectionHeader>אחרונות</SectionHeader>
      <Card>
        {HISTORY.slice(0, 2).map((h, i) => (
          <React.Fragment key={h.src}>
            {i > 0 && <Divider />}
            <div data-f={3 + i}>
              <ListItem title={h.src} summary={h.dst} focused={focus === 3 + i}
                trailing={h.saved ? <Star /> : <Chevron />} onClick={() => onOpenHistory(h)} />
            </div>
          </React.Fragment>
        ))}
      </Card>
    </div>
  );
}

function LangPickerScreen({ role, query, focus, current, typing, onBack, onSelect }) {
  const q = query.trim();
  const list = LANGS.filter(l => l.name.includes(q) && (role === "from" || l.code !== "auto"));
  const recents = q ? [] : RECENT_LANGS.map(byCode);
  let idx = 0;
  const row = (l, key) => {
    const i = ++idx;
    return (
      <div data-f={i} key={key}>
        <ListItem title={l.name} focused={focus === i}
          trailing={l.code === current ? <Icon name="check" size={36} color="var(--fos-accent)" /> : null}
          onClick={() => onSelect(l.code)} />
      </div>
    );
  };
  return (
    <div>
      <TopBar title={role === "from" ? "תרגם מ" : "תרגם אל"} onBack={onBack} />
      <div data-f="0" style={{ padding: "0 var(--fos-space-screen) var(--fos-space-2)" }}>
        <TextField value={query} placeholder="חפש שפה" focused={focus === 0} showCaret={typing} />
      </div>
      {recents.length > 0 && (
        <React.Fragment>
          <SectionHeader>אחרונות</SectionHeader>
          <Card>{recents.map((l, i) => (
            <React.Fragment key={"r" + l.code}>{i > 0 && <Divider />}{row(l, "r" + l.code)}</React.Fragment>
          ))}</Card>
        </React.Fragment>
      )}
      <SectionHeader>כל השפות</SectionHeader>
      {list.length === 0
        ? <EmptyState icon="search_off" title="לא נמצאה שפה" subtitle="נסה מילה אחרת" />
        : <Card>{list.map((l, i) => (
            <React.Fragment key={l.code}>{i > 0 && <Divider />}{row(l, l.code)}</React.Fragment>
          ))}</Card>}
    </div>
  );
}

function HistoryScreen({ tab, focus, onBack, onTab, onOpen }) {
  const list = tab === 1 ? HISTORY.filter(h => h.saved) : HISTORY;
  return (
    <div>
      <TopBar title="היסטוריה" onBack={onBack} onMenu={() => {}} />
      <div data-f="0"><TabRow items={["הכול", "שמורים"]} selected={tab} focusedIndex={focus === 0 ? tab : -1} onSelect={onTab} /></div>
      {list.length === 0
        ? <EmptyState icon="bookmark" title="אין תרגומים שמורים" subtitle="לחץ על שמור בתרגום כדי להוסיף" />
        : (
          <Card>
            {list.map((h, i) => (
              <React.Fragment key={h.src}>
                {i > 0 && <Divider />}
                <div data-f={i + 1}>
                  <ListItem title={h.src} summary={h.dst + " · " + byCode(h.from).name + " ← " + byCode(h.to).name}
                    focused={focus === i + 1} trailing={h.saved ? <Star /> : <Chevron />} onClick={() => onOpen(h)} />
                </div>
              </React.Fragment>
            ))}
          </Card>
        )}
    </div>
  );
}

function TalkScreen({ listening, focus, onBack, onToggle }) {
  return (
    <div style={{ minHeight: 896, display: "flex", flexDirection: "column" }}>
      <TopBar title="שיחה" onBack={onBack} onMenu={() => {}} />
      <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: "var(--fos-space-5)", padding: "0 var(--fos-space-screen)" }}>
        {TALK.map((t, i) => {
          const he = t.side === "he";
          return (
            <div key={i} style={{ alignSelf: he ? "flex-start" : "flex-end", maxWidth: "84%", background: he ? "var(--fos-surface)" : "var(--fos-glass)", borderRadius: "var(--fos-radius-main)", padding: "var(--fos-space-5) var(--fos-space-6)", display: "flex", flexDirection: "column", gap: "var(--fos-space-2)" }}>
              <div style={sectionLabel}>{he ? "עברית" : "אנגלית"}</div>
              <div style={{ fontSize: "var(--fos-size-base)", lineHeight: "var(--fos-line-height)", direction: he ? "rtl" : "ltr", textAlign: he ? "right" : "left" }}>{t.text}</div>
              <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)", lineHeight: "var(--fos-line-height)", direction: he ? "ltr" : "rtl", textAlign: he ? "left" : "right" }}>{t.alt}</div>
            </div>
          );
        })}
      </div>
      <div data-f="0" style={{ padding: "var(--fos-space-8) var(--fos-space-screen) var(--fos-space-9)", display: "flex", flexDirection: "column", alignItems: "center", gap: "var(--fos-space-4)" }}>
        <div onClick={onToggle} style={{ width: 168, height: 168, borderRadius: 999, background: listening ? "var(--fos-accent-20)" : "var(--fos-text-08)", border: "4px solid " + (focus === 0 ? "var(--fos-accent)" : "transparent"), boxSizing: "border-box", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }}>
          <Icon name={listening ? "graphic_eq" : "mic"} size={80} color={listening ? "var(--fos-accent)" : "var(--fos-text-60)"} />
        </div>
        <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)" }}>{listening ? "מקשיב · עברית" : "לחץ על אישור כדי לדבר"}</div>
      </div>
    </div>
  );
}

Object.assign(window, { LANGS, HISTORY, TALK, ACTIONS, byCode, TranslateScreen, LangPickerScreen, HistoryScreen, TalkScreen });
