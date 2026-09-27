# Request for Quotation - FutureOS "Pro" Slider Keypad Android Phone

**Project:** FutureOS Pro (5" slider keypad Android phone, no touchscreen)
**Service requested:** Full ODM - mainboard (PCBA) design, BSP/firmware bring-up, mechanical design including slide mechanism, tooling, custom display sourcing, certification support, mass production
**Target volume:** 5,000 units first order (custom panel MOQ), 10,000 units/year
**Target unit cost (FOB, incl. packaging):** USD 135 (4G). Please also quote a 5G variant
**Date:** 2026-09-27

---

## 1. Product summary

A slider-type 4G Android phone with a large screen and physical keys only; there is **no touchscreen**.
The front carries the D-pad, soft keys and Call/End. The numeric keypad (0-9, `*`, `#`) is on a sled that **slides out downward** under the screen.
The software (≈30 Android apps + system UI) is already written and running on a Duoqin/Qin F22 Pro (MT6768). The UI is laid out on a 320×480 dp canvas, so the Pro needs a **2:3 panel at density 480** and no software changes.

Why a slider: a 5" 2:3 screen is 106 mm tall. With a full keypad below it, a bar phone would be ≈81 × 191 mm. As a slider it is 151 mm closed.

## 2. Required specification

| Item | Requirement |
|---|---|
| SoC | MediaTek Helio G99 (MT6789). 5G variant: Dimensity 6300 (MT6835) - state which you propose |
| RAM / Storage | 6GB LPDDR4X / 128GB UFS 2.2 (eMMC acceptable). Please also quote 4GB / 64GB |
| microSD | Yes |
| OS | Android 13 or newer (AOSP-based, GMS **not** required) |
| Display | 5.0" IPS, **960×1440 (exact 2:3)**, MIPI-DSI, ≈346 ppi, active area ≈70.4 × 105.7 mm. **No touch panel**. This is a custom panel - please propose a panel vendor, MOQ and tooling cost. If a stock 2:3 panel close to this exists, propose it |
| Front keys | D-pad 4-way + OK, Call, End, 2 soft keys (9 keys). **No Home key**. Key pitch 21 × 10 mm |
| Slide-out keypad | 0-9, `*`, `#` (12 keys) on the sled. Metal domes, white backlight on all keys |
| Side keys | Vol+, Vol-, Power |
| Slide mechanism | Spring-assisted, ≥100,000 open/close cycles, FPC rated for the same. Sled travel ≈42 mm |
| Slide detection | **Hall sensor + magnet** reporting open/closed as an input switch (`SW_KEYPAD_SLIDE` / `SW_LID`), so Android updates `Configuration.hardKeyboardHidden` |
| Cellular | 4G LTE, **VoLTE/IMS required**, Dual SIM. Bands: B1/B3/B7/B8/B20/B28 (Israel). 5G variant: n1/n3/n7/n8/n28/n78 |
| Wi-Fi / BT / GNSS | Wi-Fi 5 (2.4/5GHz); BT 5.x + BLE; GPS + GLONASS + Galileo + BeiDou |
| NFC | Yes (HCE) |
| USB | USB-C, OTG, 18W fast charging |
| Audio | Loudspeaker, receiver, 2 microphones (noise cancelling), 3.5mm jack with detection |
| Cameras | Rear 13MP AF + LED flash; front 5MP |
| Sensors | Accelerometer + gyroscope, **calibrated magnetometer**, **barometer**, ambient light, proximity, hardware step counter |
| Other | IR transmitter (consumer IR), vibration motor |
| Battery | Li-Po 4,000mAh, IEC 62133 |
| Size target | ≈ 82 × 151 × 16.5 mm closed (≈193 mm open), ≈225 g (3D enclosure model supplied) |
| Housing | PC/ABS with reinforced frame (metal mid-frame or glass-fibre PC acceptable) |

## 3. Software / firmware requirements (important)

1. **Unlockable bootloader and root access** available to us (our system apps use `su`). A locked production build is fine as long as we can build/sign our own images.
2. **Delivery of kernel source and device tree** (GPL) and the vendor BSP build environment, so we can build our own AOSP image.
3. Ability to pre-install our apps as **system/privileged apps** and remove all vendor apps.
4. `ro.sf.lcd_density=480`; `android.hardware.faketouch` declared; no touch input device.
5. Front keys and sled keys on the same keypad input device; slide switch reported as described above.
6. Sensor HAL must expose `TYPE_MAGNETIC_FIELD` (calibrated) and `TYPE_PRESSURE`.
7. VoLTE working with Israeli operators (Partner, Cellcom, Pelephone, HOT Mobile, Golan). Please state your experience with Israeli VoLTE whitelisting.
8. Security patches / OTA mechanism - please describe.

## 4. Differences from FutureOS Regular (for ODMs quoting both)

- Slider form factor, two keypads, slide mechanism and Hall sensor
- 5" 960×1440 custom panel instead of 3.5" 640×960 stock
- Helio G99 instead of G85, 6GB/128GB instead of 4GB/64GB
- NFC, Wi-Fi 5, 18W charging, 13MP + 5MP cameras, 4,000mAh
- Two PCBs (main board + sled keypad board) joined by FPC

## 5. Certification

Target market: **Israel** first. Please quote support for: Israeli MoC type approval, SAR, CE/RED test reports, IEC 62133 battery, operator VoLTE acceptance testing. For the 5G variant, also 5G NR operator acceptance.

## 6. What we supply

- Full functional specification (this document)
- 3D enclosure concept (OpenSCAD / STL) with key pitch 21 × 10 mm - `futureos_family.scad`, MODEL=pro, PRO_FORM=slider (open and closed)
- The complete Android application suite (APK + source) for integration testing
- A Qin F22 Pro reference unit on request

## 7. Please quote

1. **NRE** (one-time): PCBA design, BSP, mechanical design, **slide mechanism**, tooling/molds, **custom display tooling**, samples, certification support - itemized
2. **Unit price** at 5,000 / 10,000 / 20,000 units, with BOM breakdown - for 4G (6GB/128GB), 4G (4GB/64GB) and 5G
3. **Schedule**: EVT / DVT / PVT / MP milestones, including display lead time and slider life testing
4. **MOQ** for the device and for the custom panel
5. Number of **samples** included (EVT/DVT)
6. Warranty and defect rate (DOA) terms, including slider mechanism
7. Slider phones you have produced (references)
8. Payment terms

Please reply with a quotation and any questions. Alternative proposals that reduce cost or time (e.g. a bar form factor, or an existing slider platform of yours) are welcome - please describe exactly what changes.
