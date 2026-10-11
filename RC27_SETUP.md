# RC27 — Party closure notice — 2.0.0-rc27 / 22025

When the room poll confirms that the current room no longer exists, a guest leaves the room and sees a modal notice: "The party was closed" / "El party fue cerrado", according to the interface language. OK is focused for the TV remote; OK or Back dismisses the notice. It is displayed at app level, including when the guest is outside the player, and only once for the detached room.

An HTTP/network exception does not trigger this notice. Explicitly leaving the Party does not trigger it. Closure detection uses the existing room poll (normally every two seconds, plus network time). An expired/deleted room produces the same notice; merely turning off the host TV without closing the room is not an explicit room closure.

Includes RC26 recovery, RC25 Friends navigation and RC24 audio changes. No additional SQL. Upload and start a new TV build. Kotlin syntax and archive checked; Actions compilation and physical host/guest validation remain pending.
