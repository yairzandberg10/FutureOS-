/* Sample data for both calls variations. Plain script — no JSX. */
const CALL_LOG = [
  { day: "היום", items: [
    { name: "מיכל לוי", phone: "052-3334455", dir: "in", time: "14:02", dur: "4:12" },
    { name: "052-3334455", phone: "052-3334455", dir: "missed", time: "12:40" },
    { name: "דני כהן", phone: "054-7778899", dir: "out", time: "11:15", dur: "0:48" }
  ]},
  { day: "אתמול", items: [
    { name: "שירה דהן", phone: "052-9991122", dir: "in", time: "19:30", dur: "12:03" },
    { name: "אבי מזרחי", phone: "050-1112233", dir: "missed", time: "17:08" },
    { name: "נועה ברק", phone: "053-9998877", dir: "out", time: "09:22", dur: "2:31" }
  ]}
];

const FAVORITES = [
  { name: "מיכל לוי", phone: "052-3334455" },
  { name: "אבי מזרחי", phone: "050-1112233" },
  { name: "אמא", phone: "050-4445566" },
  { name: "דני כהן", phone: "054-7778899" }
];

const CONTACTS_ALL = [
  { name: "אבי מזרחי", phone: "050-1112233" },
  { name: "אמא", phone: "050-4445566" },
  { name: "דני כהן", phone: "054-7778899" },
  { name: "מיכל לוי", phone: "052-3334455" },
  { name: "נועה ברק", phone: "053-9998877" },
  { name: "שירה דהן", phone: "052-9991122" }
];

const DIR = {
  in: { icon: "call_received", label: "נכנסת" },
  out: { icon: "call_made", label: "יוצאת" },
  missed: { icon: "call_missed", label: "לא נענתה" }
};

const KEYPAD = [
  { d: "1", s: "" }, { d: "2", s: "ABC" }, { d: "3", s: "DEF" },
  { d: "4", s: "GHI" }, { d: "5", s: "JKL" }, { d: "6", s: "MNO" },
  { d: "7", s: "PQRS" }, { d: "8", s: "TUV" }, { d: "9", s: "WXYZ" },
  { d: "*", s: "" }, { d: "0", s: "+" }, { d: "#", s: "" }
];

const flatLog = () => CALL_LOG.flatMap(g => g.items);
const initials = (n) => (/[A-Za-z\u0590-\u05FF]/.test(n[0]) ? n.trim().split(" ").slice(0, 2).map(w => w[0]).join("") : "#");

Object.assign(window, { CALL_LOG, FAVORITES, CONTACTS_ALL, DIR, KEYPAD, flatLog, initials });
