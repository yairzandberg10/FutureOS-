# Request for Quotation - FutureOS "Mini" Keypad Android Phone

**Project:** FutureOS Mini (2.8" keypad Android phone, no touchscreen)
**Service requested:** Full ODM - mainboard (PCBA) design, BSP/firmware bring-up, mechanical design, tooling, custom display sourcing, certification support, mass production
**Target volume:** 5,000 units first order (custom panel MOQ), 10,000 units/year
**Target unit cost (FOB, incl. packaging):** USD 70 (3GB RAM) / USD 77 (4GB RAM option)
**Date:** 2026-09-27

---

## 1. Product summary

A small bar-type 4G Android keypad phone. All user interaction is via physical keys; there is **no touchscreen**.
It is the compact sibling of FutureOS Regular (3.5", 640×960). The software (≈30 Android apps + system UI) is already written and running on a Duoqin/Qin F22 Pro (MT6768). The UI is laid out on a 320×480 dp canvas, so the Mini needs a **2:3 panel at density 240** and no software changes.

Closest reference products: Duoqin Qin F21 Pro / similar 2.8" Android keypad phones - without the touch panel.

## 2. Required specification

| Item | Requirement |
|---|---|
| SoC | Unisoc T606 (or T616). Alternative accepted: MediaTek Helio G36/G85 if cost allows - state which you propose |
| RAM / Storage | 3GB / 32GB eMCP. **Please also quote 4GB / 64GB** as an option |
| microSD | Yes |
| OS | Android 12 or newer (AOSP-based, GMS **not** required) |
| Display | 2.8" IPS, **480×720 (exact 2:3)**, MIPI-DSI, ≈309 ppi, active area ≈39.5 × 59.2 mm. **No touch panel**. This is a custom panel - please propose a panel vendor, MOQ and tooling cost. If a stock 2:3 panel close to this exists, propose it |
| Keypad | 21 keys, metal domes, white backlight: D-pad 4-way + OK, 0-9, `*`, `#`, Call, End, 2 soft keys. **No Home key**. Side: Vol+, Vol-, Power. Volume = two separate keys (9 × 3 mm, 0.6 mm proud) on the right side, centre ≈30 mm below the top edge, Vol+ above Vol- with a tactile dot; metal domes on a side-key FPC with steel stiffener, max 1.3 mm deep inside the wall. Key pitch 14 × 8 mm |
| Cellular | 4G LTE, **VoLTE/IMS required**, Dual SIM. Bands: B1/B3/B7/B8/B20/B28 (Israel) |
| Wi-Fi | Two SKUs on the same board: with Wi-Fi 2.4/5GHz, and without (Wi-Fi chip DNP) |
| Bluetooth / GNSS | BT 5.x + BLE; GPS + GLONASS + Galileo |
| USB | USB-C, OTG, 10W charging |
| Audio | Loudspeaker, receiver, 2 microphones (noise cancelling), 3.5mm jack with detection |
| Cameras | Rear 5MP fixed focus + LED flash (flash also used as flashlight). **No front camera** |
| Sensors | Accelerometer, **calibrated magnetometer**, ambient light, proximity. Step counter preferred (accelerometer with built-in pedometer is fine). Barometer and gyroscope not required |
| Other | Vibration motor. No IR, no NFC |
| Battery | Li-Po 1,500mAh, IEC 62133 |
| Size target | ≈ 51 × 129 × 12 mm, ≈110 g (3D enclosure model supplied) |
| Housing | PC/ABS |

## 3. Software / firmware requirements (important)

1. **Unlockable bootloader and root access** available to us (our system apps use `su`). A locked production build is fine as long as we can build/sign our own images.
2. **Delivery of kernel source and device tree** (GPL) and the vendor BSP build environment, so we can build our own AOSP image.
3. Ability to pre-install our apps as **system/privileged apps** and remove all vendor apps.
4. `ro.sf.lcd_density=240`; `android.hardware.faketouch` declared; no touch input device.
5. Sensor HAL must expose `TYPE_MAGNETIC_FIELD` (calibrated).
6. VoLTE working with Israeli operators (Partner, Cellcom, Pelephone, HOT Mobile, Golan). Please state your experience with Israeli VoLTE whitelisting.
7. Security patches / OTA mechanism - please describe.
8. The device must run ≈30 Jetpack Compose apps smoothly with 3GB RAM. Please state the low-memory configuration you recommend (zRAM size, LMK settings).

## 4. Differences from FutureOS Regular (for ODMs quoting both)

- Smaller panel (2.8" 480×720 custom vs 3.5" 640×960 stock)
- Unisoc T606 instead of Helio G85, 3GB/32GB instead of 4GB/64GB
- No front camera, no barometer, no gyroscope, no IR
- 1,500mAh instead of 3,000mAh
- Same keypad layout and key count, smaller pitch

## 5. Certification

Target market: **Israel** first. Please quote support for: Israeli MoC type approval, SAR, CE/RED test reports, IEC 62133 battery, operator VoLTE acceptance testing.

## 6. What we supply

- Full functional specification (this document)
- 3D enclosure concept (OpenSCAD / STL) with key pitch 14 × 8 mm - `futureos_family.scad`, MODEL=mini
- The complete Android application suite (APK + source) for integration testing
- A Qin F22 Pro reference unit on request

## 7. Please quote

1. **NRE** (one-time): PCBA design, BSP, mechanical design, tooling/molds, **custom display tooling**, samples, certification support - itemized
2. **Unit price** at 5,000 / 10,000 / 20,000 units, with BOM breakdown - for 3GB/32GB and 4GB/64GB
3. **Schedule**: EVT / DVT / PVT / MP milestones, including display lead time
4. **MOQ** for the device and for the custom panel
5. Number of **samples** included (EVT/DVT)
6. Warranty and defect rate (DOA) terms
7. Similar keypad Android phones you have produced (references)
8. Payment terms

Please reply with a quotation and any questions. Alternative proposals that reduce cost or time (e.g. reusing an existing small keypad-phone platform, or a stock panel of a similar size) are welcome - please describe exactly what changes.
