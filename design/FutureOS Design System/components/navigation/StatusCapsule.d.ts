/**
 * StatusCapsule — a status-bar capsule (DynamicCapsule). Translucent (#1C1C1E at 55%, 0.5dp 15% white
 * hairline), 22dp tall, a pill, over a transparent bar. The leading capsule is dynamic: it widens to
 * show live activity, then returns to the time and app glyphs.
 */
export interface StatusCapsuleProps {
  /** What the capsule shows. "idle" is the time and the app glyphs; the other four are live activity:
   *  call (success fill, black ink), charging (bolt in success), notification (app glyph + app name),
   *  media (music note + title). In the app the call shows for as long as it lasts, charging and
   *  notification for about 3 seconds, media while it plays; priority is call, event, media, idle.
   *  Default "idle". */
  state?: "idle" | "call" | "charging" | "notification" | "media";
  /** The time, shown only in the idle state at 14sp / 600. e.g. "07:30". */
  time?: string;
  /** The live text at 12sp: the call duration ("1:24"), "בטעינה · 76%", the app name (ellipsized past
   *  90dp), the media title (ellipsized past 110dp). Ignored when idle. */
  label?: string;
  /** Idle state: FosIcon names of the apps with pending notifications, 14dp white, after the time.
   *  Four at most, then "+N". */
  apps?: string[];
  /** Notification state: FosIcon name of the app glyph. Default "chat". */
  icon?: string;
  /** Static content (the trailing device-status capsule: glyphs at 70% white, the battery percent,
   *  the battery). Overrides `state`. */
  children?: React.ReactNode;
  style?: React.CSSProperties;
}
export function StatusCapsule(props: StatusCapsuleProps): JSX.Element;
