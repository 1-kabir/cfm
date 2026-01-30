# CFM v3 Test Cases

## Test Environment Setup
1. Build the plugin: `gradlew shadowJar`
2. Copy to server: `plugin/build/libs/CFM-1.0-SNAPSHOT-all.jar`
3. Restart server
4. Have WorldEdit installed and permissions configured

---

## Test 1: Fence Connectivity ✓

**Objective**: Verify fences connect to adjacent fences automatically

**Steps**:
1. Use `/cfm create build a straight oak fence line 5 blocks long`
2. Expected JSON from LLM:
```json
{
  "metadata": {"name": "Fence Line", "workflow": "patterns"},
  "blocks": [
    {
      "type": "minecraft:oak_fence",
      "pattern": "line",
      "x1": 0, "y1": 0, "z1": 0,
      "x2": 4, "y2": 0, "z2": 0
    }
  ]
}
```

**Expected Result**:
- Fences appear connected (center post with horizontal bars to neighbors)
- NO isolated fence posts
- Opening/closing gates works if present

**Verification**: Walk along fence, visually confirm connections

---

## Test 2: Glass Pane Connectivity ✓

**Objective**: Glass panes connect to adjacent panes

**Steps**:
1. Use `/cfm create build a 3x3 glass pane window`
2. Expected JSON:
```json
{
  "blocks": [
    {
      "type": "minecraft:glass_pane",
      "pattern": "flat",
      "x1": 0, "y1": 0, "z1": 0,
      "x2": 2, "y2": 2, "z2": 0
    }
  ]
}
```

**Expected Result**:
- Panes should form connected window (no cross patterns on edges)
- Edge panes appear flat
- Interior panes connect on all adjacent sides

---

## Test 3: Door Placement & Opening ✓

**Objective**: Doors placed correctly and don't break when opened

**Steps**:
1. Use `/cfm create build a simple oak door`
2. Expected JSON:
```json
{
  "blocks": [
    {
      "type": "minecraft:oak_door[facing=north,half=lower]",
      "pattern": "door",
      "x1": 0, "y1": 0, "z1": 0
    },
    {
      "type": "minecraft:oak_door[facing=north,half=upper]",
      "pattern": "door",
      "x1": 0, "y1": 1, "z1": 0
    }
  ]
}
```

**Expected Result**:
- Door appears with both halves
- Right-click to open: Door swings open smoothly
- Right-click to close: Door returns to closed position
- Door does NOT break or drop as item when opened

**Critical**: If door breaks on opening, the validation failed

---

## Test 4: Door Stacking Prevention ✓

**Objective**: System prevents doors from being placed on top of other doors

**Steps**:
1. Create JSON with invalid door stacking:
```json
{
  "blocks": [
    {
      "type": "minecraft:oak_door[facing=north,half=lower]",
      "pattern": "door",
      "x1": 0, "y1": 0, "z1": 0
    },
    {
      "type": "minecraft:oak_door[facing=north,half=upper]",
      "pattern": "door",
      "x1": 0, "y1": 1, "z1": 0
    },
    {
      "type": "minecraft:oak_door[facing=north,half=lower]",
      "pattern": "door",
      "x1": 0, "y1": 2, "z1": 0
    }
  ]
}
```

**Expected Result**:
- Only first door (lower + upper) placed
- Third block (stacked door) skipped with warning in console
- Check console: `Skipping invalid door placement at BlockVector3...`

---

## Test 5: Directional Stairs ✓

**Objective**: Stairs face correct direction based on facing state

**Steps**:
1. Use `/cfm create build oak stairs ascending east for 5 blocks`
2. Expected JSON:
```json
{
  "blocks": [
    {
      "type": "minecraft:oak_stairs[facing=east,half=bottom]",
      "pattern": "line",
      "x1": 0, "y1": 0, "z1": 0,
      "x2": 4, "y2": 0, "z2": 0
    }
  ]
}
```

**Expected Result**:
- Stairs should ascend from west to east
- Standing at X=0, you can walk UP the stairs toward X=4
- Stair fronts face EAST (+X direction)

**Test All Directions**:
- North (facing=north): Ascends toward -Z
- South (facing=south): Ascends toward +Z
- East (facing=east): Ascends toward +X
- West (facing=west): Ascends toward -X

---

## Test 6: Farmland & Crops ✓

**Objective**: Crops placed on top of farmland, not before it

