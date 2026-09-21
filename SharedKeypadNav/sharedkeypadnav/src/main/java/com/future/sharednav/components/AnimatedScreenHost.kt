package com.future.sharednav.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.future.sharednav.theme.FutureTransitions

/**
 * מחליף `when (route) { ... }` בניווט ידני (backStack/route) - עם מעבר
 * מונפש בכיוון הנכון. לפני זה, בכל האפליקציות עם ניווט ידני (Music,
 * Sfarim, Fitness, Tools, Clock, Frixa, Gallery, Files, Contact...) מסך
 * פשוט הוחלף במסך אחר באותו פריים, בלי שום סימן לאן הלכנו.
 *
 * הכיוון נגזר מ-[depthOf]: מסך עמוק יותר נכנס קדימה, רדוד יותר חוזר
 * אחורה, ואותו עומק (מעבר בין טאבים, או החלפת הראש של המחסנית) עושה fade.
 * [depthOf] חייב להיות פונקציה של המצב עצמו ולא של משתנה חיצוני - הוא
 * נקרא על שני המצבים ברגע חישוב המעבר, כשהמשתנה החיצוני כבר השתנה:
 * ```
 * AnimatedScreenHost(targetState = route, depthOf = { if (it == Route.Home) 0 else 1 }) { r ->
 *     when (r) { ... }
 * }
 * ```
 * למחסנית ידנית (`backStack`) ר' [AnimatedBackStackHost], שרושם את העומק
 * של כל מצב בעצמו.
 *
 * על פוקוס: בזמן המעבר שני המסכים מורכבים. המסך הנכנס מבקש פוקוס ב-
 * LaunchedEffect משלו כמו תמיד, וה-BackHandler שלו נרשם אחרי של המסך
 * היוצא ולכן גובר עליו - אין צורך לשנות שום מסך קיים.
 */
@Composable
fun <S> AnimatedScreenHost(
    targetState: S,
    depthOf: (S) -> Int,
    modifier: Modifier = Modifier,
    contentKey: (S) -> Any? = { it },
    content: @Composable (S) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        contentKey = contentKey,
        transitionSpec = {
            val from = depthOf(initialState)
            val to = depthOf(targetState)
            when {
                to > from -> FutureTransitions.forward()
                to < from -> FutureTransitions.backward()
                else -> FutureTransitions.fadeThrough()
            }
        },
        label = "futureScreen",
    ) { state ->
        Box(modifier = Modifier.fillMaxSize()) { content(state) }
    }
}

/**
 * גרסה למחסנית ידנית (`mutableStateListOf<Route>`), שבה המסכים עצמם לא
 * יודעים באיזה עומק הם. העומק של כל מצב נרשם ברגע שהוא הופך לראש
 * המחסנית, כך שההשוואה בין "המסך שיוצא" ל"מסך שנכנס" נכונה גם כשאותו
 * Route מופיע פעמיים בעומקים שונים.
 */
@Composable
fun <S> AnimatedBackStackHost(
    backStack: List<S>,
    modifier: Modifier = Modifier,
    contentKey: (S) -> Any? = { it },
    content: @Composable (S) -> Unit,
) {
    val entry = BackStackEntry(backStack.last(), backStack.size)
    AnimatedScreenHost(
        targetState = entry,
        depthOf = { it.depth },
        modifier = modifier,
        contentKey = { contentKey(it.route) },
    ) { content(it.route) }
}

private data class BackStackEntry<S>(val route: S, val depth: Int)
