/* FutureOS · בלוטות' — screens. Composed from the shipped components.
   Two non-component surfaces: the device hero on the device screen (a large
   glyph + name + status, no component covers it) and the pairing code block
   inside the pairing dialog, which repeats the dialog tokens. */
const { TopBar, Card, Divider, SectionHeader, ListItem, SettingItem, EmptyState, FosIcon: Icon, Switch, Button, ProgressBar, Badge } = window.FutureOSDesignSystem_3ab611;

const PAIRED = [
  { id: "buds", name: "אוזניות", type: "אוזניות", icon: "headphones", battery: 80, connected: true },
  { id: "car", name: "מערכת רכב", type: "רכב", icon: "directions_car", battery: null, connected: false },
  { id: "speaker", name: "רמקול סלון", type: "רמקול", icon: "speaker", battery: 45, connected: false }
];

const NEARBY = [
  { id: "watch", name: "שעון כושר", icon: "watch" },
  { id: "laptop", name: "מחשב נייד", icon: "laptop_mac" },
  { id: "phone", name: "הטלפון של דנה", icon: "smartphone" },
  { id: "kbd", name: "מקלדת אלחוטית", icon: "keyboard" }
];

const MENU = [
  { label: "רענן", icon: "refresh" },
  { label: "שם המכשיר", icon: "edit" },
  { label: "קבצים שהתקבלו", icon: "folder" },
  { label: "הגדרות", icon: "settings" },
  { label: "נתק הכל", icon: "link_off", destructive: true }
];

const Chevron = () => <Icon name="keyboard_arrow_left" size={36} color="var(--fos-text-30)" />;
const sectionLabel = { fontSize: "var(--fos-size-summary)", color: "var(--fos-text-55)", letterSpacing: "var(--fos-tracking-section)" };

const DeviceGlyph = ({ icon, color }) => (
  <Icon name={icon} size={44} color={color || "var(--fos-accent)"} />
);

/* status line under a paired device name: connected + battery, or plain state */
function statusOf(d) {
  if (d.state === "connecting") return "מתחבר";
  if (d.connected) return d.battery != null ? "מחובר · " + d.battery + "%" : "מחובר";
  return "מותאם";
}

function RootScreen({ on, devices, nearby, scanning, name, focus, onToggle, onName, onOpen, onPair }) {
  let i = 1;
  return (
    <div>
      <TopBar title="בלוטות'" onMenu={() => {}} />
      <Card>
        <div data-f="0">
          <SettingItem title="בלוטות'" summary={on ? "מופעל" : "כבוי"} icon="bluetooth"
            trailing={<Switch on={on} onChange={onToggle} />} focused={focus === 0} onClick={onToggle} />
        </div>
        <Divider />
        <div data-f="1">
          <SettingItem title="שם המכשיר" summary={name} icon="badge" chevron
            focused={focus === 1} onClick={onName} />
        </div>
      </Card>

      {!on ? (
        <EmptyState icon="bluetooth_disabled" title="בלוטות' כבוי"
          subtitle="לחץ על אישור כדי להפעיל ולחפש מכשירים" />
      ) : (
        <div>
          <SectionHeader>מכשירים מותאמים</SectionHeader>
          {devices.length === 0 ? (
            <EmptyState icon="devices" title="אין מכשירים מותאמים"
              subtitle="בחר מכשיר מהרשימה שלמטה כדי להתאים" />
          ) : (
            <Card>
              {devices.map((d, n) => {
                const f = ++i;
                return (
                  <React.Fragment key={d.id}>
                    {n > 0 && <Divider />}
                    <div data-f={f}>
                      <ListItem title={d.name} summary={statusOf(d)} focused={focus === f}
                        onClick={() => onOpen(d)}
                        trailing={
                          <div style={{ display: "flex", alignItems: "center", gap: "var(--fos-space-3)" }}>
                            <DeviceGlyph icon={d.icon} color={d.connected ? "var(--fos-accent)" : "var(--fos-text-40)"} />
                            <Chevron />
                          </div>
                        } />
                    </div>
                    {d.state === "connecting" && (
                      <div style={{ padding: "0 var(--fos-space-6) var(--fos-space-4)" }}><ProgressBar /></div>
                    )}
                  </React.Fragment>
                );
              })}
            </Card>
          )}

          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "var(--fos-space-9) var(--fos-space-7) var(--fos-space-3)" }}>
            <div style={sectionLabel}>מכשירים זמינים</div>
            {scanning && <div style={{ ...sectionLabel, color: "var(--fos-text-40)" }}>מחפש</div>}
          </div>
          {scanning && <div style={{ padding: "0 var(--fos-space-7) var(--fos-space-3)" }}><ProgressBar /></div>}
          <Card>
            {nearby.map((d, n) => {
              const f = ++i;
              return (
                <React.Fragment key={d.id}>
                  {n > 0 && <Divider />}
                  <div data-f={f}>
                    <ListItem title={d.name} focused={focus === f} onClick={() => onPair(d)}
                      trailing={<DeviceGlyph icon={d.icon} color="var(--fos-text-40)" />} />
                  </div>
                </React.Fragment>
              );
            })}
          </Card>
        </div>
      )}
    </div>
  );
}

