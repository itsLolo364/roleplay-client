// Minecraft 3D avatar renderer (CSS). Renders both skin layers:
// layer 1 (base) and layer 2 (hat/jacket/sleeves/pants overlay).
window.Avatar = (function () {
    const FACE_CFG = {
        head: { front: [8, 8], back: [24, 8], right: [0, 8], left: [16, 8], top: [8, 0], bottom: [16, 0] },
        body: { front: [20, 20], back: [32, 20], right: [16, 20], left: [28, 20], top: [20, 16], bottom: [28, 16] },
        arm:  { front: [44, 20], back: [52, 20], right: [40, 20], left: [48, 20], top: [44, 16], bottom: [52, 16] },
        leg:  { front: [4, 20], back: [12, 20], right: [0, 20], left: [8, 20], top: [4, 16], bottom: [12, 16] },
        lArm: { front: [36, 32], back: [44, 32], right: [32, 32], left: [40, 32], top: [16, 44], bottom: [20, 44] },
        lLeg: { front: [4, 32], back: [12, 32], right: [0, 32], left: [8, 32], top: [0, 44], bottom: [4, 44] }
    };

    // Layer 2 (overlay): hat + jacket/sleeves/pants. The left-limb overlays live in
    // the scratch area (rows 32-47) built by normalizeSkin.
    const OVERLAY_CFG = {
        hat:    { front: [40, 8], back: [56, 8], right: [32, 8], left: [48, 8], top: [40, 0], bottom: [48, 0] },
        jacket: { front: [20, 52], back: [32, 52], right: [16, 52], left: [28, 52], top: [20, 48], bottom: [28, 48] },
        sleeve: { front: [44, 52], back: [52, 52], right: [40, 52], left: [48, 52], top: [44, 48], bottom: [52, 48] },
        pants:  { front: [4, 52], back: [12, 52], right: [0, 52], left: [8, 52], top: [4, 48], bottom: [12, 48] },
        sleeveL:{ front: [52, 32], back: [60, 32], right: [48, 32], left: [56, 32], top: [24, 44], bottom: [28, 44] },
        pantsL: { front: [20, 32], back: [28, 32], right: [16, 32], left: [24, 32], top: [8, 44], bottom: [12, 44] }
    };

    function normalizeSkin(img) {
        const c = document.createElement('canvas');
        c.width = 64; c.height = 64;
        const g = c.getContext('2d');
        g.imageSmoothingEnabled = false;
        g.drawImage(img, 0, 0, 64, 32, 0, 0, 64, 32);
        const tall = img.height > 32;
        if (tall) {
            g.drawImage(img, 0, 32, 64, 32, 0, 32, 64, 32);
        }
        const flip = (sx, sy, w, h, dx, dy, vertical) => {
            g.save();
            if (vertical) { g.translate(dx, dy + h); g.scale(1, -1); }
            else { g.translate(dx + w, dy); g.scale(-1, 1); }
            g.drawImage(img, sx, sy, w, h, 0, 0, w, h);
            g.restore();
        };
        // Left limbs = mirror of the right limbs, drawn into scratch area rows 32-47.
        const V = true;
        flip(44, 20, 4, 12, 36, 32);     flip(52, 20, 4, 12, 44, 32);      // lArm front/back
        flip(48, 20, 4, 12, 32, 32, V);  flip(40, 20, 4, 12, 40, 32, V);   // lArm right/left
        flip(44, 16, 4, 4, 16, 44);      flip(52, 16, 4, 4, 20, 44);       // lArm top/bottom
        flip(4, 20, 4, 12, 4, 32);       flip(12, 20, 4, 12, 12, 32);      // lLeg front/back
        flip(8, 20, 4, 12, 0, 32, V);    flip(0, 20, 4, 12, 8, 32, V);     // lLeg right/left
        flip(4, 16, 4, 4, 0, 44);        flip(12, 16, 4, 4, 4, 44);        // lLeg top/bottom
        if (tall) {
            flip(44, 52, 4, 12, 52, 32); flip(52, 52, 4, 12, 60, 32);      // sleeveL front/back
            flip(48, 52, 4, 12, 48, 32, V); flip(40, 52, 4, 12, 56, 32, V);// sleeveL right/left
            flip(44, 48, 4, 4, 24, 44);  flip(52, 48, 4, 4, 28, 44);       // sleeveL top/bottom
            flip(4, 52, 4, 12, 20, 32);  flip(12, 52, 4, 12, 28, 32);      // pantsL front/back
            flip(8, 52, 4, 12, 16, 32, V); flip(0, 52, 4, 12, 24, 32, V);  // pantsL right/left
            flip(4, 48, 4, 4, 8, 44);    flip(12, 48, 4, 4, 12, 44);       // pantsL top/bottom
        }
        return { tex: c.toDataURL(), tall };
    }

    function makeBox(tex, s, bw, bh, bd, tw, th, td, cfg) {
        const box = document.createElement('div');
        box.className = 'mc-part';
        const faces = [
            ['front', tw, th, bw, bh, 'Y', 0],
            ['back', tw, th, bw, bh, 'Y', 180],
            ['right', td, th, bd, bh, 'Y', 90],
            ['left', td, th, bd, bh, 'Y', -90],
            ['top', tw, td, bw, bd, 'X', 90],
            ['bottom', tw, td, bw, bd, 'X', -90]
        ];
        for (const [name, tpw, tph, w, h, axis, rot] of faces) {
            const [tx, ty] = cfg[name];
            const face = document.createElement('div');
            face.className = 'mc-face';
            face.style.width = (w * s) + 'px';
            face.style.height = (h * s) + 'px';
            face.style.left = (-w * s / 2) + 'px';
            face.style.top = (-h * s / 2) + 'px';
            face.style.backgroundImage = `url(${tex})`;
            const sx = w / tpw, sy = h / tph;
            face.style.backgroundSize = `${64 * s * sx}px ${64 * s * sy}px`;
            face.style.backgroundPosition = `${-tx * s * sx}px ${-ty * s * sy}px`;
            const rotT = axis === 'X' ? `rotateX(${rot}deg)` : `rotateY(${rot}deg)`;
            face.style.transform = `${rotT} translateZ(${bd * s / 2}px)`;
            box.appendChild(face);
        }
        return box;
    }

    function makeHeadCube(tex, face) {
        const scene = document.createElement('div');
        scene.className = 'mc-scene';
        scene.style.width = Math.round(face * 1.7) + 'px';
        scene.style.height = Math.round(face * 1.7) + 'px';
        const cube = document.createElement('div');
        cube.className = 'mc-cube';
        cube.style.transform = 'rotateX(-14deg) rotateY(45deg)';
        cube.appendChild(makeBox(tex, face / 8, 8, 8, 8, 8, 8, 8, FACE_CFG.head));
        cube.appendChild(makeBox(tex, face / 8, 9, 9, 9, 8, 8, 8, OVERLAY_CFG.hat));
        scene.appendChild(cube);
        return scene;
    }

    function makeLimb(tex, u, x, y, cfg, cls, tall, overlayCfg) {
        const pivot = document.createElement('div');
        pivot.className = 'mc-pivot ' + cls;
        pivot.style.left = (x * u) + 'px';
        pivot.style.top = (y * u) + 'px';
        const box = makeBox(tex, u, 4, 12, 4, 4, 12, 4, cfg);
        box.style.left = '0px';
        box.style.top = (6 * u) + 'px';
        pivot.appendChild(box);
        if (tall && overlayCfg) {
            const ov = makeBox(tex, u, 4.5, 12, 4.5, 4, 12, 4, overlayCfg);
            ov.style.left = '0px';
            ov.style.top = (6 * u) + 'px';
            pivot.appendChild(ov);
        }
        return pivot;
    }

    function makeFigure(tex, u, tall) {
        const wrap = document.createElement('div');
        wrap.className = 'mc-rig-wrap';
        const rig = document.createElement('div');
        rig.className = 'mc-rig';
        rig.style.width = (16 * u) + 'px';
        rig.style.height = (32 * u) + 'px';
        const anim = document.createElement('div');
        anim.className = 'mc-anim';
        anim.style.width = '100%';
        anim.style.height = '100%';

        const head = makeBox(tex, u, 8, 8, 8, 8, 8, 8, FACE_CFG.head);
        head.style.left = '0px'; head.style.top = (4 * u) + 'px';
        anim.appendChild(head);

        const hat = makeBox(tex, u, 9, 9, 9, 8, 8, 8, OVERLAY_CFG.hat);
        hat.style.left = '0px'; hat.style.top = (4 * u) + 'px';
        anim.appendChild(hat);

        const body = makeBox(tex, u, 8, 12, 4, 8, 12, 4, FACE_CFG.body);
        body.style.left = '0px'; body.style.top = (14 * u) + 'px';
        anim.appendChild(body);

        if (tall) {
            const jacket = makeBox(tex, u, 8.5, 12, 4.5, 8, 12, 4, OVERLAY_CFG.jacket);
            jacket.style.left = '0px'; jacket.style.top = (14 * u) + 'px';
            anim.appendChild(jacket);
        }

        anim.appendChild(makeLimb(tex, u, -6, 8, FACE_CFG.lArm, 'mc-swing-a', tall, OVERLAY_CFG.sleeveL));
        anim.appendChild(makeLimb(tex, u, 6, 8, FACE_CFG.arm, 'mc-swing-b', tall, OVERLAY_CFG.sleeve));
        anim.appendChild(makeLimb(tex, u, -2, 20, FACE_CFG.lLeg, 'mc-swing-b', tall, OVERLAY_CFG.pantsL));
        anim.appendChild(makeLimb(tex, u, 2, 20, FACE_CFG.leg, 'mc-swing-a', tall, OVERLAY_CFG.pants));

        rig.appendChild(anim);
        wrap.appendChild(rig);

        const shadow = document.createElement('div');
        shadow.className = 'mc-shadow';
        shadow.style.width = (15 * u) + 'px';
        wrap.appendChild(shadow);

        return wrap;
    }

    return { normalizeSkin, makeBox, makeHeadCube, makeLimb, makeFigure };
})();
