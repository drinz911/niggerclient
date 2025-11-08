package dev.lvstrng.argon.module;

import dev.lvstrng.argon.utils.EncryptedString;

public enum Category {
	MISC(EncryptedString.of("Misc")),
	COMBAT(EncryptedString.of("Combat")),
	RENDER(EncryptedString.of("Render")),
	CLIENT(EncryptedString.of("Client"));
	public final CharSequence name;

	Category(CharSequence name) {
		this.name = name;
	}
}
