# ui_ux_specification.md

**Project:** mini daw, a compact DAW for Android phones. All sound comes from samples.
**Stage:** 4 of 6 (feature specifications) · **Version:** 2.1 · **Date:** 2026-11-21
**Change in 1.1:** results of `feature_specifications.md`. Changed parts: Section 15.1 (entry Export), Section 15.4 (measurements), Section 16.3 (Hint for limited values).
**Change in 1.2:** results of Stage 5. The screen "Measurements" also takes the verdicts of the owner (D-094). Changed part: Section 15.4.
**Change in 1.3:** final check of Stage 6. Six functions of version 1.0 have their places on the screen (FR-TR-05, FR-SM-10, FR-CR-06, FR-LIB-02, FR-LIB-08, FR-H2M-13). Changed parts: Sections 3, 6.2, 7.1, 7.2, 8.1, 8.2 and 9.1.
**Change in 2.0:** version 1.1 adds the Decomposer as a second main screen (D-104). New part: Section 19. Changed parts: convention 3, Sections 1.2, 2.2, 3 (Mode switch), 8.2, 13, 15.3, 16.2, 17 and 18.
**Change in 2.1:** TK-014. The owner approved a mockup of the workspace; it is the visual reference (D-116), and its 12 colors fill the palette. Changed part: Section 1.4 (rules 1 and 8).
**Companion documents:** `requirements.md`, `architecture.md`, `feature_specifications.md` and `project_decisions.md` in their newest versions. IDs such as `FR-`, `NFR-`, `CON-`, `D-`, `AD-` and `V-` point to them.

---

## How to read this document

Conventions:

1. Capitalized names (Playlist, Track, Mixer channel) are terms of `requirements.md` Section 2. Section 1.2 adds the names of the parts of the screen.
2. All sizes are in dp. Sizes of text are in sp. Every size is provisional until the check V-01 has measured the screen of the phone.
3. Priorities follow `requirements.md`: P0 is the MVP, P1 is version 1.0, P2 comes later. V1.1 is version 1.1. A part without a mark is P0.
4. This document fixes layout and behaviour. `feature_specifications.md` fixes the functions behind the controls: ranges, formulas and limits.
5. Each editor has a table of gestures. A row of this table is a gesture. A column is the place that the finger touches.
6. The decisions D-076 to D-080 of `project_decisions.md` record the main choices of this document.

| Item that the brief asks for (§4 E) | Section |
|---|---|
| Main landscape workspace | 2 |
| Playlist window | 4 |
| Piano roll | 6 |
| Sample browser | 8 |
| Recording interface | 9 |
| Mixer window | 10 |
| Mixer channel strips and controls | 10.3, 10.4 |
| Effects configuration interface | 11 |
| Audio extraction interface | 12 |
| Panel-switching behavior | 2.3, 2.4, 13 |
| Navigation and interaction patterns | 14 |
| Behaviour of important controls and interactions | 3, and the tables of gestures in Sections 4 to 12 |

Section 5 (Modules), Section 7 (Channel Rack), Section 15 (sheets) and Section 16 (first launch and messages) complete the picture. Section 19 describes the Decomposer of version 1.1. The brief does not name it (D-104).

---

## 1. Basis

### 1.1 Principles

1. **Height is scarce.** The screen is about 400 dp high in landscape (CON-04). A horizontal bar exists only where it is needed. Lists of tools are vertical.
2. **Both thumbs work.** Controls for frequent actions sit near the left edge and near the right edge.
3. **Select first, then act.** A quick drag always scrolls. An item moves only after it is selected, or after the finger has held it.
4. **Nothing blocks.** A message appears without stopping the work. A dialog appears only when the app needs a decision (`architecture.md` Section 13.1).
5. **Every edit can be undone** (NFR-UX-06).
6. **A touch that plays a sound sounds at once.** This holds for keys, step buttons, new Notes and previews.
7. **Hidden parts keep their state.** Scroll position, zoom and selection survive every switch (FR-WS-06).

### 1.2 Names of the parts of the screen

| Name | Meaning |
|---|---|
| Global bar | The bar at the top edge of the Studio. It is always visible there (FR-WS-07). |
| Decomposer bar | The bar at the top edge of the Decomposer (version 1.1, Section 19.2). |
| Panel | One of the two areas below the global bar: the panel of the Playlist and the panel of the Editing workspace. |
| Border | The line between the two panels. The user can drag it. |
| Module strip | The column of buttons that switches the Module of the Editing workspace. |
| Ruler | The row above the lanes. It shows bars and beats. |
| Lane | The row of one Track in the Playlist. Clips lie in it. |
| Track header | The cell at the left end of a lane. It shows the name and the controls of the Track. |
| Action bar | A small bar with the actions for the current selection. |
| Pattern bar | The row at the top of the Piano roll and of the Channel Rack. It names the open Pattern. |
| Strip | The column of one channel in the Mixer (FR-MX-04). |
| Channel page | The page with the Effect slots and the settings of one Mixer channel. |
| Sheet | A page that opens over the workspace: menu of Projects, settings, diagnostics view. |
| Hint | A short message at the bottom edge. It disappears by itself. |

### 1.3 Screen

| Item | Value (prov.) |
|---|---|
| Size of the screen in landscape, estimated | 891 × 411 dp |
| Reference area for this document, after the insets | 854 × 400 dp |
| Global bar | 44 dp high |
| Area of the panels | 854 × 356 dp |

Rules:

1. The app uses the full screen and hides the system bars (FR-WS-11). A swipe from an edge shows them for a moment.
2. No control lies under the display cutout. The global bar and the panels start behind the inset of the cutout.
3. Both landscape rotations work (FR-WS-01). The layout stays the same. Only the inset changes its side.
4. Widths are flexible. Heights of bars and rows are fixed. A wider or higher screen shows more of the editors.
5. The check V-01 measures the real size and the insets. Stage 5 plans one review of all sizes after this check.

### 1.4 Look

The theme is dark (NFR-UX-04).

| Role | Use | Value (prov.) |
|---|---|---|
| Background | Behind the panels | `#101214` |
| Surface | Panels and bars | `#1B1E22` |
| Raised surface | Sheets and menus | `#262A30` |
| Weak line, strong line | Grids of the editors | `#2A2F36`, `#3A414B` |
| Text, weak text | Labels and values | `#E6E8EB`, `#9AA3AD` |
| Accent | Selection, playhead, active buttons | `#4FC3F7` |
| Record | Record control, running Take | `#EF5350` |
| Meter low, middle, high | Level meters | `#66BB6A`, `#FFCA28`, `#EF5350` |
| Warning | Marks for missing Samples and for clipping | `#FFB300` |

Rules:

1. A palette of 12 colors serves Tracks, Mixer channels, Patterns and Sampler instruments. A new item takes the next color of the palette. The colors, in this order (D-116): `#E8735A`, `#E0A030`, `#C9C24A`, `#6CC070`, `#3FB8A8`, `#4AA3DF`, `#6E7FE8`, `#A178E0`, `#D66BC2`, `#E0607E`, `#9C8F7A`, `#7FA0B0`.
2. Sizes of text: 11 sp in dense places, 13 sp as the normal size, 16 sp for titles of sheets. Numbers use digits of equal width.
3. Touch areas follow NFR-UX-02: 48 × 48 dp, or 40 × 40 dp inside dense strips.
4. No state is shown by color alone. A muted Track also shows a crossed icon.
5. Motion is short: at most 150 ms. Scroll and zoom have no animation. The app respects the system setting that removes animations.
6. A light vibration marks a few events (list below).
7. All text of the UI is English and lives in resource files (NFR-UX-05, D-026).
8. The mockup in `docs/design/workspace-mockup/` is the visual reference of the workspace (D-116). This document stays the source of sizes and behaviour.

