# DRS OS Phase Plan

Status ledger and forward plan. This file is updated at the end of each
phase. It deliberately separates what is **done** from what is **proposed**;
proposed phases are not started without explicit project-owner
authorization.

## Phase 0 — Environment & feasibility audit (COMPLETE, 2026-10-16)

Report: `download/DRS_OS_Phase0_Environment_Audit.docx` (workspace export
area; 22 pages, evidence-based).

- Measured: Debian 13 container, 2 vCPU x86-64, 4.06 GiB RAM (4 GiB cgroup
  hard limit, no swap), 9.3 GiB free on 9.9 GiB overlay rootfs, no
  /dev/kvm, no sudo, git 2.47.3, Python 3.12.15, OpenJDK 21 JRE only,
  Repo launcher absent.
- Verified from official sources: AOSP requires ≥ 64 GB RAM, ≥ 400 GB disk,
  64-bit Linux; the android-latest-release manifest resolves to revision
  `android17-release`; newest published tag android-17.0.0_r1.
- Verdict: full AOSP checkout/build **infeasible in the sandbox**;
  feasible after preparation on a compliant user-provided host. Zero-cost
  cloud alternatives reviewed; none close the gap; trial credits excluded
  (payment instrument required).
- Workspace recovery note (Phase 1): the sandbox was reset between
  sessions; the `drs-os/` directory did not exist at Phase 1 start and was
  recreated from the Phase 1 specification and Phase 0 baseline. Phase 0
  measurements were re-verified at Phase 1 start (same host profile: 2
  vCPU, 4.1 GiB RAM, 9.2 GiB free disk, git 2.47.3, Python 3.12.15).

## Phase 1 — Scaffolding, architecture & validation tooling (COMPLETE)

This repository. Delivered: repository skeleton with substantive
documentation (architecture, security, compatibility, build), four ADRs,
local-manifest template, branding/design-system specifications with
verified WCAG contrast ratios, and six static validation scripts, all
executed with real exit statuses recorded in the Phase 1 report. No AOSP
download, no build, no flashing, no paid resources, no external
publication.

## Phase 2 — Integration readiness, native design & security foundation (COMPLETE)

Delivered (all changes to the existing scaffold; nothing discarded):

- **Placeholder hazard remediation:** the executable-shaped placeholder
  manifest was moved out of the production path to
  `manifests/templates/drs-os-local.xml.template` (non-executable by
  extension); `manifests/local_manifests/` is now empty-by-policy;
  `check_manifests.sh` hardened to REJECT placeholder remotes in
  production scope (negative control demonstrated: exit 1).
- **Integration design:** integration map with 4-way classification
  (upstream / DRS-existing / proposed / hardware-dependent),
  upstream-sync & pinning procedure, dependency-provenance policy,
  integration-readiness report (docs/build/integration-readiness.md).
- **Security foundation:** control ledger re-based on the 5-status
  taxonomy (upstream capability / DRS design requirement /
  implementation pending / test pending / verified implementation),
  implementation checklist with evidence-gated milestones, security
  test plan (20 tests, verified-vs-planned stated honestly).
- **Native UI specification:** 12 system surfaces with upstream
  components, integration points, lifecycle, RTL, a11y, performance,
  and pre-integration tests (docs/ui/native-system-surfaces.md).
- **Deterministic automation:** rule-engine specification + Python
  reference model + 25 unit tests — executed green after catching and
  fixing 3 real test defects; no AI anywhere (ADR-0004).
- **Tooling hardening:** unsafe-command scan, endpoint allowlist,
  automation sub-check, honest-tripwire disclaimers.
- **Audit findings recorded:** sandbox mode-bit drift (644→755 across
  all files, content identical) detected via git status and corrected;
  validation baseline reproduced before changes.

Blocked-on (honest): compliant build host; DRS repository
authorization. No AOSP download/build, no flashing, no paid services,
no external publication.

## Phase 3 — Checkout & first boot (PROPOSED — requires authorization)

Goal: execute the gated checkout on a compliant host. Prerequisites:
host checklist passed, DRS remotes authorized and verified, branch
re-verified, manifest SHA pinned. Proposed work items:

1. **Host preparation checklist run-through** (docs/build/prerequisites.md)
   on the target development host; record tool versions into the decision
   log; no downloads in this sandbox.
