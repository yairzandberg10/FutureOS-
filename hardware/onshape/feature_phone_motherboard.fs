// =====================================================================
//  מאדרבורד לטלפון פיצ'ר (Feature Phone) - 110 x 50 x 1.6 מ"מ
//  סקריפט חד-פעמי ל-Part Studio ב-Onshape.
//
//  כל הרכיבים (SoC, מחברים, קריסטל וכו') מיוצגים כנפחי "keep-out"
//  פשוטים (תיבות/גלילים מורמים מעל פני הלוח) - מטרתם בדיקת פינוי
//  מכני מול השאסי בלבד, ולא ייצוג אלקטרוני/מכני מדויק של הרכיבים.
//
//  איך להשתמש (חשוב - שימו לב לסדר):
//  1. ב-Onshape: Part Studio -> לשונית "+" -> Feature Studio.
//  2. אל תמחקו את שתי השורות הראשונות שה-Feature Studio יצר אוטומטית
//     (FeatureScript <מספר גרסה>; import(...);) - הן כבר מכילות את
//     מספר הגרסה הנכון לחשבון שלכם. פשוט הדביקו את שאר הקובץ הזה
//     (מ-"annotation" ואילך) מתחתן.
//  3. חזרו ל-Part Studio, לחצו על "Custom feature" -> "Feature Phone
//     Motherboard" -> OK. הלוח המלא ייבנה בפעולה אחת.
// =====================================================================