Events with a light vibration:

1. The pick-up of an item after a hold.
2. The return of a control to its default.
3. The refusal of a move.

---

## 2. Main landscape workspace

### 2.1 Zones

```text
+----------------------------------------------------------------------------------+
| Global bar (44)                                                                  |
+--------+----------------------------------+--+--------------------------+--------+
| Corner | Ruler (40)                       |  | Pattern bar (40)         | Module |
| (88)   |                                  |B |                          | strip  |
+--------+----------------------------------+o +--------------------------+ (44)   |
| Track  | Lanes                            |r | Module                   |        |
| headers|                                  |d |                          |        |
| (88)   |                                  |e |                          |        |
|        |                                  |r |                          |        |
+--------+----------------------------------+--+--------------------------+--------+
  Panel of the Playlist                          Panel of the Editing workspace
```

| Zone | Size (prov.) | Content |
|---|---|---|
| Global bar | full width × 44 | Transport, record, tempo, undo, swap, Mixer, menu (Section 3) |
| Panel of the Playlist | 55 % of the width at the first launch | Corner, Ruler, Track headers, lanes (Section 4) |
| Border | 12 wide; its touch area is 24 wide | A handle with three dots |
| Panel of the Editing workspace | 45 % of the width at the first launch | One Module (Sections 5 to 12) |
| Module strip | 44 wide | One button per Module (Section 5) |

The panel of the Playlist is on the left at the first launch (FR-WS-02).

### 2.2 Layers

Up to version 1.0 the workspace is the only screen (`architecture.md` Section 6.1). Version 1.1 adds the Decomposer (Section 19).

| Layer | Opens by | Closes by |
|---|---|---|
| Mixer (Section 10) | The Mixer button of the global bar | The same button, or the Back gesture |
| Sheet (Section 15) | The Menu button of the global bar | The Back gesture, or a tap beside the sheet |
| Menu or picker | A tap on a button with the mark ▾, or a hold on an item | A choice, a tap beside it, or the Back gesture |
| Dialog | An action that cannot be undone | A choice |

The Back gesture closes the top layer. On the bare workspace it leaves the app. On the bare Decomposer it returns to the workspace.

### 2.3 Border between the panels (FR-WS-04)

1. A drag of the Border changes the widths of both panels. The editors follow the finger without delay.
2. A visible panel is at least 240 dp wide.
3. A drag past this limit hides the panel. The other panel then takes the full width.
4. A hidden panel leaves a handle of 16 dp at its edge. A tap or a drag on the handle brings the panel back to its last width.
5. A double tap on the Border restores the widths of the first launch.
6. The widths survive a restart (FR-WS-10).

### 2.4 Swap of the panels (FR-WS-03)

1. The Swap button of the global bar exchanges the left and the right panel.
2. Scroll position, zoom, selection and playback stay unchanged.
3. The Module strip always sits at the outer edge of the panel of the Editing workspace. It moves with the panel.
4. The Track headers always sit at the left end of the lanes, because time runs from left to right.
5. The sides survive a restart (FR-WS-10).

---

## 3. Global bar (FR-WS-07)

Controls from left to right:

| No. | Control | Width | Tap | Other gesture | What it shows |
|---|---|---|---|---|---|
| 1 | Play | 48 | Starts playback at the playhead. A second tap stops it (FR-TR-01). | Hold: moves the playhead to the start of the song. | A triangle when stopped, a square when playing |
| 2 | Record | 48 | Starts a Take with the current target. A second tap ends the Take (FR-REC-01). | Hold: opens the Recorder (Section 9). | Red while a Take runs. A small mark names the target. |
| 3 | Mode | 76 | Switches between Song mode and Pattern mode (FR-TR-02). | — | "SONG" or "PAT" |
| 4 | Position | 72 | — | — | Bar, beat and Step, for example "5.2.3". During a Take: the elapsed time in red. |
| 5 | Tempo | 64 | Opens the tempo menu. | Vertical drag: changes the tempo by 1 BPM per 8 dp. | The tempo, for example "120" |
| 6 | Metronome | 44 | Switches the metronome on or off (FR-TR-09). | — | Lit when on |
| 7 | Status | rest | Opens the menu of Projects. | — | Name of the Project. Marks for a running job and for overload. |
| 8 | Undo | 44 | Undoes the last step (FR-PRJ-04). | — | Dim when nothing can be undone |
| 9 | Redo | 44 | Repeats the undone step. | — | Dim when nothing can be repeated |
| 10 | Swap | 44 | Exchanges the panels (Section 2.4). | — | — |
| 11 | Mode switch (version 1.1) | 44 | Shows the Decomposer (Section 19.1, FR-DEC-01). | — | A mark when a Decomposition job is ready or has failed |
| 12 | Mixer | 48 | Opens or closes the Mixer (FR-WS-08). | — | Lit while the Mixer is open |
| 13 | Menu | 44 | Opens the Project menu (Section 15). | — | — |

The fixed controls need 576 dp. From version 1.1 on they need 620 dp. The Status control takes the rest.

Rules:

1. Every control of NFR-UX-01 needs one tap: start a Take, play, open the Mixer, undo. A switch of the Module needs one tap on the Module strip.
2. Play and Record sit at the left end. Mixer and Menu sit at the right end. Each thumb reaches its end.
3. Stop returns the playhead to the place where playback started. The next Play repeats the same passage.
4. The Record control is enabled as soon as the workspace is drawn (FR-REC-01).
5. The global bar stays visible and usable while the Mixer or a sheet is open.

Tempo menu:

1. A large field shows the tempo. A drag on the field changes it.
2. The buttons − and + change the tempo by 1 BPM.
3. The range is 30 to 300 BPM (FR-TR-03).
4. A change is heard at once during playback. One drag is one step of undo.
5. P1: a Tap button sets the tempo from the rhythm of the taps (FR-TR-12).
6. P1: a list sets the time signature of the Project (FR-TR-05).

---

## 4. Playlist window

### 4.1 Layout

| Part | Size (prov.) | Content |
|---|---|---|
| Corner | 88 × 40 | The button of the Playlist menu. It shows the grid value, for example "1/4". |
| Ruler | rest × 40 | Numbers of the bars, marks for the beats, the playhead. P1: the loop region. |
| Track header | 88 × height of the Track | Color, name, mute button, name of the Mixer channel (FR-PL-10) |
| Lane | rest × height of the Track | Clips |
| Cell "+ Track" | 88 × 40, below the last Track header | Adds a Track |

| Setting | Values (prov.) | Default |
|---|---|---|
| Height of a Track | 40 or 56 | 40 |
| Zoom of the time axis | 4 to 96 dp per beat | 24 dp per beat |
| Grid value | bar, 1/4, 1/8, 1/16, 1/32, off (FR-TR-07). P1: triplets. | 1/4 |

At a height of 40 dp, eight Tracks fill the panel almost exactly. The Ruler is 40 dp high, so that a finger can hit it.

Playlist menu (button in the Corner):

1. Grid value.
2. Height of the Tracks.
3. Add the open Pattern at the playhead (FR-PL-04).
4. Add a Track.
5. P1: follow the playhead, on or off.

### 4.2 Gestures in the lanes

