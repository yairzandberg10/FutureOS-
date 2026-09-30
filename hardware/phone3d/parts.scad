// Every solid of the FutureOS Regular assembly as its own STL, already in assembly coordinates.
// It re-uses the modules of ../openscad/futureos_family.scad unchanged (PART is set to "none" so the family file draws nothing by itself)
// and applies exactly the transforms that its own assembly() uses. Z = 0 is the front face, the body grows toward negative Z.
// Usage: openscad -D 'MODEL="regular"' -D 'PART_OUT="front_shell"' -o stl/regular_front_shell.stl parts.scad
include <../openscad/futureos_family.scad>
PART_OUT = "front_shell";
if (PART_OUT == "front_shell")   translate([0, 0, -FRONT_T]) front_shell();
if (PART_OUT == "back_shell")    translate([0, 0, -BODY_T]) back_shell();
if (PART_OUT == "keypad")        translate([0, 0, -FRONT_T]) keypad(FRONT_KEYS);
if (PART_OUT == "markings")      translate([0, 0, -FRONT_T]) keypad_markings(FRONT_KEYS);
if (PART_OUT == "side_keys")     side_keys_placed();
if (PART_OUT == "glass")         translate([0, 0, -0.35]) screen_window(0.3);
if (PART_OUT == "display_area")  translate([CX - SCR_W / 2, Y_SCR + LCD_BOTTOM, -0.04]) cube([SCR_W, SCR_H, 0.02]);
if (PART_OUT == "punch")         translate([CX, BODY_L - TOP_BAND - LCD_SIDE - PUNCH_D, -0.02]) cylinder(d = PUNCH_D, h = 0.04);
if (PART_OUT == "lcd")           translate([0, 0, -FRONT_T]) translate([CX - SCR_OUT_W / 2, Y_SCR, -LCD_T + FRONT_T - LCD_SINK]) cube([SCR_OUT_W, SCR_OUT_H, LCD_T]);
if (PART_OUT == "battery")       translate([0, 0, -FRONT_T]) translate([CX - BATT[0] / 2, BODY_L - TOP_BAND - BATT[1], BATT_TOP_Z + FRONT_T - BATT[2]]) cube(BATT);
if (PART_OUT == "pcba")          translate([0, 0, -FRONT_T]) translate([CX - BOARD[0] / 2, BOTTOM_BAND - 1, -BOARD_Z - 1.5]) cube([BOARD[0], BOARD[1], BOARD_Z]);
if (PART_OUT == "side_fpc")      translate([0, 0, -FRONT_T]) translate([BODY_W - WALL - VOL_IN, SK_Y0 - VOL_FLANGE, VOL_Z + FRONT_T - VOL_H / 2 - VOL_FLANGE]) cube([VOL_FPC_T, SK_Y1 - SK_Y0 + 2 * VOL_FLANGE, VOL_H + 2 * VOL_FLANGE]);
