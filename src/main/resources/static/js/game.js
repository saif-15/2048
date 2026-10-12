/*
 * 2048 game engine. Runs entirely in the browser; finished games are POSTed to
 * /api/scores (protected by Spring Security + CSRF). An in-progress game is kept
 * in localStorage so a page refresh does not lose it.
 */
(function () {
    'use strict';

    const SIZE = 4;
    const WIN_TILE = 2048;

    const root = document.getElementById('game');
    if (!root) return;

    const username = root.dataset.username;
    const submitUrl = root.dataset.submitUrl;
    const storageKey = '2048-state-' + username;
    const csrfToken = metaContent('_csrf');
    const csrfHeader = metaContent('_csrf_header');

    const el = {
        board: document.getElementById('board'),
        score: document.getElementById('score'),
        best: document.getElementById('best'),
        moves: document.getElementById('moves'),
        time: document.getElementById('time'),
        overlay: document.getElementById('overlay'),
        overlayTitle: document.getElementById('overlay-title'),
        overlayMessage: document.getElementById('overlay-message'),
        keepPlaying: document.getElementById('keep-playing'),
        saveScore: document.getElementById('save-score'),
        tryAgain: document.getElementById('try-again'),
        newGame: document.getElementById('new-game'),
        undo: document.getElementById('undo'),
        sideBest: document.getElementById('side-best'),
        sideRank: document.getElementById('side-rank'),
        sideGames: document.getElementById('side-games'),
    };

    let best = parseInt(root.dataset.best || '0', 10);
    let state = load() || freshState();
    let submitting = false;

    // ---- state ----------------------------------------------------------

    function freshState() {
        const s = {
            grid: new Array(SIZE * SIZE).fill(0),
            score: 0,
            moves: 0,
            elapsed: 0,
            won: false,
            keepPlaying: false,
            over: false,
            submitted: false,
            undo: null, // the position before the last move; cleared once used
        };
        addRandomTile(s);
        addRandomTile(s);
        return s;
    }

    function load() {
        try {
            const s = JSON.parse(localStorage.getItem(storageKey));
            if (s && Array.isArray(s.grid) && s.grid.length === SIZE * SIZE && !s.submitted) return s;
        } catch (e) { /* ignore corrupt state */ }
        return null;
    }

    function save() {
        try { localStorage.setItem(storageKey, JSON.stringify(state)); } catch (e) { /* storage full / disabled */ }
    }

    function addRandomTile(s) {
        const empty = [];
        s.grid.forEach((v, i) => { if (!v) empty.push(i); });
        if (!empty.length) return -1;
        const index = empty[Math.floor(Math.random() * empty.length)];
        s.grid[index] = Math.random() < 0.9 ? 2 : 4;
        return index;
    }

    /** Board indices of line k, ordered in the direction tiles slide towards. */
    function lineIndices(direction, k) {
        const out = [];
        for (let j = 0; j < SIZE; j++) {
            let r, c;
            switch (direction) {
                case 'left':  r = k; c = j; break;
                case 'right': r = k; c = SIZE - 1 - j; break;
                case 'up':    r = j; c = k; break;
                default:      r = SIZE - 1 - j; c = k; break; // down
            }
            out.push(r * SIZE + c);
        }
        return out;
    }

    function canMove(grid) {
        for (let r = 0; r < SIZE; r++) {
            for (let c = 0; c < SIZE; c++) {
                const v = grid[r * SIZE + c];
                if (!v) return true;
                if (c + 1 < SIZE && grid[r * SIZE + c + 1] === v) return true;
                if (r + 1 < SIZE && grid[(r + 1) * SIZE + c] === v) return true;
            }
        }
        return false;
    }

    function highestTile() {
        return Math.max.apply(null, state.grid);
    }

    function isPaused() {
        return state.over || (state.won && !state.keepPlaying);
    }

    // ---- moves ----------------------------------------------------------

    function move(direction) {
        if (isPaused()) return;

        const before = snapshot();
        let moved = false;
        let gained = 0;
        const merged = [];

        for (let k = 0; k < SIZE; k++) {
            const idx = lineIndices(direction, k);
            const values = idx.map(i => state.grid[i]).filter(v => v);
            const line = [];
            for (let i = 0; i < values.length; i++) {
                if (i + 1 < values.length && values[i] === values[i + 1]) {
                    const v = values[i] * 2;
                    gained += v;
                    merged.push(idx[line.length]);
                    line.push(v);
                    i++;
                } else {
                    line.push(values[i]);
                }
            }
            while (line.length < SIZE) line.push(0);
            idx.forEach((gi, j) => {
                if (state.grid[gi] !== line[j]) {
                    state.grid[gi] = line[j];
                    moved = true;
                }
            });
        }

        if (!moved) return;

        state.undo = before;
        state.score += gained;
        state.moves++;
        const spawned = addRandomTile(state);
        if (!state.won && highestTile() >= WIN_TILE) state.won = true;
        if (!canMove(state.grid)) state.over = true;

        save();
        render(spawned, merged);

        if (state.over) {
            showOverlay('Game over!', 'Saving your score…', false);
            submitScore();
        } else if (state.won && !state.keepPlaying) {
            showOverlay('You win!', 'You reached the 2048 tile. Keep going for a higher score, or save now.', true);
        }
    }

    function snapshot() {
        return {
            grid: state.grid.slice(),
            score: state.score,
            moves: state.moves,
            won: state.won,
            keepPlaying: state.keepPlaying,
        };
    }

    /** Takes back the last move. Only one step: another move is needed before undoing again. */
    function undo() {
        if (!state.undo || isPaused()) return;
        const prev = state.undo;
        state.grid = prev.grid;
        state.score = prev.score;
        state.moves = prev.moves;
        state.won = prev.won;
        state.keepPlaying = prev.keepPlaying;
        state.undo = null;
        save();
        render();
        el.board.focus();
    }

    // ---- rendering ------------------------------------------------------

    function render(spawned, merged) {
        merged = merged || [];
        const frag = document.createDocumentFragment();
        state.grid.forEach((v, i) => {
            const cell = document.createElement('div');
            cell.className = 'cell';
            if (v) {
                const tile = document.createElement('div');
                tile.className = 'tile ' + (v > 2048 ? 't-super' : 't-' + v);
                if (v >= 1024) tile.classList.add(v >= 16384 ? 'tiny-font' : 'small-font');
                if (i === spawned) tile.classList.add('tile-new');
                if (merged.indexOf(i) !== -1) tile.classList.add('tile-merged');
                tile.textContent = v;
                cell.appendChild(tile);
            }
            frag.appendChild(cell);
        });
        el.board.replaceChildren(frag);
        el.score.textContent = state.score;
        el.best.textContent = Math.max(best, state.score);
        el.moves.textContent = state.moves;
        el.time.textContent = formatTime(state.elapsed);
        el.undo.disabled = !state.undo || isPaused();
    }

    function formatTime(seconds) {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return m + ':' + (s < 10 ? '0' : '') + s;
    }

    function showOverlay(title, message, wonMode) {
        el.overlayTitle.textContent = title;
        el.overlayMessage.textContent = message;
        el.overlay.classList.toggle('won', !!wonMode);
        el.keepPlaying.hidden = !wonMode;
        el.saveScore.hidden = !wonMode;
        el.tryAgain.hidden = !!wonMode;
        el.overlay.hidden = false;
        el.undo.disabled = true;
    }

    function hideOverlay() {
        el.overlay.hidden = true;
    }

    // ---- server communication -------------------------------------------

    function submitScore() {
        if (state.submitted || submitting || state.moves === 0) return Promise.resolve(null);
        submitting = true;

        const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
        if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

        const body = {
            score: state.score,
            highestTile: highestTile(),
            moves: state.moves,
            durationSeconds: state.elapsed,
            won: state.won,
        };

        return fetch(submitUrl, { method: 'POST', headers, body: JSON.stringify(body), credentials: 'same-origin' })
            .then(res => {
                if (res.redirected || res.status === 401 || res.status === 403) {
                    throw new Error('Your session expired. Log in again to save scores.');
                }
                if (!res.ok) {
                    return res.json().catch(() => ({})).then(err => {
                        throw new Error(err.error || 'Could not save the score (HTTP ' + res.status + ').');
                    });
                }
                return res.json();
            })
            .then(result => {
                state.submitted = true;
                state.over = true;
                save();
                best = result.highestScore;
                el.best.textContent = best;
                if (el.sideBest) el.sideBest.textContent = best;
                if (el.sideRank) el.sideRank.textContent = '#' + result.rank;
                if (el.sideGames) el.sideGames.textContent = result.gamesPlayed;
                const headline = result.newHighScore ? 'New personal best! ' : '';
                showOverlay(state.won ? 'Well played!' : 'Game over!',
                    headline + 'Score ' + result.score + ' saved. Global rank #' + result.rank + '.', false);
                return result;
            })
            .catch(err => {
                showOverlay('Game over!', err.message, false);
                el.saveScore.hidden = false;
                el.saveScore.textContent = 'Retry save';
                return null;
            })
            .finally(() => { submitting = false; });
    }

    function startNewGame() {
        state = freshState();
        save();
        hideOverlay();
        el.saveScore.textContent = 'Save score';
        render();
        el.board.focus();
    }

    // ---- input ----------------------------------------------------------

    const KEYS = {
        ArrowLeft: 'left', ArrowRight: 'right', ArrowUp: 'up', ArrowDown: 'down',
        a: 'left', d: 'right', w: 'up', s: 'down',
        A: 'left', D: 'right', W: 'up', S: 'down',
    };

    document.addEventListener('keydown', e => {
        if (e.altKey || e.ctrlKey || e.metaKey) return;
        const target = e.target;
        if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA')) return;
        if (e.key === 'u' || e.key === 'U') {
            e.preventDefault();
            undo();
            return;
        }
        const direction = KEYS[e.key];
        if (direction) {
            e.preventDefault();
            move(direction);
        }
    });

    let touchStart = null;
    el.board.addEventListener('touchstart', e => {
        if (e.touches.length !== 1) return;
        touchStart = { x: e.touches[0].clientX, y: e.touches[0].clientY };
    }, { passive: true });
    el.board.addEventListener('touchend', e => {
        if (!touchStart) return;
        const dx = e.changedTouches[0].clientX - touchStart.x;
        const dy = e.changedTouches[0].clientY - touchStart.y;
        touchStart = null;
        if (Math.max(Math.abs(dx), Math.abs(dy)) < 30) return;
        if (Math.abs(dx) > Math.abs(dy)) move(dx > 0 ? 'right' : 'left');
        else move(dy > 0 ? 'down' : 'up');
    });

    el.tryAgain.addEventListener('click', startNewGame);

    el.undo.addEventListener('click', undo);

    el.keepPlaying.addEventListener('click', () => {
        state.keepPlaying = true;
        save();
        hideOverlay();
        render();
        el.board.focus();
    });

    el.saveScore.addEventListener('click', () => {
        state.over = true;
        save();
        showOverlay('Saving…', 'Saving your score…', false);
        submitScore();
    });

    el.newGame.addEventListener('click', () => {
        // Every game that was actually played counts, so an abandoned game is saved first.
        if (!state.submitted && state.moves > 0) {
            state.over = true;
            submitScore().then(result => { if (result) startNewGame(); });
        } else {
            startNewGame();
        }
    });

    // Game clock: only runs while the tab is visible and the game is active.
    setInterval(() => {
        if (document.hidden || isPaused() || state.moves === 0) return;
        state.elapsed++;
        el.time.textContent = formatTime(state.elapsed);
        if (state.elapsed % 5 === 0) save();
    }, 1000);

    // ---- boot -----------------------------------------------------------

    function metaContent(name) {
        const m = document.querySelector('meta[name="' + name + '"]');
        return m ? m.getAttribute('content') : null;
    }

    render();
    if (state.over && !state.submitted) {
        showOverlay('Game over!', 'Saving your score…', false);
        submitScore();
    } else if (state.won && !state.keepPlaying) {
        showOverlay('You win!', 'You reached the 2048 tile. Keep going for a higher score, or save now.', true);
    }
    el.board.focus();
})();
