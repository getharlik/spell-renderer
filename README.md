# Spell Renderer

A fast, lightweight OpenGL wrapper for Minecraft, designed for rendering 2D objects with a clean, fluent API.

![preview](.github/assets/preview.png)

## Features

- **Shape builder** - chainable API for positioning, sizing, coloring, and rounding.
- **Batched rendering** - shapes of the same type are merged into a single draw call.
- **Custom shaders** - dedicated shader programs for rectangles, images, text, and glass.
- **MSDF text rendering** - crisp text at any scale via signed distance field fonts.
- **Glass effect** - frosted or liquid glass panels with adjustable blur, refraction, RGB split, saturation and rim light.
- **DPI scaling** - ability to change scale of the elements.
- **Render Context** - different context for different scenarios: GUI, HUD. (WORLD is in development!)`
- **Scissors** - ability to set a region and draw without going out of its bounds.
- **Dragging** - make any shape and its children draggable with the left mouse button.

## Requirements

- Minecraft: `1.21.8`
- Fabric Loader: `>=0.18.5`
- Fabric API: `0.136.1+1.21.8`
- Java: `21`

## Installation

### Gradle
```gradle
repositories {
    maven { url = "https://api.modrinth.com/maven" }
}

dependencies {
    modImplementation "maven.modrinth:spell-renderer:26.4"

    // Optional: bundle Spell Renderer inside your mod JAR
    // so users don't need to install it separately.
    include "maven.modrinth:spell-renderer:26.4"
}
```

### Maven
```maven
<repositories>
    <repository>[spell-renderer-26.4.jar](build/libs/spell-renderer-26.4.jar)
        <id>modrinth</id>
        <url>https://api.modrinth.com/maven</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>maven.modrinth</groupId>
        <artifactId>spell-renderer</artifactId>
        <version>26.4</version>
    </dependency>
</dependencies>
```

### fabric.mod.json
```json
"depends": {
  "spell-renderer": "26.4"
}
```

## Usage

### Initializing:

```java
    @Override
    public void onInitializeClient() { // SpellRenderer.on() SHOULD ONLY BE CALLED FROM THIS METHOD!
        Spellrenderer.on(RenderContext.GUI, () -> {  // GUI context - rendered on top of everything.
            // You can draw inside the listener.
            Builder.rect()
                    .pos(15, 15, 10)
                    .size(100, 75)
                    .radius(8)
                    .color(Color.WHITE)
                    .submit();
        
            // Or (Recommended way)
            EventBus.post(new RenderGUIEvent()); // Use an eventbus and post events for each context.
        });
    }
```

### Adding icons & fonts

**Icons - are the same atlas + json msdf fonts. The only difference additional `"names"` json object.**

**1.** Generate msdf atlas + json.

You can generate them using Chlumsky's [MSDF Atlas Generator](https://github.com/Chlumsky/msdf-atlas-gen).

Recomended flags:
```shell
-font ExampleFont.ttf -charset charset.txt -type msdf -size 48 -pxrange 4 -format png -imageout ExampleFont.png -json ExampleFont.json
```

**2.** For icons add `"names": {"name": unicode}` to your font's json.

```jsonc
{
  "atlas":{"type":"msdf","distanceRange":4,"distanceRangeMiddle":0,"size":48,"width":84,"height":84,"yOrigin":"bottom"},
  "metrics":{"emSize":1,"lineHeight":1.0900000000000001,"ascender":0.84999999999999998,"descender":-0.14999999999999999,"underlineY":0.01,"underlineThickness":0},
  "glyphs":[{"unicode":65,"advance":1,"planeBounds":{"left":0.0020833333333333199,"bottom":-0.052083333333333336,"right":0.8979166666666667,"top":0.76041666666666663},"atlasBounds":{"left":0.5,"bottom":44.5,"right":43.5,"top":83.5}}],
  "kerning":[],
  "names": {
    "backward": 65   // "name": unicode
  }
}
```

**3.** Init inside the code.

```java
ClientLifecycleEvents.CLIENT_STARTED.register(client -> { // ONLY THIS EVENT. NOT onInitializeClient()!
    FontManager.load("key", ResourceLocation.fromNamespaceAndPath("Your mod id", "fonts/inter"));
    // WRITE THE FILE NAME WITHOUT THE EXTENSION e.g. fonts/inter.png -> fonts/inter
});
```

### Available shapes

| Shape    | Description                                 |
|----------|---------------------------------------------|
| `rect`   | Rounded rectangle with per-corner colors    |
| `circle` | Filled circle                               |
| `image`  | Textured quad with optional rounded corners |
| `text`   | SDF-rendered string                         |
| `glass`  | Frosted-glass blurred panel                 |
| `icon`   | Mapped name parcing for icon fonts          |

Every shape also exposes `width()`, `height()` and `bounds()` (a `Region` in screen space; circles are offset by their radius since they are positioned by center).

### Glass

```java
Builder.glass()
        .pos(20, 20)
        .size(200, 120)
        .radius(16)
        .blur(16)          // blur radius in pixels, 0 = none (default 12)
        .refraction(1.2f)  // lens bend at the edges, 0 = flat frosted glass (default 1)
        .chromatic(4)      // RGB split at the edges in pixels, 0 = off (default 0)
        .saturation(1.4f)  // 0 = grayscale, 1 = unchanged, >1 = vivid (default 1)
        .highlight(0.3f)   // rim light brightness, 0 = off (default 0)
        .color(Color.WHITE) // tint
        .submit();
```

Blur cost is the same for any radius. `chromatic` takes three times as many texture reads, so leave it at 0 when you don't need it.

### Dragging

Call `.draggable("id")` on a shape to let the user drag it with the left mouse button. Anything parented to it moves with it and can be used to grab it.

```java
var panel = Builder.rect().pos(50, 50).size(200, 120).color(Color.DARK_GRAY).draggable("panel");
panel.submit();

Builder.text("Hello").pos(8, 8).parent(panel).submit(); // moves with, and grabs, the panel
```

- The id identifies the drag state across frames, so keep it stable and unique.
- The draggable shape is clamped so it never leaves the screen.
- A drag starts once the cursor moves more than 3px after the press. The press itself is consumed, so it never reaches a Minecraft screen underneath.
- Only the visible part of a shape inside `Scissors` can be grabbed.
- `GUI` draggables work whenever they are submitted; `HUD` draggables only while the chat is open. If both overlap, `GUI` wins.
- Offsets are kept in memory only. Persist them yourself:

```java
float x = DragManager.INSTANCE.offsetX("panel"), y = DragManager.INSTANCE.offsetY("panel"); // save
DragManager.INSTANCE.setOffset("panel", x, y);                                            // load
DragManager.INSTANCE.reset("panel");                                                      // back to original position
```

`DragManager.INSTANCE.isDragging()` and `getActiveId()` tell you what is being dragged right now.

## License

[MIT](LICENSE)

## TODO

1. Implement kernings
2. Implement 3d world rendering
