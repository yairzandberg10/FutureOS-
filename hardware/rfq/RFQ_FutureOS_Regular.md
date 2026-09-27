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
| Keypad | 21 keys, metal domes, white backlight: D-pad 4-way + OK, 0-9, `*`, `#`, Call, End, 2 soft keys. **No Home key**. Side: Vol+, Vol-, Power. Volume = two separate keys (10 × 3 mm, 0.6 mm proud) on the right side, centre ≈36 mm below the top edge, Vol+ above Vol- with a tactile dot. Power = separate key below Vol- (8 × 3 mm, 6 mm gap, lengthwise groove), wired to PMIC PWRKEY. All three on metal domes on one side-key FPC with steel stiffener, max 1.3 mm deep inside the wall. A working 5×5 matrix keyboard prototype (KiCad + gerbers) is available as layout reference |
| Key legends | Digit + Hebrew + Latin on 2-9. Hebrew per the Israeli standard (same as our T9): 2 דהו, 3 אבג, 4 מנ, 5 יכל, 6 זחט, 7 רשת, 8 צק, 9 סעפ (final forms not printed). Latin: 2 ABC, 3 DEF, 4 GHI, 5 JKL, 6 MNO, 7 PQRS, 8 TUV, 9 WXYZ. End key shows a hang-up icon (Power is the side key) |
| Cellular | 4G LTE, **VoLTE/IMS required**, Dual SIM. Bands: B1/B3/B7/B8/B20/B28 (Israel) |
| Wi-Fi | Two SKUs on the same board: with Wi-Fi 2.4/5GHz, and without (Wi-Fi chip DNP) |
| Bluetooth / GNSS | BT 5.x + BLE; GPS + GLONASS + Galileo |
| USB | USB-C, OTG, 10W charging |
| Audio | Loudspeaker, receiver, 2 microphones (noise cancelling), 3.5mm jack with detection |
| Cameras | Rear 8MP AF + LED flash; front 2MP |
| Sensors | Accelerometer + gyroscope, **calibrated magnetometer**, **barometer**, ambient light, proximity, hardware step counter |
| Other | IR transmitter (consumer IR), vibration motor |
| Battery | Li-Po 3,000mAh, IEC 62133, max 47 × 83 × 4.5 mm (the right side keys need the width) |
| Size target | ≈ 62 × 156 mm, **thickness 10 mm maximum** (9.5 mm preferred), ≈135 g (3D enclosure model supplied) |
| Housing | PC/ABS |

## 3. Software / firmware requirements (important)

1. **Unlockable bootloader and root access** available to us (our system apps use `su`). A locked production build is fine as long as we can build/sign our own images.
2. **Delivery of kernel source and device tree** (GPL) and the vendor BSP build environment, so we can build our own AOSP image.
3. Ability to pre-install our apps as **system/privileged apps** and remove all vendor apps.
4. `ro.sf.lcd_density=320`; `android.hardware.faketouch` declared; no touch input device.
5. Sensor HAL must expose `TYPE_MAGNETIC_FIELD` (calibrated) and `TYPE_PRESSURE`.
6. VoLTE working with Israeli operators (Partner, Cellcom, Pelephone, HOT Mobile, Golan). Please state your experience with Israeli VoLTE whitelisting.
7. Security patches / OTA mechanism - please describe.

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
