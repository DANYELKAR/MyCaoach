---
name: Warm Electric
colors:
  surface: '#141219'
  surface-dim: '#141219'
  surface-bright: '#3b3840'
  surface-container-lowest: '#0f0d14'
  surface-container-low: '#1d1a22'
  surface-container: '#211e26'
  surface-container-high: '#2b2931'
  surface-container-highest: '#36333b'
  on-surface: '#e6e0eb'
  on-surface-variant: '#e1bec2'
  inverse-surface: '#e6e0eb'
  inverse-on-surface: '#322f37'
  outline: '#a9898d'
  outline-variant: '#5a4044'
  surface-tint: '#ffb2be'
  primary: '#ffb2be'
  on-primary: '#660026'
  primary-container: '#ff4d7d'
  on-primary-container: '#5a0020'
  inverse-primary: '#b90f4d'
  secondary: '#d0bcff'
  on-secondary: '#3c0091'
  secondary-container: '#571bc1'
  on-secondary-container: '#c4abff'
  tertiary: '#45dfa4'
  on-tertiary: '#003825'
  tertiary-container: '#00a574'
  on-tertiary-container: '#003120'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffd9de'
  primary-fixed-dim: '#ffb2be'
  on-primary-fixed: '#400015'
  on-primary-fixed-variant: '#900038'
  secondary-fixed: '#e9ddff'
  secondary-fixed-dim: '#d0bcff'
  on-secondary-fixed: '#23005c'
  on-secondary-fixed-variant: '#5516be'
  tertiary-fixed: '#68fcbf'
  tertiary-fixed-dim: '#45dfa4'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005137'
  background: '#141219'
  on-background: '#e6e0eb'
  surface-variant: '#36333b'
typography:
  large-title:
    fontFamily: Inter
    fontSize: 34px
    fontWeight: '700'
    lineHeight: 41px
    letterSpacing: 0.37px
  title-1:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: 0.36px
  title-2:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.26px
  title-3:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 25px
    letterSpacing: -0.45px
  headline:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: -0.43px
  body:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: -0.43px
  callout:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 21px
    letterSpacing: -0.32px
  subheadline:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: -0.24px
  footnote:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: -0.08px
  caption-1:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0px
  caption-2:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 13px
    letterSpacing: 0.07px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system powers a modern, AI-assisted conversational icebreaker client for adult daters and communicators. The design persona balances high emotional intelligence with dynamic energy: confident, playful, empathetic, and unobtrusive. The interface is engineered as an intimate night-time companion—crafted to reduce social anxiety, stimulate wit, and inspire authentic dialogue.

The aesthetic blends **Apple Human Interface Guidelines (HIG) rigor** with **tactile warmth and subtle luminescence**. Dark-mode first by default, it relies on deep obsidian depths, luminous electric-rose to violet focal points, and frosted material surfaces. Tactile responsiveness, fluid spring physics, and subtle back-surface illumination make drafting messages feel effortless, discrete, and empowering.

## Colors

The palette leverages a deep purple-cast black foundation punctuated by high-chroma warm accents.

- **Foundational Canvas**: Canvas Base `#0F0D14`, Primary Surface `#1A1722`, Elevated Sheet Surface `#252233`.
- **Core Chromas**: Primary Rose `#FF4D7D` paired with Electric Violet `#8B5CF6`. The signature accent is a 135° diagonal gradient transitioning from `#FF4D7D` directly into `#8B5CF6`.
- **Labels & Hierarchy**: Label Primary `#F5F3FA` (high contrast, crisp readability), Label Secondary `#A09BB0` (supporting metadata, inputs, and placeholders).
- **Separators & Outlines**: Hairline stroke `#3A3650`.
- **Semantic Feedback**: Success `#34D399`, Warning `#FBBF24`, Destructive `#F87171`.

Apply the signature gradient exclusively to primary actions, interactive selection rings, and high-value AI suggestion highlights. Keep background surfaces neutral and dark to preserve battery life and eliminate visual fatigue.

## Typography

Typography strictly respects native iOS proportions, optical tracking, and Dynamic Type scaling hierarchies. System fonts map to `Inter` for layout parity across multi-platform rendering pipelines.

- **Dynamic Contrast**: Headlines leverage semibold and bold weights for rapid scanning in social scenarios.
- **Reading Comfort**: Body copy preserves an accessible 17pt/22pt ratio to make generated icebreaker variants effortless to evaluate and select.
- **Capsule & Button Microcopy**: Use `Headline` (17pt, 600 weight) centered within 52pt tap affordances to maintain tap precision.

## Layout & Spacing

Layout geometry is locked to an 8pt spatial grid (with a strict 4pt baseline step for micro-elements):