| Gesture | On an empty place | On a Clip | On a selected Clip |
|---|---|---|---|
| Tap | Clears the selection | Selects the Clip | Nothing |
| Double tap | Adds a Pattern clip of the open Pattern at this place | Pattern clip: opens its Pattern in the Piano roll (FR-WS-09). Audio clip: shows its Sample in the Sample browser. | Same as on a Clip |
| Drag | Scrolls | Scrolls | Moves the Clip in time and between Tracks (FR-PL-05) |
| Hold of 200 ms, then drag | P1: draws a rectangle that selects Clips | Picks the Clip up and moves it | Moves the Clip |
| Drag of an end handle | — | — | Trims the start or the end (FR-PL-06, FR-PL-07) |
| Pinch | Zooms the time axis around the fingers (FR-PL-11) | Same | Same |

Rules for a move:

1. The Clip follows the finger. Its start jumps to the grid value.
2. A ghost shows the old place until the finger lifts.
3. If the new place would overlap another Clip, the Clip turns red. On release it returns to its old place, and a Hint says why (FR-PL-08).
4. The view scrolls by itself when the finger comes near an edge of the lanes.
5. One move is one step of undo.
6. Edits work during playback (FR-PL-13).

Rules for a trim:

1. A selected Clip shows one handle at each end. The touch area of a handle is 24 dp wide and lies half outside the Clip.
2. An Audio clip cannot grow beyond its Sample (FR-PL-06).
3. A Pattern clip that is longer than its Pattern shows a mark at each repeat (FR-PL-07).

### 4.3 Action bar of a Clip

The Action bar appears below the Ruler when a Clip is selected. It is 40 dp high.

| Action | For | Priority |
|---|---|---|
| Duplicate: places a copy right after the Clip | Both kinds | P0 |
| Delete | Both kinds | P0 |
| Open: same as a double tap | Both kinds | P0 |
| Make unique: gives the Clip its own copy of the Pattern (FR-PT-02) | Pattern clip | P0 |
| Split at the playhead | Both kinds | P1 |
| Gain (FR-PL-14) | Both kinds | P1 |

### 4.4 Ruler and playhead

| Gesture on the Ruler | Effect |
|---|---|
| Tap | Moves the playhead to this place, also during playback (FR-TR-08) |
| Drag | Moves the playhead with the finger |
| Hold, then drag (P1) | Marks the loop region (FR-TR-11) |

Rules:

1. The playhead is a vertical line in the accent color. It crosses the Ruler and all lanes.
2. During playback the view follows the playhead in pages. When the playhead passes 85 % of the visible width, the view jumps, and the playhead stands at 15 %.
3. A scroll by the user stops the following until the next Play.
4. The position of the playhead follows the sound that the user hears (FR-TR-10).

### 4.5 Track header

| Gesture | Effect |
|---|---|
| Tap on the header | Selects the Track. Its Strip is marked in the Mixer (FR-MX-16). |
| Tap on the mute button | Mutes the Track, or ends the mute |
| Double tap on the name | Renames the Track |
| Hold | Opens the menu of the Track |

Menu of a Track:

1. Rename.
2. Color.
3. Mixer channel: a list of all Mixer channels and the entry "New channel" (FR-MX-02).
4. Delete. If the Track was the last one of its Mixer channel, the app offers to delete the channel too (FR-MX-16).
5. P1: solo, move up, move down.

### 4.6 Look of a Clip

| Case | Look |
|---|---|
| Audio clip | Rounded block in the color of the Track. Waveform of its region (FR-PL-12). Name in small text. |
| Pattern clip | Rounded block in the color of the Pattern. Name of the Pattern. P1: a small picture of the Notes. |
| Selected Clip | Outline in the accent color, two handles |
| Clip on a muted Track | Dim |
| Clip whose Sample is missing | Hatched, with a warning mark (NFR-REL-04) |
| Waveform not loaded yet | Plain block |

### 4.7 Ways to add a Clip

1. Hold a row of the Sample browser, then drag it onto a lane (FR-PL-03).
2. Choose "Add at playhead" in the menu of a Sample.
3. Double tap on an empty place of a lane. This adds the open Pattern.
4. Choose "Add to Playlist" in the menu of a Pattern (FR-PL-04).

The commands of items 2 and 4 use the selected Track. If its span is not free, or no Track is selected, the app adds a new Track.

---

## 5. Editing workspace and its Modules

Buttons of the Module strip, from top to bottom:

| No. | Module | Priority | Mark on the button |
|---|---|---|---|
| 1 | Piano roll (Section 6) | P0 | — |
| 2 | Channel Rack (Section 7) | P0 | — |
| 3 | Sample browser (Section 8) | P0 | — |
| 4 | Recorder (Section 9) | P0 | A red dot while a Take runs |
| 5 | Effect editor (Section 11) | P0 | — |
| 6 | Extraction (Section 12) | P1 | The number of jobs that wait for the user |

Each button is 44 × 44 dp. The active Module is lit.

Rules:

1. A tap switches the Module at once (FR-WS-05).
2. A switch does not stop playback and does not change the Project (FR-WS-06).
3. A hidden Module keeps its scroll position, zoom and selection.
4. Other parts can switch the Module. A double tap on a Pattern clip shows the Piano roll. A tap on an Effect slot shows the Effect editor (FR-WS-09).
5. The active Module survives a restart (FR-WS-10).

---

## 6. Piano roll

### 6.1 Layout

| Part | Size (prov.) | Content |
|---|---|---|
| Pattern bar | full width × 40 | Buttons of Section 6.2 |
| Thin ruler | rest × 16 | Numbers of the bars. Display only. |
| Keyboard strip | 56 wide | Keys. Every C carries its name. The Root note is marked. |
| Grid | rest | Rows of pitches, columns of time, Notes of the open Part |
| P1: velocity lane | rest × 56, at the bottom | One bar per Note (FR-PT-09) |

| Setting | Values (prov.) | Default |
|---|---|---|
| Height of a row | 14 to 40 | 24 (NFR-UX-02) |
| Zoom of the time axis | 16 to 256 dp per beat | 64 dp per beat |
| Grid value | Same list as in the Playlist | 1/16 |

### 6.2 Pattern bar

| No. | Control | Tap | Shown in the Channel Rack too |
|---|---|---|---|
| 1 | Pattern button "Pattern 1 ▾" | Opens the list of Patterns | Yes |
| 2 | Part button: color dot, name of the Sampler instrument, ▾ | Opens the list of Parts | No |
| 3 | Pencil | Switches the Pencil on or off | No |
| 4 | Length "2 bars", with − and + | Changes the length of the Pattern by one bar (FR-PT-01) | Yes |
| 5 | Grid button "1/16 ▾" | Opens the list of grid values | No |

In a narrow panel, controls 4 and 5 move into a menu "⋯".

List of Patterns (FR-PT-08):

1. One row per Pattern. A tap opens the Pattern.
2. "New Pattern".
3. A hold on a row opens its menu: rename, color, duplicate, add to Playlist at the playhead, delete.
4. The app asks first when a Pattern with Pattern clips is deleted.

List of Parts (FR-PT-06):

1. One row per Part of the open Pattern. A tap opens the Part.
2. "Add Part": a Sampler instrument of the Project, or a new one from a Sample.
3. "Replace the Sampler instrument" of the open Part.
4. "Settings of the Sampler instrument": Root note, play mode, volume. P1: pan, envelope, loop, start and end points, fine tuning, own Mixer channel (FR-SM-09, FR-SM-10, FR-MX-19).

