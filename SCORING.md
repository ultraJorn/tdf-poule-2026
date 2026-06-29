# How scoring works

This documents the actual rules implemented in
`backend/src/main/java/com/tdfpoule/service/ScoringService.java` — it's the
single source of truth the app computes from; this file just explains it in
plain language with worked examples. If the two ever disagree, the code wins.

## 1. Finish-order points (per stage)

Whoever the organizer enters as finishing 1st–15th in a stage earns points
from this table. 16th place and below earns 0.

| Place | 1  | 2  | 3  | 4  | 5  | 6  | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 |
|-------|----|----|----|----|----|----|---|---|---|----|----|----|----|----|----|
| Points| 25 | 20 | 16 | 14 | 12 | 10 | 8 | 7 | 6 | 5  | 4  | 3  | 2  | 1  | 1  |

(`FINISH_POINTS = {25, 20, 16, 14, 12, 10, 8, 7, 6, 5, 4, 3, 2, 1, 1}` in code.)

## 2. Jersey bonuses (per stage)

Whoever holds each jersey *after* that stage gets a flat bonus, on top of any
finish-order points they also earned that same stage:

| Jersey            | Meaning                  | Bonus |
|-------------------|---------------------------|-------|
| Yellow            | GC leader                 | +8    |
| Green             | Points classification     | +5    |
| Polka dot         | King of the Mountains     | +5    |
| White             | Best young rider          | +3    |

A rider doesn't need to be in the top 15 to earn a jersey bonus (e.g. the GC
leader who didn't win the stage still gets the yellow jersey's +8). A rider
*can* hold more than one jersey at once and stacks both bonuses — the game
doesn't try to enforce real-world jersey-combination rules (e.g. "the leader
can't also win the young rider jersey unless..."); whatever the organizer
enters is what scores.

## 3. A rider's points for one stage

```
rider_stage_points = finish_points (0 if outside top 15)
                    + yellow_bonus (if they hold it)
                    + green_bonus  (if they hold it)
                    + polka_bonus  (if they hold it)
                    + white_bonus  (if they hold it)
```

On the **final stage only** (stage 21), add the final GC bonus below on top
of this.

## 3b. Final GC bonus (stage 21 only, one time)

Every stage has a "General classification after this stage (top 10)" field
the organizer can fill in. For stages 1–20 this is **purely informational**
— it shows on the Results tab so players can follow how the GC is shaping
up, but it never scores any points. Only the GC entered for the **last
stage** pays out, as a one-time bonus worth **10x** an equivalent stage
finish:

| GC rank | 1   | 2   | 3   | 4   | 5   | 6   | 7  | 8  | 9  | 10 |
|---------|-----|-----|-----|-----|-----|-----|----|----|----|----| 
| Bonus   | 250 | 200 | 160 | 140 | 120 | 100 | 80 | 70 | 60 | 50 |

(`GC_FINAL_BONUS = {250, 200, 160, 140, 120, 100, 80, 70, 60, 50}` in code —
literally `FINISH_POINTS[0..9]` x 10.) This is added on top of whatever that
same final stage's own finish-order points and jersey bonuses already are —
a rider who wins stage 21 outright *and* finishes top of the final GC earns
both. The yellow jersey's stage-by-stage +8 bonus (section 2) still applies
too, so winning the Tour outright is worth roughly the GC bonus (250) plus
21 days of +8 yellow-jersey bonuses (~168) plus whatever stage placings that
rider picked up along the way — the final GC is the single biggest point
swing in the game, by design.

## 4. A team's points for one stage

A player's score for a given stage is the **sum of every rider currently on
their roster** for that stage — not just the riders who scored. Benched
riders (on the roster but outside the top 15 with no jersey) simply
contribute 0 for that stage; they don't subtract anything.

Only stages the organizer has entered a result for and locked count at all —
an unscored stage contributes 0 to everyone, it isn't skipped or guessed at.

## 5. Swaps: which roster counts for which stage

