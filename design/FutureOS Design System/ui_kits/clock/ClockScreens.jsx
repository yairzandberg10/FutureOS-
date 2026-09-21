const { TopBar, Card, Divider, SettingItem, Switch, EmptyState, ProgressBar } = window.FutureOSDesignSystem_3ab611;

const ALARMS = [
  { time: "07:00", days: "ימי חול", on: true },
  { time: "08:30", days: "שבת", on: false },
  { time: "13:15", days: "חד פעמי", on: true }
];

const ZONES = [
  { city: "ירושלים", zone: "עכשיו", time: "07:24" },
  { city: "לונדון", zone: "אתמול · מינוס 2ש׳", time: "05:24" },
  { city: "ניו יורק", zone: "אתמול · מינוס 7ש׳", time: "00:24" },
  { city: "בנגקוק", zone: "פלוס 4ש׳", time: "11:24" }
];

function ClockScreen() {
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: "var(--fos-space-3)" }}>
      <div style={{ fontFamily: "var(--fos-font-mono)", fontSize: "var(--fos-size-clock)", fontWeight: "var(--fos-weight-light)", lineHeight: 1 }}>07:24</div>
      <div style={{ fontSize: "var(--fos-size-base)", color: "var(--fos-text-60)" }}>יום שני, 13 בספטמבר</div>
    </div>
  );
}

function AlarmsScreen({ focus, alarms, onToggle, onEdit }) {
  return (
    <div>
      <TopBar title="מעורר" onMenu={() => {}} />
      <Card>
        {alarms.map((a, i) => (
          <div key={a.time}>
            {i > 0 && <Divider />}
            <SettingItem title={a.time} summary={a.days} icon="alarm" focused={focus === i}
              trailing={<Switch on={a.on} />} onClick={() => onEdit(i)} />
          </div>
        ))}
      </Card>
      <div style={{ padding: "var(--fos-space-8) var(--fos-space-9)", fontSize: "var(--fos-size-summary)", color: "var(--fos-text-40)" }}>
        אישור פותח את בורר השעה. מקש התפריט מוסיף מעורר חדש.
      </div>
    </div>
  );
}

function StopwatchScreen({ running, value }) {
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <TopBar title="סטופר" />
      {running
        ? <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: "var(--fos-space-8)", padding: "0 var(--fos-space-7)" }}>
            <div style={{ fontFamily: "var(--fos-font-mono)", fontSize: "var(--fos-size-clock)", fontWeight: "var(--fos-weight-light)", lineHeight: 1 }}>00:42</div>
            <div style={{ width: "100%" }}><ProgressBar value={value} /></div>
          </div>
        : <div style={{ flex: 1 }}><EmptyState icon="timer" title="הסטופר עצור" subtitle="לחץ על אישור כדי להתחיל" style={{ height: "100%" }} /></div>}
    </div>
  );
}

function WorldClockScreen({ focus }) {
  return (
    <div>
      <TopBar title="שעון עולמי" onMenu={() => {}} />
      <Card>
        {ZONES.map((z, i) => (
          <div key={z.city} data-f={i}>
            {i > 0 && <Divider />}
            <SettingItem title={z.city} summary={z.zone} icon="public" chevron={false} focused={focus === i}
              trailing={<span style={{ fontFamily: "var(--fos-font-mono)", fontSize: "var(--fos-size-title)", color: "var(--fos-text)" }}>{z.time}</span>} />
          </div>
        ))}
      </Card>
      <div style={{ padding: "var(--fos-space-8) var(--fos-space-9)", fontSize: "var(--fos-size-summary)", color: "var(--fos-text-40)" }}>
        מקש התפריט מוסיף עיר.
      </div>
    </div>
  );
}

function TimerScreen({ running, value }) {
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <TopBar title="טיימר" onMenu={() => {}} />
      {running
        ? <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: "var(--fos-space-8)", padding: "0 var(--fos-space-7)" }}>
            <div style={{ fontFamily: "var(--fos-font-mono)", fontSize: "var(--fos-size-clock)", fontWeight: "var(--fos-weight-light)", lineHeight: 1 }}>04:35</div>
            <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)" }}>מתוך 5:00 · בישול</div>
            <div style={{ width: "100%" }}><ProgressBar value={value} /></div>
          </div>
        : <div style={{ flex: 1 }}><EmptyState icon="hourglass_empty" title="אין טיימר פעיל" subtitle="לחץ על אישור כדי להגדיר משך" style={{ height: "100%" }} /></div>}
    </div>
  );
}

Object.assign(window, { ClockScreen, AlarmsScreen, StopwatchScreen, WorldClockScreen, TimerScreen, ALARMS, ZONES });
