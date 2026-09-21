const { TopBar, Card, Divider, SectionHeader, SettingItem, ListItem, Chip, Button, FosIcon: Icon, TextField, EmptyState } = window.FutureOSDesignSystem_3ab611;

const S = { s7: "var(--fos-space-7)", s3: "var(--fos-space-3)", s5: "var(--fos-space-5)", s8: "var(--fos-space-8)", s9: "var(--fos-space-9)" };
const col = (gap) => ({ display: "flex", flexDirection: "column", gap });

function Avatar({ name, size = 176, icon }) {
  return (
    <div style={{ width: size, height: size, borderRadius: "var(--fos-radius-full)", background: "var(--fos-glass)", display: "flex", alignItems: "center", justifyContent: "center", flex: "0 0 auto" }}>
      {icon
        ? <Icon name={icon} size={Math.round(size * 0.44)} color="var(--fos-text-60)" />
        : <span style={{ fontSize: Math.round(size * 0.3), fontWeight: 500, color: "var(--fos-text-70)", fontFamily: "var(--fos-font-display)" }}>{window.initials(name)}</span>}
    </div>
  );
}

function ALog({ focus, filter, onOpen, onMenu }) {
  const groups = window.CALL_LOG
    .map(g => ({ day: g.day, items: filter === "missed" ? g.items.filter(i => i.dir === "missed") : g.items }))
    .filter(g => g.items.length);
  let i = -1;
  return (
    <div>
      <TopBar title="שיחות" onMenu={onMenu} />
      <div style={{ padding: `0 ${S.s7}`, display: "flex", gap: S.s3 }}>
        <Chip state={filter === "all" ? "selected" : "idle"}>הכל</Chip>
        <Chip state={filter === "missed" ? "selected" : "idle"}>לא נענו</Chip>
      </div>
      {groups.length === 0
        ? <EmptyState icon="call_missed" title="אין שיחות שלא נענו" subtitle="לחץ f כדי לחזור לכל השיחות" />
        : groups.map(g => (
          <div key={g.day}>
            <SectionHeader>{g.day}</SectionHeader>
            <Card>
              {g.items.map((c, k) => {
                const idx = ++i;
                return (
                  <React.Fragment key={c.name + c.time}>
                    {k > 0 && <Divider />}
                    <div data-f={idx}>
                      <SettingItem title={c.name} summary={window.DIR[c.dir].label + (c.dur ? " · " + c.dur : "")}
                        icon={window.DIR[c.dir].icon} focused={focus === idx} chevron={false}
                        onClick={() => onOpen(c)}
                        trailing={<span style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-40)", fontVariantNumeric: "tabular-nums" }}>{c.time}</span>} />
                    </div>
                  </React.Fragment>
                );
              })}
            </Card>
          </div>
        ))}
    </div>
  );
}

function ADialer({ digits, match, onCall }) {
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <TopBar title="מקלדת" />
      <div style={{ padding: `0 ${S.s7} ${S.s5}`, minHeight: 128, ...col("6px"), justifyContent: "center", alignItems: "center" }}>
        <div style={{ fontSize: "var(--fos-size-header)", fontWeight: 300, letterSpacing: "2px", direction: "ltr", color: digits ? "var(--fos-text)" : "var(--fos-text-30)", fontFamily: "var(--fos-font-display)" }}>
          {digits || "הקלד מספר"}
        </div>
        <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-accent)", minHeight: 34 }}>{match ? match.name : ""}</div>
      </div>
      <div style={{ flex: 1, padding: `0 ${S.s9}`, display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: S.s3, alignContent: "center" }}>
        {window.KEYPAD.map(k => (
          <div key={k.d} style={{ height: 116, borderRadius: "var(--fos-radius-card)", background: "var(--fos-calc-button)", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 2 }}>
            <span style={{ fontSize: "var(--fos-size-screen-title)", fontWeight: 500, color: "var(--fos-text)" }}>{k.d}</span>
            {k.s && <span style={{ fontSize: "var(--fos-size-badge)", color: "var(--fos-text-40)", letterSpacing: "1px" }}>{k.s}</span>}
          </div>
        ))}
      </div>
      <div style={{ padding: `${S.s5} ${S.s9} ${S.s7}` }}>
        <Button variant="primary" fullWidth focused={!!digits} onClick={onCall}>התקשר</Button>
      </div>
    </div>
  );
}

