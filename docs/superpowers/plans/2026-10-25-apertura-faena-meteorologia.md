# Apertura Faena Meteorology Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move weather readiness out of “Condiciones iniciales” into a prominent weather card above the form, without representing unavailable telemetry as real.

**Architecture:** Keep this as a UI-only change in the existing Compose screen. Retain the existing `PreChequeo.condicionesListas` behavior through a manual confirmation alongside the weather card; report measurement values as unavailable until a real source exists.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Compose UI tests, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-25-apertura-faena-meteorologia-design.md`

## Global Constraints

- No fictitious values or claims of real-time telemetry.
- Preserve the current opening validation and persistence model.
- Do not add telemetry integrations, permissions, or dependencies.

## Review Focus

- No telemetry configured: both measurement sections must communicate unavailable values; assert in the UI test.
- Weather readiness remains unconfirmed until manually checked; assert on initial and selected state.
- Existing required operational checklist stays functional; cover with the navigation test.

---

### Task 1: Add prominent weather card and move readiness confirmation

**Files:**
- Modify: `app/src/main/java/com/duoc/aquaflow/ui/screens/AperturaFaenaScreen.kt`
- Test: `app/src/androidTest/java/com/duoc/aquaflow/AperturaFaenaUiTest.kt`
- Test: `app/src/androidTest/java/com/duoc/aquaflow/NavegacionFaenaUiTest.kt`

**Interfaces:**
- Consumes: `FaenaUiState.apertura.condicionesListas`, `PreChequeo`, `AquaSectionCard`, and the screen's existing `onCambiar`.
- Produces: A weather card above “Datos de la faena” with unavailable status for surface wind and tide; its readiness confirmation updates `condicionesListas` through `onCambiar`.

- [x] **Step 1: Add failing Compose UI assertions**

Create `AperturaFaenaUiTest` using `createComposeRule()` and a default `FaenaUiState`. Assert that “Condiciones en Pontón”, “Viento Superficie”, “Estado Marea”, and “Sin medición disponible” are visible before “Datos de la faena”; assert the weather readiness checkbox begins unchecked and updates the supplied `PreChequeo` when clicked.

- [x] **Step 2: Run the focused UI test**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.duoc.aquaflow.AperturaFaenaUiTest'`
Expected: FAIL because the weather card and moved confirmation do not exist.

- [x] **Step 3: Implement the weather card**

In `AperturaFaenaScreen.kt`, add a Compose card matching the reference hierarchy (header/status, two metric panels), show “Sin medición disponible” instead of sample values, and place it above “Datos de la faena”. Move “Mar y meteorología aptas” confirmation into that card. Leave only the vessel and communications rows in “Condiciones iniciales”. Avoid claims of “tiempo real” or “mar seguro”.

- [x] **Step 4: Verify focused UI and navigation tests**

Run: `.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.duoc.aquaflow.AperturaFaenaUiTest,com.duoc.aquaflow.NavegacionFaenaUiTest'`
Expected: PASS, including the existing navigation flow that checks all initial conditions.

- [x] **Step 5: Run opening unit tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.duoc.aquaflow.AperturaFaenaTest`
Expected: PASS; existing readiness validation remains unchanged.
