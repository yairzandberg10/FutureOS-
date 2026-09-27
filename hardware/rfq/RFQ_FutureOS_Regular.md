# Request for Quotation - FutureOS "Regular" Keypad Android Phone

**Project:** FutureOS Regular (3.5" keypad Android phone, no touchscreen)
**Service requested:** Full ODM - mainboard (PCBA) design, BSP/firmware bring-up, mechanical design, tooling, certification support, mass production
**Target volume:** 3,000 units first order, 10,000 units/year
**Target unit cost (FOB, incl. packaging):** USD 90
**Date:** 2026-09-27

---

## 1. Product summary

A bar-type 4G Android keypad phone. All user interaction is via physical keys; there is **no touchscreen**.
The software (≈30 Android apps + system UI) is already written and running on a Duoqin/Qin F22 Pro (MT6768). We need hardware that runs it at least as well, plus the gaps listed in section 4.

Closest reference product: **Duoqin Qin F22 Pro** (same screen, same SoC family) - without the touch panel.

## 2. Required specification

| Item | Requirement |
|---|---|
| SoC | MediaTek Helio G85 (MT6769) or G88. Alternative accepted: Unisoc T606/T616 - state which you propose |
| RAM / Storage | 4GB LPDDR4X / 64GB eMMC 5.1 (uMCP acceptable) |
| microSD | Yes |
| OS | Android 12 or newer (AOSP-based, GMS **not** required) |
| Display | 3.5" IPS, **640×960**, MIPI-DSI, 320dpi (same panel as Qin F22 Pro). **No touch panel** |
| Keypad | 21 keys, metal domes, white backlight: D-pad 4-way + OK, 0-9, `*`, `#`, Call, End, 2 soft keys. **No Home key** (End acts as Home, see 3.8). Side: Vol+, Vol-, Power. A working 5×5 matrix keyboard prototype (KiCad + gerbers) is available as layout reference |
| Cellular | 4G LTE, **VoLTE/IMS required**, Dual SIM. Bands: B1/B3/B7/B8/B20/B28 (Israel) |
| Wi-Fi | Two SKUs on the same board: with Wi-Fi 2.4/5GHz, and without (Wi-Fi chip DNP) |
| Bluetooth / GNSS | BT 5.x + BLE; GPS + GLONASS + Galileo |
| USB | USB-C, OTG, 10W charging |
| Audio | Loudspeaker, receiver, 2 microphones (noise cancelling), 3.5mm jack with detection |
| Cameras | Rear 8MP AF + LED flash; front 2MP |
| Sensors | Accelerometer + gyroscope, **calibrated magnetometer**, **barometer**, ambient light, proximity, hardware step counter |
| Other | IR transmitter (consumer IR), vibration motor |
| Battery | Li-Po 3,000mAh, IEC 62133 |
| Size target | ≈ 62 × 156 × 12 mm, ≈145 g (3D enclosure model supplied) |
| Housing | PC/ABS |

## 3. Software / firmware requirements (important)

1. **Unlockable bootloader and root access** available to us (our system apps use `su`). A locked production build is fine as long as we can build/sign our own images.
2. **Delivery of kernel source and device tree** (GPL) and the vendor BSP build environment, so we can build our own AOSP image.
3. Ability to pre-install our apps as **system/privileged apps** and remove all vendor apps.
4. `ro.sf.lcd_density=320`; `android.hardware.faketouch` declared; no touch input device.
5. Sensor HAL must expose `TYPE_MAGNETIC_FIELD` (calibrated) and `TYPE_PRESSURE`.
6. VoLTE working with Israeli operators (Partner, Cellcom, Pelephone, HOT Mobile, Golan). Please state your experience with Israeli VoLTE whitelisting.
7. Security patches / OTA mechanism - please describe.
8. Keypad driver (mtk-kpd or equivalent) mapped to standard Android keycodes: `DPAD_*`, `DPAD_CENTER`, `0`-`9`, `STAR`, `POUND`, `CALL`, `ENDCALL`, `MENU` (left soft key), `BACK` (right soft key). There is no Home key: End must go Home outside a call and sleep when already Home (`Settings.System.end_button_behavior` default = 3).

## 4. Gaps vs. Qin F22 Pro that the new device must close

- Calibrated magnetometer (F22 Pro exposes only uncalibrated)
- Barometer (missing on F22 Pro)
- No touch panel
- Clean firmware with no vendor apps

## 5. Certification

Target market: **Israel** first. Please quote support for: Israeli MoC type approval, SAR, CE/RED test reports, IEC 62133 battery, operator VoLTE acceptance testing.

## 6. What we supply

- Full functional specification (this document)
- 3D enclosure concept (OpenSCAD / STL) with key pitch 17 × 9.5 mm - `futureos_family.scad`, MODEL=regular
- The complete Android application suite (APK + source) for integration testing
- A Qin F22 Pro reference unit on request

## 7. Please quote

1. **NRE** (one-time): PCBA design, BSP, mechanical design, tooling/molds, samples, certification support - itemized
2. **Unit price** at 3,000 / 5,000 / 10,000 units, with BOM breakdown
3. **Schedule**: EVT / DVT / PVT / MP milestones
4. **MOQ**
5. Number of **samples** included (EVT/DVT)
6. Warranty and defect rate (DOA) terms
7. Similar keypad Android phones you have produced (references)
8. Payment terms

Please reply with a quotation and any questions. Alternative proposals that reduce cost or time (e.g. reusing an existing keypad-phone platform of yours) are welcome - please describe exactly what changes.
