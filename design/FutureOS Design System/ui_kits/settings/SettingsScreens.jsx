const { TopBar, Card, SectionHeader, Divider, SettingItem, Switch, Slider, FosIcon: Icon, ConfirmDialog, OptionsMenu, Chip } = window.FutureOSDesignSystem_3ab611;

const GROUPS = [
  { header: "כללי", rows: [
    { id: "display", title: "תצוגה", summary: "בהירות, גודל טקסט", icon: "brightness_6" },
    { id: "sound", title: "צלילים", summary: "עוצמת מדיה ורינגטון", icon: "volume_up" },
    { id: "accent", title: "צבע הדגשה", summary: "לבן", icon: "palette" }
  ]},
  { header: "מערכת", rows: [
    { id: "language", title: "שפה", summary: "עברית", icon: "language" },
    { id: "keys", title: "מקשים", summary: "קיצורי מקשים מספריים", icon: "keyboard" },
    { id: "about", title: "אודות", summary: "FutureOS 1.0", icon: "info" }
  ]}
];

function SettingsRoot({ focus, onOpen, accentName }) {
  let i = -1;
  return (
    <div>
      <TopBar title="הגדרות" onBack={() => {}} onMenu={() => {}} />
      {GROUPS.map(g => (
        <div key={g.header}>
          <SectionHeader>{g.header}</SectionHeader>
          <Card>
            {g.rows.map((r, k) => {
              i++;
              const idx = i;
              return (
                <div key={r.id} data-f={idx}>
                  {k > 0 && <Divider />}
                  <SettingItem
                    title={r.title}
                    summary={r.id === "accent" ? accentName : r.summary}
                    icon={r.icon}
                    focused={focus === idx}
                    onClick={() => onOpen(r.id, idx)}
                  />
                </div>
              );
            })}
          </Card>
        </div>
      ))}
    </div>
  );
}

function DisplayScreen({ focus, values, onBack, onToggle }) {
  return (
    <div>
      <TopBar title="תצוגה" onBack={onBack} onMenu={() => {}} />
      <div style={{ marginTop: "var(--fos-space-2)", display: "flex", flexDirection: "column", gap: "var(--fos-space-5)" }}>
        <div data-f="0"><Slider label="בהירות מסך" value={values.brightness} focused={focus === 0} /></div>
        <div data-f="1"><Slider label="גודל טקסט" value={values.textSize} focused={focus === 1} /></div>
      </div>
      <SectionHeader>תצוגה</SectionHeader>
      <Card>
        <div data-f="2"><SettingItem title="מצב כהה" summary={values.dark ? "מופעל" : "כבוי"} icon="dark_mode"
          focused={focus === 2} trailing={<Switch on={values.dark} />} onClick={() => onToggle("dark")} /></div>
        <Divider />
        <div data-f="3"><SettingItem title="בהירות אדפטיבית" summary="התאמה לתאורת הסביבה" icon="brightness_auto"
          focused={focus === 3} trailing={<Switch on={values.adaptive} />} onClick={() => onToggle("adaptive")} /></div>
        <Divider />
        <div data-f="4"><SettingItem title="אנימציות" summary={values.anim ? "מופעל" : "כבוי"} icon="animation"
          focused={focus === 4} trailing={<Switch on={values.anim} />} onClick={() => onToggle("anim")} /></div>
      </Card>
    </div>
  );
}

const ACCENTS = [
  { name: "לבן", value: "#FFFFFF" },
  { name: "תכלת", value: "#64D2FF" },
  { name: "כתום", value: "#FF9F0A" },
  { name: "ירוק", value: "#30D158" },
  { name: "סגול", value: "#BF5AF2" }
];

function AccentScreen({ focus, selected, onBack, onPick }) {
  return (
    <div>
      <TopBar title="צבע הדגשה" onBack={onBack} />
      <Card>
        {ACCENTS.map((a, i) => (
          <div key={a.name}>
            {i > 0 && <Divider />}
            <SettingItem
              title={a.name}
              icon="circle"
              chevron={false}
              focused={focus === i}
              onClick={() => onPick(i)}
              trailing={selected === i ? <Icon name="check" size={44} color="var(--fos-accent)" /> : <div style={{ width: 44 }} />}
              style={{ "--fos-accent": a.value }}
            />
          </div>
        ))}
      </Card>
      <div style={{ padding: "var(--fos-space-8) var(--fos-space-9)", fontSize: "var(--fos-size-summary)", color: "var(--fos-text-40)", lineHeight: 1.4 }}>
        צבע ההדגשה הוא הצבע היחיד שהמשתמש בוחר. הוא מסמן פוקוס ובחירה בלבד.
      </div>
    </div>
  );
}

function SoundScreen({ focus, values, onBack }) {
  return (
    <div>
      <TopBar title="צלילים" onBack={onBack} />
      <div style={{ marginTop: "var(--fos-space-2)", display: "flex", flexDirection: "column", gap: "var(--fos-space-5)" }}>
        <Slider label="עוצמת מדיה" value={values.media} focused={focus === 0} />
        <Slider label="עוצמת רינגטון" value={values.ring} focused={focus === 1} />
        <Slider label="עוצמת התראות" value={values.notif} focused={focus === 2} />
      </div>
    </div>
  );
}

function AboutScreen({ focus, onBack, onReset }) {
  return (
    <div>
      <TopBar title="אודות" onBack={onBack} />
      <Card>
        <SettingItem title="גרסה" summary="FutureOS 1.0" icon="info" chevron={false} focused={focus === 0} />
        <Divider />
        <SettingItem title="מסך" summary="640 × 960 · צפיפות 2.0" icon="smartphone" chevron={false} focused={focus === 1} />
        <Divider />
        <SettingItem title="איפוס הגדרות" icon="restart_alt" focused={focus === 2} onClick={onReset} />
      </Card>
    </div>
  );
}

Object.assign(window, { SettingsRoot, DisplayScreen, AccentScreen, SoundScreen, AboutScreen, ACCENTS, GROUPS });
