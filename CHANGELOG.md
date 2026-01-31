# CFM v3/v4 Update Summary

## Date: 2026-01-31

---

## 🔧 CODE FIXES (BlockPlacementEngine.java)

### 1. Stairs & Directional State Fix ✓
- **Problem**: Stairs always faced North, ignoring LLM input.
- **Cause**: WorldEdit's legacy parser was failing on `[facing=east]` and defaulting to North.
- **Fix**: Switched to `Bukkit.createBlockData()` which fully supports modern 1.13+ state syntax.
- **Result**: Stairs now correctly face North/South/East/West as requested.

### 2. Auto-Door Functionality ✓
- **Problem**: Lower/Upper door halves invalid or missing.
- **Fix**: Engine now detects placement of a Lower Door (`half=lower`).
- **Automation**: It **automatically** generates and places the correct Upper Door (`half=upper`) at `y+1`.
- **Simplification**: LLM prompt updated to ONLY request the lower half.

### 3. "Nuclear" Connection Fix (Neighbors) ✓
- **Refinement**: Explicitly updates neighbors of placed blocks (bidirectional) to ensure snapping.
- **Logic**: Uses `MultipleFacing` data to force-set invisible connection states.

---

## 📄 PROMPT ENGINEERING (v4)

### 1. CFM v4.md (Execution)
- **Simplify Doors**: Instructions updated to *only* output lower door half.
- **Vertical Levels**: Explicit warnings about "Floor Thickness" vs "Furniture Height" to prevent buried objects.
- **Aesthetics**: Stronger emphasis on `single` blocks for detailing.

### 2. CFM PLAN v4.md (Planning)
- **Vertical Planning**: Added "Interior Floor Y" field to the planning phase to force the AI to calculate furniture height before building.

---

## 🚀 DEPLOYMENT

1. **Build**: `gradlew shadowJar`
2. **Deploy**: Copy JAR to plugins folder.
3. **Usage**:
   - Connections: Work automatically.
   - Stairs/Doors: Work automatically with current code.
   - Prompts: Use v4 prompts for best results (manual input or update code later).