annotation { "Feature Type Name" : "Feature Phone Motherboard" }
export const featurePhoneMotherboard = defineFeature(function(context is Context, id is Id, definition is map)
    precondition
    {
        annotation { "Name" : "Build board" }
        definition.build is boolean;
    }
    {
        const topPlane = qCreatedBy(makeId("Top"), EntityType.FACE);

        // --- קבועי גיאומטריה (מ"מ) ---
        const boardLength = 110;
        const boardWidth = 50;
        const boardThickness = 1.6 * millimeter;

        const boardId = id + "board";

        // =================================================================
        // 1. גוף הלוח הבסיסי
        // =================================================================
        var sketchBoard = newSketch(context, id + "sketchBoard", { "sketchPlane" : topPlane });
        skRectangle(sketchBoard, "boardRect", {
                "firstCorner" : vector(0, 0) * millimeter,
                "secondCorner" : vector(boardLength, boardWidth) * millimeter
        });
        skSolve(sketchBoard);

        opExtrude(context, boardId, {
                "entities" : qSketchRegion(id + "sketchBoard"),
                "direction" : vector(0, 0, 1),
                "endBound" : BoundingType.BLIND,
                "depth" : boardThickness
        });

        // =================================================================
        // 2. חורי הרכבה (4x, קוטר 3.2 מ"מ, 5 מ"מ מהקצוות)
        // =================================================================
        const mountHoleCenters = [
            vector(5, 5), vector(105, 5), vector(5, 45), vector(105, 45)
        ];
        const mountHoleIds = ["mountHoleA", "mountHoleB", "mountHoleC", "mountHoleD"];

        for (var i = 0; i < size(mountHoleCenters); i += 1)
        {
            var holeId = id + mountHoleIds[i];
            var skHole = newSketch(context, holeId + "sk", { "sketchPlane" : topPlane });
            skCircle(skHole, "c", {
                    "center" : mountHoleCenters[i] * millimeter,
                    "radius" : 1.6 * millimeter
            });
            skSolve(skHole);
            opExtrude(context, holeId + "ext", {
                    "entities" : qSketchRegion(holeId + "sk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : 5 * millimeter
            });
            opBoolean(context, holeId + "cut", {
                    "tools" : qCreatedBy(holeId + "ext", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.SUBTRACTION
            });
        }

        // =================================================================
        // 3. מחבר מסך FPC (26x3 מ"מ, גובה 2 מ"מ) - קרוב לקצה העליון,
        //    מיושר עם מסך 640x960 שמתחבר דרך פס גמיש (flex)
        // =================================================================
        {
            var sk = newSketch(context, id + "fpcSk", { "sketchPlane" : topPlane });
            skRectangle(sk, "r", {
                    "firstCorner" : vector(42, 45.5) * millimeter,
                    "secondCorner" : vector(68, 48.5) * millimeter
            });
            skSolve(sk);
            opExtrude(context, id + "fpcExt", {
                    "entities" : qSketchRegion(id + "fpcSk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : boardThickness + 2 * millimeter
            });
            opBoolean(context, id + "fpcUnion", {
                    "tools" : qCreatedBy(id + "fpcExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.UNION
            });
        }

        // =================================================================
        // 4. רכיב SoC (מעבד/מודול ראשי) - 14x14 מ"מ, גובה 1.3 מ"מ
        // =================================================================
        {
            var sk = newSketch(context, id + "socSk", { "sketchPlane" : topPlane });
            skRectangle(sk, "r", {
                    "firstCorner" : vector(25, 18) * millimeter,
                    "secondCorner" : vector(39, 32) * millimeter
            });
            skSolve(sk);
            opExtrude(context, id + "socExt", {
                    "entities" : qSketchRegion(id + "socSk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : boardThickness + 1.3 * millimeter
            });
            opBoolean(context, id + "socUnion", {
                    "tools" : qCreatedBy(id + "socExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.UNION
            });
        }

        // =================================================================
        // 5. רכיב PMIC/זיכרון - 9x9 מ"מ, גובה 1.1 מ"מ
        // =================================================================
        {
            var sk = newSketch(context, id + "pmicSk", { "sketchPlane" : topPlane });
            skRectangle(sk, "r", {
                    "firstCorner" : vector(50.5, 20.5) * millimeter,
                    "secondCorner" : vector(59.5, 29.5) * millimeter
            });
            skSolve(sk);
            opExtrude(context, id + "pmicExt", {
                    "entities" : qSketchRegion(id + "pmicSk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : boardThickness + 1.1 * millimeter
            });
            opBoolean(context, id + "pmicUnion", {
                    "tools" : qCreatedBy(id + "pmicExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.UNION
            });
        }

        // =================================================================
        // 6. קריסטל אוסילטור - 3.2x2.5 מ"מ, גובה 1 מ"מ
        // =================================================================
        {
            var sk = newSketch(context, id + "xtalSk", { "sketchPlane" : topPlane });
            skRectangle(sk, "r", {
                    "firstCorner" : vector(30.4, 33.75) * millimeter,
                    "secondCorner" : vector(33.6, 36.25) * millimeter
            });
            skSolve(sk);
            opExtrude(context, id + "xtalExt", {
                    "entities" : qSketchRegion(id + "xtalSk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : boardThickness + 1 * millimeter
            });
            opBoolean(context, id + "xtalUnion", {
                    "tools" : qCreatedBy(id + "xtalExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.UNION
            });
        }

        // =================================================================
        // 7. כותרת מקלדת מטריצה (12 פינים, פיץ' 2.54 מ"מ, קוטר חור 1.0 מ"מ)
        //    ממורכזת לאורך ציר X, סמוך לקצה התחתון - לחיבור מטריצת המקשים
        //    הפיזית של הטלפון (ראו SharedKeypadNav / Keyboard בפרויקט)
        // =================================================================
        const keypadPinCount = 12;
        const keypadPitch = 2.54;
        const keypadCenterX = 55;
        const keypadY = 4;
        const keypadStartX = keypadCenterX - (keypadPinCount - 1) * keypadPitch / 2;
        const keypadPinIds = [
            "keypadPin0", "keypadPin1", "keypadPin2", "keypadPin3",
            "keypadPin4", "keypadPin5", "keypadPin6", "keypadPin7",
            "keypadPin8", "keypadPin9", "keypadPin10", "keypadPin11"
        ];

        for (var i = 0; i < keypadPinCount; i += 1)
        {
            var pinId = id + keypadPinIds[i];
            var skPin = newSketch(context, pinId + "sk", { "sketchPlane" : topPlane });
            skCircle(skPin, "c", {
                    "center" : vector(keypadStartX + i * keypadPitch, keypadY) * millimeter,
                    "radius" : 0.5 * millimeter
            });
            skSolve(skPin);
            opExtrude(context, pinId + "ext", {
                    "entities" : qSketchRegion(pinId + "sk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : 5 * millimeter
            });
            opBoolean(context, pinId + "cut", {
                    "tools" : qCreatedBy(pinId + "ext", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.SUBTRACTION
            });
        }

        // =================================================================
        // 8. מחבר סוללה JST-PH דו-פיני - 6x3 מ"מ, גובה 2.6 מ"מ
        // =================================================================
        {
            var sk = newSketch(context, id + "battSk", { "sketchPlane" : topPlane });
            skRectangle(sk, "r", {
                    "firstCorner" : vector(7, 42.5) * millimeter,
                    "secondCorner" : vector(13, 45.5) * millimeter
            });
            skSolve(sk);
            opExtrude(context, id + "battExt", {
                    "entities" : qSketchRegion(id + "battSk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : boardThickness + 2.6 * millimeter
            });
            opBoolean(context, id + "battUnion", {
                    "tools" : qCreatedBy(id + "battExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.UNION
            });
        }

        // =================================================================
        // 9. מחבר USB-C - חריץ פתוח בקצה הימני (9x3.2 מ"מ) + נפח המחבר
        // =================================================================
        {
            // חריץ שחופף 6 מ"מ מעבר לקצה הלוח כדי לפתוח את השפה בפועל
            var skNotch = newSketch(context, id + "usbNotchSk", { "sketchPlane" : topPlane });
            skRectangle(skNotch, "r", {
                    "firstCorner" : vector(104, 20.5) * millimeter,
                    "secondCorner" : vector(116, 29.5) * millimeter
            });
            skSolve(skNotch);
            opExtrude(context, id + "usbNotchExt", {
                    "entities" : qSketchRegion(id + "usbNotchSk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : 5 * millimeter
            });
            opBoolean(context, id + "usbNotchCut", {
                    "tools" : qCreatedBy(id + "usbNotchExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.SUBTRACTION
            });

            // נפח גוף המחבר, בתוך הלוח וסמוך לחריץ
            var skBody = newSketch(context, id + "usbBodySk", { "sketchPlane" : topPlane });
            skRectangle(skBody, "r", {
                    "firstCorner" : vector(99.5, 21.5) * millimeter,
                    "secondCorner" : vector(106.5, 28.5) * millimeter
            });
            skSolve(skBody);
            opExtrude(context, id + "usbBodyExt", {
                    "entities" : qSketchRegion(id + "usbBodySk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : boardThickness + 3.2 * millimeter
            });
            opBoolean(context, id + "usbBodyUnion", {
                    "tools" : qCreatedBy(id + "usbBodyExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.UNION
            });
        }

        // =================================================================
        // 10. חריץ מגש SIM/microSD - קצה שמאלי (12x2 מ"מ)
        // =================================================================
        {
            var sk = newSketch(context, id + "simSk", { "sketchPlane" : topPlane });
            skRectangle(sk, "r", {
                    "firstCorner" : vector(-6, 9) * millimeter,
                    "secondCorner" : vector(6, 21) * millimeter
            });
            skSolve(sk);
            opExtrude(context, id + "simExt", {
                    "entities" : qSketchRegion(id + "simSk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : 5 * millimeter
            });
            opBoolean(context, id + "simCut", {
                    "tools" : qCreatedBy(id + "simExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.SUBTRACTION
            });
        }

        // =================================================================
        // 11. ג'ק אוזניות 3.5 מ"מ - חריץ בפינה ימנית-עליונה + נפח גלילי
        //     (הגליל מיוצג ניצב לפני הלוח כ"קיפוינג" בלבד, לא כברז אוזניות
        //     אמיתי שיוצא בצד - לצורך בדיקת פינוי מהיר בשאסי)
        // =================================================================
        {
            var skNotch = newSketch(context, id + "audioNotchSk", { "sketchPlane" : topPlane });
            skRectangle(skNotch, "r", {
                    "firstCorner" : vector(104, 41) * millimeter,
                    "secondCorner" : vector(116, 47) * millimeter
            });
            skSolve(skNotch);
            opExtrude(context, id + "audioNotchExt", {
                    "entities" : qSketchRegion(id + "audioNotchSk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : 5 * millimeter
            });
            opBoolean(context, id + "audioNotchCut", {
                    "tools" : qCreatedBy(id + "audioNotchExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.SUBTRACTION
            });

            var skJack = newSketch(context, id + "audioJackSk", { "sketchPlane" : topPlane });
            skCircle(skJack, "c", {
                    "center" : vector(104, 44) * millimeter,
                    "radius" : 3 * millimeter
            });
            skSolve(skJack);
            opExtrude(context, id + "audioJackExt", {
                    "entities" : qSketchRegion(id + "audioJackSk"),
                    "direction" : vector(0, 0, 1),
                    "endBound" : BoundingType.BLIND,
                    "depth" : boardThickness + 5 * millimeter
            });
            opBoolean(context, id + "audioJackUnion", {
                    "tools" : qCreatedBy(id + "audioJackExt", EntityType.BODY),
                    "targets" : qCreatedBy(boardId, EntityType.BODY),
                    "operationType" : BooleanOperationType.UNION
            });
        }
    });