function AList({ title, rows, focus, empty, onOpen, onMenu, trailing }) {
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <TopBar title={title} onMenu={onMenu} />
      {rows.length === 0
        ? <div style={{ flex: 1 }}><EmptyState icon={empty.icon} title={empty.title} subtitle={empty.subtitle} style={{ height: "100%" }} /></div>
        : <div style={{ padding: `0 ${S.s7}`, ...col("var(--fos-space-item)") }}>
            {rows.map((r, k) => (
              <div key={r.name + k} data-f={k}>
                <ListItem title={r.name} summary={r.phone} focused={focus === k} onClick={() => onOpen(r)}
                  trailing={trailing ? trailing(r) : null} />
              </div>
            ))}
          </div>}
    </div>
  );
}

function ASearch({ query, rows, focus, onOpen }) {
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <TopBar title="חיפוש" onBack={() => {}} />
      <div style={{ padding: `0 ${S.s7} ${S.s5}` }}>
        <TextField value={query} placeholder="שם או מספר" focused showCaret />
      </div>
      {rows.length === 0
        ? <div style={{ flex: 1 }}><EmptyState icon="search_off" title="אין תוצאות" subtitle="נסה שם או ספרות אחרות" style={{ height: "100%" }} /></div>
        : <div style={{ padding: `0 ${S.s7}`, ...col("var(--fos-space-item)") }}>
            {rows.map((r, k) => (
              <div key={r.name} data-f={k}>
                <ListItem title={r.name} summary={r.phone} focused={focus === k} onClick={() => onOpen(r)} />
              </div>
            ))}
          </div>}
    </div>
  );
}

function AContact({ contact, focus, onCall, onBack, onMenu }) {
  const history = window.flatLog().filter(c => c.phone === contact.phone).slice(0, 3);
  const actions = [
    { title: "התקשר", icon: "call" },
    { title: "שלח הודעה", icon: "chat" },
    { title: "הוסף למועדפים", icon: "star" }
  ];
  return (
    <div>
      <TopBar title="איש קשר" onBack={onBack} onMenu={onMenu} />
      <div style={{ ...col("var(--fos-space-3)"), alignItems: "center", padding: `${S.s5} 0 ${S.s8}` }}>
        <Avatar name={contact.name} />
        <div style={{ fontSize: "var(--fos-size-screen-title)", fontWeight: 700, fontFamily: "var(--fos-font-display)" }}>{contact.name}</div>
        <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)", direction: "ltr", fontVariantNumeric: "tabular-nums" }}>{contact.phone}</div>
      </div>
      <Card>
        {actions.map((a, k) => (
          <React.Fragment key={a.title}>
            {k > 0 && <Divider />}
            <div data-f={k}>
              <SettingItem title={a.title} icon={a.icon} chevron={false} focused={focus === k}
                onClick={k === 0 ? onCall : undefined} />
            </div>
          </React.Fragment>
        ))}
      </Card>
      {history.length > 0 && (
        <>
          <SectionHeader>היסטוריה</SectionHeader>
          <Card>
            {history.map((c, k) => (
              <React.Fragment key={c.time + k}>
                {k > 0 && <Divider />}
                <SettingItem title={window.DIR[c.dir].label} summary={c.dur ? c.dur : "—"} icon={window.DIR[c.dir].icon}
                  chevron={false}
                  trailing={<span style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-40)" }}>{c.time}</span>} />
              </React.Fragment>
            ))}
          </Card>
        </>
      )}
    </div>
  );
}

