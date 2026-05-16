/**
 * SABI Reef Watermark Generator  v4
 * ─────────────────────────────────────────────────────────────────────────────
 * Changes vs v3:
 *  - FIXED reef profile: removed erroneous L{VIEW_LEFT},0 in path construction
 *    that was creating a rectangular spike from bottom to top.
 *  - Fish now swim on curved S-arc paths via animateMotion (cubic bezier).
 *  - preserveAspectRatio="none": SVG always spans full viewport height.
 *    y=0 → browser top, y=900 → browser bottom — no gap at base regardless
 *    of viewport height. Slight non-uniform y-scaling is imperceptible for
 *    a decorative watermark.
 *  - Speed variation and flip logic preserved from v3.
 *
 * SVG coordinate system:
 *   Reef right edge: x = 120 (W constant, unchanged)
 *   SVG viewport:    x ∈ [-80 … 120]  (200 units wide = 200px div)
 *   Reef thin top:   x ≈ 10, so corals can reach x ≈ -60 without clipping
 *   Fish turnaround: x = -145 (path endpoint, inside left viewport edge)
 */
(function () {
    'use strict';

    var container = document.getElementById('reefWatermark');
    if (!container) return;

    var params     = window.SABI_REEF_PARAMS || {};
    var aquariumId = params.aquariumId || 42;
    var fishCount  = params.fishCount  || 0;

    // ── Seeded PRNG: Mulberry32 ────────────────────────────────────────────
    function mulberry32(seed) {
        return function () {
            seed  = (seed + 0x6D2B79F5) | 0;
            var t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
            t     = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
        };
    }
    function fnv1a(s) {
        var h = 0x811c9dc5;
        for (var i = 0; i < s.length; i++) {
            h ^= s.charCodeAt(i);
            h  = Math.imul(h, 0x01000193) >>> 0;
        }
        return h;
    }

    var rng = mulberry32(fnv1a(String(aquariumId) + 'sabi-reef-2026'));

    // ── Viewport & coordinate constants ───────────────────────────────────
    var W          = 120;   // reef right edge (unchanged)
    var H          = 900;
    var VIEW_LEFT  = -80;   // extended water area to the left (+80px ≈ 2cm)
    var SVG_W      = W - VIEW_LEFT;   // = 200

    // ── Counts ─────────────────────────────────────────────────────────────
    var coralCount   = Math.max(2, Math.min(7, 2 + Math.floor(rng() * 3 + rng() * 2)));
    var fishSilCount = Math.min(7, Math.max(0, Math.ceil(fishCount / 2)));

    // ── Reef profile ───────────────────────────────────────────────────────
    function profileX(t) { return 10 + 82 * (0.04 * t + 0.96 * t * t * t); }

    function displace(pts, amp) {
        var out = [pts[0]];
        for (var i = 0; i < pts.length - 1; i++) {
            var a = pts[i], b = pts[i + 1];
            var my = (a[0] + b[0]) * 0.5;
            var mx = (a[1] + b[1]) * 0.5 + (rng() - 0.5) * amp;
            mx = Math.max(7, Math.min(W - 4, mx));
            out.push([my, mx]);
            out.push(b);
        }
        return out;
    }

    var n = 14, pts = [];
    for (var i = 0; i <= n; i++) { var t = i / n; pts.push([t * H, profileX(t)]); }
    pts = displace(pts, 22);
    pts = displace(pts, 11);
    pts = displace(pts, 5);

    function reefX(y) {
        for (var i = 0; i < pts.length - 1; i++) {
            if (pts[i][0] <= y && pts[i + 1][0] >= y) {
                var f = (y - pts[i][0]) / (pts[i + 1][0] - pts[i][0]);
                return pts[i][1] + (pts[i + 1][1] - pts[i][1]) * f;
            }
        }
        return pts[pts.length - 1][1];
    }

    // ── Layer 1: Reef silhouette ───────────────────────────────────────────
    // Path: right edge top → right edge bottom → extended left edge bottom
    // → profile curve from bottom to top → Z closes back to right edge top.
    // NOTE: do NOT add L{VIEW_LEFT},0 here — that would create a full rectangle
    //       frame and produce a vertical spike on the left side.
    var rev = pts.slice().reverse();
    var d   = 'M' + W + ',0 L' + W + ',' + H + ' L' + VIEW_LEFT + ',' + H;
    for (var k = 0; k < rev.length; k++) {
        d += ' L' + rev[k][1].toFixed(1) + ',' + rev[k][0].toFixed(1);
    }
    d += ' Z';

    // ── Layer 2: Coral branches (unchanged from v2) ────────────────────────
    function branchLines(x0, y0, angle, len, depth, sw, out) {
        if (depth <= 0 || len < 2.5) return;
        var x1 = x0 + Math.cos(angle) * len;
        var y1 = y0 + Math.sin(angle) * len;
        out.push(
            '<line x1="' + x0.toFixed(1) + '" y1="' + y0.toFixed(1) +
            '" x2="' + x1.toFixed(1) + '" y2="' + y1.toFixed(1) +
            '" stroke-width="' + sw.toFixed(1) + '" stroke-linecap="round"/>'
        );
        var spread = 0.30 + rng() * 0.50;
        var lenF   = 0.50 + rng() * 0.24;
        branchLines(x1, y1, angle - spread,        len * lenF,          depth - 1, sw * 0.68, out);
        branchLines(x1, y1, angle + spread * 0.85, len * (lenF - 0.05), depth - 1, sw * 0.68, out);
    }

    var CORAL_COLORS = ['#0891b2', '#0ea5e9', '#38bdf8', '#0369a1'];
    var coralMarkup  = [];

    for (var c = 0; c < coralCount; c++) {
        var tPos  = 0.05 + ((c + rng() * 0.6) / coralCount) * 0.90;
        var baseY = tPos * H;
        var bx    = reefX(baseY);
        var angle = Math.PI + 0.20 + rng() * 0.55;   // 191°–223° = UP-LEFT in SVG
        var len   = 18 + rng() * 35;
        var depth = 2 + Math.floor(rng() * 3);
        var color = CORAL_COLORS[Math.floor(rng() * CORAL_COLORS.length)];
        var sw    = 1.2 + rng() * 1.1;
        var swDur = (4.5 + rng() * 6.0).toFixed(1);
        var swBeg = (-rng() * 9).toFixed(1) + 's';
        var smilSway =
            '<animateTransform attributeName="transform" type="rotate"' +
            ' values="-2;2;-2" keyTimes="0;0.5;1"' +
            ' calcMode="spline" keySplines="0.45 0 0.55 1;0.45 0 0.55 1"' +
            ' dur="' + swDur + 's" begin="' + swBeg + '" repeatCount="indefinite"' +
            ' additive="sum"/>';
        var lines = [];
        branchLines(0, 0, angle, len, depth, sw, lines);
        coralMarkup.push(
            '<g transform="translate(' + bx.toFixed(1) + ',' + baseY.toFixed(1) + ')"' +
            ' stroke="' + color + '" fill="none" opacity="0.65">' +
            lines.join('') + smilSway + '</g>'
        );
    }

    // ── Layer 3: Fish silhouettes with curved animateMotion + flip ────────
    //
    // Structure: 3 nested <g>:
    //   outer   – opacity only
    //   middle  – animateMotion along a cubic bezier path (gentle S-arc)
    //             path: M145,fy  C80,fy-bv  -80,fy+bv  -145,fy
    //                         C-80,fy+bv  80,fy-bv  145,fy
    //             rotate="0" keeps the fish horizontal (no tangent tilt)
    //   inner   – SMIL scale discrete (flip at midpoint: 1,1 → -1,1)
    //
    // Bow variance (bv): 8–12 px – very subtle vertical arc per fish.
    // Speed variation: per-fish random keySpline control points.
    //   keyPoints="0;0.5;1" maps path midpoint to time outRT.
    //
    var fishMarkup = [];
    var FISH_COLORS = [
        { body: '#0891b2', tail: '#0369a1' },
        { body: '#0ea5e9', tail: '#0891b2' },
        { body: '#38bdf8', tail: '#0369a1' }
    ];

    for (var f = 0; f < fishSilCount; f++) {
        var fy  = 80 + rng() * 760;
        var sc  = 0.85 + rng() * 0.70;
        var dur = (35 + rng() * 20).toFixed(1);   // 35–55s variety
        var beg = (-rng() * 55).toFixed(1) + 's'; // pre-offset so fish appear immediately

        // outR: fraction of cycle for outward (right→left) leg (asymmetric timing)
        var outR  = 0.35 + rng() * 0.15;
        var outRT = outR.toFixed(3);

        // Per-fish acceleration profile
        var ks1 = (0.15 + rng() * 0.25).toFixed(2);  // 0.15–0.40
        var ks2 = (0.60 + rng() * 0.25).toFixed(2);  // 0.60–0.85
        var splines = ks1 + ' 0 ' + ks2 + ' 1; ' + ks1 + ' 0 ' + ks2 + ' 1';

        // Subtle vertical bow: ±8–12 px — creates life-like S-arc while swimming
        // Keep bv as a Number (do NOT call .toFixed here) — fy+bv must be numeric addition,
        // not string concatenation (fy+"9.8" would give "345.69.8" and break toFixed).
        var bv  = 8 + rng() * 4;
        var fyU = (fy - bv).toFixed(1);   // slightly up   (SVG y↓)
        var fyD = (fy + bv).toFixed(1);   // slightly down

        // Cubic bezier motion path: right → (S-arc) → left → (S-arc) → right
        // Outward leg:  C80,fy-bv  -80,fy+bv  -145,fy  (dip up then down = S)
        // Return leg:   C-80,fy+bv  80,fy-bv  145,fy   (mirror S)
        var motionPath =
            'M145,' + fy.toFixed(1) +
            ' C80,' + fyU + ' -80,' + fyD + ' -145,' + fy.toFixed(1) +
            ' C-80,' + fyU + ' 80,' + fyD + ' 145,' + fy.toFixed(1);

        var op  = (0.50 + rng() * 0.25).toFixed(2);
        var fc  = FISH_COLORS[f % FISH_COLORS.length];

        var rx  = (8   * sc).toFixed(1);
        var ry  = (3.5 * sc).toFixed(1);
        var tx  = (9   * sc).toFixed(1);
        var tfx = (16  * sc).toFixed(1);
        var tfy = (5.5 * sc).toFixed(1);
        var ex  = (-5  * sc).toFixed(1);
        var er  = (1.3 * sc).toFixed(1);

        // animateMotion along the bezier arc — rotate="0" keeps fish horizontal
        var smilMotion =
            '<animateMotion' +
            ' path="' + motionPath + '"' +
            ' rotate="0"' +
            ' keyPoints="0;0.5;1"' +
            ' keyTimes="0;' + outRT + ';1"' +
            ' calcMode="spline" keySplines="' + splines + '"' +
            ' dur="' + dur + 's" begin="' + beg + '" repeatCount="indefinite"/>';

        // Flip: faces left (scale 1,1) on outward leg; faces right (scale -1,1) on return
        var smilFlip =
            '<animateTransform attributeName="transform" type="scale"' +
            ' values="1,1; -1,1"' +
            ' keyTimes="0;' + outRT + '"' +
            ' calcMode="discrete"' +
            ' dur="' + dur + 's" begin="' + beg + '" repeatCount="indefinite"/>';

        fishMarkup.push(
            '<g opacity="' + op + '">' +         // opacity wrapper
            '<g>' + smilMotion +                 // curved motion path
              '<g>' + smilFlip +                 // instant flip at turnaround
                '<ellipse cx="0" cy="0" rx="' + rx + '" ry="' + ry + '" fill="' + fc.body + '"/>' +
                '<polygon points="' + tx + ',0 ' + tfx + ',-' + tfy + ' ' + tfx + ',' + tfy + '"' +
                        ' fill="' + fc.tail + '"/>' +
                '<circle cx="' + ex + '" cy="-1" r="' + er + '" fill="#e0f2fe" opacity="0.85"/>' +
              '</g>' +
            '</g>' +
            '</g>'
        );
    }

    // ── Assemble SVG ───────────────────────────────────────────────────────
    container.innerHTML =
        '<svg xmlns="http://www.w3.org/2000/svg"' +
        ' viewBox="' + VIEW_LEFT + ' 0 ' + SVG_W + ' ' + H + '"' +
        ' preserveAspectRatio="none"' +
        ' role="presentation" aria-hidden="true"' +
        ' style="position:absolute;top:0;right:0;height:100%;width:100%;">' +

        // Layer 1: reef silhouette (opacity 0.20)
        '<g opacity="0.20">' +
        '<path fill="#075985" d="' + d + '"/>' +
        '<ellipse fill="#0369a1" cx="6"  cy="898" rx="17" ry="10"/>' +
        '<ellipse fill="#0891b2" cx="29" cy="897" rx="20" ry="12"/>' +
        '<ellipse fill="#075985" cx="55" cy="899" rx="15" ry="9"/>' +
        '<ellipse fill="#0891b2" opacity="0.80" cx="19" cy="894" rx="8"  ry="5"/>' +
        '<ellipse fill="#0369a1" opacity="0.80" cx="42" cy="895" rx="10" ry="6"/>' +
        '</g>' +

        // Layer 2: corals (opacity 0.65 per coral)
        '<g>' + coralMarkup.join('') + '</g>' +

        // Layer 3: fish (opacity 0.50–0.75 per fish, bounce + flip)
        '<g>' + fishMarkup.join('') + '</g>' +

        '</svg>';

}());

