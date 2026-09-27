# Request for Quotation - FutureOS "Pro" Square-Screen Keypad Android Phone

**Project:** FutureOS Pro (4.5" square-screen bar keypad Android phone, no touchscreen)
**Service requested:** Full ODM - mainboard (PCBA) design, BSP/firmware bring-up, mechanical design, tooling, display sourcing, certification support, mass production
**Target volume:** 3,000-5,000 units first order, 10,000 units/year
**Target unit cost (FOB, incl. packaging):** USD 122 (4G). Please also quote a 5G variant
**Date:** 2026-09-27

---

## 1. Product summary

A bar-type 4G Android phone with a large **square** screen above a full keypad. All user interaction is via physical keys; there is **no touchscreen**.
The software (≈30 Android apps + system UI) is already written and running on a Duoqin/Qin F22 Pro (MT6768). On this model the UI canvas is 480×480 dp (density 480).

Why a square screen: a 5" 2:3 screen is 106 mm tall, and with a full keypad below it the phone would be ≈180 mm long. A 4.5" 1:1 screen (81 × 81 mm) keeps almost the same area and the phone stays ≈155 mm long.

Closest reference products: Unihertz Titan / BlackBerry Passport (square 1440×1440 panels) - without the touch panel and with a numeric keypad instead of QWERTY.

## 2. Required specification

| Item | Requirement |
|---|---|
| SoC | MediaTek Helio G99 (MT6789). 5G variant: Dimensity 6300 (MT6835) - state which you propose |
| RAM / Storage | 6GB LPDDR4X / 128GB UFS 2.2 (eMMC acceptable). Please also quote 4GB / 64GB |
| microSD | Yes |
| OS | Android 13 or newer (AOSP-based, GMS **not** required) |
| Display | 4.5" IPS, **1440×1440 (square, 1:1)**, MIPI-DSI, ≈453 ppi, active area ≈80.8 × 80.8 mm. **No touch panel**. Existing panel class (as used in Unihertz Titan / BlackBerry Passport) preferred - please name the panel you propose. 1080×1080 acceptable as a cost option |
| Keypad | 21 keys, metal domes, white backlight: D-pad 4-way + OK, 0-9, `*`, `#`, Call, End, 2 soft keys. **No Home key**. Side: Vol+, Vol-, Power. Volume = two separate keys (11 × 3 mm, 0.6 mm proud) on the right side, centre ≈38 mm below the top edge, Vol+ above Vol- with a tactile dot. Power = separate key below Vol- (8 × 3 mm, 6 mm gap, lengthwise groove), wired to PMIC PWRKEY. All three on metal domes on one side-key FPC with steel stiffener, max 1.3 mm deep inside the wall. Key pitch 21 × 10 mm |
| Key legends | Digit + Hebrew + Latin on 2-9. Hebrew per the Israeli standard (same as our T9): 2 דהו, 3 אבג, 4 מנ, 5 יכל, 6 זחט, 7 רשת, 8 צק, 9 סעפ (final forms not printed). Latin: 2 ABC, 3 DEF, 4 GHI, 5 JKL, 6 MNO, 7 PQRS, 8 TUV, 9 WXYZ. End key shows a hang-up icon (Power is the side key) |
| Cellular | 4G LTE, **VoLTE/IMS required**, Dual SIM. Bands: B1/B3/B7/B8/B20/B28 (Israel). 5G variant: n1/n3/n7/n8/n28/n78 |
| Wi-Fi / BT / GNSS | Wi-Fi 5 (2.4/5GHz); BT 5.x + BLE; GPS + GLONASS + Galileo + BeiDou |
| NFC | Yes (HCE) |
| USB | USB-C, OTG, 18W fast charging |
| Audio | Loudspeaker, receiver, 2 microphones (noise cancelling), 3.5mm jack with detection |
| Cameras | Rear 13MP AF + LED flash; front 5MP |
| Sensors | Accelerometer + gyroscope, **calibrated magnetometer**, **barometer**, ambient light, proximity, hardware step counter |
| Other | IR transmitter (consumer IR), vibration motor |
| Battery | Li-Po 4,000mAh, IEC 62133 |
| Size target | ≈ 84 × 155 × 12.5 mm, ≈185 g (3D enclosure model supplied) |
| Housing | PC/ABS |

## 3. Software / firmware requirements (important)

1. **Unlockable bootloader and root access** available to us (our system apps use `su`). A locked production build is fine as long as we can build/sign our own images.
2. **Delivery of kernel source and device tree** (GPL) and the vendor BSP build environment, so we can build our own AOSP image.
3. Ability to pre-install our apps as **system/privileged apps** and remove all vendor apps.
4. `ro.sf.lcd_density=480` (360 for a 1080×1080 panel); `android.hardware.faketouch` declared; no touch input device.
5. Sensor HAL must expose `TYPE_MAGNETIC_FIELD` (calibrated) and `TYPE_PRESSURE`.
6. VoLTE working with Israeli operators (Partner, Cellcom, Pelephone, HOT Mobile, Golan). Please state your experience with Israeli VoLTE whitelisting.
7. Security patches / OTA mechanism - please describe.

## 4. Differences from FutureOS Regular (for ODMs quoting both)

- 4.5" 1440×1440 square panel instead of 3.5" 640×960, wider body (84 mm)
- Helio G99 instead of G85, 6GB/128GB instead of 4GB/64GB
- NFC, Wi-Fi 5, 18W charging, 13MP + 5MP cameras, 4,000mAh

## 5. Certification

Target market: **Israel** first. Please quote support for: Israeli MoC type approval, SAR, CE/RED test reports, IEC 62133 battery, operator VoLTE acceptance testing. For the 5G variant, also 5G NR operator acceptance.

## 6. What we supply

- Full functional specification (this document)
- 3D enclosure concept (OpenSCAD / STL) with key pitch 21 × 10 mm - `futureos_family.scad`, MODEL=pro
- The complete Android application suite (APK + source) for integration testing
- A Qin F22 Pro reference unit on request

## 7. Please quote

1. **NRE** (one-time): PCBA design, BSP, mechanical design, tooling/molds, samples, certification support - itemized
2. **Unit price** at 5,000 / 10,000 / 20,000 units, with BOM breakdown - for 4G (6GB/128GB), 4G (4GB/64GB) and 5G
3. **Schedule**: EVT / DVT / PVT / MP milestones, including display lead time
4. **MOQ** for the device and for the panel
5. Number of **samples** included (EVT/DVT)
6. Warranty and defect rate (DOA) terms
7. Similar keypad or square-screen phones you have produced (references)
8. Payment terms

Please reply with a quotation and any questions. Alternative proposals that reduce cost or time (e.g. a 1080×1080 panel, or an existing platform of yours) are welcome - please describe exactly what changes.