### 6.3 Gestures in the grid

| Gesture | On an empty place | On a Note | On a selected Note |
|---|---|---|---|
| Tap | Pencil on: adds a Note and plays it. Pencil off: clears the selection. | Selects the Note and plays it | Plays the Note |
| Drag | Scrolls | Scrolls | Moves the Note in time and in pitch (FR-PT-04) |
| Hold of 200 ms, then drag | P1: draws a rectangle that selects Notes | Picks the Note up and moves it | Moves the Note |
| Drag of the end handle | — | — | Changes the length |
| Pinch | Horizontal: zooms the time axis. Vertical: changes the height of the rows (FR-PT-07). | Same | Same |

Rules:

1. A new Note starts on the grid. It takes the length of the Note that the user edited last. The first Note of a session is one grid value long.
2. A new Note has the velocity 100.
3. A Note plays once when it is added and each time its pitch changes during a move (FR-PT-05).
4. Notes cannot lie beyond the end of the Pattern. The area behind the end is dim. A Hint points to the Length control.
5. The Pencil is on at the start. Its state survives a restart.
6. When a Part opens, the grid scrolls to the range of its Notes. An empty Part shows the Root note in the middle.
7. P1: the Notes of the other Parts appear as a dim background (FR-CR-08).

Action bar of a Note:

| Action | Priority |
|---|---|
| Delete | P0 |
| Duplicate, transpose by a semitone or an octave, quantize, velocity (FR-PT-09) | P1 |

### 6.4 Keyboard strip

1. A touch on a key plays the Sampler instrument of the open Part at this pitch (FR-PT-05).
2. In the play mode gate, the sound lasts as long as the finger stays. In the play mode one-shot, the whole Sample plays.
3. A slide across the keys plays each key that the finger enters.
4. The strip never scrolls the grid. Only a drag in the grid scrolls.

---

## 7. Channel Rack

### 7.1 Layout

| Part | Size (prov.) | Content |
|---|---|---|
| Pattern bar | full width × 40 | Pattern button and Length (Section 6.2). P1: the switch "All instruments". A narrow panel shows it in the menu "⋯". |
| Row of a Part | full width × 44 | Mute button, name with color, volume control, step buttons (FR-CR-02) |
| Row "+ Add Part" | full width × 44 | Opens the choice of a Sampler instrument (FR-CR-03) |

| Width of the panel | Head of a row | Step buttons per page |
|---|---|---|
| Wide: 600 dp or more | Mute 40, name 96, volume 40 | 16 (FR-CR-12) |
| Narrow | Mute 36, name 56. The volume moves into the menu of the row. | 8 |

A step button is 40 dp high. It is at least 30 dp wide. Every group of four Steps has its own shade.

### 7.2 Gestures

| Gesture | Place | Effect |
|---|---|---|
| Tap | Step button | Adds a Note at the Root note, or removes it. A new Note sounds once when the Transport is stopped (FR-CR-05). |
| Drag across step buttons | Step buttons | P1: switches every Step that the finger enters (FR-CR-13) |
| Swipe left or right | Area of the step buttons | Shows the next or the previous page of Steps |
| Tap | Name of a row | Opens the Part in the Piano roll (FR-CR-02) |
| Tap | Mute button | Mutes the Sampler instrument in every Pattern (FR-CR-04) |
| Vertical drag | Volume control | Changes the volume of the Sampler instrument |
| Hold | Name of a row | Opens the menu of the row |

Rules:

1. A Note from a step button is one Step long and has the velocity 100.
2. During playback, the Step that sounds is lit in every row (FR-CR-10).
3. A row shows step buttons only while every Note of its Part starts on a Step at the Root note. Otherwise the row shows a small picture of its Notes. A tap on the picture opens the Piano roll (FR-CR-09).
4. The step buttons and the Piano roll show the same Notes. A change in one appears in the other at once.

Menu of a row:

1. Settings of the Sampler instrument (Section 6.2).
2. Replace the Sample.
3. Remove the Part. The app asks first when the Part holds Notes (FR-CR-03).
4. P1: solo, pan, move up, move down (FR-CR-06).

P1: with the switch "All instruments" on, the Channel Rack shows a row for every Sampler instrument of the Project (FR-CR-06). A row without a Part has empty step buttons. A tap on one of them adds the Part first and then the Note.

Choice in "+ Add Part":

1. A Sampler instrument of the Project that the Pattern does not use yet.
2. "New from a Sample". The Sample browser opens in its mode for choosing. The new Sampler instrument starts in the play mode one-shot (FR-CR-11).

---

## 8. Sample browser

### 8.1 Layout

| Part | Size (prov.) | Content |
|---|---|---|
| Head | full width × 40 | Buttons of the groups, Import button. P1: search field and filter button. |
| Row of a Sample | full width × 44 | Preview button, name, length, menu button "⋮" (FR-LIB-02). P1: a small waveform. |

Groups: Factory, Recordings, Imported. Version 1.0 adds Extracted (FR-LIB-01).

### 8.2 Gestures

| Gesture | Place | Effect |
|---|---|---|
| Tap | Row or preview button | Plays the Sample as a preview. A second tap stops it. Another preview replaces it (FR-LIB-03). |
| Hold of 200 ms, then drag | Row | Carries the Sample to a lane of the Playlist and places an Audio clip (FR-PL-03) |
| Tap | Menu button "⋮" | Opens the menu of the Sample |
| Tap | Import button | Opens the file picker of the system (FR-LIB-04) |
| Tap | Button of a group | Shows the Samples of this group |

Menu of a Sample (FR-LIB-05):

| Action | Priority |
|---|---|
| Add at playhead | P0 |
| Use as Sampler instrument | P0 |
| Rename | P0 |
| Delete. The app asks first (NFR-UX-06). | P0 |
| Convert to Notes (FR-H2M-11) | P1 |
| Trim, normalize (FR-LIB-10) | P1 |
| Favorite, tags (FR-LIB-02) | P1 |
| Extract a sound from this Sample (FR-AIX-01) | P1 |
| Decompose this Sample (FR-DEC-03) | V1.1 |

Rules:

1. "Use as Sampler instrument" needs two taps from the list (FR-SM-03). It creates the Sampler instrument and adds a Part for it to the open Pattern. Then the Piano roll opens.
2. A preview works while the Project plays. It bypasses the Mixer (FR-MX-14).
3. During an import the row of the new Sample shows progress. The user can cancel (NFR-PERF-08).
4. A Sample that a Project still uses can be deleted. It leaves the list. The Project keeps playing it (FR-LIB-06).
5. In the mode for choosing, a tap on a row picks the Sample and returns to the caller. The preview button still plays.
6. P1: the search field keeps the Samples whose name holds the text. The filter button offers "Favorites" and every tag (FR-LIB-02).
7. P1: when another app shares an audio file, a sheet offers two actions: "Add to Library" and "Extract a sound" (FR-LIB-08, FR-AIX-01). From version 1.1 on, it offers a third action: "Decompose" (Section 19.7).

---

## 9. Recording interface

### 9.1 Record control and Recorder

Recording has two places:

1. The Record control of the global bar. One tap starts a Take from any state of the workspace (FR-REC-01).
2. The Recorder, a Module. It shows the level and the settings.

Layout of the Recorder:

| Part | Content | Priority |
|---|---|---|
| Target switch | "Audio" or "Humming" (FR-REC-02) | P0 |
| Level meter | Peak of the input, with a mark for clipping (FR-REC-04) | P0 |
| Time | Elapsed time and the limit of the target (FR-REC-06) | P0 |
| Large button | Starts and ends a Take. It does the same as the Record control. | P0 |
| Count-in | 0, 1 or 2 bars (FR-REC-08) | P1 |
| Metronome during the Take | On or off | P1 |
| Hint about the speaker | Appears when the speaker plays during a Take | P1 |
| Place of the Notes | "New Pattern", or "New Part of the open Pattern" (FR-H2M-13) | P1 |

### 9.2 Course of a Take

| Step | What the user sees |
|---|---|
| Very first Take | The system asks for the microphone permission. No other dialog comes first (FR-REC-01). |
| Take runs | The Record control and the Position control are red. The Position control shows the elapsed time. The Recorder shows the level. |
| Take ends | By a tap, at the limit, or when the app leaves the foreground (FR-REC-05) |
| Target "Audio" | The new Audio clip appears at the place where the Take started. A Hint names the Track (FR-REC-02). |
| Target "Humming" | The Recorder shows the progress of the conversion and a Cancel button. Then the Piano roll opens with the new Part (FR-H2M-04). |
| No Notes found | A message says so. The Take stays in the Library (FR-H2M-08). |

Rules:

1. The app remembers the last target (FR-REC-02).
2. A Take can start while the Project plays (FR-REC-03).
3. If the user refuses the permission, the Recorder explains how to grant it. A button opens the settings of the system. Everything else keeps working (FR-REC-07).
4. With Bluetooth output, the Recorder shows a warning about the timing of the Take (NFR-AUD-05).

P1: a panel "Convert again" follows a conversion (FR-H2M-10).

| Control of the panel | Values |
|---|---|
| Shortest Note | A list of note values |
| Range of pitch | Lowest and highest Note |
| Shift | By octaves |
| Quantization | Off, 1/16, 1/8 (FR-H2M-07) |
| Compare | Plays the Take, then the Pattern |

---

## 10. Mixer window

### 10.1 Place and sizes (D-077)

The Mixer is a layer over the panels. The global bar stays visible.

| Size | Covers | Use |
|---|---|---|
| Half | The panel of the Editing workspace. Its width is at least 320 dp. | The Playlist stays visible and usable while the user mixes. |
| Full | Both panels | All Strips are visible at once. |

Rules:

1. One tap on the Mixer button opens the Mixer. One more tap, or the Back gesture, closes it (FR-WS-08).
2. Closing restores the workspace exactly. Playback never stops.
3. A button in the tool column switches between Half and Full. The size survives a restart.
4. The Mixer opens with the Strip of the selected Track in view (FR-MX-16).

### 10.2 Layout

```text
Full size:
+---------------------------------------+------------------+--------+------+
| Strips (scroll sideways)              | Channel page     | Master | Tool |
| 56 each                               | 280, when open   | 64     | col. |
|                                       |                  |        | 44   |
+---------------------------------------+------------------+--------+------+

Half size:
+--------------------------+--------+------+
| Strips, or Channel page  | Master | Tool |
+--------------------------+--------+------+
```

| Part | Size (prov.) | Content |
|---|---|---|
| Area of the Strips | rest | One Strip per Mixer channel, in the order of the channels. It scrolls sideways. |
| Channel page | 280 wide in Full size | Section 10.5. In Half size it takes the place of the Strips and has a back arrow. |
| Strip of the Master channel | 64 wide | Always visible at the right end |
| Tool column | 44 wide | Button for the size. Button "+" for a new Mixer channel. |

In Full size about eight Strips fit beside an open Channel page. Without the Channel page, thirteen fit.

### 10.3 Strip of a Mixer channel (FR-MX-04)

Parts from top to bottom:

| Part | Height (prov.) | Content | Gestures |
|---|---|---|---|
| Head | 44 | Color, name, four dots for the Effect slots, short names of the assigned Tracks | Tap: selects the Strip and opens its Channel page. Double tap: renames. |
| Pan | 44 | A knob with a mark at the center | Horizontal drag: changes the pan. Double tap: center (FR-MX-05). |
| Fader with meter | 188 | Fader, scale in dB, level meter for left and right, clip indicator | See below |
| Mute | 40 | Button "M" | Tap: mutes the channel (FR-MX-06) |
| Solo | 40 | Button "S" | Tap: solos the channel. Several channels can be soloed. |

The heights add up to 356 dp. On a higher screen the fader grows.

Fader:

1. A drag changes the volume relative to the place of the touch. The fader never jumps to the finger.
2. The range goes from silence to +6 dB (FR-MX-05). The scale has marks at +6, 0, −6, −12, −24 and −48 dB.
3. A double tap sets 0 dB.
4. A bubble shows the value in dB during the drag.
5. When the finger moves more than 56 dp sideways, the fader moves at a quarter of the speed. This allows fine steps.
6. A change is heard at once and without a click (FR-MX-13). One drag is one step of undo.

Level meter and clip indicator:

1. Two thin bars show the peak of the left and the right side after the fader (FR-MX-07).
2. A small line marks the recent maximum.
3. The clip indicator is a red block above the bars. It lights when the level of the channel exceeds full scale. A tap clears it (FR-MX-08).
4. `feature_specifications.md` defines the fall of the bars and the time of the mark.

Scroll of the Strips: a drag that starts sideways scrolls the Strips. A drag that starts upward or downward on a fader moves the fader.

### 10.4 Strip of the Master channel

| Part | Content |
|---|---|
| Head | Name "Master". A tap opens its Channel page. |
| Readout | The recent maximum of the output as a number in dB |
| Fader with meter | Same behaviour as in Section 10.3 |
| Clip indicator | Lights when the output exceeds full scale. It stays lit until the user taps it (FR-MX-08). |

The Master channel has no pan, no mute and no solo (FR-MX-06).

### 10.5 Channel page

| Part | Content | Priority |
|---|---|---|
| Head | Back arrow in Half size. Color and name. A tap on the name renames the channel. | P0 |
| Effect slots | Four rows of 44 dp. Each shows the number, the name of the effect or "Empty", a bypass switch and a menu button (FR-MX-09). | P0 |
| Tracks | Names of the Tracks that send to this channel | P0 |
| Output | "To: Master ▾". The list holds the Master channel and the Mixer channels that would form no loop (FR-MX-11). | P1 |
| Delete channel | Enabled when no Track sends to the channel | P0 |

| Gesture on an Effect slot | Effect |
|---|---|
| Tap on an empty slot | Opens the list of effects. A choice adds the effect and opens its editor. |
| Tap on a used slot | Opens the Effect editor (FR-WS-09) |
| Tap on the bypass switch | Switches the effect off or on without a click (FR-FX-06) |
| Tap on the menu button | Replace, remove. P1: move up, move down. |

### 10.6 Relation between Tracks and Strips (FR-MX-16)

1. A selected Track marks its Strip. The Mixer scrolls this Strip into view.
2. A selected Strip marks the Track headers of its Tracks.
3. A Strip shows the short names of its Tracks in its Head.
4. A new Track creates a new Mixer channel with the same name and color (FR-MX-02).
5. The menu of a Track assigns the Track to another Mixer channel (Section 4.5).

---

## 11. Effects configuration interface

The Effect editor is a Module (FR-FX-05). The Mixer shows the same two pages inside its Channel page (D-078).

### 11.1 First page: Effect slots of one channel