Each team starts with the N riders (`teamSize`, set by the organizer) drafted
at sign-up. A swap (rider out, rider in) **takes effect from the next stage
onward**, never retroactively:

- If a swap is made after stage 8 has been scored, the outgoing rider is
  still credited with all the points they earned in stages 1–8.
- The incoming rider only starts earning points for the player from stage 9
  onward (`effectiveFromStage = currentStage + 1` at the moment the swap is
  made).
- This is recalculated per stage from the full history of swaps, not just
  "whoever is on the roster right now" — so re-checking an old stage's score
  always reflects who was actually drafted for that stage at the time.

Swaps are also bounded by the organizer's settings: a fixed `budgetCap` (the
incoming rider's price can't push the roster's total cost over it) and a
fixed `totalSwaps` for the whole race (not per stage).

**Exception — the pre-race period.** Real-world startlists can still change
right up to the start of stage 1 (a rider gets added, or ends up not
actually starting), so swaps made while `poule.currentStage == 0` are free
and uncapped: they don't decrement `totalSwaps` at all, no matter how many
you make. The moment the organizer locks in stage 1's result, `currentStage`
becomes 1 and the normal limited `totalSwaps` budget starts counting from
then on — this is a state check (`TeamService.swap()`), not a clock-based
window, so it doesn't matter exactly when during the pre-race period a swap
happens.

The app separately shows a live countdown to stage 1's real confirmed start
(2026-07-04, 17:05 CEST, from `schedule/tour_de_france_2026_schedule.json`
via `ScheduleService`, computed in the real **Europe/Paris** timezone) on
the "My team" tab — that's purely informational context for players, not
what actually gates free swaps.

## 6. Season total

A player's overall score is just the sum of their per-stage team points,
added up across every stage the organizer has locked so far. That's exactly
what "My team" and the Leaderboard tab display — there's no separate
multiplier, bonus round, or tiebreaker logic beyond this sum.

## Worked example

Organizer enters Stage 1: Pogačar wins (1st) and takes yellow; Vingegaard is
2nd; Philipsen takes green for a strong points finish without placing top 15.

```
Pogačar:    25 (1st) + 8 (yellow) = 33
Vingegaard: 20 (2nd)              = 20
Philipsen:  0 (outside top 15) + 5 (green) = 5
```

Player A drafted Pogačar + 8 others (none of whom scored) → 33 points for
stage 1.

After stage 5, Player A swaps Pogačar out for Vingegaard (1 of their 3
allowed swaps used). From stage 6 onward, Vingegaard's points count toward
Player A's total instead of Pogačar's — but stages 1–5, already scored with
Pogačar on the roster, are untouched. Player A's season total is simply:
`(stage 1 points) + (stage 2 points) + ... + (stage 21 points)`, where stages
1–5 used the Pogačar-inclusive roster and stages 6–21 use the
Vingegaard-inclusive one.

## Where this lives in the app

- **Entering results**: Admin tab → "Enter stage result" (dropdown form or
  CSV paste) — see `frontend/src/components/dashboard/AdminStageEntry.vue`.
- **Computing scores**: `ScoringService.computeStagePoints()` (per-stage
  finish/jersey points), `ScoringService.computeFinalGcBonus()` (the
  stage-21-only GC bonus, merged into that stage's points by
  `StageService.saveResult()`), and `ScoringService.teamTotal()` (season
  total + breakdown) — all server-side; the frontend never computes scores
  itself, only displays what the API returns.
- **Changing the rules**: if you want different point values or bonus
  amounts, edit the `FINISH_POINTS`/`GC_FINAL_BONUS` arrays and `JERSEY_BONUS`
  map at the top of `ScoringService.java` and redeploy the backend — there's
  no UI for changing the scoring formula itself (team size/budget/swap
  limits are configurable per poule in Admin → Settings, but point values
  are not). `StageService` decides which stage counts as "final" by taking
  the highest `stage_number` that exists for that poule, not a hardcoded 21
  — so this still works correctly if a poule's stage count ever differs.
