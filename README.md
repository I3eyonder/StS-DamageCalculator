# Slay the Spire Damage Calculator

A Slay the Spire mod that calculates and displays the damage output of your cards for each enemy and predicts the incoming damage for the next turn.

## Features

- **Real-time Damage Calculation**: Hover over cards to see damage calculations for each enemy
- **Next Turn Prediction**: Displays incoming damage from monsters for the next turn
- **Card Effect Consideration**: Accounts for various card effects including:
  - Attack cards with damage modifiers
  - Block cards
  - Healing effects
  - Exhaust effects
- **Power and Relic Support**: Considers active powers and relics when calculating outcomes
- **Orb Effects**: Calculates passive and evoked orb effects
- **Monster Attack Intent**: Predicts damage based on monster attack intentions

## Installation

This is a Slay the Spire mod that requires:
- **Slay the Spire** (game version: 12-18-2022)
- **ModTheSpire** (version 3.30.0+)
- Dependencies: `basemod`, `stslib`

### Steps:

1. Place the compiled `.jar` file in your `mods` folder
2. Launch Slay the Spire through ModTheSpire
3. Enable the mod in the mod list

## Building

This project uses Maven and Kotlin.

### Prerequisites:
- Java 8 or higher
- Maven 3.6+
- Kotlin plugin for Maven

### Compile:
```bash
mvn clean package
```

The compiled mod will be available in the `target/dmgcalculator.jar` file.

## Project Structure

```
src/main/kotlin/dmgcalculator/
├── DmgCalculatorMod.kt           # Main mod entry point
├── entities/                      # Data models
│   ├── Action.kt                 # Damage/healing actions
│   ├── CreatureInfo.kt           # Creature statistics
│   ├── ExhaustInfo.kt            # Card exhaust information
│   ├── Outcome.kt                # Damage/block outcomes
│   ├── Range.kt                  # Min/max ranges
│   └── SimpleCardInfo.kt         # Simple card representation
├── interfaces/                    # Event subscribers
│   └── PlayerEndTurnSubscriber.kt
├── patches/                       # Game patches
│   └── PlayerUpdateHook.kt
├── publisher/                     # Event publishers
│   └── PlayerEndTurnPublisher.kt
├── renderer/                      # UI rendering
│   ├── MonsterRenderer.kt        # Monster damage display
│   ├── PlayerRenderer.kt         # Player damage display
│   └── TextSegment.kt            # Text rendering
└── util/                          # Utility functions
    ├── CardUtils.kt
    ├── CreatureUtils.kt
    ├── ReflectionUtils.kt
    ├── RendererUtils.kt
    ├── TextureLoader.kt
    └── Utils.kt
```

## How It Works

### Player Turn (Damage Calculation)
1. When hovering over a card, the mod:
   - Extracts the card's base damage and effects
   - Applies active power modifiers (Rage, Block Return, etc.)
   - Accounts for relic triggers
   - Simulates card duplications (DoubleTap, Duplication, Echo, Necronomicon)
   - Processes hand-based effects (FeelNoPain, exhaust penalties)
   - Calculates orb passives and evokes
   - Compiles worst and best case scenarios

2. Results are displayed above the player character and monsters

### Monster Turn (Damage Prediction)
- Analyzes monster attack intents
- Calculates incoming damage based on:
  - Monster damage values
  - Passive orb effects (Frost orbs)
  - Active powers (Strength, vulnerability modifiers)
  - Player block and defense capabilities

## Known Issues

### Calculation Accuracy
- Some complex power interactions with specific card combinations may not be fully accounted for
- Exhaust calculations may be inaccurate when multiple cards trigger exhaust in the same turn
- Cards with duplication powers such as Duplication may not be calculated correctly

### Card Support
- Some newer cards added in updates and custom cards from other mods may not be properly recognized

### Monster Intent
- Monster attacks from certain rare enemy combinations may not calculate correctly
- Monster attack intent changes mid-turn may not update damage predictions

## Configuration

The mod provides a configuration panel accessible through ModTheSpire's mod settings. Available options:

- **Show Block Info**: Toggle the display of block information on the screen (default: enabled)
  - When enabled, displays block calculations
  - When disabled, hides the block information

## License

This project is licensed under the Apache License 2.0. See the LICENSE file for details.

## Contributing

This is a personal mod project. Contributions and suggestions are welcome through issue reports.

## Author

**Beyonder**

## Dependencies

- **basemod**: Core modding framework
- **stslib**: Slay the Spire library utilities
- **Kotlin**: Programming language
- **libGDX**: Graphics library (via Slay the Spire)

## Support

For issues, bug reports, or feature requests, please check the Known Issues section first.