1. A channel button at the top names the channel, for example "Channel 3 ▾". Its list holds all Mixer channels and the Master channel.
2. The Module follows the selection. It shows the Mixer channel of the selected Track, or the selected Strip.
3. Below the button, the page equals the Channel page of Section 10.5.

### 11.2 Second page: editor of one effect

| Part | Size (prov.) | Content |
|---|---|---|
| Head | full width × 40 | Back arrow, name of the effect, bypass switch, menu button |
| Body | rest; it scrolls when needed | One cell of 72 × 84 per parameter |

| Kind of control | Use | Gestures |
|---|---|---|
| Knob with name and value | A parameter with a range | Vertical drag over 160 dp covers the range. Double tap: default value. More than 56 dp sideways: a quarter of the speed. |
| Switch | On or off | Tap |
| Choice | A small list of values | Tap opens the list |

Rules:

1. A change is heard at once and without a click (FR-FX-06). One drag is one step of undo.
2. The value appears below the knob in its real unit, for example "350 ms".
3. `feature_specifications.md` lists the parameters, ranges and defaults of every effect.
4. The equalizer shows its three bands side by side.
5. P1: the delay has a switch that ties its time to the tempo (FR-FX-10).

Effects of the MVP: equalizer, delay, reverb (FR-FX-02). Version 1.0 adds compressor, limiter, saturation, filter and chorus (FR-FX-03).

---

## 12. Audio extraction interface (version 1.0)

The Module Extraction has two pages: "New" and "Jobs". Two tabs at the top switch between them.

### 12.1 Page "New"

| Row | Content | Requirement |
|---|---|---|
| Source | Three buttons: "File", "Link", "Sample". A shared file arrives here too. After the choice, the row shows name and length. | FR-AIX-01 |
| Range | Fields "Start" and "End" in minutes and seconds. For a local file: a waveform with two handles and a Play button. | FR-AIX-02, FR-AIX-03, FR-AIX-05 |
| Sound | Four quick buttons: "Vocals", "Drums", "Bass", "Other". Or a text field "Describe the sound" with examples. | FR-AIX-04, FR-AIX-17 |
| Foot | What is left: budget of the job, jobs of today. The Send button. | FR-AIX-15 |

Rules:

1. If the user fills only "Start" with the time `t`, the range runs from `t − 1 s` to `t + 9 s`. The user can change it (FR-AIX-02).
2. The fields refuse a range shorter than 0.5 s or longer than 30 s (FR-AIX-03).
3. A quick button sends the request in quick mode. The text field sends a free request.
4. "Link" opens a small dialog with one field. The app accepts only a direct link to an audio file. Another link gets a clear message (FR-AIX-01, D-015).
5. The Send button is dim while a part is missing. A line below it names the missing part.

States that block the page:

| State | What the page shows |
|---|---|
| No API key | A card that explains the key. A button opens the settings (FR-AIX-19). |
| Setup on the account missing | A card that explains the step (FR-AIX-20) |
| First use | A notice: the user is responsible for the rights to the source. A button "I understand" (FR-AIX-16). |
| No network | A line "No network". The Send button is dim. |
| A limit is reached | A line that names the limit and the time of its reset (FR-AIX-15) |

### 12.2 Page "Jobs"

One row per job, 56 dp high: the sound, the name of the source, the state, the elapsed time. An active job has a Cancel button (FR-AIX-10).

| State shown | Meaning | Tap opens |
|---|---|---|
| Uploading | The phone prepares and sends the audio | Progress |
| Running | The Session works | Elapsed time and Cancel |
| Needs an answer | The Orchestrator asked a question | The question, a field for the answer, a Send button (FR-AIX-07) |
| Ready | Candidates wait for the review | The review (Section 12.3) |
| Failed | The job ended with an error | The cause and a Retry button (FR-AIX-14) |
| Cancelled | The user stopped the job | A Delete button |

### 12.3 Review of a result (FR-AIX-11)

| Part | Content |
|---|---|
| Original | A Play button for the chosen range. Only for a source on the phone. |
| Candidate, 1 to 3 | Play button, label, note of the Orchestrator, buttons "Accept" and "Reject" (FR-AIX-09) |
| Trim | Opens a waveform with two handles for a candidate |
| What was done | The steps of the Orchestrator, as a list that opens on a tap (NFR-QLT-05) |
| Cost | The cost of this job (NFR-COST-04) |
| Try again | Opens the page "New" with the same source and range. The new job is linked to this one. |

After "Accept", the Sample is in the group Extracted. Two buttons appear: "Add to Playlist" and "Use as Sampler instrument" (FR-AIX-13).

Notifications: when the app is not visible, a job that becomes "Ready", "Needs an answer" or "Failed" shows a notification. A tap opens the job (FR-AIX-10).

---

## 13. Panel-switching behavior

| Action | What changes | What stays |
|---|---|---|
| Swap button | The sides of the two panels. The Module strip moves to the new outer edge. | Widths, scroll positions, zoom, selection, playback (FR-WS-03) |
| Drag of the Border | The widths of the panels | Sides, Module, playback |
| Drag of the Border past the limit | One panel hides. The other takes the full width. | The hidden panel keeps its state. A handle stays at the edge. |
| Tap on the Module strip | The Module | The Project, playback, the state of the hidden Module (FR-WS-06) |
| Mode switch (version 1.1) | The shown screen: Studio or Decomposer. Playback stops. | The Project, the layout of the workspace, the state of the Decomposer (FR-DEC-02) |
| Mixer button | The Mixer opens over the panels | Everything below it. Playback continues (FR-WS-08). |
| Size button of the Mixer | Half or Full | The scroll position of the Strips |
| Double tap on a Pattern clip | The Module becomes the Piano roll. The Pattern opens. | The Playlist |
| Tap on an Effect slot | The Effect editor opens | The selected channel |
| Rotation by 180 degrees | The side of the inset for the cutout | Everything else (FR-WS-01) |
| Restart of the app | Nothing | Sides, widths, Module, zoom, open Pattern and Part, size of the Mixer (FR-WS-10) |

If the panel of the Editing workspace is hidden, an action that needs a Module shows this panel again at its last width.

---

## 14. Navigation and interaction patterns

### 14.1 Meaning of the gestures

| Gesture | Meaning everywhere |
|---|---|
| Tap | Select, switch, or trigger |
| Double tap | Open an item, or reset a control to its default |
| Hold of 200 ms | Pick an item up, or open its menu |
| Drag on the background | Scroll |
| Drag on a selected item or on a handle | Edit |
| Pinch | Zoom |
| Back gesture | Close the top layer |

A drag counts after the finger has moved 8 dp.

### 14.2 Selection

1. Each editor has its own selection. A selection in one editor does not clear the selection in another.
2. A tap on an empty place clears the selection of this editor.
3. The Action bar belongs to the editor that the user touched last.
4. P1: a rectangle selects several items. The Action bar then acts on all of them.

### 14.3 Messages

| Kind | Use | Behaviour |
|---|---|---|
| Hint | Result of an action, a refused move, a small warning | One line at the bottom edge for 4 s. At most one at a time. It can carry one button, for example "Retry". |
| Mark | A lasting state: clip indicator, missing Sample, state of a job | Stays until the state ends or the user taps it |
| Dialog | A decision that cannot be undone | Two buttons. The safe choice is the default. |
| Progress | Work that takes longer than 300 ms: import, conversion, export, download | A bar or a ring with a Cancel button (NFR-PERF-08) |

Actions that open a dialog:

1. Delete a Project.
2. Delete a Sample.
3. Delete a Pattern that has Pattern clips.
4. Remove a Part that holds Notes.
5. Delete all data of the Extraction jobs.

### 14.4 Undo and redo

1. Undo and Redo sit in the global bar and work in every state of the workspace.
2. After an undo, a Hint names the step, for example "Undone: move of a Clip".
3. Scroll, zoom, selection and the choice of a Module are no steps of undo (`architecture.md` Section 4.2).

### 14.5 Sound of touches

1. Keys, step buttons, new Notes and previews sound at once (principle 6).
2. A new Note from a step button sounds only while the Transport is stopped. During playback the Pattern plays it anyway.
3. `feature_specifications.md` defines the rules for audio focus.

### 14.6 Edges of the screen (NFR-UX-03)

1. No control that the user drags sideways starts within 24 dp of a screen edge, except the lanes and the grids.
2. The Track headers and the Module strip occupy the outer edges where possible. The user taps them and does not drag them sideways.
3. The app declares the edge of the lanes and of the grids as an area without the Back gesture, as far as the system allows.
4. A manual test on the phone checks the result.

### 14.7 Text entry

1. Names are edited in a small dialog with one field.
2. The field keeps the workspace visible. The keyboard does not switch to its full-screen mode.
3. A new item never asks for a name. It gets a default name, for example "Track 3" or "Take 5".

### 14.8 Access

1. Every control has a text description for the screen reader.
2. Text follows the size setting of the system up to 130 %.
3. Text has a contrast of at least 4.5 to 1 against its background.

### 14.9 Screen

The screen stays on while the Transport plays or a Take runs (`architecture.md` Section 5.11).

---

## 15. Sheets

### 15.1 Project menu

The Project menu opens at the right edge. It is 280 dp wide.

| Entry | Effect | Priority |
|---|---|---|
| New Project | Creates a Project, names it by date and time, opens it (FR-PRJ-05) | P0 |
| Projects | Opens the list of Projects | P0 |
| Rename, Duplicate | Act on the open Project | P0 |
| Export | Opens a sheet with the kinds of export. Song as WAV, with 16-bit, 24-bit or floating point values. Song as M4A. Pattern as MIDI file. Project as archive. After the export the sheet offers "Save to" and "Share" (FR-EXP-01 to -04, FR-PRJ-07). | P1 |
| Import a Project | Opens the file picker of the system (FR-PRJ-07) | P1 |
| Settings | Opens the settings | P0 |
| Diagnostics | Opens the diagnostics view (FR-SET-01) | P0 |
| Licences | Lists the licences of libraries, factory Samples and models (FR-SET-02) | P0 |

### 15.2 List of Projects (FR-PRJ-05)

1. One row per Project: name, date of the last change, length.
2. A tap opens the Project. The open Project is saved first.
3. The menu of a row offers: rename, duplicate, delete. Delete asks first.

### 15.3 Settings

| Group | Content | Priority |
|---|---|---|
| Recording | Offset for latency in milliseconds, calibration, count-in (FR-SET-03) | P1 |
| Extraction | API key, state of the setup, budget per job, jobs per day, cost per month, cost so far, deletion of all jobs and their data (FR-SET-04). From version 1.1 on, the same values serve the Decomposer (FR-DEC-22). | P1 |
| Audio | A text: the app follows the audio device that Android uses (FR-SET-05) | P0 |
| About | Version of the app | P0 |

The field for the API key hides its characters. The app never shows a stored key again. It shows only whether a key is stored (NFR-SEC-09).

### 15.4 Diagnostics view

| Part | Content |
|---|---|
| Values of the engine | Sample rate, Burst, size of the buffer, audio path, count of Underruns, load of the audio thread, number of Voices (FR-SET-01) |
| Counts | The counts of the telemetry (`architecture.md` Section 5.10) |
| Errors | The last errors of the log, and the reason of the last exit |
| Share | Sends the log file through the share sheet |
| Measurements, in test builds only | One button per measurement of the manual tests in `feature_specifications.md` Section 9 (D-073). One row per test that the owner judges by ear or by eye, with a mark for the verdict (D-094). A button shares the results as a file. |

---

## 16. First launch, empty states and texts

### 16.1 First launch

1. The workspace appears at once. No introduction, no sign-in and no picker comes first (D-019).
2. The app opens a new Project. `feature_specifications.md` defines its content: one Track, one Pattern with one Part, the default Sampler instrument.
3. The Piano roll is the active Module. The Pencil is on. A tap on a key sounds.
4. No dialog asks for a permission. The microphone permission is asked at the first Take.

### 16.2 Empty states

| Place | Text |
|---|---|
| Lanes without Clips | "Double tap to add the open Pattern. Drag a Sample here." |
| Group of the Sample browser without Samples | "No Samples yet. Record a Take or import a file." |
| Channel Rack without Parts | "Add a Part to enter beats." |
| Page "Jobs" without jobs | "No jobs yet." |
| Decomposer without a source | "Pick a file, a link or a Sample." |

### 16.3 Important Hints

| Event | Text |
|---|---|
| A move would overlap another Clip | "Clips on one Track cannot overlap." |
| A limit of `requirements.md` Section 8 is reached | "Limit reached: 8 Tracks." The text names the limit. |
| A Note would lie behind the end of the Pattern | "The Pattern ends here. Use Length to extend it." |
| A Take was placed | "Take 5 added to Track 2." |
| The conversion found no Notes | "No Notes found. The Take is in Recordings." |
| A Sample is missing | "1 Sample is missing. Its Clips are silent." |
| Repeated Underruns | "The audio stutters. Fewer effects or Tracks can help." |
| Free storage is low | "Storage is almost full." |
| Bluetooth output during a Take | "With Bluetooth the Take can lie late." |
| An export had to limit values | "312 frames were limited. Lower the Master fader." |

---

## 17. Checks of the UI

| What | How | Where |
|---|---|---|
| Geometry and hit tests of every editor | Automatic tests (`architecture.md` Section 6.3) | Host |
| Gestures: scroll against edit, hold, handles, pinch | Automatic tests with recorded touches | Host |
| Every row of the tables of gestures in Sections 4 to 12 | One automatic test per row, where no finger is needed | Host |
| Every rule and every gesture of Section 19 | One automatic test per rule, where no finger is needed | Host |
| Frame timing of scroll and zoom (NFR-PERF-07) | Measurement inside a test build (D-073) | Phone |
| Sizes of touch areas (NFR-UX-02) | A test that reads the sizes of all controls | Host |
| Reach of the thumbs, display cutout, Back gesture at the edges (NFR-UX-03), readable text | Manual test of the owner | Phone |
| Number of taps for the actions of NFR-UX-01 | Review against Section 3 | — |

`feature_specifications.md` lists the manual tests of the owner with steps and pass rules.

---

## 18. Decisions and open points

| Topic | Decision | Section |
|---|---|---|
| Layout of the workspace: global bar on top, Module strip at the outer edge, sizes | D-076 | 2, 3 |
| Mixer: a layer in the sizes Half and Full, with a Channel page | D-077 | 10 |
| Effects: one Channel page and one editor, shown in the Module and in the Mixer | D-078 | 11 |
| Gestures: select first, then act. A Pencil switch in the Piano roll. No other tool modes. | D-079 | 4.2, 6.3, 14.1 |
| Content of a new Project | D-080 | 16.1 |
| Second main screen with its own layout; place of the Mode switch | D-104 | 3, 19 |