function AIncoming({ contact, focus }) {
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "space-between", padding: `${S.s9} ${S.s9}` }}>
      <div style={{ ...col("var(--fos-space-3)"), alignItems: "center", marginTop: 40 }}>
        <div style={{ fontSize: "var(--fos-size-label)", letterSpacing: "var(--fos-tracking-section)", color: "var(--fos-text-60)" }}>שיחה נכנסת</div>
        <Avatar name={contact.name} size={200} />
        <div style={{ fontSize: "var(--fos-size-header)", fontWeight: 700, fontFamily: "var(--fos-font-display)", textAlign: "center" }}>{contact.name}</div>
        <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)", direction: "ltr" }}>{contact.phone}</div>
      </div>
      <div style={{ width: "100%", ...col("var(--fos-space-3)") }}>
        <Button variant="primary" fullWidth focused={focus === 0}>ענה</Button>
        <Button variant="destructive" fullWidth focused={focus === 1}>דחה</Button>
      </div>
    </div>
  );
}

function AActive({ contact, elapsed, focus, mute, speaker, hold }) {
  const controls = [
    { title: mute ? "בטל השתקה" : "השתק", icon: mute ? "mic_off" : "mic", on: mute },
    { title: "רמקול", icon: "volume_up", on: speaker },
    { title: hold ? "המשך" : "המתנה", icon: hold ? "play_arrow" : "pause", on: hold },
    { title: "מקלדת", icon: "dialpad" }
  ];
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <div style={{ ...col("var(--fos-space-2)"), alignItems: "center", padding: `${S.s9} 0 ${S.s8}` }}>
        <Avatar name={contact.name} size={160} />
        <div style={{ fontSize: "var(--fos-size-screen-title)", fontWeight: 700, fontFamily: "var(--fos-font-display)" }}>{contact.name}</div>
        <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-success)", fontVariantNumeric: "tabular-nums", direction: "ltr" }}>{elapsed}</div>
      </div>
      <div style={{ flex: 1, padding: `0 ${S.s9}`, display: "grid", gridTemplateColumns: "1fr 1fr", gap: S.s3, alignContent: "start" }}>
        {controls.map((c, k) => (
          <div key={c.title} data-f={k} style={{ height: 132, borderRadius: "var(--fos-radius-card)",
            background: c.on ? "var(--fos-accent-20)" : "var(--fos-glass)",
            border: focus === k ? "2px solid var(--fos-accent)" : "2px solid transparent",
            ...col("6px"), alignItems: "center", justifyContent: "center" }}>
            <Icon name={c.icon} size={40} color={c.on ? "var(--fos-accent)" : "var(--fos-text-70)"} />
            <span style={{ fontSize: "var(--fos-size-summary)", color: "var(--fos-text-60)" }}>{c.title}</span>
          </div>
        ))}
      </div>
      <div style={{ padding: `${S.s5} ${S.s9} ${S.s8}` }}>
        <Button variant="destructive" fullWidth focused={focus === 4}>סיים שיחה</Button>
      </div>
    </div>
  );
}

function AEnded({ contact, duration, focus }) {
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: S.s5, padding: S.s9 }}>
      <Avatar name={contact.name} size={160} />
      <div style={{ ...col("6px"), alignItems: "center" }}>
        <div style={{ fontSize: "var(--fos-size-screen-title)", fontWeight: 700, fontFamily: "var(--fos-font-display)" }}>{contact.name}</div>
        <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)" }}>השיחה הסתיימה · <span style={{ direction: "ltr", display: "inline-block" }}>{duration}</span></div>
      </div>
      <div style={{ width: "100%", ...col("var(--fos-space-3)"), marginTop: S.s5 }}>
        <Button variant="primary" fullWidth focused={focus === 0}>התקשר שוב</Button>
        <Button variant="quiet" fullWidth focused={focus === 1}>חזור ליומן</Button>
      </div>
    </div>
  );
}

Object.assign(window, { ALog, ADialer, AList, ASearch, AContact, AIncoming, AActive, AEnded });