- **Target Device Frame**: iPhone 15 Pro portrait canvas (393 × 852 pt).
- **Margins & Safe Areas**: Screen boundary margins default to 20pt (`1.25rem`), narrowing to 16pt (`1rem`) on high-density list arrangements.
- **Vertical Rhythm**: 
  - Standard element spacing: 8pt (`0.5rem`) and 16pt (`1rem`).
  - Section blocks and grouping boundaries: 24pt (`1.5rem`) to 32pt (`2rem`).
- **Touch Target Enclosures**: Interactive touch surfaces enforce a non-negotiable minimum bounding box of 44 × 44 pt, even when visual icon footprints are smaller (e.g. 24 × 24 pt).
- **Safe Area Insets**: Native top notch/Dynamic Island (59pt) and bottom home indicator bar (34pt) are padded out with background material bleed.

## Elevation & Depth

Visual depth is achieved through **tonal surface stacking** paired with **thin specular borders** rather than stark drop shadows:

1. **Base Floor (`#0F0D14`)**: Root application background.
2. **Layer 1 Surface (`#1A1722`)**: Standard cards, table view grouping cells, and persistent bottom tab bars. Outlined with a 1px solid stroke of `#3A3650` (or `rgba(255, 255, 255, 0.08)`).
3. **Layer 2 Floating / Modals (`#252233`)**: Half-sheets, popovers, and elevated preview bubbles.
4. **Frosted Translucency**: System chrome (navigation headers and bottom tab bars) utilizes `UIBlurEffect` with dark system materials (`rgba(26, 23, 34, 0.75)`) and 20px backdrop-filter blur.
5. **Active Glow**: Primary interactive elements project a diffuse, warm drop shadow: `box-shadow: 0px 8px 24px rgba(255, 77, 125, 0.25)`.

## Shapes

Corner rounding follows native iOS continuous curvature (squircle geometry):

- **Cards & Content Blocks**: 20pt continuous corner radius (`rounded-[20px]`).
- **Sheets & Presentation Dialogs**: 24pt continuous corner radius on top edges.
- **Primary Buttons & Filter Chips**: True capsule pills (`border-radius: 9999px`).
- **Segmented Controls & Text Fields**: 12pt continuous corner radius.
- **Grab Handles**: Capsule geometry, 36 × 5 pt, centered at 6pt from sheet top.

## Components

### Buttons
- **Primary Action Button**: Height 52pt, capsule shape. Background: `linear-gradient(135deg, #FF4D7D 0%, #8B5CF6 100%)`. Text: `#F5F3FA`, Headline 17 semibold. Subtle warm shadow (`rgba(255, 77, 125, 0.25)`).
- **Secondary Action Button**: Height 52pt, capsule shape. Background: Transparent. Border: 1.5pt solid `#3A3650`. Text: `#F5F3FA`, Headline 17 semibold. Pressed state: Background `#252233`.
- **Tertiary / Ghost Button**: Height 44pt, capsule shape. Background: Transparent. Text: `#A09BB0`.

### Chips & Filter Pills
- **Capsule Chips**: Height 36pt, capsule shape. Padding: 12pt horizontal. Unselected: Background `#1A1722`, border 1pt solid `#3A3650`, text `#A09BB0`. Selected: Background `linear-gradient(135deg, #FF4D7D, #8B5CF6)`, border transparent, text `#F5F3FA`.

### Cards & Icebreaker Output Bubbles
- **Icebreaker Message Card**: Background `#1A1722`, border 1pt solid `#3A3650`, 20pt corner radius. Padding 16pt. Contains message text, tone badge chip, word count, and quick-action icons (Copy, Refresh, Customize) with 44pt tap boundaries.

### Navigation & Tab Bars
- **Navigation Bar**: Large Title collapse-on-scroll pattern. Translucent `#1A1722` material blur with `#3A3650` bottom separator hairline.
- **Tab Bar**: 5 items (Home, People, History, Saved, Profile). Height 49pt + 34pt safe area. Unselected icon color: `#A09BB0`. Selected icon color: `#FF4D7D`. SF Symbols rendered at medium weight.

### Form Inputs & Segmented Controls
- **Segmented Control**: Height 32pt, 8pt corner radius. Track background `#0F0D14`. Active thumb `#252233` with subtle 1pt border.
- **Message Input Area**: Background `#1A1722`, border 1pt solid `#3A3650`, 16pt corner radius. Inner padding 12pt. Placeholder color `#A09BB0`.
- **Switches & Toggles**: Native iOS toggle styling. Off: `#252233` track with `#A09BB0` border. On: `#FF4D7D` track fill.

### Modal Sheets
- Present with a dark dimmed scrim (`rgba(0, 0, 0, 0.6)`), 24pt continuous corner radii, elevated surface fill `#252233`, and a `#A09BB0` centered drag grabber (36 × 5 pt).