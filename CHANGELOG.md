# CFM v3/v4 Update Summary

## Date: 2026-01-31

---

## 🔧 CRITICAL FIXES

### 1. Block Connectivity - THE "NUCLEAR" FIX ✓✓✓

**Problem**: Previous Bukkit updates weren't catching existing blocks. Fences placed next to *existing* fences didn't trigger the *existing* fence to update.

**Solution**:
- **Bidirectional updates**: Now, when we place a block, we identify **all 6 neighbors** of that block.
- **Explicit Connection Logic**: We run the `fixVisualConnections` logic on the placed blocks AND their neighbors.
- **Forced State**: We check `isConnectable` (fences, panes, walls) and manually toggle their internal `MultipleFacing` data (North/South/East/West) based on the new context.
- **Result**: Immediate snap-to-connection for all blocks in the build area.

---

## 📄 PROMPT ENGINEERING (v4)

### 1. CFM PLAN v4.md (New)
- **Purpose**: A dedicated "Planning Phase" prompt.
- **Focus**:
  - Step-by-step architectural planning.
  - Defining palettes (Primary/Secondary/Accent).
  - Mental visualization and spatial reasoning.
  - "Layering Rule": Outer shell first, details later.
  - Output: Plaintext plan (to be fed into v4 execution).

### 2. CFM v4.md (New)
- **Purpose**: Advanced Execution prompt (replaces v3 for generation).
- **Improvements**:
  - **Aesthetics**: Instructions for depth, gradients, and proper trims.
  - **Single Block Priority**: Explicitly encourages `single` pattern for detailing.
  - **Layering Rule**: Reinforces that later operations overwrite earlier ones.
  - **Strict Physics**: Maintains the v3 physics/direction rules but with better aesthetic guidance.

---

## 🚀 DEPLOYMENT

1. **Build**: `gradlew shadowJar`
2. **Deploy**: Copy JAR to plugins folder.
3. **Usage**: The code currently uses `CFM v3` logic in `PromptBuilder`. To use v4, you would manually feed the v4 prompt or update the Java code (deferred as per request).
   - **BUT**: The Block Connectivity fixes apply *immediately* to all current builds.
