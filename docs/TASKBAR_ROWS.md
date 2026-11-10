# Vista taskbar rows (0.26.4)

Enable **Expand taskbar to two rows** in Control Panel > Taskbar and Start Menu,
or hold the taskbar grip and choose **Expand taskbar**.

Quick Launch shortcuts initially appear below; running app and folder buttons
initially appear above. Start, the taskbar grip, device controls and the clock
remain below. Quick Launch and a running button for the same app are separate
instances, and each can have its own row.

Swipe either expanded row to scroll it independently. To move an app, touch it,
pause briefly, then drag it to the other row or along its current row. A stationary
hold opens the usual Vista menu. The insertion marker shows the drop position;
dropping outside the app area cancels the move. Dragging near the left or right
edge scrolls the destination row. Static mode fits each row within its own space.

Keyboard: focus an app, then Ctrl+Shift+Left/Right to reorder within its row or
Ctrl+Shift+Up/Down to move between rows. Accessible actions provide the same moves.
Mouse-wheel movement applies to the hovered row.

Row choices and slot order are included in .nthdesktop backups. Older backups
remain valid. Other desktops keep their existing compatibility rules. Scrolling
positions are temporary viewport state and are not exported.

Implementation: TaskbarItems owns the two row viewports, while TaskbarReorder and
TaskbarTouch coordinate native drag/drop and gesture classification. Keyed view
IDs distinguish `quick:<id>` and `task:<id>` without changing existing plain tags
used for session order and minimize anchors. Grouped Explorer tasks use a stable
family key. Only Quick Launch keys are decoded into the underlying pin list.