function DeviceScreen({ device, focus, onBack, onProfile, onConnect, onRename, onForget }) {
  const d = device;
  return (
    <div>
      <TopBar title="מכשיר" onBack={onBack} onMenu={() => {}} />
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "var(--fos-space-3)", padding: "var(--fos-space-5) var(--fos-space-screen) var(--fos-space-9)" }}>
        <div style={{ width: 160, height: 160, borderRadius: 999, background: "var(--fos-text-08)", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <Icon name={d.icon} size={80} color={d.connected ? "var(--fos-accent)" : "var(--fos-text-40)"} />
        </div>
        <div style={{ fontSize: "var(--fos-size-screen-title)", fontWeight: "var(--fos-weight-bold)", textWrap: "pretty", textAlign: "center" }}>{d.name}</div>
        <div style={{ fontSize: "var(--fos-size-body)", color: "var(--fos-text-60)" }}>{statusOf(d)}</div>
        {d.state === "connecting" && <div style={{ width: "60%", paddingTop: "var(--fos-space-2)" }}><ProgressBar /></div>}
      </div>

      <SectionHeader>שימוש</SectionHeader>
      <Card>
        <div data-f="0">
          <SettingItem title="שיחות ואודיו" summary={d.calls ? "מופעל" : "כבוי"} icon="call"
            trailing={<Switch on={d.calls} onChange={() => onProfile("calls")} />}
            focused={focus === 0} onClick={() => onProfile("calls")} />
        </div>
        <Divider />
        <div data-f="1">
          <SettingItem title="מדיה" summary={d.media ? "מופעל" : "כבוי"} icon="music_note"
            trailing={<Switch on={d.media} onChange={() => onProfile("media")} />}
            focused={focus === 1} onClick={() => onProfile("media")} />
        </div>
        <Divider />
        <div data-f="2">
          <SettingItem title="שנה שם" summary={d.name} icon="edit" chevron
            focused={focus === 2} onClick={onRename} />
        </div>
      </Card>

      <div style={{ display: "flex", flexDirection: "column", gap: "var(--fos-space-3)", padding: "var(--fos-space-9) var(--fos-space-screen) var(--fos-space-7)" }}>
        <div data-f="3">
          <Button variant="secondary" fullWidth focused={focus === 3} onClick={onConnect}>
            {d.connected ? "נתק" : "התחבר"}
          </Button>
        </div>
        <div data-f="4">
          <Button variant="quiet" fullWidth focused={focus === 4} onClick={onForget}>שכח מכשיר</Button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { PAIRED, NEARBY, MENU, RootScreen, DeviceScreen, statusOf });
