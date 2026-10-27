# Apertura Faena Climate and Operations Cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update the Apertura de Faena screen to show clearly simulated acceptable weather measurements and compact operational/buzo cards inspired by the supplied references.

**Architecture:** Keep changes in the existing Compose screen; preserve existing center and buzo dropdowns, automatic date/time, and supervisor state used internally. Weather values are fixed presentation references, identified with a neutral note and kept separate from the required real-world meteorology confirmation.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Compose UI tests, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-25-apertura-faena-clima-y-datos-design.md`

## Global Constraints

- No real telemetry/API integration.
- Fixed presentation values must not be represented as verified measurements, authorization, or safety criteria.
- Preserve existing center and buzo selection and automatically recorded date/time.
- Hide the supervisor field from this screen, but retain its underlying value and validation.
- Keep vessel and communications checks and existing opening rules unchanged.

## Review Focus

- Weather card: verify the given wind and tide values, neutral reference note, bold operational-limit label, and green APTA badge.
- Selectable fields: verify center and buzo dropdowns remain available.
- Date/time: verify the automatic device date and time remain visible.
- Supervisor privacy in the form: verify the session supervisor label is no longer rendered, while existing tests preserve supervisor behavior.
- Safety confirmation: verify the old “Mar y meteorología aptas” label is absent, a new real-world verification checkbox is present and still gates continuation, and demo measurements do not replace it.

---

### Task 1: Update weather card and operations/buzo layout

**Files:**
- Modify: `app/src/main/java/com/duoc/aquaflow/ui/screens/AperturaFaenaScreen.kt`
- Modify: `app/src/androidTest/java/com/duoc/aquaflow/AperturaFaenaUiTest.kt`
- Test: `app/src/androidTest/java/com/duoc/aquaflow/NavegacionFaenaUiTest.kt`
- Test: `app/src/test/java/com/duoc/aquaflow/AperturaFaenaTest.kt`

**Interfaces:**
- Consumes: Existing `FaenaUiState.apertura`, `PreChequeo`, `DropdownField`, and `AquaSectionCard`.
- Produces: Climate demo panel labeled `Condiciones Climáticas`, data cards retaining current selectable center and buzo, visible automatic date/time, and no visible supervisor field or weather checkbox.

- [ ] **Step 1: Write failing Compose UI assertions**

Update `AperturaFaenaUiTest` to assert:
- Header is `Condiciones Climáticas`.
- Weather values show `12 NUDOS SO`, `LÍMITE OPERATIVO < 20 kts` with the label in bold, `Favorable`, and `Reposo marea alta (09:45)`.
- A neutral note says `Valores referenciales de presentación; verificar en terreno antes de operar.` and the `APTA` status is green.
- `Mar y meteorología aptas` and `SIN DATOS` are absent; `Confirmo la verificación meteorológica real en terreno` remains unchecked until selected.
- Center and buzo selector labels/options remain available; automatic date/time remains visible.
- `Supervisor de la sesión` is absent.

- [ ] **Step 2: Run the focused UI test to verify RED**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.duoc.aquaflow.AperturaFaenaUiTest'`
Expected: FAIL on the old climate label/placeholder content and still-visible weather/supervisor fields.

- [ ] **Step 3: Implement climate demo values and operational cards**

In `AperturaFaenaScreen.kt`, update the climate header to `Condiciones Climáticas`, show the specified fixed reference wind/tide metrics, display the neutral note and green `APTA` badge, and render `LÍMITE OPERATIVO` in bold. Keep the mandatory `Confirmo la verificación meteorológica real en terreno` confirmation bound to the existing `condicionesListas` and opening validation. Replace the data section with compact fields: full-width center dropdown, then automatic date and time side-by-side. Use a separate compact buzo card with the existing buzo dropdown. Remove the supervisor text field, retaining the underlying data flow. Do not add a tide-shift selector or fabricate buzo identity details.

- [ ] **Step 4: Verify focused UI tests**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.duoc.aquaflow.AperturaFaenaUiTest'`
Expected: PASS for demo copy and values, field retention, layout, and supervisor-field absence.

- [ ] **Step 5: Verify navigation and opening rules**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.duoc.aquaflow.NavegacionFaenaUiTest'`
Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.duoc.aquaflow.AperturaFaenaTest`
Expected: PASS; center/buzo selection, vessel/comms checklist, supervisor ownership, and validation remain intact.
