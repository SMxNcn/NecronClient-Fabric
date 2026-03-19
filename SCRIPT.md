# Necron Script Syntax Documentation

## Basic Structure
Improved from [NecronClient Script](https://github.com/SMxNcn/NecronClient/blob/master/SCRIPT.md)

Script files use the standard **JSON** format and must be saved with a `.json` extension in the `config/odin/addons/scripts/` directory.

```json
{
  "name": "ScriptName",
  "triggerKey": "KEY_CODE",
  "enabled": true,
  "initialDelay": 0,
  "actions": [
    "// List of actions"
  ]
}
```

## Configuration Options

### Root Object Fields
| Field          | Type    | Description                                                                      | Example                  |
|:---------------|:--------|:---------------------------------------------------------------------------------|:-------------------------|
| `name`         | String  | Unique identifier for the script.                                                | `"WardrobeSwitch"`       |
| `triggerKey`   | String  | The key code to trigger the script.                                              | `"KEY_G"`, `"KEY_ENTER"` |
| `enabled`      | Boolean | Whether the script is currently active.                                          | `true`, `false`          |
| `initialDelay` | Integer | Initial delay (in milliseconds) before the first action starts after triggering. | `400`                    |
| `actions`      | Array   | An ordered list of action objects to execute.                                    | `[...]`                  |

## Action Instructions

All actions are JSON objects located within the `actions` array. Each action must include a `type` and typically a `delayAfter` field.

### Common Fields
- `type`: The type of action to perform (String).
- `delayAfter`: **Key Feature**. The time (in milliseconds) to wait **after** this action completes before executing the next one. This replaces the old standalone `-Delay` command.

### Supported Action Types

#### 1. SEND_COMMAND
Sends a command to the server. (No need to add `/` manually).
- **Parameter**: `message` (String) - The command content.
```json
{
  "type": "SEND_COMMAND",
  "message": "wardrobe",
  "delayAfter": 400
}
```

#### 2. CLICK_SLOT
Clicks a specific slot in the currently open container or inventory.
- **Parameter**: `slot` (Number) - The slot index (0-54+).
```json
{
  "type": "CLICK_SLOT",
  "slot": 44,
  "delayAfter": 150
}
```

#### 3. USE_KEY
**NOT COMPLETE YET**

Simulates pressing and releasing a specific key once (often used to close GUIs).
- **Parameter**: `keyCodeStr` (String) - The key code.
```json
{
  "type": "USE_KEY",
  "keyCodeStr": "KEY_E",
  "delayAfter": 0
}
```

#### 4. SEND_CHAT
Sends a normal chat message.
- **Parameter**: `message` (String) - The message content.
```json
{
  "type": "SEND_CHAT",
  "message": "Hello World!",
  "delayAfter": 0
}
```

#### 5. DELAY
Explicitly waits for a duration without performing other operations. (Usually, using `delayAfter` on the previous action is preferred).
- **Parameter**: `duration` (Integer) - Wait time in milliseconds.
```json
{
  "type": "DELAY",
  "duration": 1000,
  "delayAfter": 0
}
```

#### 6. SEND_CLIENT **WIP**
**NOT COMPLETE YET**

Sends a client-side notification message (supports color codes).
- **Parameter**: `message` (String) - The message content (supports `§` color codes).
```json
{
  "type": "SEND_CLIENT",
  "message": "§aScript execution complete!",
  "delayAfter": 0
}
```

## Complete Example

**Functionality**: Press **G** to automatically open the Wardrobe, then switch to Equipment #7, and finish.

```json
{
  "name": "WardrobeSwitch",
  "triggerKey": "KEY_G",
  "enabled": true,
  "initialDelay": 400,
  "actions": [
    {
      "type": "SEND_COMMAND",
      "message": "wardrobe",
      "delayAfter": 400
    },
    {
      "type": "CLICK_SLOT",
      "slot": 42,
      "delayAfter": 150
    },
    {
      "type": "CLICK_SLOT",
      "slot": 49,
      "delayAfter": 0
    }
  ]
}
```

### Execution Flow Analysis
1.  **Press G Key**.
2.  **Wait 400ms** (`initialDelay`).
3.  **Send Command** `/wardrobe` -> **Wait 400ms** (`delayAfter`) to ensure the GUI opens.
4.  **Click Slot 42** → **Wait 150ms** (`delayAfter`) to simulate human reaction time or server response.
5.  **Click Slot 49** → **End**.

## Common Key Code Reference
-   **Letters**: `"KEY_A"` through `"KEY_Z"`
-   **Numbers**: `"KEY_0"` through `"KEY_9"`
-   **Function Keys**: `"KEY_ENTER"`, `"KEY_ESCAPE"`, `"KEY_SPACE"`, `"KEY_LSHIFT"`, `"KEY_E"`, etc.