2. **Manifest finalization once remotes exist.** DRS-owned Git repositories
   do not exist yet; creating them needs authorization. When authorized:
   create repositories, replace the `.invalid` placeholders in
   `manifests/local_manifests/drs-os-local.xml`, re-run
   `build/scripts/check_manifests.sh`, and pin explicit revisions.
3. **Branch re-verification at integration time.** Confirm that
   `android-latest-release` still resolves to the expected revision
   (Phase 0 verified `android17-release` on 2026-10-16); record the exact
   manifest commit SHA that will be pinned (ADR-0001).
4. **Cuttlefish target confirmation.** Confirm the exact lunch target
   string from the checked-out tree (Phase 0 did not invent one; the AOSP
   documentation lists Cuttlefish targets, but the authoritative string
   must be read from the tree before pinning).
5. **Static tooling hardening.** Extend the secret scan and manifest
   validation based on lessons from the first real local-manifest; add
   device-layer template checks only when device files actually appear.
6. **Design-spec deepening.** Convert the Security & Privacy Center and
   Quick Settings specifications into concrete overlay resource plans
   (string/resource inventories) so implementation can start immediately
   after the first successful build.
7. **Security test scaffolding** under tests/security/ (checklist-based
   review gates for future SELinux and permission changes).

Explicitly out of scope for Phase 2: full AOSP sync (requires compliant
host + authorization), any signing-key generation, any public repository
publication, any cloud account creation.

## Phase 3–6 — Alpha applications, packaging & distribution (COMPLETE — RE-EXECUTED, 2026-10-17)

**Honest history:** phases 3–6 were originally executed in a prior session
(version 0.3.0-phase3, tag `phase-6-distribution` in that workspace) and
delivered to the user; a workspace reset then reverted the environment to
its post-Phase-2 state (second reset recorded in the project worklog;
reflog/dangling objects show no trace of the later commits in this copy).
The phase was **re-authored from scratch** in this session as version
`0.3.1-phase3`, incorporating screen-defect fixes the user reported with
device screenshots.

Delivered:

- **Three zero-permission Kotlin/Compose apps** under `packages/apps/`
  (DRSSettings, DRSAutomation, DRSSecurityCenter) — compileSdk 35 /
  minSdk 29 / targetSdk 35, Kotlin 2.0.21, Compose BOM 2024.12.01,
  Arabic-first RTL (layout direction forced RTL), dark/light theme parity
  from branding/colors.md tokens.
- **Screen-defect fixes** (from user screenshots of 0.3.0): clipped
  unlabeled top field in the rule editor → labeled OutlinedTextFields in
  a scrollable inset-aware column; oversized teal pill on the main screen
  → fixed-height (52dp) create button; Latin chip labels breaking
  mid-word → single-line chips.
- **Deterministic rule engine** implemented per
  docs/automation/rule-engine-spec.md (closed action registry, fixed
  decision order, midnight-spanning windows) + **15 unit tests, all
  passing**; actions recorded as audit entries, not executed (honest
  alpha boundary); time trigger honestly disabled in UI.
- **Verification gate** (build/scripts/make_installation_package.sh):
  per-APK hard-fail checks (package name, versionName, minSdk/targetSdk,
  Arabic label, zero android.permission.*, zero foreign uses-permission,
  apksigner verify, zip sanity) + repository validators + automation
  reference tests — all PASS.
- **Installation package**:
  `drs-os-0.3.1-phase3-alpha-installation-package.zip`
  (24,478,286 bytes; sha256 3083428db2669926c024e100e63330d7f50a0c150bd29fdaf432057ff96e39a5)
  with SHA256SUMS.txt (extraction re-verified 11/11), Arabic-first
  INSTALL.md, RELEASE_NOTES.md, LICENSE/NOTICE, and per-APK badging
  reports.
- **Distribution**: public repository `drs-os-platform` (owner CTO-DRS)
  with the source tree and a pre-release GitHub Release carrying the
  package. Upload performed with a user-supplied token via transient
  environment variable only; the token is the same over-scoped classic
  PAT previously exposed in chat — revocation advised repeatedly and is
  REQUIRED after this upload.

Honest labels: **BUILD VERIFIED — RUNTIME NOT VERIFIED** (debug-signed,
evaluation only). No AOSP checkout/build, no flashing, no paid services.

## Standing rule

No phase transition happens automatically. Each phase ends with an explicit
report and waits for authorization.
