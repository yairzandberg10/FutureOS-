const { TopBar, ListItem, EmptyState, FosIcon: Icon, Badge } = window.FutureOSDesignSystem_3ab611;

const RECENTS = [
  { name: "מיכל לוי", meta: "שיחה נכנסת · 14:02", icon: "call_received" },
  { name: "052-3334455", meta: "שיחה שלא נענתה · 12:40", icon: "call_missed", missed: true },
  { name: "דני כהן", meta: "שיחה יוצאת · אתמול", icon: "call_made" },
  { name: "שירה דהן", meta: "שיחה נכנסת · אתמול", icon: "call_received" },
  { name: "אבי מזרחי", meta: "שיחה יוצאת · יום ראשון", icon: "call_made" }
];

const CONTACTS = [
  { name: "אבי מזרחי", phone: "050-1112233", fav: true },
  { name: "דני כהן", phone: "054-7778899" },
  { name: "מיכל לוי", phone: "052-3334455", fav: true },
  { name: "נועה ברק", phone: "053-9998877" },
  { name: "שירה דהן", phone: "052-3334455" }
];

const THREADS = [
  { name: "מיכל לוי", last: "נתראה בערב", unread: 2 },
  { name: "דני כהן", last: "שלחתי לך את הכתובת", unread: 0 },
  { name: "בזק", last: "החשבון שלך זמין לצפייה", unread: 1 },
  { name: "נועה ברק", last: "תודה רבה", unread: 0 }
];

function RecentsScreen({ focus, onMenu }) {
  return (
    <div>
      <TopBar title="שיחות" onMenu={onMenu} />
      <div style={{ padding: "0 var(--fos-space-7)", display: "flex", flexDirection: "column", gap: "var(--fos-space-item)" }}>
        {RECENTS.map((r, i) => (
          <ListItem key={r.name + i} title={r.name} summary={r.meta} focused={focus === i}
            trailing={<Icon name={r.icon} size={36} color={r.missed ? "var(--fos-danger)" : "var(--fos-text-40)"} />} />
        ))}
      </div>
    </div>
  );
}

function ContactsScreen({ focus, empty, onMenu }) {
  return (
    <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <TopBar title="אנשי קשר" onMenu={onMenu} />
      {empty
        ? <div style={{ flex: 1 }}><EmptyState icon="person" title="אין אנשי קשר" subtitle="לחץ על מקש התפריט כדי להוסיף" style={{ height: "100%" }} /></div>
        : <div style={{ padding: "0 var(--fos-space-7)", display: "flex", flexDirection: "column", gap: "var(--fos-space-item)" }}>
            {CONTACTS.map((c, i) => (
              <ListItem key={c.name} title={c.name} summary={c.phone} focused={focus === i}
                trailing={c.fav ? <Icon name="star" size={36} fill={1} color="var(--fos-favorite)" /> : null} />
            ))}
          </div>}
    </div>
  );
}

function MessagesScreen({ focus, onMenu }) {
  return (
    <div>
      <TopBar title="הודעות" onMenu={onMenu} />
      <div style={{ padding: "0 var(--fos-space-7)", display: "flex", flexDirection: "column", gap: "var(--fos-space-item)" }}>
        {THREADS.map((t, i) => (
          <ListItem key={t.name} title={t.name} summary={t.last} focused={focus === i}
            trailing={t.unread ? <Badge count={t.unread} /> : null} />
        ))}
      </div>
      <div style={{ padding: "var(--fos-space-8) var(--fos-space-9)", fontSize: "var(--fos-size-summary)", color: "var(--fos-text-30)", lineHeight: 1.4 }}>
        תצוגת שרשור ההודעות עצמו לא מופיעה במקורות ולכן הושארה בכוונה מחוץ לערכה.
      </div>
    </div>
  );
}

Object.assign(window, { RecentsScreen, ContactsScreen, MessagesScreen, RECENTS, CONTACTS, THREADS });