The texts are English. This is the default of Q-10 (D-026, A-17).

Open points:

1. All sizes are provisional. The check V-01 measures the screen. One review of the sizes follows.
2. Icons are not drawn yet. The implementer uses a free icon set with a licence that D-024 allows.
3. The exact texts of dialogs and of the settings are written during implementation. Section 16 fixes the style: short, plain, without blame.

---

## 19. Decomposer (version 1.1)

The Decomposer is the second main screen of the app (FR-DEC-01, D-104). It has its own layout. `feature_specifications.md` Section 12 fixes the functions behind its controls.

### 19.1 Mode switch

| Topic | Rule |
|---|---|
| Place in the Studio | The global bar, between Swap and Mixer (Section 3) |
| Place in the Decomposer | The Decomposer bar, at the same place of the screen. Two taps at one place lead there and back. |
| What it shows | In the Studio: the icon of the Decomposer. In the Decomposer: the icon of the Studio. A small mark appears when a Decomposition job is ready or has failed. |
| Tap | Swaps the screen at once (FR-DEC-01) |
| During a Take | The Mode switch is dim and does nothing (FR-DEC-02) |

Rules:

1. A swap to the Decomposer stops playback. It closes an open sheet. An open Mixer stays open and shows again after the return.
2. Each screen keeps its state: the Project, the layout, the scroll positions, the fields of a request and the shown job (FR-DEC-02).
3. The Back gesture on the bare Decomposer returns to the Studio.
4. Every launch shows the Studio (FR-PRJ-01).
5. The swap has no animation that lasts longer than 150 ms.

### 19.2 Zones

```text
+----------------------------------------------------------------------------------+
| Decomposer bar (44)                                                              |
+------------------------------------------------+---------------------------------+
| Source row (48)                                | Tabs "New" and "Jobs" (40)      |
+------------------------------------------------+---------------------------------+
| Waveform with two handles                      | Page                            |
|                                                |                                 |
+------------------------------------------------+                                 |
| Range row (48)                                 |                                 |
+------------------------------------------------+---------------------------------+
  Source area                                      Side area
```

| Zone | Size (prov.) | Content |
|---|---|---|
| Decomposer bar | full width × 44 | The title "Decomposer", marks for a running job, the Mode switch, the Menu button |
| Source area | 60 % of the width | Source row, waveform, range row (Section 19.3) |
| Side area | 40 % of the width | The pages "New" and "Jobs". The review of a result takes this area while a job is open. |

The Menu button opens the Project menu of Section 15.1. The settings are therefore one tap away. The areas keep clear of the display cutout (FR-WS-11).

### 19.3 Source area

| Row | Content | Requirement |
|---|---|---|
| Source row | Three buttons: "File", "Link", "Sample". A shared file arrives here too. After the choice, the row shows the name and the length of the recording. | FR-DEC-03 |
| Waveform | For a source on the phone: the waveform of the recording with two handles. The part outside the range is dim. A Play button plays the range. For a link: a plain bar with the two times. | FR-DEC-04, FR-DEC-06 |
| Range row | The fields "Start" and "End" in minutes and seconds. The length of the range. The button "Whole file". | FR-DEC-04, FR-DEC-05 |

Gestures on the waveform:

| Gesture | Place | Effect |
|---|---|---|
| Drag | A handle | Moves the start or the end of the range |
| Drag | Between the handles | Moves the whole range |
| Drag | Outside the handles | Scrolls the waveform |
| Pinch | Anywhere | Zooms in time |
| Tap | Anywhere | Moves the playhead of the preview |

Rules:

1. The handles and the fields always show the same range.
2. A range shorter than 5 s or longer than 240 s is refused. The length then takes the warning color. A line names the limit (FR-DEC-05).
3. "Link" opens the dialog of Section 12.1. Another link than a direct link to an audio file gets the same message (D-105).
4. If the link ends with `#t=start,end`, the app fills both fields from it (FR-DEC-04).
5. Each handle has a touch area of 48 dp.
6. "Whole file" sets the range to the whole recording. The button is dim when the recording is longer than 240 s.
7. Without a source, the waveform shows the text "Pick a file, a link or a Sample."

### 19.4 Page "New"

| Row | Content | Requirement |
|---|---|---|
| What you get | The line "Stems as Audio clips". Two switches: "Notes for the drums" and "Notes for the bass". Both are on at first. | FR-DEC-07 |
| What it needs | What is left: the budget of the job and the jobs of today. An estimate of the time. An estimate of the size of the download: 0.4 MB for each second of the range (prov.). | FR-DEC-22 |
| Foot | The button "Decompose" | FR-DEC-09 |

Rules:

1. The button is dim while a part is missing. A line below it names the missing part.
2. The five blocking states of Section 12.1 hold here too. The state "Setup on the account missing" asks for version 2 of the setup.
3. The notice about the rights appears before the first job of either kind (FR-DEC-23).

### 19.5 Page "Jobs"

One row per Decomposition job, 56 dp high: the name of the source, the range, the state, the elapsed time. An active job has a Cancel button (FR-DEC-11).

| State shown | Meaning | Tap opens |
|---|---|---|
| Waiting | Another job runs first | Cancel |
| Uploading | The phone prepares and sends the audio | Progress |
| Running | The Session works | Elapsed time and Cancel |
| Downloading | The result arrives. The row shows its size. | Progress |
| Ready | A result waits for the review | The review (Section 19.6) |
| Failed | The job ended with an error | The cause and a Retry button (FR-DEC-21) |
| Cancelled | The user stopped the job | A Delete button |

Rules:

1. The page lists Decomposition jobs only. Extraction jobs stay in the Module Extraction (Section 12.2).
2. "Delete" asks first. It removes the files of the job. The Samples in the Library and the Projects from this result stay.
3. Without jobs, the page shows the text "No jobs yet."

### 19.6 Review of a result

| Part | Content |
|---|---|
| Tempo | A field with the tempo. The buttons "÷2" and "×2". "First bar" with the buttons − and +. The switch "Snap to 1/16" (FR-DEC-12, FR-DEC-17). |
| Notes about the tempo | The texts of `feature_specifications.md` Section 12.4 that apply |
| Parts | One row for each stem and for each Hit group: a Play button, the name, and the level or the number of hits |
| Original | A Play button for the range. Only for a source on the phone. |
| What you get | The two switches for Notes. A part that the result does not hold is dim. |
| Limits of a result | The sentence of FR-DEC-23 |
| Cost | The cost of this job (NFR-COST-04) |
| Create Project | The button. During the work it shows the steps "Importing", "Bass Notes" and "Building", with a Cancel button. |
| After the creation | Two buttons: "Open in Studio" and "Create again" (FR-DEC-20) |

Rules:

1. A preview plays through the preview player of the Sample browser (FR-LIB-03). A second tap or another preview stops it.
2. A change of the tempo, of the first bar or of the snap needs no new job (FR-DEC-20).
3. "Open in Studio" loads the new Project and swaps to the Studio. The Project that was open is saved first.
4. "Create again" builds one more Project with the current settings.

### 19.7 Entries from outside

1. When another app shares an audio file, the sheet of Section 8.2 offers a third action: "Decompose". It opens the Decomposer with this file as the source (FR-DEC-03).
2. The menu of a Sample in the Sample browser offers "Decompose this Sample" (Section 8.2).
3. When the app is not visible, a job that becomes "Ready" or "Failed" shows a notification. A tap opens the Decomposer with this job (FR-DEC-11).