**Steps**:
1. Use `/cfm create build a small wheat farm`
2. Expected JSON (proper order):
```json
{
  "blocks": [
    {
      "type": "minecraft:farmland[moisture=7]",
      "pattern": "flat",
      "x1": 0, "y1": 0, "z1": 0,
      "x2": 3, "y2": 0, "z2": 3
    },
    {
      "type": "minecraft:wheat[age=7]",
      "pattern": "flat",
      "x1": 0, "y1": 1, "z1": 0,
      "x2": 3, "y2": 1, "z2": 3
    }
  ]
}
```

**Expected Result**:
- Farmland appears at Y=0
- Wheat appears on TOP at Y=1
- Wheat does NOT pop off as items

**Failure Case**: If farmland is at Y=1 and wheat at Y=0, wheat will break

---

## Test 7: Double-Tall Plants ✓

**Objective**: Tall flowers have both halves placed correctly

**Steps**:
1. Use `/cfm create build a peony flower`
2. Expected JSON:
```json
{
  "blocks": [
    {
      "type": "minecraft:peony[half=lower]",
      "pattern": "single",
      "x1": 0, "y1": 0, "z1": 0
    },
    {
      "type": "minecraft:peony[half=upper]",
      "pattern": "single",
      "x1": 0, "y1": 1, "z1": 0
    }
  ]
}
```

**Expected Result**:
- Complete peony flower with lower and upper halves
- Breaking lower half breaks both parts
- No missing or floating halves

---

## Test 8: Wall Connectivity ✓

**Objective**: Cobblestone walls connect properly

**Steps**:
1. Use `/cfm create build a cobblestone wall 4 blocks long`
2. Expected JSON:
```json
{
  "blocks": [
    {
      "type": "minecraft:cobblestone_wall",
      "pattern": "line",
      "x1": 0, "y1": 0, "z1": 0,
      "x2": 3, "y2": 0, "z2": 0
    }
  ]
}
```

**Expected Result**:
- Walls appear connected with proper center posts
- No isolated wall segments
- Wall tops align properly

---

## Test 9: Iron Bars Connectivity ✓

**Objective**: Iron bars connect like fences

**Steps**:
1. Create a 2x2 grid of iron bars
2. Expected JSON:
```json
{
  "blocks": [
    {
      "type": "minecraft:iron_bars",
      "pattern": "flat",
      "x1": 0, "y1": 0, "z1": 0,
      "x2": 1, "y2": 0, "z2": 1
    }
  ]
}
```

**Expected Result**:
- Bars connect to form continuous grid
- No isolated bars

---

## Test 10: JSON Extraction from LLM ✓

**Objective**: System extracts JSON from markdown-wrapped responses

**Test Inputs**:

1. **Wrapped in code block**:
````
```json
{"metadata": {...}, "blocks": [...]}
```
````

2. **With explanation before**:
```
Here's your build:
{"metadata": {...}, "blocks": [...]}
```

3. **Plain JSON**:
```
{"metadata": {...}, "blocks": [...]}
```

**Expected Result**: All three formats parse successfully

---

## Test 11: Complex Structure Integration ✓

**Objective**: Build a complete cottage with all elements

**Prompt**: `/cfm create build a small oak cottage with door, windows, fence, and stairs`

**Verification Checklist**:
- [ ] Foundation placed first
- [ ] Walls/roof properly constructed
- [ ] Glass pane windows connected
- [ ] Door (2 blocks) opens/closes properly
- [ ] Stairs face correct direction
- [ ] Fence connects properly
- [ ] Overall structure looks correct
- [ ] No floating/broken blocks

---

## Known Issues to Monitor

1. **Chunk Boundaries**: Test builds spanning chunk borders
2. **WorldEdit Limits**: Test with max block limits
3. **Multiplayer**: Test concurrent builds by different players
4. **Server Lag**: Test during high server load

---

## Performance Benchmarks

**Target Metrics**:
- Small build (< 100 blocks): < 1 second
- Medium build (100-1000 blocks): < 3 seconds
- Large build (1000-5000 blocks): < 10 seconds

**Memory**: No memory leaks after 100+ builds

---

## Regression Tests

After any code change, re-run:
1. Test 3 (Door opening)
2. Test 1 (Fence connectivity)
3. Test 2 (Glass pane connectivity)
4. Test 5 (Stair directions)

These are the most critical functionality areas.
